package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class SWAPTest {
    private static final double DELTA = 1e-9;

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regSWAP", 3); // 3 qubits

        // --- Test SWAP(0, 1) ---
        Integer[] targets01 = {0, 1};

        // Input |000> (state 0) -> Q0=0, Q1=0 (same) -> output |000> (state 0)
        SWAP swap01_state0 = new SWAP(reg, targets01);
        Pair<Integer, Complex>[] result000_01 = swap01_state0.getTosAndItsCoefs(0);
        assertEquals(1, result000_01.length);
        assertEquals(0, result000_01[0].getKey());
        assertTrue(Complex.getOne().equals(result000_01[0].getValue()));

        // Input |010> (state 2) -> Q0=0, Q1=1 (different) -> output |001> (state 1)
        SWAP swap01_state2 = new SWAP(reg, targets01);
        Pair<Integer, Complex>[] result010_01 = swap01_state2.getTosAndItsCoefs(2);
        assertEquals(1, result010_01.length);
        assertEquals(1, result010_01[0].getKey()); // Corrected expectation: 1
        assertTrue(Complex.getOne().equals(result010_01[0].getValue()));

        // Input |100> (state 4) -> Q0=0, Q1=0 (same) -> output |100> (state 4)
        SWAP swap01_state4 = new SWAP(reg, targets01);
        Pair<Integer, Complex>[] result100_01 = swap01_state4.getTosAndItsCoefs(4);
        assertEquals(1, result100_01.length);
        assertEquals(4, result100_01[0].getKey()); // Corrected expectation: 4
        assertTrue(Complex.getOne().equals(result100_01[0].getValue()));

        // Input |110> (state 6) -> Q0=0, Q1=1 (different) -> output |101> (state 5)
        SWAP swap01_state6 = new SWAP(reg, targets01);
        Pair<Integer, Complex>[] result110_01 = swap01_state6.getTosAndItsCoefs(6);
        assertEquals(1, result110_01.length);
        assertEquals(5, result110_01[0].getKey()); // Corrected expectation: 5
        assertTrue(Complex.getOne().equals(result110_01[0].getValue()));

        // --- Test SWAP(1, 2) ---
        Integer[] targets12 = {1, 2};

        // Input |101> (state 5) -> Q1=0, Q2=1 (different) -> output |011> (state 3)
        SWAP swap12_state5 = new SWAP(reg, targets12);
        Pair<Integer, Complex>[] result101_12 = swap12_state5.getTosAndItsCoefs(5);
        assertEquals(1, result101_12.length);
        assertEquals(3, result101_12[0].getKey()); // Corrected expectation: 3
        assertTrue(Complex.getOne().equals(result101_12[0].getValue()));

        // Input |011> (state 3) -> Q1=1, Q2=0 (different) -> output |101> (state 5)
        SWAP swap12_state3 = new SWAP(reg, targets12);
        Pair<Integer, Complex>[] result011_12 = swap12_state3.getTosAndItsCoefs(3);
        assertEquals(1, result011_12.length);
        assertEquals(5, result011_12[0].getKey()); // Corrected expectation: 5
        assertTrue(Complex.getOne().equals(result011_12[0].getValue()));

        // Input |001> (state 1) -> Q1=0, Q2=0 (same) -> output |001> (state 1)
        SWAP swap12_state1 = new SWAP(reg, targets12);
        Pair<Integer, Complex>[] result001_12 = swap12_state1.getTosAndItsCoefs(1);
        assertEquals(1, result001_12.length);
        assertEquals(1, result001_12[0].getKey()); // Corrected expectation: 1
        assertTrue(Complex.getOne().equals(result001_12[0].getValue()));

         // Input |111> (state 7) -> Q1=1, Q2=1 (same) -> output |111> (state 7)
        SWAP swap12_state7 = new SWAP(reg, targets12);
        Pair<Integer, Complex>[] result111_12 = swap12_state7.getTosAndItsCoefs(7);
        assertEquals(1, result111_12.length);
        assertEquals(7, result111_12[0].getKey());
        assertTrue(Complex.getOne().equals(result111_12[0].getValue()));
    }

    @Test
    void testApplySWAPGate() {
        // Initial state |01>
        QubitRegister reg = new QubitRegister("regApplySWAP", 2);
        reg.getStates().clear(0);
        reg.getAmplitudes().set(0, Complex.getZero());
        reg.getStates().set(1);
        reg.getAmplitudes().set(1, Complex.getOne());

        // Apply SWAP(0, 1): |01> -> |10>
        SWAP swapGate = new SWAP(reg, new Integer[]{0, 1});
        swapGate.apply();

        BitSet state1 = reg.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(2)); // State should be |10> (index 2)
        assertTrue(Complex.getOne().equals(reg.getAmplitudes().get(2)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        swapGate = new SWAP(reg, new Integer[]{0, 1});
        swapGate.apply(); // Use the same gate instance

        BitSet state2 = reg.getStates();
        assertEquals(1, state2.cardinality());
        assertTrue(state2.get(1)); // State should be |01> (index 1)
        assertTrue(Complex.getOne().equals(reg.getAmplitudes().get(1)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testApplySWAPGateOnSuperposition() {
        // Initial state: (|01> + |10>) / sqrt(2)
        QubitRegister reg = new QubitRegister("regSWAPSuper", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getStates().clear();
        reg.getAmplitudes().set(0, Complex.getZero());
        reg.getStates().set(1); // |01>
        reg.getAmplitudes().set(1, amp);
        reg.getStates().set(2); // |10>
        reg.getAmplitudes().set(2, amp);

        // Apply SWAP(0, 1)
        // |01> -> |10>
        // |10> -> |01>
        // Result should be the same state: (|10> + |01>) / sqrt(2)
        SWAP swapGate = new SWAP(reg, new Integer[]{0, 1});
        swapGate.apply();

        BitSet finalState = reg.getStates();
        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(1)); // |01>
        assertTrue(finalState.get(2)); // |10>
        assertTrue(amp.equals(reg.getAmplitudes().get(1)));
        assertTrue(amp.equals(reg.getAmplitudes().get(2)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    // Helper from QubitRegisterTest
    private double calculateTotalProbability(QubitRegister register) {
        double totalProb = 0;
        BitSet states = register.getStates();
        ChunkedComplexArray amplitudes = register.getAmplitudes();
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            totalProb += amplitudes.get(i).modulusSquared();
        }
        return totalProb;
    }
} 