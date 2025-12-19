package com.server.protocol;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Response {
    private ConcurrentLinkedDeque<ByteBuffer> responseQueue;

    public void setResponseQueue(ConcurrentLinkedDeque<ByteBuffer> responseQueue) {
        this.responseQueue = responseQueue;
    }

    protected void addResponseBytes(ByteBuffer buffer) {
        this.responseQueue.push(buffer);
    }
}
