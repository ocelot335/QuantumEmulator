package org.example.model.gate.instances.pauli;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class YTest {
    private static final double DELTA = 1e-9;
    private final Complex I = new Complex(0, 1);
    private final Complex NEG_I = new Complex(0, -1);

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regY", 2);
        Integer[] target = {0};
        Integer[] target1 = {1};

        // --- Target Qubit 0 ---
        // Input |00> (state 0): qubit 0 is 0 -> Y|0> = i|1>. State 00 -> 01 (1). Coef i.
        Y yGate0_state0 = new Y(reg, target);
        Pair<Integer, Complex>[] result0_q0 = yGate0_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q0.length);
        assertEquals(1, result0_q0[0].getKey());
        assertTrue(I.equals(result0_q0[0].getValue()));

        // Input |01> (state 1): qubit 0 is 1 -> Y|1> = -i|0>. State 01 -> 00 (0). Coef -i.
        Y yGate0_state1 = new Y(reg, target);
        Pair<Integer, Complex>[] result1_q0 = yGate0_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_q0.length);
        assertEquals(0, result1_q0[0].getKey());
        assertTrue(NEG_I.equals(result1_q0[0].getValue()));

        // Input |10> (state 2): qubit 0 is 0 -> Y|0> = i|1>. State 10 -> 11 (3). Coef i.
        Y yGate0_state2 = new Y(reg, target);
        Pair<Integer, Complex>[] result2_q0 = yGate0_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q0.length);
        assertEquals(3, result2_q0[0].getKey());
        assertTrue(I.equals(result2_q0[0].getValue()));

        // Input |11> (state 3): qubit 0 is 1 -> Y|1> = -i|0>. State 11 -> 10 (2). Coef -i.
        Y yGate0_state3 = new Y(reg, target);
        Pair<Integer, Complex>[] result3_q0 = yGate0_state3.getTosAndItsCoefs(3);
        assertEquals(1, result3_q0.length);
        assertEquals(2, result3_q0[0].getKey());
        assertTrue(NEG_I.equals(result3_q0[0].getValue()));

         // --- Target Qubit 1 (similar logic) ---
        // Input |00> (state 0): qubit 1 is 0 -> State 00 -> 10 (2). Coef i.
        Y yGate1_state0 = new Y(reg, target1);
        Pair<Integer, Complex>[] result0_q1 = yGate1_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q1.length);
        assertEquals(2, result0_q1[0].getKey());
        assertTrue(I.equals(result0_q1[0].getValue()));

        // Input |10> (state 2): qubit 1 is 1 -> State 10 -> 00 (0). Coef -i.
        Y yGate1_state2 = new Y(reg, target1);
        Pair<Integer, Complex>[] result2_q1 = yGate1_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q1.length);
        assertEquals(0, result2_q1[0].getKey());
        assertTrue(NEG_I.equals(result2_q1[0].getValue()));
    }

    @Test
    void testApplyYGate() {
        // Test Y on qubit 0: |00> -> i|01>
        QubitRegister reg1 = new QubitRegister("regY1", 2);
        Y yGate1 = new Y(reg1, new Integer[]{0});
        yGate1.apply();

        BitSet state1 = reg1.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(1)); // State |01>
        assertTrue(I.equals(reg1.getAmplitudes().get(1)));
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // Apply Y again: i|01> -> i * Y|01> = i * (-i|00>) = |00>
        Y yGate2 = new Y(reg1, new Integer[]{0});
        yGate2.apply();

        BitSet state2 = reg1.getStates();
        assertEquals(1, state2.cardinality());
        assertTrue(state2.get(0)); // State |00>
        assertTrue(Complex.getOne().equals(reg1.getAmplitudes().get(0)));
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);
    }

    @Test
    void testApplyYGateOnSuperposition() {
        // Initial state: |+0> = (|00> + |10>) / sqrt(2)
        QubitRegister reg = new QubitRegister("regYSuper", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getAmplitudes().set(0, amp); // |00>
        reg.getAmplitudes().set(2, amp); // |10>
        reg.getStates().set(0);
        reg.getStates().set(2);

        // Apply Y on qubit 0:
        // Y (|00> + |10>) / sqrt(2) = (Y|00> + Y|10>) / sqrt(2)
        // = (i|01> + i|11>) / sqrt(2) = i * (|01> + |11>) / sqrt(2)
        Y yGate = new Y(reg, new Integer[]{0});
        yGate.apply();

        BitSet finalState = reg.getStates();
        Complex expectedAmp = I.multiply(amp); // i / sqrt(2)
        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(1)); // |01>
        assertTrue(finalState.get(3)); // |11>
        assertTrue(expectedAmp.equals(reg.getAmplitudes().get(1)));
        assertTrue(expectedAmp.equals(reg.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
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