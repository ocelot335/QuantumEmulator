package org.example.model.gate;

import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.*;

public abstract class Gate {
    protected final Integer[] targetQubitsIndices;
    protected final QubitRegister targetRegister;
    protected final GateTrace trace;
    protected final BitSet newState;
    protected final Complex[] newAmplitudes;

    protected Gate(QubitRegister register, Integer[] targetQubitsIndices) {
        this.targetQubitsIndices = targetQubitsIndices;
        this.targetRegister = register;
        this.trace = new GateTrace();
        this.newState = new BitSet(register.getStates().length());
        this.newAmplitudes = new Complex[register.getAmplitudes().length];
    }

    protected void addAmplitude(Integer from, Integer to, Complex amplitude) {
        if(!newState.get(to)) {
            newState.set(to, true);
            newAmplitudes[to] = amplitude;
        } else {
            newAmplitudes[to] = newAmplitudes[to].add(amplitude);
        }

        //add trace
        trace.addAmplitude(from, to, amplitude);
    }

    public abstract GateTrace apply();
    public abstract String toPlatformCode(String platform); // Транслирование гейта в код для платформы
}
