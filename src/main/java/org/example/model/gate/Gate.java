package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.io.Serializable;
import java.util.BitSet;

public abstract class Gate implements Serializable {
    protected final Integer[] targetQubitsIndices;
    protected final QubitRegister targetRegister;
    protected final GateTrace trace;
    protected final BitSet newState;
    protected final ChunkedComplexArray newAmplitudes;

    protected Gate(QubitRegister register, Integer[] targetQubitsIndices) {
        this.targetQubitsIndices = targetQubitsIndices;
        this.targetRegister = register;
        this.trace = new GateTrace();
        this.newState = new BitSet(register.getStates().length());
        this.newAmplitudes = new ChunkedComplexArray();
    }

    protected void addAmplitude(Integer from, Integer to, Complex amplitude) {
        if (amplitude.equals(Complex.getZero())) {
            return;
        }

        if (!newState.get(to)) {
            newState.set(to, true);
            newAmplitudes.set(to, amplitude);
        } else {
            newAmplitudes.set(to, newAmplitudes.get(to).add(amplitude));
            if (newAmplitudes.get(to).equals(Complex.getZero())) {
                newState.set(to, false);
            }
        }

        //add trace
        trace.addAmplitude(from, to, amplitude);
    }

    public abstract Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state);

    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        ChunkedComplexArray oldAmplitudes = targetRegister.getAmplitudes();

        for (int i = oldState.nextSetBit(0); i >= 0; i = oldState.nextSetBit(i + 1)) {
            for (Pair<Integer, Complex> toWithCoef : getTosAndItsCoefs(i)) {
                addAmplitude(i, toWithCoef.getKey(), oldAmplitudes.get(i).multiply(toWithCoef.getValue()));
            }
        }
        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }
}
