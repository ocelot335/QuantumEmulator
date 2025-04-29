package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class XTest {
    private static final double DELTA = 1e-9;

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regX", 2); // 2 qubits

        // Test X on Qubit 0
        // Input |00> (state 0)
        X xGate0_state0 = new X(reg, new Integer[]{0});
        Pair<Integer, Complex>[] result0_q0 = xGate0_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q0.length);
        assertEquals(1, result0_q0[0].getKey()); // 0 -> 1 (00 -> 01)
        assertTrue(Complex.getOne().equals(result0_q0[0].getValue()));

        // Input |01> (state 1)
        X xGate0_state1 = new X(reg, new Integer[]{0});
        Pair<Integer, Complex>[] result1_q0 = xGate0_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_q0.length);
        assertEquals(0, result1_q0[0].getKey()); // 1 -> 0 (01 -> 00)
        assertTrue(Complex.getOne().equals(result1_q0[0].getValue()));

        // Test X on Qubit 1
        // Input |00> (state 0)
        X xGate1_state0 = new X(reg, new Integer[]{1});
        Pair<Integer, Complex>[] result0_q1 = xGate1_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q1.length);
        assertEquals(2, result0_q1[0].getKey()); // 0 -> 2 (00 -> 10)
        assertTrue(Complex.getOne().equals(result0_q1[0].getValue()));

        // Input |01> (state 1)
        X xGate1_state1 = new X(reg, new Integer[]{1});
        Pair<Integer, Complex>[] result1_q1 = xGate1_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_q1.length);
        assertEquals(3, result1_q1[0].getKey()); // 1 -> 3 (01 -> 11)
        assertTrue(Complex.getOne().equals(result1_q1[0].getValue()));

        // Input |10> (state 2)
        X xGate1_state2 = new X(reg, new Integer[]{1});
        Pair<Integer, Complex>[] result2_q1 = xGate1_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q1.length);
        assertEquals(0, result2_q1[0].getKey()); // 2 -> 0 (10 -> 00) - Ошибка в оригинальном коде, X(1) на |10> должен дать |00>
        assertTrue(Complex.getOne().equals(result2_q1[0].getValue()));

        // Input |11> (state 3)
        X xGate1_state3 = new X(reg, new Integer[]{1});
        Pair<Integer, Complex>[] result3_q1 = xGate1_state3.getTosAndItsCoefs(3);
        assertEquals(1, result3_q1.length);
        assertEquals(1, result3_q1[0].getKey()); // 3 -> 1 (11 -> 01) - Ошибка в оригинальном коде, X(1) на |11> должен дать |01>
        assertTrue(Complex.getOne().equals(result3_q1[0].getValue()));
    }

    @Test
    void testApplyXGate() {
        // Test X on qubit 0: |00> -> |01>
        QubitRegister reg1 = new QubitRegister("regX1", 2);
        X xGate1 = new X(reg1, new Integer[]{0});
        xGate1.apply();

        BitSet state1 = reg1.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(1)); // State should be |01> (index 1)
        assertTrue(Complex.getOne().equals(reg1.getAmplitudes().get(1)));
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // Test X on qubit 1: |01> -> |11>
        X xGate2 = new X(reg1, new Integer[]{1});
        xGate2.apply();

        BitSet state2 = reg1.getStates();
        assertEquals(1, state2.cardinality());
        assertTrue(state2.get(3)); // State should be |11> (index 3)
        assertTrue(Complex.getOne().equals(reg1.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // Test X on qubit 0 again: |11> -> |10>
        X xGate3 = new X(reg1, new Integer[]{0});
        xGate3.apply();

        BitSet state3 = reg1.getStates();
        assertEquals(1, state3.cardinality());
        assertTrue(state3.get(2)); // State should be |10> (index 2)
        assertTrue(Complex.getOne().equals(reg1.getAmplitudes().get(2)));
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);
    }

    @Test
    void testApplyXGateOnSuperposition() {
        // Initial state: |+0> = (|00> + |10>) / sqrt(2)
        QubitRegister reg = new QubitRegister("regXSuper", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getAmplitudes().set(0, amp); // |00>
        reg.getAmplitudes().set(2, amp); // |10>
        reg.getStates().set(0);
        reg.getStates().set(2);

        // Apply X on qubit 0: |+0> -> |+1> = (|01> + |11>) / sqrt(2)
        X xGate = new X(reg, new Integer[]{0});
        xGate.apply();

        BitSet finalState = reg.getStates();
        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(1)); // |01>
        assertTrue(finalState.get(3)); // |11>
        assertTrue(amp.equals(reg.getAmplitudes().get(1)));
        assertTrue(amp.equals(reg.getAmplitudes().get(3)));
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