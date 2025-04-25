package org.example.model.gate.oracle;

import javafx.util.Pair;
import org.example.model.gate.Gate;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OracleGate extends Gate {

    private final String oracleName;
    private final int inputOffset;
    private final int inputSize;
    private final int ancillaIndex;
    private final Set<Integer> markedStates;


    public OracleGate(String oracleName, QubitRegister register, int inputOffset, int inputSize, int ancillaIndex, Set<Integer> markedStates) {
        super(register, combineIndices(inputOffset, inputSize, ancillaIndex));
        this.oracleName = oracleName;
        this.inputOffset = inputOffset;
        this.inputSize = inputSize;
        this.ancillaIndex = ancillaIndex;
        this.markedStates = markedStates;
    }

    private static Integer[] combineIndices(int offset, int size, int ancilla) {
        List<Integer> indices = new ArrayList<>(size + 1);
        for (int i = 0; i < size; i++) {
            indices.add(offset + i);
        }
        indices.add(ancilla);

        return indices.toArray(new Integer[0]);
    }

    @Override
    public Pair<Integer, Complex>[] getTosAndItsCoefs(Integer state) {
        int inputValue = 0;
        for (int i = 0; i < inputSize; i++) {
            int absIndex = this.inputOffset + i;
            if (((state >> absIndex) & 1) == 1) {
                inputValue |= (1 << i);//TODO: or (1 << (inputSize - 1 - i))??
            }
        }

        int ancillaValue = (state >> ancillaIndex) & 1;

        int fxValue = markedStates.contains(inputValue) ? 1 : 0;

        int newAncillaValue = ancillaValue ^ fxValue;

        int newState = state;
        if (ancillaValue != newAncillaValue) {
            newState = state ^ (1 << ancillaIndex);
        }

        @SuppressWarnings("unchecked")
        Pair<Integer, Complex>[] result = new Pair[]{new Pair<>(newState, Complex.getOne())};
        return result;
    }

    @Override
    public String toString() {
        return this.oracleName;
    }
} 