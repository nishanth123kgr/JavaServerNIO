package com.server.transport;

record ConfigKey(String name, ConfigType type, Object defaultValue, boolean required) {
}
