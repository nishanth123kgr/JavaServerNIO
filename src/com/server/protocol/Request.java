package com.server.protocol;

import java.util.HashMap;
import java.util.Map;

public class Request {

    private final Protocol protocol;

    private final Map<String, Object> attributes = new HashMap<>();

    public Request(Protocol protocol) {
        this.protocol = protocol;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object attribute) {
        this.attributes.put(key, attribute);
    }
}
