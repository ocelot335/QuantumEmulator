package org.example.model;

import lombok.Getter;
import org.example.model.gate.Gate;
import org.example.model.gate.GateResolver;
import org.example.model.gate.GateTrace;
import org.example.model.gate.oracle.OracleDefinition;
import org.example.model.gate.oracle.OracleGate;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.example.script.Command;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Emulation implements Serializable, Cloneable {
    @Getter
    private final Map<String, QubitRegister> qubitRegisters;
    @Getter
    private final Map<String, OracleDefinition> definedOracles;
    @Getter
    private GateTrace lastGateTrace;
    private final Map<String, QubitRegister> realRegisters;

    public Emulation() {
        this.qubitRegisters = new HashMap<>();
        this.realRegisters = new HashMap<>();
        this.definedOracles = new HashMap<>();
    }

    public QubitRegister getNominalRegister(String name) {
        return qubitRegisters.get(name);
    }

    public QubitRegister getRealRegister(String name) {
        return realRegisters.get(name);
    }

    public Map<String, Object> run(Command parsedCommand) {
        Map<String, Object> result = new HashMap<>();
        result.put("joinInfo", null);

        if (parsedCommand == null) {
            result.put("output", "");
            return result;
        }
        switch (parsedCommand.getType()) {
            case CREATE_REGISTER -> {
                result.put("output", processCreateRegister(parsedCommand));
                return result;
            }
            case APPLY_GATE -> {
                Map<String, Object> gateResult = processApplyGate(parsedCommand);
                result.put("output", gateResult.getOrDefault("output", ""));
                result.put("joinInfo", gateResult.get("joinInfo"));
                return result;
            }
            case MEASURE -> {
                result.put("output", processMeasure(parsedCommand));
                return result;
            }
            case DEFINE_ORACLE_CSV -> {
                result.put("output", processDefineOracleCsv(parsedCommand));
                return result;
            }
            case APPLY_ORACLE -> {
                Map<String, Object> oracleResult = processApplyOracle(parsedCommand);
                result.put("output", oracleResult.getOrDefault("output", ""));
                result.put("joinInfo", oracleResult.get("joinInfo"));
                return result;
            }
            default -> {
                result.put("output", "Неизвестный тип команды!");
                return result;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String processCreateRegister(Command command) {
        String realRegisterName = command.getArgumentAsString("realRegisterName");
        int realRegisterSize = command.getArgumentAsInt("realRegisterSize");
        List<Map<String, Object>> nominalRegistersSpecs = (List<Map<String, Object>>) command.getArgument("nominalRegisters");

        QubitRegister realRegister = new QubitRegister(realRegisterName, realRegisterSize);
        realRegisters.put(realRegisterName, realRegister);

        for (Map<String, Object> spec : nominalRegistersSpecs) {
            String nominalName = (String) spec.get("name");
            int nominalSize = (int) spec.get("size");
            int offset = (int) spec.get("offset");

            if (qubitRegisters.containsKey(nominalName)) {
                // TODO: Или разрешить переопределение? Пока запрещаем.
                throw new IllegalArgumentException("Номинальный регистр с именем '" + nominalName + "' уже существует.");
            }

            QubitRegister nominalRegister = new QubitRegister(nominalName, nominalSize, realRegister, offset);
            qubitRegisters.put(nominalName, nominalRegister);
        }

        return realRegister.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processApplyGate(Command command) {
        Map<String, Object> result = new HashMap<>();
        result.put("joinInfo", null);

        Map<String, Object> args = command.getArguments();
        String gateName = (String) args.get("gate");
        double phase = 0;
        if(args.containsKey("phase")) {
            phase = (double) args.get("phase");
        }
        List<Map<String, Object>> operandsData = (List<Map<String, Object>>) args.get("operands");

        if (operandsData == null || operandsData.isEmpty()) {
            result.put("output", "Не указаны операнды для гейта " + gateName);
            return result;
        }

        QubitRegister baseRealRegister = null;
        List<QubitRegister> involvedNominalRegisters = new ArrayList<>();
        List<QubitRegister> involvedRealRegisters = new ArrayList<>(); // Используем List для сохранения порядка добавления

        for (Map<String, Object> operandData : operandsData) {
            String nominalRegisterName = (String) operandData.get("register");
            int nominalIndex = (int) operandData.get("index");

            QubitRegister nominalRegister = qubitRegisters.get(nominalRegisterName);
            if (nominalRegister == null) {
                result.put("output", "Номинальный регистр '" + nominalRegisterName + "' не найден.");
                return result;
            }
            involvedNominalRegisters.add(nominalRegister);

            if (nominalIndex < 0 || nominalIndex >= nominalRegister.size()) {
                result.put("output", "Индекс " + nominalIndex + " вне границ номинального регистра " + nominalRegisterName + " (размер " + nominalRegister.size() + ").");
                return result;
            }

            QubitRegister currentRealRegister = nominalRegister.getRealRegister();
            if (baseRealRegister == null) {
                baseRealRegister = currentRealRegister;
                if (!involvedRealRegisters.contains(baseRealRegister)) {
                    involvedRealRegisters.add(baseRealRegister);
                }
            } else if (baseRealRegister != currentRealRegister) {
                if (!involvedRealRegisters.contains(currentRealRegister)) {
                    involvedRealRegisters.add(currentRealRegister);
                }
            }
        }

        if (involvedRealRegisters.size() > 1) {
            System.out.println("Обнаружена необходимость объединения реальных регистров: " + involvedRealRegisters.stream().map(QubitRegister::getName).collect(Collectors.joining(", ")));

            QubitRegister currentJoinedRegister = involvedRealRegisters.get(0);
            List<String> oldRegNames = new ArrayList<>();
            oldRegNames.add(currentJoinedRegister.getName());

            for (int i = 1; i < involvedRealRegisters.size(); i++) {
                QubitRegister nextRegisterToJoin = involvedRealRegisters.get(i);
                oldRegNames.add(nextRegisterToJoin.getName());

                int size1 = currentJoinedRegister.getRealSize();
                int size2 = nextRegisterToJoin.getRealSize();
                String joinedName = "join(" + currentJoinedRegister.getName() + "," + nextRegisterToJoin.getName() + ")";

                QubitRegister joinedRealRegister = QubitRegister.tensorProduct(currentJoinedRegister, nextRegisterToJoin, joinedName);

                System.out.println("Создан объединенный регистр: " + joinedName + " размера " + joinedRealRegister.getRealSize());

                List<String> nominalNamesToUpdate = new ArrayList<>(this.qubitRegisters.keySet());
                for (String nominalName : nominalNamesToUpdate) {
                    QubitRegister nominalReg = this.qubitRegisters.get(nominalName);
                    if (nominalReg.getRealRegister() == currentJoinedRegister) {
                        int newOffset = size2 + nominalReg.getOffsetInRealRegister();
                        QubitRegister updatedNominalReg = new QubitRegister(
                                nominalReg.getName(),
                                nominalReg.size(),
                                joinedRealRegister,
                                newOffset
                        );
                        this.qubitRegisters.put(nominalName, updatedNominalReg); // Заменяем старый
                        System.out.println("Номинальный регистр " + nominalName + " перенаправлен на " + joinedName + " (offset " + newOffset + ")");
                    } else if (nominalReg.getRealRegister() == nextRegisterToJoin) {
                        int newOffset = nominalReg.getOffsetInRealRegister();
                        QubitRegister updatedNominalReg = new QubitRegister(
                                nominalReg.getName(),
                                nominalReg.size(),
                                joinedRealRegister,
                                newOffset
                        );
                        this.qubitRegisters.put(nominalName, updatedNominalReg);
                        System.out.println("Номинальный регистр " + nominalName + " перенаправлен на " + joinedName + " (new offset " + newOffset + ")");
                    }
                }

                this.realRegisters.put(joinedName, joinedRealRegister);
                this.realRegisters.remove(currentJoinedRegister.getName());
                this.realRegisters.remove(nextRegisterToJoin.getName());
                System.out.println("Обновлена карта реальных регистров.");

                currentJoinedRegister = joinedRealRegister;

                Map<String, Object> joinInfo = new HashMap<>();
                joinInfo.put("joined", true);
                joinInfo.put("newRealRegName", joinedName);
                joinInfo.put("oldRealRegNames", new ArrayList<>(oldRegNames));
                joinInfo.put("initialJoinedState", getRealRegisterState(currentJoinedRegister.getName()));

                result.put("joinInfo", joinInfo);
            }
            baseRealRegister = currentJoinedRegister;
        }

        int[] absoluteIndices = new int[operandsData.size()];
        StringBuilder operandNominalNames = new StringBuilder();
        for (int i = 0; i < operandsData.size(); i++) {
            Map<String, Object> operandData = operandsData.get(i);
            String nominalRegisterName = (String) operandData.get("register");
            int nominalIndex = (int) operandData.get("index");
            QubitRegister nominalRegister = qubitRegisters.get(nominalRegisterName);
            if (nominalRegister == null || nominalRegister.getRealRegister() != baseRealRegister) {
                result.put("output", "Критическая ошибка: Номинальный регистр '" + nominalRegisterName + "' не найден или не указывает на финальный реальный регистр после объединения.");
                return result;
            }
            absoluteIndices[i] = nominalRegister.getOffsetInRealRegister() + nominalIndex;
            // int size = nominalRegister.size();
            // int offset = nominalRegister.getOffsetInRealRegister();
            // absoluteIndices[i] = offset + (size - 1 - nominalIndex); // Неправильная формула

            if (i > 0) operandNominalNames.append(", ");
            operandNominalNames.append(nominalRegisterName).append("[").append(nominalIndex).append("]");
        }

        if (baseRealRegister == null) {
            result.put("output", "Не удалось определить реальный регистр для гейта.");
            return result;
        }

        try {
            Integer[] absoluteIndicesInteger = Arrays.stream(absoluteIndices).boxed().toArray(Integer[]::new);
            Gate gate = GateResolver.resolveByName(gateName, baseRealRegister, absoluteIndicesInteger, phase);
            if (gate == null) {
                result.put("output", "Неизвестный гейт: " + gateName);
                return result;
            }
            lastGateTrace = gate.apply();
            if (lastGateTrace == null) {
                result.put("output", "Ошибка при применении гейта " + gateName);
                return result;
            }
            System.out.println("State of " + baseRealRegister.getName() + " immediately after gate.apply():");
            System.out.println(baseRealRegister);
            Map<Integer, Complex> checkState = getRealRegisterState(baseRealRegister.getName());
            System.out.println("State from getRealRegisterState: " + checkState);
        } catch (Exception e) {
            result.put("output", "Ошибка при применении гейта " + gateName + " к " + operandNominalNames
                    + " (реальный регистр: " + (baseRealRegister != null ? baseRealRegister.getName() : "null") + ", абс. индексы: " + java.util.Arrays.toString(absoluteIndices) + "): "
                    + e.getMessage());
            return result;
        }

        System.out.println("Применен гейт " + gateName + " к кубитам: " + operandNominalNames
                + " реального регистра: " + baseRealRegister.getName());
        result.put("output", baseRealRegister.toString());
        return result;
    }

    @Override
    public Emulation clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Emulation) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Критическая ошибка при клонировании объекта Emulation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Ошибка при клонировании объекта Emulation", e);
        }
    }

    public static String toBinaryString(int num, int nBits) {
        if (nBits <= 0) {
            return "";
        }

        if (nBits > 32) {
            nBits = 32;//TODO
        }

        StringBuilder binary = new StringBuilder();
        for (int i = nBits - 1; i >= 0; i--) {
            int bit = (num >> i) & 1;
            binary.append(bit);
        }
        return binary.toString();
    }

    private String processMeasure(Command command) {
        @SuppressWarnings("unchecked")
        Map<String, Object> qubitToMeasure = command.getArguments();
        String nominalRegisterName = (String) qubitToMeasure.get("register");
        int nominalIndex = (int) qubitToMeasure.get("index");

        QubitRegister nominalRegister = qubitRegisters.get(nominalRegisterName);
        if (nominalRegister == null) {
            return "Номинальный регистр '" + nominalRegisterName + "' не найден для измерения.";
        }

        try {
            Integer result = nominalRegister.measureQubit(nominalIndex);
            return result.toString();
        } catch (IndexOutOfBoundsException e) {
            return "Ошибка измерения: " + e.getMessage();
        } catch (Exception e) {
            return "Неожиданная ошибка при измерении кубита " + nominalRegisterName + "[" + nominalIndex + "]: " + e.getMessage();
        }
    }

    public Map<Integer, Complex> getRegisterState(String nominalRegisterName) {
        QubitRegister nominalRegister = qubitRegisters.get(nominalRegisterName);
        if (nominalRegister == null) {
            System.err.println("Предупреждение: Запрошено состояние для несуществующего номинального регистра '" + nominalRegisterName + "'");
            return new HashMap<>();
        }
        return getRealRegisterState(nominalRegister.getRealRegister().getName());
    }

    public Map<Integer, Complex> getRealRegisterState(String realRegisterName) {
        QubitRegister realRegister = realRegisters.get(realRegisterName);
        if (realRegister == null) {
            System.err.println("Предупреждение: Запрошено состояние для несуществующего реального регистра '" + realRegisterName + "'");
            return new HashMap<>();
        }

        Map<Integer, Complex> registerState = new HashMap<>();
        BitSet states = realRegister.getStates();
        ChunkedComplexArray amplitudes = realRegister.getAmplitudes();
        int realSize = realRegister.size();

        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            if (i < (1 << realSize)) {
                Complex amplitude = amplitudes.get(i);
                if (amplitude != null && !amplitude.equals(Complex.getZero())) {
                    registerState.put(i, amplitude);
                }
            } else {
                System.err.println("Обнаружено состояние за пределами ожидаемого размера реального регистра!");
            }
        }
        return registerState;
    }


    private String processDefineOracleCsv(Command command) {
        String oracleName = command.getArgumentAsString("oracleName");
        String csvPath = command.getArgumentAsString("csvPath");

        if (definedOracles.containsKey(oracleName)) {
            // TODO: Возможно, стоит просто обновлять, а не возвращать предупреждение?
            return "Предупреждение: Оракул с именем '" + oracleName + "' уже определен. Переопределение...";
        }

        Set<Integer> statesWhereOutputIsOne = new HashSet<>();
        int inputSize = -1;
        int maxStateValue = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (firstLine) {
                    try {
                        inputSize = Integer.parseInt(line);
                        if (inputSize <= 0) {
                            throw new IllegalArgumentException("Размер входа (inputSize) в первой строке CSV должен быть положительным целым числом.");
                        }
                        firstLine = false;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Первая непустая строка CSV файла '" + csvPath + "' должна содержать размер входа (inputSize) как целое число.", e);
                    }
                } else {
                    if (inputSize == -1) {
                        // Эта ошибка не должна возникать, если первая строка прочитана правильно
                        throw new IllegalStateException("Размер входа (inputSize) не был определен перед чтением состояний.");
                    }
                    try {
                        int stateDecimal = Integer.parseInt(line);
                        if (stateDecimal < 0) {
                            throw new IllegalArgumentException("Десятичное состояние '" + line + "' в CSV не может быть отрицательным.");
                        }
                        if (stateDecimal >= (1 << inputSize)) {
                            throw new IllegalArgumentException("Десятичное состояние " + stateDecimal + " в CSV файле превышает максимальное значение для " + inputSize + " кубитов (" + ((1 << inputSize) - 1) + ").");
                        }

                        if (!statesWhereOutputIsOne.add(stateDecimal)) {
                            System.err.println("Предупреждение: Дублирующееся десятичное состояние " + stateDecimal + " в CSV файле '" + csvPath + "' для оракула '" + oracleName + "'.");
                        }
                        maxStateValue = Math.max(maxStateValue, stateDecimal);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Неверный формат десятичного состояния '" + line + "' в CSV файле '" + csvPath + "'. Ожидается целое число.", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Ошибка: Файл CSV для оракула '" + oracleName + "' не найден: " + csvPath, e);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения CSV файла для оракула '" + oracleName + "': " + csvPath, e);
        }

        if (inputSize == -1) {
            throw new IllegalArgumentException("CSV файл '" + csvPath + "' не содержит определения размера входа (inputSize) в первой строке.");
        }

        if (maxStateValue != -1 && maxStateValue >= (1 << inputSize)) {
            throw new IllegalStateException("Внутренняя ошибка: максимальное состояние не соответствует inputSize.");
        }

        OracleDefinition definition = new OracleDefinition(oracleName, statesWhereOutputIsOne);
        definedOracles.put(oracleName, definition);

        return "Оракул '" + oracleName + "' успешно определен из файла " + csvPath + " (" + inputSize + " входов, " + statesWhereOutputIsOne.size() + " состояний с выходом 1).";
    }

    private Map<String, Object> processApplyOracle(Command command) {
        String oracleName = command.getArgumentAsString("oracleName");
        String inputRegName = command.getArgumentAsString("inputRegisterName");
        String ancillaRegName = command.getArgumentAsString("ancillaRegisterName");
        int ancillaIndex = command.getArgumentAsInt("ancillaIndex");

        Map<String, Object> result = new HashMap<>();
        result.put("joinInfo", null); // TODO: Объединение не поддерживается для оракулов

        OracleDefinition oracleDefinition = definedOracles.get(oracleName);
        if (oracleDefinition == null) {
            result.put("output", "Ошибка: Оракул с именем '" + oracleName + "' не определен.");
            return result;
        }

        List<QubitRegister> involvedNominalRegisters = new ArrayList<>();
        QubitRegister baseRealRegister = null;
        String errorMsg = null;

        QubitRegister inputNominalReg = qubitRegisters.get(inputRegName);
        if (inputNominalReg == null) {
            result.put("output", "Ошибка: Номинальный регистр '" + inputRegName + "' (вход оракула) не найден.");
            return result;
        }
        involvedNominalRegisters.add(inputNominalReg);
        baseRealRegister = inputNominalReg.getRealRegister();

        QubitRegister ancillaNominalReg = qubitRegisters.get(ancillaRegName);
        if (ancillaNominalReg == null) {
            result.put("output", "Ошибка: Номинальный регистр '" + ancillaRegName + "' (ancilla) не найден.");
            return result;
        }
        if (ancillaIndex < 0 || ancillaIndex >= ancillaNominalReg.size()) {
            result.put("output", "Ошибка: Индекс " + ancillaIndex + " вне границ номинального регистра ancilla '" + ancillaRegName + "' (размер " + ancillaNominalReg.size() + ").");
            return result;
        }
        involvedNominalRegisters.add(ancillaNominalReg);

        if (ancillaNominalReg.getRealRegister() != baseRealRegister) {
            result.put("output", "Ошибка: Регистр ancilla '" + ancillaRegName
                    + "' находится в другом реальном регистре, чем входной регистр '" + inputRegName + "'. Объединение не поддерживается.");
            return result;
        }

        int realSize = baseRealRegister.getRealSize();
        int inputNominalSize = inputNominalReg.size();
        int inputOffset = inputNominalReg.getOffsetInRealRegister();

        int ancillaAbsIndex = ancillaNominalReg.getOffsetInRealRegister() + ancillaIndex;

        if (ancillaAbsIndex >= inputOffset && ancillaAbsIndex < (inputOffset + inputNominalSize)) {
            result.put("output", "Ошибка: Кубит ancilla (" + ancillaRegName + "[" + ancillaIndex + "])" +
                    " пересекается с входным регистром '" + inputRegName + "'.");
            return result;
        }

        try {
            OracleGate oracleGate = new OracleGate(
                    oracleName,
                    baseRealRegister,
                    inputOffset,
                    inputNominalSize,
                    ancillaAbsIndex,
                    oracleDefinition.getStatesWhereOutputIsOne()
            );

            this.lastGateTrace = oracleGate.apply();

        } catch (Exception e) {
            result.put("output", "Ошибка при применении оракула '" + oracleName + "': " + e.getMessage());
            return result;
        }

        result.put("output", "Оракул '" + oracleName + "' применен.");
        return result;
    }
}
