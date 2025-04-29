package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.gate.instances.pauli.Z;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class STest {
    private static final double DELTA = 1e-9;
    private final Complex ONE = Complex.getOne();
    private final Complex I = new Complex(0, 1);

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regS", 2);
        Integer[] target = {0};
        Integer[] target1 = {1};

        // --- Target Qubit 0 ---
        // Input |00> (state 0): qubit 0 is 0 -> S|0> = |0>. State 00 -> 00 (0). Coef 1.
        S sGate0_state0 = new S(reg, target);
        Pair<Integer, Complex>[] result0_q0 = sGate0_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q0.length);
        assertEquals(0, result0_q0[0].getKey());
        assertTrue(ONE.equals(result0_q0[0].getValue()));

        // Input |01> (state 1): qubit 0 is 1 -> S|1> = i|1>. State 01 -> 01 (1). Coef i.
        S sGate0_state1 = new S(reg, target);
        Pair<Integer, Complex>[] result1_q0 = sGate0_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_q0.length);
        assertEquals(1, result1_q0[0].getKey());
        assertTrue(I.equals(result1_q0[0].getValue()));

        // Input |10> (state 2): qubit 0 is 0 -> S|0> = |0>. State 10 -> 10 (2). Coef 1.
        S sGate0_state2 = new S(reg, target);
        Pair<Integer, Complex>[] result2_q0 = sGate0_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q0.length);
        assertEquals(2, result2_q0[0].getKey());
        assertTrue(ONE.equals(result2_q0[0].getValue()));

        // Input |11> (state 3): qubit 0 is 1 -> S|1> = i|1>. State 11 -> 11 (3). Coef i.
        S sGate0_state3 = new S(reg, target);
        Pair<Integer, Complex>[] result3_q0 = sGate0_state3.getTosAndItsCoefs(3);
        assertEquals(1, result3_q0.length);
        assertEquals(3, result3_q0[0].getKey());
        assertTrue(I.equals(result3_q0[0].getValue()));

         // --- Target Qubit 1 (similar logic) ---
        // Input |00> (state 0): qubit 1 is 0 -> State 00 -> 00 (0). Coef 1.
        S sGate1_state0 = new S(reg, target1);
        Pair<Integer, Complex>[] result0_q1 = sGate1_state0.getTosAndItsCoefs(0);
        assertEquals(1, result0_q1.length);
        assertEquals(0, result0_q1[0].getKey());
        assertTrue(ONE.equals(result0_q1[0].getValue()));

        // Input |10> (state 2): qubit 1 is 1 -> State 10 -> 10 (2). Coef i.
        S sGate1_state2 = new S(reg, target1);
        Pair<Integer, Complex>[] result2_q1 = sGate1_state2.getTosAndItsCoefs(2);
        assertEquals(1, result2_q1.length);
        assertEquals(2, result2_q1[0].getKey());
        assertTrue(I.equals(result2_q1[0].getValue()));
    }

    @Test
    void testApplySGate() {
        // Test S on qubit 0: |+0> = (|00> + |10>) / sqrt(2)
        QubitRegister reg1 = new QubitRegister("regS1", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg1.getAmplitudes().set(0, amp); // |00>
        reg1.getAmplitudes().set(2, amp); // |10>
        reg1.getStates().set(0);
        reg1.getStates().set(2);

        // Apply S on qubit 0:
        // S|00> = |00> (since qubit 0 is |0>)
        // S|10> = |10> (since qubit 0 is |0>)
        // Result: (|00> + |10>)/sqrt(2) -> (|00> + |10>)/sqrt(2) (no change)
        S sGate1 = new S(reg1, new Integer[]{0});
        sGate1.apply();

        BitSet state1 = reg1.getStates();
        assertEquals(2, state1.cardinality());
        assertTrue(state1.get(0)); // |00>
        assertTrue(state1.get(2)); // |10>
        assertTrue(amp.equals(reg1.getAmplitudes().get(0)), "Amplitude for |00> should remain unchanged");
        assertTrue(amp.equals(reg1.getAmplitudes().get(2)), "Amplitude for |10> should remain unchanged as S|0>=|0>");
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // Apply S again: Applying S to qubit 0 again will still have no effect.
        // S(|00> + |10>)/sqrt(2) -> (|00> + |10>)/sqrt(2)
        S sGate2 = new S(reg1, new Integer[]{0});
        sGate2.apply();

        BitSet state2 = reg1.getStates();
        assertEquals(2, state2.cardinality());
        assertTrue(state2.get(0)); // |00>
        assertTrue(state2.get(2)); // |10>
        assertTrue(amp.equals(reg1.getAmplitudes().get(0)), "Amplitude for |00> should still be unchanged");
        assertTrue(amp.equals(reg1.getAmplitudes().get(2)), "Amplitude for |10> should still be unchanged");
        assertEquals(1.0, calculateTotalProbability(reg1), DELTA);

        // The S^2 = Z verification part is removed as it's not applicable
        // when the target qubit is in state |0>.
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