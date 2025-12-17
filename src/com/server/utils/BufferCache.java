package com.server.utils;

import java.nio.ByteBuffer;

public class BufferCache {

    static SynchronizedStack<ByteBuffer> bufferCache = new SynchronizedStack<>();

    public static ByteBuffer getBufferFromCache() {
        return bufferCache.pop();
    }

    public static void putBufferInCache(ByteBuffer buffer) {
        bufferCache.push(buffer);
    }

}
