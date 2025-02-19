package org.example.model.gate;

import org.example.model.gate.pauli.X;
import org.example.model.gate.pauli.Y;
import org.example.model.gate.pauli.Z;
import org.example.model.qubit.QubitRegister;

public class GateResolver {

    public static Gate resolveByName(String name, QubitRegister register, Integer[] indices) {
        return switch (name) {
            case "X" -> new X(register, indices);
            case "Y" -> new Y(register, indices);
            case "Z" -> new Z(register, indices);
            case "H" -> new H(register, indices);
            case "SWAP" -> new SWAP(register, indices);
            default -> null;
        };
    }
}
