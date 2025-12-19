package com.server.protocol;

public interface RequestProcessor {
    void process(Request request, Response response);
}
