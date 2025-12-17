package com.server.transport;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private final int PORT;
    private int pollerCount = 4;

    private int nextPoller = -1;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

    private final Poller[] pollers;

    public Server(int port, int pollerCount) {
        this.PORT = port;
        this.pollerCount = pollerCount;
        pollers = new Poller[pollerCount];
    }

    Server(int port) {
        this.PORT = port;
        pollers = new Poller[pollerCount];
    }


    public void start() {

        for (int i = 0; i < pollerCount; i++) {
            Poller poller = new Poller(this);
            Thread thread = new Thread(poller, "com.server.transport.Poller " + (i + 1));
            thread.start();
            pollers[i] = poller;
        }

        Acceptor acceptor = new Acceptor(this);

        Thread acceptorThread = new Thread(acceptor, "com.server.transport.Acceptor");

        acceptorThread.start();


    }

    public int getPORT() {
        return PORT;
    }

    public Poller getPoller() {
        nextPoller = (nextPoller + 1) % pollerCount;
        return pollers[nextPoller];
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
}
