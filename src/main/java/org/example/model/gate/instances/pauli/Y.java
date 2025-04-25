package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class Y extends Gate {
    public Y(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        if ((state >> targetQubitsIndices[0]) % 2 == 0) {
            return new Pair[]{new Pair<>(state ^ (1 << targetQubitsIndices[0]), new Complex(0, 1))};
        } else {
            return new Pair[]{new Pair<>(state ^ (1 << targetQubitsIndices[0]), new Complex(0, -1))};
        }
    }

    @Override
    public String toString() {
        return "Y";
    }
}
