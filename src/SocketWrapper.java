import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketWrapper {

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private SelectionKey key;

    private final ConcurrentLinkedQueue<ByteBuffer> responseOutputQueue = new ConcurrentLinkedQueue<>();


    SocketWrapper(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ConcurrentLinkedQueue<ByteBuffer> getResponseOutputQueue() {
        return responseOutputQueue;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }
}
