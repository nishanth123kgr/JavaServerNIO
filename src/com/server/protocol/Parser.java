package com.server.protocol;

import com.server.transport.SocketWrapper;

public interface Parser {
    Request parse(SocketWrapper socketWrapper);
}
