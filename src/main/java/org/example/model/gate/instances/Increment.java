package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class Increment extends Gate {
    private final boolean increment;

    public Increment(QubitRegister register, Integer[] targetQubitsIncices, boolean increment) {
        super(register, targetQubitsIncices);
        this.increment = increment;
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        int numOfStates = 1 << targetRegister.size();
        if (increment) {
            return new Pair[]{new Pair<>((state + 1) % numOfStates, Complex.getOne())};
        } else {
            return new Pair[]{new Pair<>((state + -1 + numOfStates) % numOfStates, Complex.getOne())};
        }
    }

    @Override
    public String toString() {
        if (this.increment) {
            return "INC";
        } else {
            return "DEC";
        }
    }
}
