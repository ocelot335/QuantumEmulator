package org.example.model.gate;

import javafx.util.Pair;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

public class T extends Gate{
    public T(QubitRegister register, Integer[] targetQubitsIncices) {
        super(register, targetQubitsIncices);
    }


    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        if((state>>targetQubitsIndices[0])%2==0) {
            return new Pair[]{new Pair<>(state, Complex.getOne())};
        } else {
            return new Pair[]{new Pair<>(state, new Complex(1/Math.sqrt(2),1/Math.sqrt(2)))};
        }
    }

    @Override
    public String toString() {
        return "T";
    }
}
