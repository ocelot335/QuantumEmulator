package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class SWAP extends Gate {
    public SWAP(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    @Override
    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        Integer id1 = targetQubitsIndices[0];
        Integer id2 = targetQubitsIndices[1];
        if ((state >> id1) % 2 == (state >> id2) % 2) {
            return new Pair[]{new Pair<>(state, Complex.getOne())};
        } else {
            return new Pair[]{new Pair<>(state ^ ((1 << id1) | (1 << id2)), Complex.getOne())};
        }
    }

    @Override
    public String toString() {
        return "SWAP";
    }
}
