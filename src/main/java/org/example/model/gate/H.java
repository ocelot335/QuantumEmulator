package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.BitSet;

public class H extends Gate {
    public H(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    @Override
    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        Complex coef = new Complex(1.0 / Math.sqrt(2));
        if ((state >> targetQubitsIndices[0]) % 2 == 0) {
            return new Pair[]{
                new Pair<>(state, coef),
                new Pair<>(state ^ (1 << targetQubitsIndices[0]), coef)
            };
        } else {
            return new Pair[]{
                new Pair<>(state, coef.multiply(new Complex(-1))),
                new Pair<>(state ^ (1 << targetQubitsIndices[0]), coef)
            };
        }
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer id = targetQubitsIndices[0];
        for (int i = oldState.nextSetBit(0); i >= 0; i = oldState.nextSetBit(i + 1)) {
            if ((i >> id) % 2 == 0) {
                addAmplitude(i, i ^ (1 << id), oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
                addAmplitude(i, i, oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
            } else {
                addAmplitude(i, i ^ (1 << id), oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
                addAmplitude(i, i, oldAmplitudes[i].divide(new Complex(-Math.sqrt(2))));
            }
        }

        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }

    @Override
    public String toString() {
        return "H";
    }
}
