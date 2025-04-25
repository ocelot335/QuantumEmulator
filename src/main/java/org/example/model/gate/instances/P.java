package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class P extends Gate {
    private final double phase;

    public P(QubitRegister register, double phase, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
        this.phase = phase;
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        if ((state >> targetQubitsIndices[0]) % 2 == 0) {
            return new Pair[]{new Pair<>(state, Complex.getOne())};
        } else {
            return new Pair[]{new Pair<>(state, Complex.expI(phase))};
        }
    }

    @Override
    public String toString() {
        return "P";
    }
}
