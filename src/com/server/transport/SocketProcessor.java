package com.server.transport;

import com.server.protocol.Parser;
import com.server.protocol.ResponseBuilder;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class SocketProcessor implements Runnable {

    protected final SocketWrapper socketWrapper;

    private final Poller poller;

    public SocketProcessor(SocketWrapper wrapper, Poller poller) {
        this.socketWrapper = wrapper;
        this.poller = poller;
    }

    boolean readSocket() {
        try {
            SocketChannel channel = socketWrapper.getChannel();
            ByteBuffer readBuffer = socketWrapper.getReadBuffer();

            readBuffer.clear();

            int totalBytesRead = 0;
            while (true) {
                int r = channel.read(readBuffer);
                if (r > 0) {
                    totalBytesRead += r;
                } else if (r == 0) {
                    break;
                } else {
                    closeConnection();
                    return false;
                }
            }

            return totalBytesRead > 0;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    void closeConnection() {
        poller.register(socketWrapper, -1);
    }

    protected abstract void process();


    @Override
    public void run() {
        if (readSocket()) {
            Parser parser = new Parser();
            parser.parse();
            process();
            ResponseBuilder builder = new ResponseBuilder();
            builder.build(socketWrapper.getResponseOutputQueue());

            poller.register(socketWrapper, SelectionKey.OP_WRITE);
        } else {
            closeConnection();
        }

    }
}
