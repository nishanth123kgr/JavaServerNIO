package com.server.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Acceptor implements Runnable {

    private final Server server;

    Acceptor(Server server) {
        this.server = server;
    }

    @Override
    public void run() {

        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(server.getPORT()));
            ssc.configureBlocking(false);


            while (true) {
                SocketChannel socketChannel = ssc.accept();

                if (socketChannel != null) {
                    SocketWrapper socketWrapper = new SocketWrapper(socketChannel);

                    this.server.getPoller().register(socketWrapper, SelectionKey.OP_READ);
                }

            }
        } catch (IOException e) {

        }
    }
}
