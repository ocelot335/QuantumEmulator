package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class X extends Gate {

    public X(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        return new Pair[]{new Pair<>(state ^ (1 << targetQubitsIndices[0]), Complex.getOne())};
    }

    @Override
    public String toString() {
        return "X";
    }
}
