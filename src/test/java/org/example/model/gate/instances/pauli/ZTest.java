package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class ZTest {
    private static final double DELTA = 1e-9;
    private final Complex ONE = Complex.getOne();
    private final Complex NEG_ONE = new Complex(-1);

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regZ", 2);
        Integer[] target = {0};
        Integer[] target1 = {1};

        // --- Target Qubit 0 ---
        // Input |00> (state 0): qubit 0 is 0 -> Z|0> = |0>. State 00 -> 00 (0). Coef 1.
        Z zGate0_state0 = new Z(reg, target);
        Pair<Integer, Complex>[] result0_q0 = zGate0_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q0.length);
        assertEquals(0, result0_q0[0].getKey());
        assertTrue(ONE.equals(result0_q0[0].getValue()));

        // Input |01> (state 1): qubit 0 is 1 -> Z|1> = -|1>. State 01 -> 01 (1). Coef -1.
        Z zGate0_state1 = new Z(reg, target);
        Pair<Integer, Complex>[] result1_q0 = zGate0_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_q0.length);
        assertEquals(1, result1_q0[0].getKey());
        assertTrue(NEG_ONE.equals(result1_q0[0].getValue()));

        // Input |10> (state 2): qubit 0 is 0 -> Z|0> = |0>. State 10 -> 10 (2). Coef 1.
        Z zGate0_state2 = new Z(reg, target);
        Pair<Integer, Complex>[] result2_q0 = zGate0_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q0.length);
        assertEquals(2, result2_q0[0].getKey());
        assertTrue(ONE.equals(result2_q0[0].getValue()));

        // Input |11> (state 3): qubit 0 is 1 -> Z|1> = -|1>. State 11 -> 11 (3). Coef -1.
        Z zGate0_state3 = new Z(reg, target);
        Pair<Integer, Complex>[] result3_q0 = zGate0_state3.getTosAndItsCoefs(3);
        assertEquals(1, result3_q0.length);
        assertEquals(3, result3_q0[0].getKey());
        assertTrue(NEG_ONE.equals(result3_q0[0].getValue()));

        // --- Target Qubit 1 (similar logic) ---
        // Input |00> (state 0): qubit 1 is 0 -> State 00 -> 00 (0). Coef 1.
        Z zGate1_state0 = new Z(reg, target1);
        Pair<Integer, Complex>[] result0_q1 = zGate1_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q1.length);
        assertEquals(0, result0_q1[0].getKey());
        assertTrue(ONE.equals(result0_q1[0].getValue()));

        // Input |10> (state 2): qubit 1 is 1 -> State 10 -> 10 (2). Coef -1.
        Z zGate1_state2 = new Z(reg, target1);
        Pair<Integer, Complex>[] result2_q1 = zGate1_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q1.length);
        assertEquals(2, result2_q1[0].getKey());
        assertTrue(NEG_ONE.equals(result2_q1[0].getValue()));
    }

    @Test
    void testApplyZGate() {
        // Test Z on qubit 0: |+0> = (|00> + |10>) / sqrt(2)
        QubitRegister reg1 = new QubitRegister("regZ1", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg1.getAmplitudes().set(0, amp); // |00>
        reg1.getAmplitudes().set(2, amp); // |10>
        reg1.getStates().set(0);
        reg1.getStates().set(2);

        // Apply Z on qubit 0:
        // Z|00> = |00> (since qubit 0 is |0>)
        // Z|10> = |10> (since qubit 0 is |0>)
        // Result: (|00> + |10>)/sqrt(2) -> (|00> + |10>)/sqrt(2) (no change)
        Z zGate1 = new Z(reg1, new Integer[]{0});
        zGate1.apply();

        BitSet state1 = reg1.getStates();
        assertEquals(2, state1.cardinality());
        assertTrue(state1.get(0)); // |00>
        assertTrue(state1.get(2)); // |10>
        assertTrue(amp.equals(reg1.getAmplitudes().get(0)), "Amplitude for |00> should remain unchanged");
        assertTrue(amp.equals(reg1.getAmplitudes().get(2)), "Amplitude for |10> should remain unchanged as Z|0>=|0>");
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // Apply Z again: Applying Z to qubit 0 again will still have no effect.
        // Z(|00> + |10>)/sqrt(2) -> (|00> + |10>)/sqrt(2)
        Z zGate2 = new Z(reg1, new Integer[]{0});
        zGate2.apply();

        BitSet state2 = reg1.getStates();
        assertEquals(2, state2.cardinality());
        assertTrue(state2.get(0)); // |00>
        assertTrue(state2.get(2)); // |10>
        assertTrue(amp.equals(reg1.getAmplitudes().get(0)), "Amplitude for |00> should still be unchanged");
        assertTrue(amp.equals(reg1.getAmplitudes().get(2)), "Amplitude for |10> should still be unchanged");
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);
    }

     @Test
    void testApplyZGateOnBasis() {
        // Test Z on qubit 0: |01> -> -|01>
        QubitRegister reg1 = new QubitRegister("regZBasis", 2);
        reg1.getStates().clear(0); // Start in |01> (state 1)
        reg1.getAmplitudes().set(0, Complex.getZero());
        reg1.getStates().set(1);
        reg1.getAmplitudes().set(1, ONE);

        Z zGate1 = new Z(reg1, new Integer[]{0});
        zGate1.apply();

        BitSet state1 = reg1.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(1)); // State still |01>
        assertTrue(NEG_ONE.equals(reg1.getAmplitudes().get(1))); // Amplitude is -1
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);
     }

    // Helper
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