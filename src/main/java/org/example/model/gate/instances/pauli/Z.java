package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class Z extends Gate {
    public Z(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        if ((state >> targetQubitsIndices[0]) % 2 == 0) {
            return new Pair[]{new Pair<>(state, Complex.getOne())};
        } else {
            return new Pair[]{new Pair<>(state, new Complex(-1))};
        }
    }

    @Override
    public String toString() {
        return "Z";
    }
}
