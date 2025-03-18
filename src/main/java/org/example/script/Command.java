package org.example.script;

import java.util.HashMap;
import java.util.List;
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
        return (String) arguments.get(key);
    }

    public Integer getArgumentAsInt(String key) {
        return (Integer) arguments.get(key);
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", arguments=" + arguments +
                '}';
    }
}