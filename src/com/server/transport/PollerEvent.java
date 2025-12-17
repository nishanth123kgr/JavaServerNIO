package com.server.transport;

public class PollerEvent {

    SocketWrapper socketWrapper;
    int interestOps;

    public PollerEvent(SocketWrapper socketWrapper, int interestOps) {
        this.reset(socketWrapper, interestOps);
    }

    public void reset(SocketWrapper socketWrapper, int interestOps) {
        this.socketWrapper = socketWrapper;
        this.interestOps = interestOps;
    }

    public void reset() {
        this.reset(null, 0);
    }

    @Override
    public String toString() {
        return "com.server.transport.PollerEvent " + socketWrapper + " interestOps " + interestOps;
    }
}
