package org.example.model.gate.instances;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class PTest {
    private static final double DELTA = 1e-9;
    private final Complex ONE = Complex.getOne();

    @Test
    void testGetTosAndItsCoefs() {
        QubitRegister reg = new QubitRegister("regP", 2);
        Integer[] target = {0};
        double phase1 = Math.PI / 3.0;
        double phase2 = -Math.PI / 6.0;
        Complex phaseCoef1 = Complex.expI(phase1);
        Complex phaseCoef2 = Complex.expI(phase2);

        // --- Target Qubit 0, Phase PI/3 ---
        // Input |00> (state 0): qubit 0 is 0 -> P|0> = |0>. State 00 -> 00 (0). Coef 1.
        P pGate1_state0 = new P(reg, phase1, target);
        Pair<Integer, Complex>[] result1_0_q0 = pGate1_state0.getTosAndItsCoefs(0);
        assertEquals(1, result1_0_q0.length);
        assertEquals(0, result1_0_q0[0].getKey());
        assertTrue(ONE.equals(result1_0_q0[0].getValue()));

        // Input |01> (state 1): qubit 0 is 1 -> P|1> = exp(i*phase)|1>. State 01 -> 01 (1). Coef phaseCoef1.
        P pGate1_state1 = new P(reg, phase1, target);
        Pair<Integer, Complex>[] result1_1_q0 = pGate1_state1.getTosAndItsCoefs(1);
        assertEquals(1, result1_1_q0.length);
        assertEquals(1, result1_1_q0[0].getKey());
        assertTrue(phaseCoef1.equals(result1_1_q0[0].getValue()));

        // --- Target Qubit 0, Phase -PI/6 ---
        // Input |01> (state 1): qubit 0 is 1 -> P|1> = exp(i*phase)|1>. State 01 -> 01 (1). Coef phaseCoef2.
        P pGate2_state1 = new P(reg, phase2, target);
        Pair<Integer, Complex>[] result2_1_q0 = pGate2_state1.getTosAndItsCoefs(1);
        assertEquals(1, result2_1_q0.length);
        assertEquals(1, result2_1_q0[0].getKey());
        assertTrue(phaseCoef2.equals(result2_1_q0[0].getValue()));

        // Input |11> (state 3): qubit 0 is 1 -> P|1> = exp(i*phase)|1>. State 11 -> 11 (3). Coef phaseCoef2.
        P pGate2_state3 = new P(reg, phase2, target);
        Pair<Integer, Complex>[] result2_3_q0 = pGate2_state3.getTosAndItsCoefs(3);
        assertEquals(1, result2_3_q0.length);
        assertEquals(3, result2_3_q0[0].getKey());
        assertTrue(phaseCoef2.equals(result2_3_q0[0].getValue()));
    }

    @Test
    void testApplyPGateSpecialCases() {
        // Test P(pi) which should be Z
        QubitRegister regZ = new QubitRegister("regP_Z", 1);
        regZ.getAmplitudes().set(0, new Complex(1.0/Math.sqrt(2))); // |+>
        regZ.getAmplitudes().set(1, new Complex(1.0/Math.sqrt(2)));
        regZ.getStates().set(1);
        P pGateZ = new P(regZ, Math.PI, new Integer[]{0});
        pGateZ.apply();
        // Expected: Z|+> = |-> = (|0> - |1>)/sqrt(2)
        assertTrue(regZ.getAmplitudes().get(0).equals(new Complex(1.0/Math.sqrt(2))));
        assertTrue(regZ.getAmplitudes().get(1).equals(new Complex(-1.0/Math.sqrt(2))));
        assertEquals(1.0, calculateTotalProbability(regZ), DELTA);

        // Test P(pi/2) which should be S
        QubitRegister regS = new QubitRegister("regP_S", 1);
        regS.getAmplitudes().set(0, new Complex(1.0/Math.sqrt(2))); // |+>
        regS.getAmplitudes().set(1, new Complex(1.0/Math.sqrt(2)));
        regS.getStates().set(1);
        P pGateS = new P(regS, Math.PI / 2.0, new Integer[]{0});
        pGateS.apply();
        // Expected: S|+> = (|0> + i|1>)/sqrt(2)
        assertTrue(regS.getAmplitudes().get(0).equals(new Complex(1.0/Math.sqrt(2))));
        assertTrue(regS.getAmplitudes().get(1).equals(new Complex(0, 1.0/Math.sqrt(2)))); // i/sqrt(2)
        assertEquals(1.0, calculateTotalProbability(regS), DELTA);

        // Test P(pi/4) which should be T
        QubitRegister regT = new QubitRegister("regP_T", 1);
        regT.getAmplitudes().set(0, new Complex(1.0/Math.sqrt(2))); // |+>
        regT.getAmplitudes().set(1, new Complex(1.0/Math.sqrt(2)));
        regT.getStates().set(1);
        P pGateT = new P(regT, Math.PI / 4.0, new Integer[]{0});
        pGateT.apply();
        // Expected: T|+> = (|0> + exp(i*pi/4)|1>)/sqrt(2)
        Complex tPhase = Complex.expI(Math.PI / 4.0);
        assertTrue(regT.getAmplitudes().get(0).equals(new Complex(1.0/Math.sqrt(2))));
        assertTrue(regT.getAmplitudes().get(1).equals(tPhase.multiply(new Complex(1.0/Math.sqrt(2)))));
        assertEquals(1.0, calculateTotalProbability(regT), DELTA);
    }

    @Test
    void testApplyPGateGeneral() {
        QubitRegister reg = new QubitRegister("regPGen", 2);
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getAmplitudes().set(0, amp); // |00>
        reg.getAmplitudes().set(1, amp); // |01>
        reg.getStates().set(0);
        reg.getStates().set(1);
        // State = (|00> + |01>)/sqrt(2)

        double phase = Math.PI / 6.0;
        Complex phaseCoef = Complex.expI(phase);
        P pGate = new P(reg, phase, new Integer[]{0}); // Apply to qubit 0

        // Apply P(phase) on qubit 0:
        // P(|00> + |01>)/sqrt(2) = (P|00> + P|01>)/sqrt(2)
        // = (|00> + exp(i*phase)|01>)/sqrt(2)
        pGate.apply();

        BitSet finalState = reg.getStates();
        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(0)); // |00>
        assertTrue(finalState.get(1)); // |01>
        assertTrue(amp.equals(reg.getAmplitudes().get(0))); // Coef is 1
        assertTrue(amp.multiply(phaseCoef).equals(reg.getAmplitudes().get(1))); // Coef is phaseCoef
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