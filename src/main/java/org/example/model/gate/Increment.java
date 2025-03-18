package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.BitSet;

public class Increment extends Gate {
    private final boolean increment;

    public Increment(QubitRegister register, Integer[] targetQubitsIncices, boolean increment) {
        super(register, targetQubitsIncices);
        this.increment = increment;
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        return null;
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer numOfStates = 1 << targetRegister.size();
        for (int i = oldState.nextSetBit(0); i >= 0; i = oldState.nextSetBit(i + 1)) {
            if (increment) {
                addAmplitude(i, (i + 1) % numOfStates, oldAmplitudes[i]);
            } else {
                addAmplitude(i, (i - 1 + numOfStates) % numOfStates, oldAmplitudes[i]);
            }
        }

        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }

    @Override
    public String toString() {
        if(this.increment) {
            return "INC";
        } else {
            return "DEC";
        }
    }
}
