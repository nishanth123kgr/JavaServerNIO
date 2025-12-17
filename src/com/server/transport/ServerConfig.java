package com.server.transport;

import com.server.protocol.Protocols;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class ServerConfig {
    private static final Map<String, String> rawConfig = new HashMap<>();
    private static final Map<String, Object> typedConfig = new HashMap<>();

    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_PROTOCOL = "server.protocol";


    private static final Map<String, ConfigKey> SCHEMA = Map.of(SERVER_PORT, new ConfigKey(SERVER_PORT, ConfigType.INT, 8080, true), SERVER_PROTOCOL, new ConfigKey(SERVER_PROTOCOL, ConfigType.PROTOCOL, Protocols.HTTP, true));

    private static Object parseValue(String value, ConfigType type) {
        return switch (type) {
            case STRING -> value;
            case INT -> Integer.parseInt(value);
            case LONG -> Long.parseLong(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            case PROTOCOL -> Protocols.valueOf(value.toUpperCase());
        };
    }

    private static void buildTypedConfig() {
        for (ConfigKey key : SCHEMA.values()) {

            String raw = rawConfig.get(key.name());

            if (raw == null) {
                if (key.required() && key.defaultValue() == null) {
                    throw new RuntimeException("Missing required config: " + key.name());
                }
                typedConfig.put(key.name(), key.defaultValue());
                continue;
            }

            try {
                Object parsed = parseValue(raw, key.type());
                typedConfig.put(key.name(), parsed);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value for " + key.name() + " expected " + key.type() + " but got '" + raw + "'");
            }
        }
    }


    private static void loadFile(Path path) throws Exception {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            buffer.flip();

            String content = StandardCharsets.UTF_8.decode(buffer).toString();

            for (String line : content.split("\\R")) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx < 0) {
                    throw new RuntimeException("Invalid config line: " + line);
                }

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                rawConfig.put(key, value);
            }
        }
    }


    static {
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "server.properties");
            loadFile(path);
            buildTypedConfig();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) typedConfig.get(key);
    }
}
