package org.example.model.gate;

import org.example.model.gate.instances.*;
import org.example.model.gate.instances.pauli.X;
import org.example.model.gate.instances.pauli.Y;
import org.example.model.gate.instances.pauli.Z;
import org.example.model.qubit.QubitRegister;

public class GateResolver {

    public static Gate resolveByName(String name, QubitRegister register, Integer[] indices) {
        if (name.startsWith("C")) {
            String baseGateName = name.substring(1);
            Integer[] targetIndices = new Integer[indices.length - 1];
            Integer[] controlIndices = new Integer[]{indices[0]};
            System.arraycopy(indices, 1, targetIndices, 0, indices.length - 1);


            Gate baseGate = resolveByName(baseGateName, register, targetIndices);
            if (baseGate == null) {
                return null;
            }
            return new ControlledGate(register, baseGate, controlIndices);
        }

        return switch (name) {
            case "X", "NOT" -> new X(register, indices);
            case "Y" -> new Y(register, indices);
            case "Z" -> new Z(register, indices);
            case "H" -> new H(register, indices);
            case "S" -> new S(register, indices);
            case "T" -> new T(register, indices);
            case "SWAP" -> new SWAP(register, indices);
            case "INC" -> new Increment(register, indices, true);
            case "DEC" -> new Increment(register, indices, false);
            default -> null;
        };
    }
}
