package org.example.model.gate;

import org.example.model.qubit.Complex;
import org.example.model.qubit.Qubit;
import org.example.model.qubit.QubitRegister;

import java.util.BitSet;

public class SWAP extends Gate{
    public SWAP(QubitRegister register, Integer[] targetQubitsIncices)  {
        super(register, targetQubitsIncices);
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer id1 = targetQubitsIndices[0];
        Integer id2 = targetQubitsIndices[1];
        for (int i = oldState.nextSetBit(0); i >= 0 ; i = oldState.nextSetBit(i+1)) {
            if((i>>id1)%2==(i>>id2)%2) {
                addAmplitude(i, i, oldAmplitudes[i]);
            } else {
                addAmplitude(i, i^((1<<id1)|(1<<id2)), oldAmplitudes[i]);
            }
        }
        targetRegister.setStates(newState);
        targetRegister.setAmplitudes(newAmplitudes);
        return this.trace;
    }

    @Override
    public String toPlatformCode(String platform) {
        //todo
        return "NOT IMPL";
    }
}
