package org.example.model.gate.pauli;

import org.example.model.gate.Gate;
import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.BitSet;

public class Z extends Gate {
    public Z(QubitRegister register, Integer[] targetQubitsIncices)  {
        super(register, targetQubitsIncices);
    }

    @Override
    public GateTrace apply() {
        BitSet oldState = targetRegister.getStates();
        Complex[] oldAmplitudes = targetRegister.getAmplitudes();

        Integer id = targetQubitsIndices[0];
        for (int i = oldState.nextSetBit(0); i >= 0 ; i = oldState.nextSetBit(i+1)) {
            if((i>>id)%2==0) {
                addAmplitude(i, i, oldAmplitudes[i]);
            } else {
                addAmplitude(i, i, oldAmplitudes[i].multiply(new Complex(-1)));
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
