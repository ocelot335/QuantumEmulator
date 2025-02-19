package org.example.script;

//CR N name - создать регистр с именем name из N кубитов
//СQ name := CR 1 name
//CX name1[id] name2[id]
//X name[id]
//M name[id]
//name == name[0] if name.len = 1

import java.util.*;

public class Parser {
    private static final Set<String> gateNames = new HashSet<>(Arrays.asList("X","Y", "H", "SWAP"));

    public static Command parse(String command) {
        String[] tokens = command.split("\\s+");
        if (tokens.length == 0) {
            return null;
        }

        String commandName = tokens[0];
        Map<String, Object> args = new HashMap<>();

        if (commandName.equals("CR")) {
            if (tokens.length != 2) {
                //TODO: исключение
                return null;
            }
            String[] crParts = tokens[1].split("[\\[\\]]");
            if (crParts.length != 2) {
                //TODO: исключение
                return null;
            }
            try {
                int size = Integer.parseInt(crParts[1]);
                return createQuantumRegister(crParts[0], size);
            } catch (NumberFormatException e) {
                //TODO: исключение
                return null;
            }
        } else if (commandName.equals("CQ")) {
            if (tokens.length != 2) {
                //TODO: исключение
                return null;
            }
            return createQuantumRegister(tokens[1], 1); // CQ name := CR name[1] по сути создает регистр размера 1
        } else if (gateNames.contains(commandName)) { // Проверяем, является ли команда гейтом
            args.put("gate", commandName);
            List<Map<String, Object>> operands = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                Map<String, Object> operand = parseOperand(tokens[i]);
                if (operand == null) {
                    //TODO: исключение
                    return null;
                }
                operands.add(operand);
            }
            args.put("operands", operands);
            return new Command(Command.CommandType.APPLY_GATE, args);
        } else if (commandName.equals("M")) { // Отдельно обрабатываем Measure, если нужно особое поведение
            //TODO: измерение регистра
            //TODO: !!!

            if (tokens.length != 2) {
                //TODO: исключение
                return null;
            }
            Map<String, Object> operand = parseOperand(tokens[1]);
            if (operand == null) {
                //TODO: исключение
                return null;
            }
            args.put("operand", operand);
            return new Command(Command.CommandType.MEASURE, args);
        }
        else {
            System.out.println("Неизвестная команда: " + command);
            return null;
        }
    }

    private static String[] getQubitInRegisterFromString(String arg) {
        String[] parts = arg.split("[\\[\\]]");
        if (parts.length == 1) {
            return new String[]{parts[0], "0"};
        }

        return new String[]{parts[0], parts[1]};
    }

    private static Map<String, Object> parseOperand(String arg) {
        Map<String, Object> operand = new HashMap<>();
        String[] parts = arg.split("[\\[\\]]");
        String registerName = parts[0];
        operand.put("register", registerName);
        if (parts.length > 1) {
            try {
                int index = Integer.parseInt(parts[1]);
                operand.put("index", index);
            } catch (NumberFormatException e) {
                //TODO: исключение
                return null;
            }
        } else {
            operand.put("index", 0); // По умолчанию индекс 0, если не указан
        }
        return operand;
    }

    private static Command createQuantumRegister(String name, int size) {
        Map<String, Object> args = new HashMap<>();
        args.put("name", name);
        args.put("size", size);
        return new Command(Command.CommandType.CREATE_REGISTER, args);
    }
}
