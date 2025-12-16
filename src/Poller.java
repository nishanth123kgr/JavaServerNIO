import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Poller implements Runnable {

    private final SynchronizedQueue<PollerEvent> events = new SynchronizedQueue<>();
    private final SynchronizedStack<PollerEvent> eventCache = new SynchronizedStack<>();

    private final Selector selector;
    private final AtomicInteger wakeupCounter = new AtomicInteger(0);

    int keyCount = 0;

    private final Server server;


    Poller(Server server) {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.server = server;

    }

    @Override
    public void run() {

        while (true) {
            this.events();

            try {
                if (this.wakeupCounter.get() > 0) {
                    this.keyCount = this.selector.selectNow();
                } else {
                    long selectorTimeout = 1000;
                    this.keyCount = this.selector.select(selectorTimeout);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.wakeupCounter.set(0);

            Iterator<SelectionKey> iterator = keyCount > 0 ? selector.selectedKeys().iterator() : null;

            while (iterator != null && iterator.hasNext()) {
                SelectionKey key = iterator.next();

                iterator.remove();

                if (!key.isValid()) continue;

                SocketWrapper wrapper = (SocketWrapper) key.attachment();

                try {
                    if (key.isReadable()) {
                        wrapper.setKey(key);
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                        SocketProcessor socketProcessor = new SocketProcessor(wrapper, this);
                        server.getExecutor().submit(socketProcessor);
                    }

                    if (key.isWritable()) {
                        handleWritable(wrapper);
                    }
                } catch (CancelledKeyException ignored) {
                } catch (Throwable t) {
                    t.printStackTrace();
                }


            }


        }

    }

    private void closeWrapper(SocketWrapper wrapper) {
        try {
            SelectionKey k = wrapper.getKey();
            if (k != null) k.cancel();
            SocketChannel ch = wrapper.getChannel();
            if (ch != null) ch.close();
        } catch (IOException ignored) {
        }
    }

    private void events() {

        PollerEvent event;
        while ((event = events.poll()) != null) {

            SocketWrapper socketWrapper = event.socketWrapper;
            SocketChannel channel = socketWrapper.getChannel();

            if (event.interestOps == SelectionKey.OP_READ) {
                try {
                    channel.configureBlocking(false);
                    channel.register(this.selector, SelectionKey.OP_READ, socketWrapper);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (event.interestOps == SelectionKey.OP_WRITE) {
                SelectionKey key = socketWrapper.getKey();
                if (key != null && key.isValid()) {
                    try {
                        key.interestOps(SelectionKey.OP_WRITE);
                    } catch (CancelledKeyException ignore) {
                        closeWrapper(socketWrapper);
                    }
                } else {
                    try {
                        channel.configureBlocking(false);
                        SelectionKey newKey = channel.register(this.selector, SelectionKey.OP_WRITE, socketWrapper);
                        socketWrapper.setKey(newKey);
                    } catch (IOException ex) {
                        closeWrapper(socketWrapper);
                    }
                }
            } else if (event.interestOps == -1) {
                closeWrapper(socketWrapper);

            }

            event.reset();
            eventCache.push(event);


        }


    }

    private PollerEvent createPollerEvent(SocketWrapper socketWrapper, int ops) {
        PollerEvent event = eventCache.pop();

        if (event == null) {
            event = new PollerEvent(socketWrapper, ops);
        } else {
            event.reset(socketWrapper, ops);
        }

        return event;
    }


    public void register(SocketWrapper socketWrapper, int ops) {
        PollerEvent event = createPollerEvent(socketWrapper, ops);
        events.offer(event);
        if (wakeupCounter.incrementAndGet() > 0) {
            selector.wakeup();
        }
    }

    private void handleWritable(SocketWrapper wrapper) throws IOException {
        SocketChannel ch = wrapper.getChannel();

        Queue<ByteBuffer> q = wrapper.getResponseOutputQueue();
        while (true) {
            ByteBuffer buf = q.peek();
            if (buf == null) break;

            ch.write(buf);

            if (buf.hasRemaining()) {
                break;
            } else {
                q.poll();
            }
        }

        if (q.isEmpty()) {
            closeWrapper(wrapper);
        }
    }

}
