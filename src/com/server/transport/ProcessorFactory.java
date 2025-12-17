package com.server.transport;

import com.server.protocol.Protocols;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ProcessorFactory {

    private static final Map<Protocols, String> protocols = Map.of(Protocols.HTTP, "com.server.protocol.http.HttpProcessor");


    public static SocketProcessor create(SocketWrapper socketWrapper, Poller poller) {

        Protocols protocol = ServerConfig.get(ServerConfig.SERVER_PROTOCOL);
        if (protocol == null) {
            throw new RuntimeException("Protocol Not Found");
        }

        String className = protocols.get(protocol);
        if (className == null) {
            throw new RuntimeException("Invalid Protocol: " + protocol + " Available Protocols: " + protocols.keySet());
        }

        try {
            Class<?> clazz = Class.forName(className);

            Constructor<?> constructor = clazz.getConstructor(SocketWrapper.class, Poller.class);

            return (SocketProcessor) constructor.newInstance(socketWrapper, poller);

        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
