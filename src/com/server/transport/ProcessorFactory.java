package com.server.transport;

import com.server.protocol.Protocol;
import com.server.protocol.RequestProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ProcessorFactory {

    private static final Map<Protocol, String> protocols = Map.of(Protocol.HTTP, "com.server.protocol.http.HttpRequestProcessor");


    public static RequestProcessor getProcessor() {

        Protocol protocol = ServerConfig.get(ServerConfig.SERVER_PROTOCOL);
        if (protocol == null) {
            throw new RuntimeException("Protocol Not Found");
        }

        String className = protocols.get(protocol);
        if (className == null) {
            throw new RuntimeException("Invalid Protocol: " + protocol + " Available Protocols: " + protocols.keySet());
        }

        try {
            Class<?> clazz = Class.forName(className);

            Constructor<?> constructor = clazz.getConstructor();

            return (RequestProcessor) constructor.newInstance();

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
