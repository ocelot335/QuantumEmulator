package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class ControlledGate extends Gate {
    private final Gate gateToControl;
    private final Integer[] controlQubitsIndices;

    public ControlledGate(QubitRegister register, Gate gateToControl, Integer[] controlQubitsIndices) {
        super(register, gateToControl.targetQubitsIndices);
        this.gateToControl = gateToControl;
        this.controlQubitsIndices = controlQubitsIndices;
    }

    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        if ((state >> controlQubitsIndices[0]) % 2 == 1) {
            return gateToControl.getTosAndItsCoefs(state);
        } else {
            return new Pair[]{new Pair<>(state, Complex.getOne())};
        }
    }

    @Override
    public String toString() {
        return "C" + gateToControl.toString();
    }
}
