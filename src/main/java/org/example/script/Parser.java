package org.example.script;

//CR name1[N1], name2[N2], ... - создать реальный регистр, объединяющий номинальные
//СQ name := CR name[1]
//CX name1[id] name2[id]
//X name[id]
//M name[id]
//name == name[0] if name.len = 1

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static final Set<String> gateNames = new HashSet<>(Arrays.asList("X", "NOT", "Y", "H", "P", "S", "T", "SWAP", "INC", "DEC"));
    private static final Pattern REGISTER_DEF_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_]+)" + Pattern.quote("[") + "(\\d+)" + Pattern.quote("]")
    );
    private static int realRegisterCounter = 0;

    public enum CommandTypeInParser {
        CREATE_REGISTER("CR"),
        APPLY_GATE(""),
        MEASURE("M"),
        DEFINE_ORACLE_CSV("DEFINE_ORACLE_CSV"),
        APPLY_ORACLE("APPLY_ORACLE"),
        UNKNOWN("");

        private final String keyword;

        CommandTypeInParser(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }

        public static CommandTypeInParser fromString(String text) {
            String upperText = text.toUpperCase();
            for (CommandTypeInParser type : CommandTypeInParser.values()) {
                if (type != UNKNOWN && type != APPLY_GATE && !type.keyword.isEmpty() && upperText.startsWith(type.keyword)) {
                    return type;
                }
            }
            if (upperText.matches("^[A-Z].*")) {
                return APPLY_GATE;
            }
            return UNKNOWN;
        }
    }

    public static Command parse(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }

        CommandTypeInParser type = CommandTypeInParser.fromString(line);
        String content = line;
        if (type != CommandTypeInParser.APPLY_GATE && type != CommandTypeInParser.UNKNOWN && !type.getKeyword().isEmpty()) {
            content = line.substring(type.getKeyword().length()).trim();
        }

        try {
            return switch (type) {
                case CREATE_REGISTER -> parseCreateRegister(content);
                case APPLY_GATE -> parseApplyGate(line);
                case MEASURE -> parseMeasure(content);
                case DEFINE_ORACLE_CSV -> parseDefineOracleCsv(content);
                case APPLY_ORACLE -> parseApplyOracle(content);
                case UNKNOWN -> throw new IllegalArgumentException("Неизвестная команда: " + line);
            };
        } catch (Exception e) {
            System.err.println("Ошибка парсинга команды '" + line + "': " + e.getMessage());
            throw new IllegalArgumentException("Ошибка парсинга команды: " + e.getMessage(), e);
        }
    }

    private static Command parseCreateRegister(String content) {
        List<Map<String, Object>> nominalRegisters = new ArrayList<>();
        int totalSize = 0;
        Set<String> names = new HashSet<>();

        String[] definitions = content.split(",");
        for (String definition : definitions) {
            Matcher matcher = REGISTER_DEF_PATTERN.matcher(definition.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Неверный формат определения регистра: '" + definition.trim() + "'. Используйте формат: name[size]");
            }
            String name = matcher.group(1);
            int size;
            try {
                size = Integer.parseInt(matcher.group(2));
                if (size <= 0) {
                    throw new IllegalArgumentException("Размер регистра '" + name + "' должен быть положительным числом");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Неверный формат размера для регистра '" + name + "'. Используйте целое число");
            }

            totalSize += size;
        }
        int offsetBase = totalSize;
        for (String definition : definitions) {
            Matcher matcher = REGISTER_DEF_PATTERN.matcher(definition.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Неверный формат определения регистра: '" + definition.trim() + "'. Используйте формат: name[size]");
            }
            String name = matcher.group(1);
            int size;
            try {
                size = Integer.parseInt(matcher.group(2));
                if (size <= 0) {
                    throw new IllegalArgumentException("Размер регистра '" + name + "' должен быть положительным числом");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Неверный формат размера для регистра '" + name + "'. Используйте целое число");
            }

            if (!names.add(name)) {
                throw new IllegalArgumentException("Имя номинального регистра должно быть уникальным в рамках одной команды CR: " + name);
            }

            offsetBase -= size;
            Map<String, Object> nominalSpec = new HashMap<>();
            nominalSpec.put("name", name);
            nominalSpec.put("size", size);
            nominalSpec.put("offset", offsetBase);
            nominalRegisters.add(nominalSpec);
        }

        if (nominalRegisters.isEmpty()) {
            throw new IllegalArgumentException("Команда CR должна определять хотя бы один регистр");
        }

        String realRegisterName = "real_reg_" + (realRegisterCounter++);
        Map<String, Object> commandArgs = new HashMap<>();
        commandArgs.put("realRegisterName", realRegisterName);
        commandArgs.put("realRegisterSize", totalSize);
        commandArgs.put("nominalRegisters", nominalRegisters);

        return new Command(Command.CommandType.CREATE_REGISTER, commandArgs);
    }

    private static Command parseApplyGate(String line) {
        String[] tokens = line.split("\\s+", 3);
        String commandName = tokens[0];

        Map<String, Object> commandArgs = new HashMap<>();
        commandArgs.put("gate", commandName);
        List<Map<String, Object>> operands = new ArrayList<>();
        String argsString = "";

        if (commandName.equalsIgnoreCase("P")) {
            if (tokens.length < 3) {
                throw new IllegalArgumentException("Для гейта P необходимо указать фазу и целевой кубит. Формат: P <phase> <register[index]>");
            }
            try {
                double phase = Double.parseDouble(tokens[1]);
                commandArgs.put("phase", phase);
                argsString = tokens[2];
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Неверный формат фазы для гейта P: '" + tokens[1] + "'. Ожидается число.");
            }
        } else {
            if (tokens.length > 1) {
                argsString = tokens[1];
                if (tokens.length > 2) {
                    argsString += " " + tokens[2];
                }
            }
        }

        if (!argsString.isEmpty()) {
            String[] operandTokens = argsString.split("\\s+");
            for (String token : operandTokens) {
                Map<String, Object> operand = parseOperand(token);
                if (operand == null) {
                    throw new IllegalArgumentException("Неверный формат операнда: " + token);
                }
                operands.add(operand);
            }
        }

        if (operands.isEmpty()) {
            throw new IllegalArgumentException("Не указаны операнды для гейта " + commandName);
        }
        
        if (commandName.equalsIgnoreCase("P") && operands.size() != 1) {
            throw new IllegalArgumentException("Гейт P должен применяться ровно к одному кубиту.");
        }
        
        commandArgs.put("operands", operands);
        return new Command(Command.CommandType.APPLY_GATE, commandArgs);
    }

    private static Command parseMeasure(String content) {
        Map<String, Object> operand = parseOperand(content);
        if (operand == null) {
            throw new IllegalArgumentException("Неверный формат операнда: " + content);
        }
        return new Command(Command.CommandType.MEASURE, operand);
    }

    private static Map<String, Object> parseOperand(String arg) {
        Map<String, Object> operand = new HashMap<>();
        Matcher matcher = REGISTER_DEF_PATTERN.matcher(arg.trim());

        if (matcher.matches()) {
            String registerName = matcher.group(1);
            int index;
            try {
                index = Integer.parseInt(matcher.group(2));
                if (index < 0) {
                    throw new IllegalArgumentException("Индекс кубита не может быть отрицательным");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Неверный формат индекса кубита. Используйте целое число");
            }
            operand.put("register", registerName);
            operand.put("index", index);
            return operand;
        } else {
            return null;
        }
    }

    private static Command parseDefineOracleCsv(String content) {
        Pattern pattern = Pattern.compile("(\\w+)\\s+\"(.*?)\""); // Имя - идентификатор, путь - в кавычках
        Matcher matcher = pattern.matcher(content);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Неверный формат DEFINE_ORACLE_CSV. Ожидается: DEFINE_ORACLE_CSV OracleName \"path/to/file.csv\"");
        }

        String oracleName = matcher.group(1);
        String csvPath = matcher.group(2);

        if (csvPath.isEmpty()) {
            throw new IllegalArgumentException("Путь к CSV файлу не может быть пустым для DEFINE_ORACLE_CSV.");
        }

        Map<String, Object> args = new HashMap<>();
        args.put("oracleName", oracleName);
        args.put("csvPath", csvPath);

        return new Command(org.example.script.Command.CommandType.DEFINE_ORACLE_CSV, args);
    }

    private static Command parseApplyOracle(String content) {
        // Ожидаемый формат: APPLY_ORACLE OracleName InputReg -> AncillaReg[Index]
        Pattern pattern = Pattern.compile("(\\w+)\\s+(\\w+)\\s*->\\s*(\\w+)\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(content);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Неверный формат APPLY_ORACLE. Ожидается: APPLY_ORACLE OracleName InputReg -> AncillaReg[Index]");
        }

        String oracleName = matcher.group(1);
        String inputRegisterName = matcher.group(2);
        String ancillaRegName = matcher.group(3);
        int ancillaIndex;

        try {
            ancillaIndex = Integer.parseInt(matcher.group(4));
            if (ancillaIndex < 0) {
                throw new IllegalArgumentException("Индекс кубита ancilla не может быть отрицательным.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат индекса кубита ancilla. Используйте целое число.", e);
        }

        if (inputRegisterName.isEmpty()) {
            throw new IllegalArgumentException("Необходимо указать имя входного регистра для APPLY_ORACLE.");
        }

        Map<String, Object> args = new HashMap<>();
        args.put("oracleName", oracleName);
        args.put("inputRegisterName", inputRegisterName);
        args.put("ancillaRegisterName", ancillaRegName);
        args.put("ancillaIndex", ancillaIndex);

        return new Command(org.example.script.Command.CommandType.APPLY_ORACLE, args);
    }
}
