package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.io.Serializable;
import java.util.BitSet;

public abstract class Gate implements Serializable {
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
        if (amplitude.equals(Complex.getZero())) {
            return;
        }

        if (!newState.get(to)) {
            newState.set(to, true);
            newAmplitudes[to] = amplitude;
        } else {
            newAmplitudes[to] = newAmplitudes[to].add(amplitude);
            if (newAmplitudes[to].equals(Complex.getZero())) {
                newState.set(to, false);
                newAmplitudes[to] = null;
            }
        }

        //add trace
        trace.addAmplitude(from, to, amplitude);
    }

    public abstract Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state);

    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        for (int i = oldState.nextSetBit(0); i >= 0; i = oldState.nextSetBit(i + 1)) {
            for (Pair<Integer, Complex> toWithCoef: getTosAndItsCoefs(i)) {
                addAmplitude(i, toWithCoef.getKey(), oldAmplitudes[i].multiply(toWithCoef.getValue()));
            }
        }
        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }
}
