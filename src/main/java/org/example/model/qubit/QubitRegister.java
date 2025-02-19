package org.example.model.qubit;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;

public class QubitRegister implements Serializable {
    private final int numQubits;
    private final Qubit[] qubits;

    @Getter
    @Setter
    private BitSet states;

    @Getter
    @Setter
    private Complex[] amplitudes;


    private final String name;

    public QubitRegister(String name, int numQubits) {
        this.name = name;
        this.numQubits = numQubits;
        this.qubits = new Qubit[numQubits];
        for (int i = 0; i <numQubits; i++) {
            this.qubits[i] = new Qubit(this, name, i);
        }
        states = new BitSet(1<<numQubits);
        states.set(0,true);
        amplitudes = new Complex[1<<numQubits];
        for (int i = 0; i < 1<<numQubits; i++) {
            amplitudes[i] = Complex.getZero();
        }
        amplitudes[0] = Complex.getOne();
    }

    public int size() {
        return numQubits;
    }

    public Qubit getQubit(int index) {
        return qubits[index];
    }
}
