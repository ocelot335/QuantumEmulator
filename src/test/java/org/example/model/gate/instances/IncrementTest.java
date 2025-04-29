package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class IncrementTest {
    private static final double DELTA = 1e-9;
    private final Complex ONE = Complex.getOne();

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regIncDec", 3); // 3 qubits, size 8
        Integer[] dummyTarget = {}; // Target qubits not used by Increment

        // --- Increment --- (increment=true)
        Increment incGate = new Increment(reg, dummyTarget, true);

        // Input |000> (state 0) -> Output |001> (state 1), Coef 1
        Pair<Integer, Complex>[] inc_res0 = incGate.getTosAndItsCoefs(0);
        assertEquals(1, inc_res0.length);
        assertEquals(1, inc_res0[0].getKey());
        assertTrue(ONE.equals(inc_res0[0].getValue()));

        // Input |011> (state 3) -> Output |100> (state 4), Coef 1
        Pair<Integer, Complex>[] inc_res3 = incGate.getTosAndItsCoefs(3);
        assertEquals(1, inc_res3.length);
        assertEquals(4, inc_res3[0].getKey());
        assertTrue(ONE.equals(inc_res3[0].getValue()));

        // Input |111> (state 7) -> Output |000> (state 0) (wrap around), Coef 1
        Pair<Integer, Complex>[] inc_res7 = incGate.getTosAndItsCoefs(7);
        assertEquals(1, inc_res7.length);
        assertEquals(0, inc_res7[0].getKey());
        assertTrue(ONE.equals(inc_res7[0].getValue()));

        // --- Decrement --- (increment=false)
        Increment decGate = new Increment(reg, dummyTarget, false);

        // Input |001> (state 1) -> Output |000> (state 0), Coef 1
        Pair<Integer, Complex>[] dec_res1 = decGate.getTosAndItsCoefs(1);
        assertEquals(1, dec_res1.length);
        assertEquals(0, dec_res1[0].getKey());
        assertTrue(ONE.equals(dec_res1[0].getValue()));

        // Input |100> (state 4) -> Output |011> (state 3), Coef 1
        Pair<Integer, Complex>[] dec_res4 = decGate.getTosAndItsCoefs(4);
        assertEquals(1, dec_res4.length);
        assertEquals(3, dec_res4[0].getKey());
        assertTrue(ONE.equals(dec_res4[0].getValue()));

        // Input |000> (state 0) -> Output |111> (state 7) (wrap around), Coef 1
        Pair<Integer, Complex>[] dec_res0 = decGate.getTosAndItsCoefs(0);
        assertEquals(1, dec_res0.length);
        assertEquals(7, dec_res0[0].getKey());
        assertTrue(ONE.equals(dec_res0[0].getValue()));
    }

    @Test
    void testApplyIncrementDecrement() {
        QubitRegister reg = new QubitRegister("regApplyIncDec", 3);
        Integer[] dummyTarget = {};

        // Start at |010> (state 2)
        reg.getStates().clear(0);
        reg.getAmplitudes().set(0, Complex.getZero());
        reg.getStates().set(2);
        reg.getAmplitudes().set(2, ONE);

        // Increment: |010> -> |011> (state 3)
        Increment incGate = new Increment(reg, dummyTarget, true);
        incGate.apply();
        BitSet state1 = reg.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(3));
        assertTrue(ONE.equals(reg.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        // Increment again: |011> -> |100> (state 4)
        Increment incGate2 = new Increment(reg, dummyTarget, true);
        incGate2.apply();
        BitSet state2 = reg.getStates();
        assertEquals(1, state2.cardinality());
        assertTrue(state2.get(4));
        assertTrue(ONE.equals(reg.getAmplitudes().get(4)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        // Decrement: |100> -> |011> (state 3)
        Increment decGate1 = new Increment(reg, dummyTarget, false);
        decGate1.apply();
        BitSet state3 = reg.getStates();
        assertEquals(1, state3.cardinality());
        assertTrue(state3.get(3));
        assertTrue(ONE.equals(reg.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        // Decrement again: |011> -> |010> (state 2)
        Increment decGate2 = new Increment(reg, dummyTarget, false);
        decGate2.apply();
        BitSet state4 = reg.getStates();
        assertEquals(1, state4.cardinality());
        assertTrue(state4.get(2));
        assertTrue(ONE.equals(reg.getAmplitudes().get(2)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testApplyIncrementDecrementWrapAround() {
        QubitRegister reg = new QubitRegister("regIncDecWrap", 2); // 2 qubits, size 4
        Integer[] dummyTarget = {};

        // Start at |11> (state 3)
        reg.getStates().clear(0);
        reg.getAmplitudes().set(0, Complex.getZero());
        reg.getStates().set(3);
        reg.getAmplitudes().set(3, ONE);

        // Increment: |11> -> |00> (state 0)
        Increment incGate = new Increment(reg, dummyTarget, true);
        incGate.apply();
        BitSet state1 = reg.getStates();
        assertEquals(1, state1.cardinality());
        assertTrue(state1.get(0));
        assertTrue(ONE.equals(reg.getAmplitudes().get(0)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        // Decrement: |00> -> |11> (state 3)
        Increment decGate = new Increment(reg, dummyTarget, false);
        decGate.apply();
        BitSet state2 = reg.getStates();
        assertEquals(1, state2.cardinality());
        assertTrue(state2.get(3));
        assertTrue(ONE.equals(reg.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testApplyIncrementOnSuperposition() {
         // Initial state: (|00> + |10>) / sqrt(2) for 2 qubits
        QubitRegister reg = new QubitRegister("regIncSuper", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getAmplitudes().set(0, amp); // |00>
        reg.getAmplitudes().set(2, amp); // |10>
        reg.getStates().set(0);
        reg.getStates().set(2);

        // Apply Increment:
        // INC (|00> + |10>) / sqrt(2) = (|01> + |11>) / sqrt(2)
        Increment incGate = new Increment(reg, new Integer[]{}, true);
        incGate.apply();

        BitSet finalState = reg.getStates();
        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(1)); // |01>
        assertTrue(finalState.get(3)); // |11>
        assertTrue(amp.equals(reg.getAmplitudes().get(1)));
        assertTrue(amp.equals(reg.getAmplitudes().get(3)));
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