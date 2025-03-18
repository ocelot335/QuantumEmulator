package org.example.translation;

import org.example.model.Emulation;
import org.example.script.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CirqTranslator implements QuantumTranslator {
    private final Map<String, Integer> registerSizes = new HashMap<>();

    @Override
    public String translate(List<Command> commands, Emulation context) {
        StringBuilder code = new StringBuilder();
        
        code.append("import cirq\n");
        code.append("import numpy as np\n\n");

        for (Command command : commands) {
            if (command.getType() == Command.CommandType.CREATE_REGISTER) {
                String name = command.getArgumentAsString("name");
                int size = command.getArgumentAsInt("size");
                registerSizes.put(name, size);
            }
        }

        code.append("# Создание кубитов\n");
        for (Map.Entry<String, Integer> entry : registerSizes.entrySet()) {
            String name = entry.getKey();
            int size = entry.getValue();
            code.append(String.format("%s = [cirq.GridQubit(i, 0) for i in range(%d)]\n", name, size));
        }
        code.append("\n");

        code.append("# Создание квантовой схемы\n");
        code.append("circuit = cirq.Circuit()\n\n");

        code.append("# Применение квантовых вентилей\n");
        for (Command command : commands) {
            if (command.getType() == Command.CommandType.APPLY_GATE) {
                String gate = (String) command.getArgument("gate");
                List<Map<String, Object>> operands = (List<Map<String, Object>>) command.getArgument("operands");
                
                switch (gate) {
                    case "H":
                        String hReg = (String) operands.get(0).get("register");
                        int hIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.H(%s[%d]))\n", hReg, hIdx));
                        break;
                    case "X":
                    case "NOT":
                        String xReg = (String) operands.get(0).get("register");
                        int xIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.X(%s[%d]))\n", xReg, xIdx));
                        break;
                    case "Y":
                        String yReg = (String) operands.get(0).get("register");
                        int yIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.Y(%s[%d]))\n", yReg, yIdx));
                        break;
                    case "Z":
                        String zReg = (String) operands.get(0).get("register");
                        int zIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.Z(%s[%d]))\n", zReg, zIdx));
                        break;
                    case "S":
                        String sReg = (String) operands.get(0).get("register");
                        int sIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.S(%s[%d]))\n", sReg, sIdx));
                        break;
                    case "T":
                        String tReg = (String) operands.get(0).get("register");
                        int tIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.T(%s[%d]))\n", tReg, tIdx));
                        break;
                    case "SWAP":
                        String reg1 = (String) operands.get(0).get("register");
                        String reg2 = (String) operands.get(1).get("register");
                        int idx1 = (int) operands.get(0).get("index");
                        int idx2 = (int) operands.get(1).get("index");
                        code.append(String.format("    circuit.append(cirq.SWAP(%s[%d], %s[%d]))\n", reg1, idx1, reg2, idx2));
                        break;
                    case "INC":
                        String incReg = (String) operands.get(0).get("register");
                        int incIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.X(%s[%d]))\n", incReg, incIdx));
                        break;
                    case "DEC":
                        String decReg = (String) operands.get(0).get("register");
                        int decIdx = (int) operands.get(0).get("index");
                        code.append(String.format("    circuit.append(cirq.X(%s[%d]))\n", decReg, decIdx));
                        break;
                    case "CX":
                        String controlReg = (String) operands.get(0).get("register");
                        String targetReg = (String) operands.get(1).get("register");
                        int controlIdx = (int) operands.get(0).get("index");
                        int targetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("    circuit.append(cirq.CNOT(%s[%d], %s[%d]))\n", controlReg, controlIdx, targetReg, targetIdx));
                        break;
                    case "CH":
                        String chControlReg = (String) operands.get(0).get("register");
                        String chTargetReg = (String) operands.get(1).get("register");
                        int chControlIdx = (int) operands.get(0).get("index");
                        int chTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("    circuit.append(cirq.ControlledGate(cirq.H)(%s[%d], %s[%d]))\n", chControlReg, chControlIdx, chTargetReg, chTargetIdx));
                        break;
                    case "CS":
                        String csControlReg = (String) operands.get(0).get("register");
                        String csTargetReg = (String) operands.get(1).get("register");
                        int csControlIdx = (int) operands.get(0).get("index");
                        int csTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("    circuit.append(cirq.ControlledGate(cirq.S)(%s[%d], %s[%d]))\n", csControlReg, csControlIdx, csTargetReg, csTargetIdx));
                        break;
                    case "CT":
                        String ctControlReg = (String) operands.get(0).get("register");
                        String ctTargetReg = (String) operands.get(1).get("register");
                        int ctControlIdx = (int) operands.get(0).get("index");
                        int ctTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("    circuit.append(cirq.ControlledGate(cirq.T)(%s[%d], %s[%d]))\n", ctControlReg, ctControlIdx, ctTargetReg, ctTargetIdx));
                        break;
                    default:
                        code.append(String.format("    # Неподдерживаемый вентиль: %s\n", gate));
                }
            } else if (command.getType() == Command.CommandType.MEASURE) {
                Map<String, Object> operand = (Map<String, Object>) command.getArgument("operand");
                String regName = (String) operand.get("register");
                int idx = (int) operand.get("index");
                code.append(String.format("circuit.append(cirq.measure(%s[%d], key='result_%d'))\n", regName, idx, idx));
            }
        }

        code.append("\n# Выполнение схемы\n");
        code.append("simulator = cirq.Simulator()\n");
        code.append("result = simulator.run(circuit, repetitions=1000)\n");
        code.append("print('Результаты измерений:', result.histogram(key='result_0'))\n");

        return code.toString();
    }
} 