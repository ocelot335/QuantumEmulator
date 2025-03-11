package org.example.model;

import org.example.model.gate.Gate;
import org.example.model.gate.GateResolver;
import org.example.model.qubit.Complex;
import org.example.model.qubit.Qubit;
import org.example.model.qubit.QubitRegister;
import org.example.script.Command;
import org.example.script.Parser;

import java.io.*;
import java.util.*;

public class Emulation implements Serializable, Cloneable {
    private Map<String, QubitRegister> qubitRegisters;
    public Emulation() {
        this.qubitRegisters = new HashMap<>();
    }


    //apply(String) - отправляем в парсер, получаем операцию(здесь либо отдельно аргументы получаем
    // и используем синглтон или статик класс,
    // либо сразу в одноразовом объекте операции), совершаем операцию

    public String run(String command) {
        Command parsedCommand = Parser.parse(command);
        if(parsedCommand==null) {
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
        qubitRegisters.put(registerName,new QubitRegister(registerName, numOfQubits));
        return "|"+toBinaryString(0, numOfQubits)+">: " + Complex.getOne()+"\n";
    }

    private String processApplyGate(Command command) {
        Map<String, Object> args = command.getArguments();
        String gateName = (String) args.get("gate");
        List<Map<String, Object>> operandsData = (List<Map<String, Object>>) args.get("operands");

        if (operandsData == null || operandsData.isEmpty()) {
            return  "Не указаны операнды для гейта " + gateName;
        }
        QubitRegister baseRegister = null;
        Integer[] incices = new Integer[operandsData.size()];
        for (int i = 0; i < operandsData.size(); i++) {
            Map<String, Object> operandData = operandsData.get(i);
            String registerName = (String) operandData.get("register");
            int index = (int) operandData.get("index");

            QubitRegister register = qubitRegisters.get(registerName);
            if(baseRegister == null) {
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
            incices[i] =index;
            if (incices[i] == null) {
                return "Кубит не найден в регистре " + registerName + " по индексу " + index + ".";
            }
        }

        applyGate(gateName, baseRegister, incices); // Применяем гейт к кубитам
        System.out.println("Применен гейт " + gateName + " к кубитам: " + java.util.Arrays.toString(incices)+" регистра: " + baseRegister.toString() );
        String output = "";
        BitSet bs = baseRegister.getStates();
        Complex[] ampls = baseRegister.getAmplitudes();;
        for (int i = bs.nextSetBit(0); i >= 0 ; i = bs.nextSetBit(i+1)) {
            output += "|"+toBinaryString(i, baseRegister.size())+">: " + ampls[i].toString()+"\n";
        }
        return output;
    }

    @Override
    public Emulation clone() {
        try {
            // Сериализуем объект в поток байтов
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            // Десериализуем объект из потока байтов, создавая глубокую копию
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Emulation) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Ошибка при клонировании объекта Emulation", e);
        }
    }

    public static String toBinaryString(int num, int nBits) {
        if (nBits <= 0) {
            return ""; // Обработка некорректного ввода
        }

        if (nBits > 32) {
            nBits = 32; // Ограничение до 32 бит (максимум для int)
        }

        StringBuilder binary = new StringBuilder();
        for (int i = nBits - 1; i >= 0; i--) {
            int bit = (num >> i) & 1; // Сдвигаем и получаем i-й бит
            binary.append(bit);
        }
        return binary.toString();
    }

    private String processMeasure(Command command) {
        HashMap<String, Object> qubitToMeasure = (HashMap<String, Object>) command.getArguments().get("operand");
        Qubit qubit = qubitRegisters.get((String) qubitToMeasure.get("register")).getQubit((Integer) qubitToMeasure.get("index"));
        return String.valueOf(qubit.sample());
    }

    public void applyGate(String gateName, QubitRegister register, Integer[] indices) {
        Gate gate = GateResolver.resolveByName(gateName, register, indices);
        gate.apply();
    }

    /*@Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Emulation state:\n");
        //sb.append(register);
        sb.append("\nApplied Gates:\n");
        for (Gate gate : gatesHistory) {
            sb.append(gate.toString()).append("\n");
        }
        return sb.toString();
    }*/
}
