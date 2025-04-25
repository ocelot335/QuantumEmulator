package org.example.translation;

import org.example.model.Emulation;
import org.example.script.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QiskitTranslator implements QuantumTranslator {
    private final Map<String, Integer> registerSizes = new HashMap<>();
    private final Map<String, Integer> registerOffsets = new HashMap<>();

    @Override
    public String translate(List<Command> commands, Emulation context) {
        StringBuilder code = new StringBuilder();

        code.append("from qiskit import QuantumCircuit, QuantumRegister, ClassicalRegister\n");
        code.append("from qiskit_aer import AerSimulator\n");
        code.append("from qiskit.visualization import plot_histogram\n");
        code.append("import matplotlib.pyplot as plt\n\n");

        int totalQubits = 0;
        for (Command command : commands) {
            if (command.getType() == Command.CommandType.CREATE_REGISTER) {
                String name = command.getArgumentAsString("name");
                int size = command.getArgumentAsInt("size");
                registerSizes.put(name, size);
                registerOffsets.put(name, totalQubits);
                totalQubits += size;
            }
        }

        code.append("# Создание квантовых и классических регистров\n");
        for (Map.Entry<String, Integer> entry : registerSizes.entrySet()) {
            String name = entry.getKey();
            int size = entry.getValue();
            code.append(String.format("%s = QuantumRegister(%d, '%s')\n", name, size, name));
            code.append(String.format("c_%s = ClassicalRegister(%d, 'c_%s')\n", name, size, name));
        }

        code.append("\n# Создание квантовой схемы\n");
        code.append("qc = QuantumCircuit(");
        code.append(String.join(", ", registerSizes.keySet()));
        code.append(", ");
        code.append(String.join(", ", registerSizes.keySet().stream().map(name -> "c_" + name).toList()));
        code.append(")\n\n");

        code.append("# Применение квантовых вентилей\n");
        for (Command command : commands) {
            if (command.getType() == Command.CommandType.APPLY_GATE) {
                String gate = (String) command.getArgument("gate");
                List<Map<String, Object>> operands = (List<Map<String, Object>>) command.getArgument("operands");

                switch (gate) {
                    case "H":
                        String hReg = (String) operands.get(0).get("register");
                        int hIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.h(%s[%d])\n", hReg, hIdx));
                        break;
                    case "X":
                    case "NOT":
                        String xReg = (String) operands.get(0).get("register");
                        int xIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.x(%s[%d])\n", xReg, xIdx));
                        break;
                    case "Y":
                        String yReg = (String) operands.get(0).get("register");
                        int yIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.y(%s[%d])\n", yReg, yIdx));
                        break;
                    case "Z":
                        String zReg = (String) operands.get(0).get("register");
                        int zIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.z(%s[%d])\n", zReg, zIdx));
                        break;
                    case "S":
                        String sReg = (String) operands.get(0).get("register");
                        int sIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.s(%s[%d])\n", sReg, sIdx));
                        break;
                    case "T":
                        String tReg = (String) operands.get(0).get("register");
                        int tIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.t(%s[%d])\n", tReg, tIdx));
                        break;
                    case "SWAP":
                        String reg1 = (String) operands.get(0).get("register");
                        String reg2 = (String) operands.get(1).get("register");
                        int idx1 = (int) operands.get(0).get("index");
                        int idx2 = (int) operands.get(1).get("index");
                        code.append(String.format("qc.swap(%s[%d], %s[%d])\n", reg1, idx1, reg2, idx2));
                        break;
                    case "INC":
                        String incReg = (String) operands.get(0).get("register");
                        int incIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.x(%s[%d])\n", incReg, incIdx));
                        break;
                    case "DEC":
                        String decReg = (String) operands.get(0).get("register");
                        int decIdx = (int) operands.get(0).get("index");
                        code.append(String.format("qc.x(%s[%d])\n", decReg, decIdx));
                        break;
                    case "CX":
                        String controlReg = (String) operands.get(0).get("register");
                        String targetReg = (String) operands.get(1).get("register");
                        int controlIdx = (int) operands.get(0).get("index");
                        int targetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("qc.cx(%s[%d], %s[%d])\n", controlReg, controlIdx, targetReg, targetIdx));
                        break;
                    case "CH":
                        String chControlReg = (String) operands.get(0).get("register");
                        String chTargetReg = (String) operands.get(1).get("register");
                        int chControlIdx = (int) operands.get(0).get("index");
                        int chTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("qc.ch(%s[%d], %s[%d])\n", chControlReg, chControlIdx, chTargetReg, chTargetIdx));
                        break;
                    case "CS":
                        String csControlReg = (String) operands.get(0).get("register");
                        String csTargetReg = (String) operands.get(1).get("register");
                        int csControlIdx = (int) operands.get(0).get("index");
                        int csTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("qc.cs(%s[%d], %s[%d])\n", csControlReg, csControlIdx, csTargetReg, csTargetIdx));
                        break;
                    case "CT":
                        String ctControlReg = (String) operands.get(0).get("register");
                        String ctTargetReg = (String) operands.get(1).get("register");
                        int ctControlIdx = (int) operands.get(0).get("index");
                        int ctTargetIdx = (int) operands.get(1).get("index");
                        code.append(String.format("qc.ct(%s[%d], %s[%d])\n", ctControlReg, ctControlIdx, ctTargetReg, ctTargetIdx));
                        break;
                    default:
                        code.append(String.format("# Неподдерживаемый вентиль: %s\n", gate));
                }
            } else if (command.getType() == Command.CommandType.MEASURE) {
                Map<String, Object> operand = (Map<String, Object>) command.getArgument("operand");
                String regName = (String) operand.get("register");
                int idx = (int) operand.get("index");
                code.append(String.format("qc.measure(%s[%d], c_%s[%d])\n", regName, idx, regName, idx));
            }
        }

        code.append("\n# Выполнение схемы\n");
        code.append("simulator = AerSimulator()\n");
        code.append("job = simulator.run(qc, shots=1000)\n");
        code.append("result = job.result()\n");
        code.append("counts = result.get_counts(qc)\n\n");

        code.append("# Визуализация результатов\n");
        code.append("plt.figure(figsize=(10, 6))\n");
        code.append("plot_histogram(counts)\n");
        code.append("plt.title('Результаты измерений')\n");
        code.append("plt.show()\n");

        return code.toString();
    }
}
