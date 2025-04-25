package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

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
    public String toString() {
        return "H";
    }
}
