package org.example.model;

import org.example.model.gate.Gate;
import org.example.model.gate.GateResolver;
import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.example.script.Command;
import org.example.script.Parser;

import java.io.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Emulation implements Serializable, Cloneable {
    private final Map<String, QubitRegister> qubitRegisters;
    private GateTrace lastGateTrace;
    private Map<Integer, Complex> currentState;

    public Emulation() {
        this.qubitRegisters = new HashMap<>();
        this.currentState = new HashMap<>();
    }


    //apply(String) - отправляем в парсер, получаем операцию(здесь либо отдельно аргументы получаем
    // и используем синглтон или статик класс,
    // либо сразу в одноразовом объекте операции), совершаем операцию

    public String run(Command parsedCommand) {

        if (parsedCommand == null) {
            return "";
        }
        switch (parsedCommand.getType()) {
            case CREATE_REGISTER -> {
                return processCreateRegister(parsedCommand);
            }
            case APPLY_GATE -> {
                return processApplyGate(parsedCommand);
            }
            case MEASURE -> {
                return processMeasure(parsedCommand);
            }
            default -> {
                return "Неизвестный тип команды!";
            }
        }
    }

    private String processCreateRegister(Command command) {
        String registerName = command.getArgumentAsString("name");
        int numOfQubits = command.getArgumentAsInt("size");
        qubitRegisters.put(registerName, new QubitRegister(registerName, numOfQubits));
        return "|" + toBinaryString(0, numOfQubits) + ">: " + Complex.getOne() + "\n";
    }

    private String processApplyGate(Command command) {
        Map<String, Object> args = command.getArguments();
        String gateName = (String) args.get("gate");
        List<Map<String, Object>> operandsData = (List<Map<String, Object>>) args.get("operands");

        if (operandsData == null || operandsData.isEmpty()) {
            return "Не указаны операнды для гейта " + gateName;
        }
        QubitRegister baseRegister = null;
        Integer[] incices = new Integer[operandsData.size()];
        for (int i = 0; i < operandsData.size(); i++) {
            Map<String, Object> operandData = operandsData.get(i);
            String registerName = (String) operandData.get("register");
            int index = (int) operandData.get("index");

            QubitRegister register = qubitRegisters.get(registerName);
            if (baseRegister == null) {
                baseRegister = register;
            } else if (baseRegister != register) {
                System.out.println("Нельзя применять гейт на разные регистры! Создайте общий регистр");
            }
            if (register == null) {
                return "Регистр " + registerName + " не найден.";
            }
            if (index < 0 || index >= register.size()) {
                return "Индекс " + index + " вне границ регистра " + registerName + ".";
            }
            incices[i] = index;
            if (incices[i] == null) {
                return "Кубит не найден в регистре " + registerName + " по индексу " + index + ".";
            }
        }

        try {
            Gate gate = GateResolver.resolveByName(gateName, baseRegister, incices);
            if (gate == null) {
                return "Неизвестный гейт: " + gateName;
            }
            lastGateTrace = gate.apply();
            if (lastGateTrace == null) {
                return "Ошибка при применении гейта " + gateName;
            }
        } catch (Exception e) {
            return "Ошибка при применении гейта " + gateName + ": " + e.getMessage();
        }

        System.out.println("Применен гейт " + gateName + " к кубитам: " + java.util.Arrays.toString(incices) + " регистра: " + baseRegister);
        String output = "";
        BitSet bs = baseRegister.getStates();
        Complex[] ampls = baseRegister.getAmplitudes();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            output += "|" + toBinaryString(i, baseRegister.size()) + ">: " + ampls[i].toString() + "\n";
        }
        return output;
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
        HashMap<String, Object> qubitToMeasure = (HashMap<String, Object>) command.getArguments().get("operand");
        return  qubitRegisters.get((String) qubitToMeasure.get("register"))
                .measureQubit((Integer) qubitToMeasure.get("index")).toString();
    }

    public GateTrace getLastGateTrace() {
        return lastGateTrace;
    }

    public Map<Integer, Complex> getCurrentState() {
        if (qubitRegisters.isEmpty()) {
            return new HashMap<>();
        }

        // Берем первый регистр (в текущей реализации у нас всегда один регистр)
        QubitRegister register = qubitRegisters.values().iterator().next();
        Map<Integer, Complex> currentState = new HashMap<>();

        BitSet states = register.getStates();
        Complex[] amplitudes = register.getAmplitudes();

        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            currentState.put(i, amplitudes[i]);
        }

        return currentState;
    }
}
