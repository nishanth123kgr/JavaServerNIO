package com.server.transport;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class ProcessorFactory {

    private static final Map<String, String> protocols = Map.of("http", "com.server.protocol.http.HttpProcessor");

    private static final Map<String, String> properties = new HashMap<>();

    static {
        Path propertiesPath = Paths.get(System.getProperty("user.dir"), "server.properties");

        try (FileChannel fileChannel = FileChannel.open(propertiesPath, StandardOpenOption.READ)) {

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            fileChannel.read(buffer);

            buffer.flip();

            byte[] propertiesBytes = new byte[buffer.remaining()];

            buffer.get(propertiesBytes);

            String propertiesString = new String(propertiesBytes, StandardCharsets.UTF_8);

            String[] propLines = propertiesString.split("\n");

            if (propLines.length > 0) {
                for (int i = 0; i < propLines.length; i++) {

                    String propLine = propLines[i];

                    String[] props = propLine.split("=");

                    if (props.length != 2) {
                        throw new RuntimeException("Invalid Property Line: " + propLine + " (Line Number: " + (i + 1) + " )");
                    }

                    properties.put(props[0].strip(), props[1].strip());
                }
            } else {
                throw new RuntimeException("Invalid Properties File");
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static SocketProcessor create(SocketWrapper socketWrapper, Poller poller) {

        String protocol = properties.get("protocol");
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
