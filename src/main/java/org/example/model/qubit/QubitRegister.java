package org.example.model.qubit;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Random;

public class QubitRegister implements Serializable {
    private final int numQubits;

    @Getter
    @Setter
    private BitSet states;

    @Getter
    @Setter
    private Complex[] amplitudes;

    private final String name;
    private final Random random;

    public QubitRegister(String name, int numQubits) {
        this.name = name;
        this.numQubits = numQubits;
        states = new BitSet(1 << numQubits);
        states.set(0, true);
        amplitudes = new Complex[1 << numQubits];
        for (int i = 0; i < 1 << numQubits; i++) {
            amplitudes[i] = Complex.getZero();
        }
        amplitudes[0] = Complex.getOne();
        this.random = new Random();
    }

    public int size() {
        return numQubits;
    }

    public Integer sampleQubit(int index) {
        double zeroProb = 0;
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            if ((i >> index) % 2 == 0) {
                zeroProb += amplitudes[i].modulusSquared();
            }
        }


        if (random.nextDouble() <= zeroProb) {
            return 0;
        } else {
            return 1;
        }
    }

    public Integer measureQubit(int index) {
        double zeroProb = 0;
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            if ((i >> index) % 2 == 0) {
                zeroProb += amplitudes[i].modulusSquared();
            }
        }

        //TODO
        if (random.nextDouble() <= zeroProb) {
            return 0;
        } else {
            return 1;
        }
    }

    public String getName() {
        return name;
    }

    public BitSet getStates() {
        return states;
    }

    public void setStates(BitSet states) {
        this.states = states;
    }

    public Complex[] getAmplitudes() {
        return amplitudes;
    }

    public void setAmplitudes(Complex[] amplitudes) {
        this.amplitudes = amplitudes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            if (amplitudes[i] != null) {
                String binaryState = String.format("%" + numQubits + "s", 
                    Integer.toBinaryString(i)).replace(' ', '0');
                sb.append("|").append(binaryState).append(">: ")
                  .append(amplitudes[i]).append("\n");
            }
        }
        return sb.toString();
    }
}
