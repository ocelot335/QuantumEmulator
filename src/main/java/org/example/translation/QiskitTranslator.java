package org.example.translation;

import org.example.model.Emulation;
import org.example.script.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QiskitTranslator implements Translator {
    private Map<String, Integer> registerSizes = new HashMap<>();

    @Override
    public String translate(List<Command> commands, Emulation context) {
        StringBuilder code = new StringBuilder();
        code.append("from qiskit import QuantumCircuit\n" +
                "from qiskit_aer import AerSimulator\n\n");

        int totalQubits = 0;
        int totalClassicalBits = 0;
        for (Command command : commands) {
            if (command.getType() == Command.CommandType.CREATE_REGISTER) {
                String name = command.getArgumentAsString("name");
                int size = command.getArgumentAsInt("size");
                registerSizes.put(name, size);
                totalQubits += size;
                totalClassicalBits += size;
            }
        }
        code.append("qc = QuantumCircuit(").append(totalQubits).append(", ").append(totalClassicalBits).append(")\n\n");

        int qubitOffset = 0;
        Map<String, Integer> registerOffsets = new HashMap<>();
        for (Command command : commands) {
            if (command.getType() == Command.CommandType.CREATE_REGISTER) {
                String name = command.getArgumentAsString("name");
                registerOffsets.put(name, qubitOffset);
                qubitOffset += registerSizes.get(name);
            } else if (command.getType() == Command.CommandType.APPLY_GATE) {
                String gate = (String) command.getArgument("gate");
                List<Map<String, Object>> operands = (List<Map<String, Object>>) command.getArgument("operands");
                switch (gate) {
                    case "H":
                        int hQubit = getQubitIndex(operands.get(0), registerOffsets);
                        code.append("qc.h(").append(hQubit).append(")\n");
                        break;
                    case "X":
                        int xQubit = getQubitIndex(operands.get(0), registerOffsets);
                        code.append("qc.x(").append(xQubit).append(")\n");
                        break;
                    case "Y":
                        int yQubit = getQubitIndex(operands.get(0), registerOffsets);
                        code.append("qc.y(").append(yQubit).append(")\n");
                        break;
                    case "Z":
                        int zQubit = getQubitIndex(operands.get(0), registerOffsets);
                        code.append("qc.z(").append(zQubit).append(")\n");
                        break;
                    case "SWAP":
                        int swapQubit1 = getQubitIndex(operands.get(0), registerOffsets);
                        int swapQubit2 = getQubitIndex(operands.get(1), registerOffsets);
                        code.append("qc.swap(").append(swapQubit1).append(", ").append(swapQubit2).append(")\n");
                        break;
                    case "CX":
                        int controlQubit = getQubitIndex(operands.get(0), registerOffsets);
                        int targetQubit = getQubitIndex(operands.get(1), registerOffsets);
                        code.append("qc.cx(").append(controlQubit).append(", ").append(targetQubit).append(")\n");
                        break;
                    default:
                        //TODO: throw new Exception("Unsuported Gate");
                }
            } else if (command.getType() == Command.CommandType.MEASURE) {
                //TODO
                /*Map<String, Object> operand = (Map<String, Object>) command.getArgument("operand");
                int qubit = getQubitIndex(operand, registerOffsets);
                code.append("qc.measure(").append(qubit).append(", ").append(qubit).append(")\n");*/
            }
        }

        code.append("\n# Выполнение схемы\n");
        code.append("simulator = AerSimulator()\n");
        code.append("job = simulator.run(qc, shots=1000)\n");
        code.append("result = job.result()\n");
        code.append("counts = result.get_counts(qc)\n");
        code.append("print('Результаты измерений:', counts)\n");

        return code.toString();
    }

    private int getQubitIndex(Map<String, Object> operand, Map<String, Integer> registerOffsets) {
        String registerName = (String) operand.get("register");
        int index = (int) operand.get("index");
        return registerOffsets.get(registerName) + index;
    }

    @Override
    public String getFileExtension() {
        return ".py";
    }

    @Override
    public String getPlatformName() {
        return "Qiskit";
    }
}
