package org.example.model.gate;

import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

public class H extends Gate {
    public H(QubitRegister register, Integer[] targetQubitsIncices)  {
        super(register, targetQubitsIncices);
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer id = targetQubitsIndices[0];
        for (int i = oldState.nextSetBit(0); i >= 0 ; i = oldState.nextSetBit(i+1)) {
            if((i>>id)%2==0) {
                addAmplitude(i, i^(1<<id), oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
                addAmplitude(i, i, oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
            } else {
                addAmplitude(i, i^(1<<id), oldAmplitudes[i].divide(new Complex(Math.sqrt(2))));
                addAmplitude(i, i, oldAmplitudes[i].divide(new Complex(-Math.sqrt(2))));
            }
        }

        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }

    @Override
    public String toPlatformCode(String platform) {
        //TODO
        return "";
    }
}
