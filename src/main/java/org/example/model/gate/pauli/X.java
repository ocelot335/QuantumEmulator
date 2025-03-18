package org.example.model.gate.pauli;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class X extends Gate {

    public X(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        return new Pair[]{new Pair<>(state ^ (1<<targetQubitsIndices[0]), Complex.getOne())};
    }

    @Override
    public String toString() {
        return "X";
    }
}
