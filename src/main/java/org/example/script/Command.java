package org.example.script;

import java.util.Map;

public class Command {

    public enum CommandType {
        CREATE_REGISTER,
        APPLY_GATE,
        MEASURE
    }

    private final CommandType type;
    private final Map<String, Object> arguments;

    public Command(CommandType type, Map<String, Object> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    public CommandType getType() {
        return type;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Object getArgument(String key) {
        return arguments.get(key);
    }

    public String getArgumentAsString(String key) {
        Object value = arguments.get(key);
        return value instanceof String ? (String) value : null;
    }

    public Integer getArgumentAsInt(String key) {
        Object value = arguments.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", arguments=" + arguments +
                '}';
    }
}