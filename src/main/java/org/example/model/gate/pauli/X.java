package org.example.model.gate.pauli;

import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;
import org.example.model.qubit.Qubit;
import org.example.model.gate.Gate;
import org.example.model.qubit.QubitRegister;

import java.util.*;

public class X extends Gate {

    public X(QubitRegister register, Integer[] targetQubitsIncices)  {
        super(register, targetQubitsIncices);
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer id = targetQubitsIndices[0];
        for (int i = oldState.nextSetBit(0); i >= 0 ; i = oldState.nextSetBit(i+1)) {
            addAmplitude(i, i^(1<<id), oldAmplitudes[i]);
        }
        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }

    @Override
    public String toPlatformCode(String platform) {
        return switch (platform.toLowerCase()) {
            case "qsharp" -> "X(q" + targetQubitsIndices + ");";
            case "qiskit" -> "qc.x(q[" + targetQubitsIndices + "])";
            case "cirq" -> "cirq.X(q[" + targetQubitsIndices + "])";
            default -> throw new IllegalArgumentException("Unsupported platform: " + platform);
        };
    }
}
