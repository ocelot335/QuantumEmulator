package org.example.model.gate;

import org.example.model.gate.instances.SWAP;
import org.example.model.gate.instances.pauli.X;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;

public class ControlledGateTest {
    private static final double DELTA = 1e-9;

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

    @Test
    void testApplyCNOTGate() {
        // Test CNOT(0, 1)
        QubitRegister regCNOT = new QubitRegister("regCNOT", 2);
        X xGate = new X(regCNOT, new Integer[]{1}); // X on target qubit 1
        ControlledGate cnotGate = new ControlledGate(regCNOT, xGate, new Integer[]{0}); // Control on qubit 0

        // Input |00> -> Output |00> (control 0 is off)
        regCNOT.getStates().clear();
        regCNOT.getAmplitudes().clear();
        regCNOT.getStates().set(0);
        regCNOT.getAmplitudes().set(0, Complex.getOne());
        cnotGate.apply();
        BitSet state00 = regCNOT.getStates();
        assertEquals(1, state00.cardinality());
        assertTrue(state00.get(0));
        assertTrue(Complex.getOne().equals(regCNOT.getAmplitudes().get(0)));
        assertEquals(1.0, calculateTotalProbability(regCNOT), DELTA);

        // Input |01> -> Output |11> (control 0 is on)
        regCNOT.getStates().clear();
        regCNOT.getAmplitudes().clear();
        regCNOT.getStates().set(1);
        regCNOT.getAmplitudes().set(1, Complex.getOne());
        // Recreate gate as apply modifies internal state
        xGate = new X(regCNOT, new Integer[]{1});
        cnotGate = new ControlledGate(regCNOT, xGate, new Integer[]{0});
        cnotGate.apply();
        BitSet state01 = regCNOT.getStates();
        assertEquals(1, state01.cardinality());
        assertTrue(state01.get(3)); // State should be |11> (index 3)
        assertTrue(Complex.getOne().equals(regCNOT.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(regCNOT), DELTA);

        // Input |10> -> Output |10> (control 0 is off)
        regCNOT.getStates().clear();
        regCNOT.getAmplitudes().clear();
        regCNOT.getStates().set(2);
        regCNOT.getAmplitudes().set(2, Complex.getOne());
        xGate = new X(regCNOT, new Integer[]{1});
        cnotGate = new ControlledGate(regCNOT, xGate, new Integer[]{0});
        cnotGate.apply();
        BitSet state10 = regCNOT.getStates();
        assertEquals(1, state10.cardinality());
        assertTrue(state10.get(2)); // State should be |10> (index 2)
        assertTrue(Complex.getOne().equals(regCNOT.getAmplitudes().get(2)));
        assertEquals(1.0, calculateTotalProbability(regCNOT), DELTA);

        // Input |11> -> Output |01> (control 0 is on)
        regCNOT.getStates().clear();
        regCNOT.getAmplitudes().clear();
        regCNOT.getStates().set(3);
        regCNOT.getAmplitudes().set(3, Complex.getOne());
        xGate = new X(regCNOT, new Integer[]{1});
        cnotGate = new ControlledGate(regCNOT, xGate, new Integer[]{0});
        cnotGate.apply();
        BitSet state11 = regCNOT.getStates();
        assertEquals(1, state11.cardinality());
        assertTrue(state11.get(1)); // State should be |01> (index 1)
        assertTrue(Complex.getOne().equals(regCNOT.getAmplitudes().get(1)));
        assertEquals(1.0, calculateTotalProbability(regCNOT), DELTA);
    }

     @Test
    void testApplyCCNOTGate() {
        // Test CCNOT(0, 1, 2) - Toffoli Gate
        QubitRegister regCCNOT = new QubitRegister("regCCNOT", 3);
        X xGate = new X(regCCNOT, new Integer[]{2}); // X on target qubit 2
        ControlledGate cxGate = new ControlledGate(regCCNOT, xGate, new Integer[]{1}); // CX(1, 2)
        ControlledGate ccnotGate = new ControlledGate(regCCNOT, cxGate, new Integer[]{0}); // C(0, CX(1, 2)) = CCNOT(0, 1, 2)

        // Input |011> -> Output |111> (controls 0 and 1 are on)
        regCCNOT.getStates().clear();
        regCCNOT.getAmplitudes().clear();
        regCCNOT.getStates().set(3);
        regCCNOT.getAmplitudes().set(3, Complex.getOne());
        // Need to recreate gates because apply modifies internal state
        xGate = new X(regCCNOT, new Integer[]{2});
        cxGate = new ControlledGate(regCCNOT, xGate, new Integer[]{1});
        ccnotGate = new ControlledGate(regCCNOT, cxGate, new Integer[]{0});
        ccnotGate.apply();
        BitSet state011 = regCCNOT.getStates();
        assertEquals(1, state011.cardinality());
        assertTrue(state011.get(7)); // State should be |111> (index 7)
        assertTrue(Complex.getOne().equals(regCCNOT.getAmplitudes().get(7)));

        // Input |110> -> Output |110> (control 0 is off)
        regCCNOT.getStates().clear();
        regCCNOT.getAmplitudes().clear();
        regCCNOT.getStates().set(6);
        regCCNOT.getAmplitudes().set(6, Complex.getOne());
        xGate = new X(regCCNOT, new Integer[]{2});
        cxGate = new ControlledGate(regCCNOT, xGate, new Integer[]{1});
        ccnotGate = new ControlledGate(regCCNOT, cxGate, new Integer[]{0});
        ccnotGate.apply();
        BitSet state110 = regCCNOT.getStates();
        assertEquals(1, state110.cardinality());
        assertTrue(state110.get(6)); // State should be |110> (index 6)
        assertTrue(Complex.getOne().equals(regCCNOT.getAmplitudes().get(6)));

        // Input |111> -> Output |011> (controls 0 and 1 are on)
        regCCNOT.getStates().clear();
        regCCNOT.getAmplitudes().clear();
        regCCNOT.getStates().set(7);
        regCCNOT.getAmplitudes().set(7, Complex.getOne());
        xGate = new X(regCCNOT, new Integer[]{2});
        cxGate = new ControlledGate(regCCNOT, xGate, new Integer[]{1});
        ccnotGate = new ControlledGate(regCCNOT, cxGate, new Integer[]{0});
        ccnotGate.apply();
        BitSet state111 = regCCNOT.getStates();
        assertEquals(1, state111.cardinality());
        assertTrue(state111.get(3)); // State should be |011> (index 3)
        assertTrue(Complex.getOne().equals(regCCNOT.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(regCCNOT), DELTA); // Check probability at the end
    }

    @Test
    void testApplyCSWAPGate() {
        // Test CSWAP(0, 1, 2) - Fredkin Gate
        QubitRegister regCSWAP = new QubitRegister("regCSWAP", 3);
        SWAP swapGate = new SWAP(regCSWAP, new Integer[]{1, 2}); // SWAP target qubits 1 and 2
        ControlledGate cswapGate = new ControlledGate(regCSWAP, swapGate, new Integer[]{0}); // Control on qubit 0

        // Input |010> -> Output |010> (control 0 is off)
        regCSWAP.getStates().clear();
        regCSWAP.getAmplitudes().clear();
        regCSWAP.getStates().set(2);
        regCSWAP.getAmplitudes().set(2, Complex.getOne());
        swapGate = new SWAP(regCSWAP, new Integer[]{1, 2});
        cswapGate = new ControlledGate(regCSWAP, swapGate, new Integer[]{0});
        cswapGate.apply();
        BitSet state010 = regCSWAP.getStates();
        assertEquals(1, state010.cardinality());
        assertTrue(state010.get(2));
        assertTrue(Complex.getOne().equals(regCSWAP.getAmplitudes().get(2)));

        // Input |110> -> Output |110> (control 0 is off, no swap)
        regCSWAP.getStates().clear();
        regCSWAP.getAmplitudes().clear();
        regCSWAP.getStates().set(6);
        regCSWAP.getAmplitudes().set(6, Complex.getOne());
        swapGate = new SWAP(regCSWAP, new Integer[]{1, 2});
        cswapGate = new ControlledGate(regCSWAP, swapGate, new Integer[]{0});
        cswapGate.apply();
        BitSet state110 = regCSWAP.getStates();
        assertEquals(1, state110.cardinality());
        assertTrue(state110.get(6)); // State should be |110> (index 6)
        assertTrue(Complex.getOne().equals(regCSWAP.getAmplitudes().get(6)));

        // Input |101> -> Output |011> (control 0 is on, swap 1 and 2)
        regCSWAP.getStates().clear();
        regCSWAP.getAmplitudes().clear();
        regCSWAP.getStates().set(5);
        regCSWAP.getAmplitudes().set(5, Complex.getOne());
        swapGate = new SWAP(regCSWAP, new Integer[]{1, 2});
        cswapGate = new ControlledGate(regCSWAP, swapGate, new Integer[]{0});
        cswapGate.apply();
        BitSet state101 = regCSWAP.getStates();
        assertEquals(1, state101.cardinality());
        assertTrue(state101.get(3)); // State should be |011> (index 3)
        assertTrue(Complex.getOne().equals(regCSWAP.getAmplitudes().get(3)));

        // Input |111> -> Output |111> (control 0 is on, swap 1 and 2 - no change)
        regCSWAP.getStates().clear();
        regCSWAP.getAmplitudes().clear();
        regCSWAP.getStates().set(7);
        regCSWAP.getAmplitudes().set(7, Complex.getOne());
        swapGate = new SWAP(regCSWAP, new Integer[]{1, 2});
        cswapGate = new ControlledGate(regCSWAP, swapGate, new Integer[]{0});
        cswapGate.apply();
        BitSet state111 = regCSWAP.getStates();
        assertEquals(1, state111.cardinality());
        assertTrue(state111.get(7));
        assertTrue(Complex.getOne().equals(regCSWAP.getAmplitudes().get(7)));
        assertEquals(1.0, calculateTotalProbability(regCSWAP), DELTA); // Check probability at the end
    }
} 