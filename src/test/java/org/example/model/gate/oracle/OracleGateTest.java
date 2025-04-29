package org.example.model.gate.oracle;

import javafx.util.Pair;
import org.example.model.qubit.ChunkedComplexArray;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class OracleGateTest {

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
    void testGetTosAndItsCoefs() {
        // 3 input qubits (offset 0, size 3), 1 ancilla (index 3). Total 4 qubits.
        QubitRegister reg = new QubitRegister("regOracleGet", 4);
        Set<Integer> markedStates = new HashSet<>();
        markedStates.add(5); // Mark state |101>
        OracleGate oracle = new OracleGate("TestOracle", reg, 0, 3, 3, markedStates);

        // Test Case 1: Input |0100> (4). Input value = 010 = 2 (not marked). Ancilla = 0.
        // Expected output: |0100> (4)
        Pair<Integer, Complex>[] result1 = oracle.getTosAndItsCoefs(4);
        assertEquals(1, result1.length);
        assertEquals(4, result1[0].getKey());
        assertTrue(Complex.getOne().equals(result1[0].getValue()));

        // Test Case 2: Input |0101> (5). Input value = 101 = 5 (marked). Ancilla = 0.
        // Expected output: |1101> (13) (ancilla flipped 0 -> 1)
        Pair<Integer, Complex>[] result2 = oracle.getTosAndItsCoefs(5);
        assertEquals(1, result2.length);
        assertEquals(13, result2[0].getKey()); // 5 ^ (1 << 3) = 0101 ^ 1000 = 1101 = 13
        assertTrue(Complex.getOne().equals(result2[0].getValue()));

        // Test Case 3: Input |1101> (13). Input value = 101 = 5 (marked). Ancilla = 1.
        // Expected output: |0101> (5) (ancilla flipped 1 -> 0)
        Pair<Integer, Complex>[] result3 = oracle.getTosAndItsCoefs(13);
        assertEquals(1, result3.length);
        assertEquals(5, result3[0].getKey()); // 13 ^ (1 << 3) = 1101 ^ 1000 = 0101 = 5
        assertTrue(Complex.getOne().equals(result3[0].getValue()));

        // Test Case 4: Input |1100> (12). Input value = 100 = 4 (not marked). Ancilla = 1.
        // Expected output: |1100> (12)
        Pair<Integer, Complex>[] result4 = oracle.getTosAndItsCoefs(12);
        assertEquals(1, result4.length);
        assertEquals(12, result4[0].getKey());
        assertTrue(Complex.getOne().equals(result4[0].getValue()));

        // Test Case 5: Different offset. 2 inputs (offset 1, size 2), 1 ancilla (index 0). Total 3 qubits (Q2, Q1, Q0)
        // Input qubits are Q2, Q1. Ancilla is Q0.
        QubitRegister regOffset = new QubitRegister("regOracleOffset", 3);
        Set<Integer> markedStatesOffset = new HashSet<>();
        markedStatesOffset.add(2); // Mark input state |10>
        OracleGate oracleOffset = new OracleGate("OffsetOracle", regOffset, 1, 2, 0, markedStatesOffset);

        // Input |100> (4). Input value (Q2,Q1) = 10 = 2 (marked). Ancilla (Q0) = 0.
        // Expected output: |101> (5) (ancilla flipped 0 -> 1)
        Pair<Integer, Complex>[] resultOffset1 = oracleOffset.getTosAndItsCoefs(4);
        assertEquals(1, resultOffset1.length);
        assertEquals(5, resultOffset1[0].getKey()); // 4 ^ (1 << 0) = 100 ^ 001 = 101 = 5
        assertTrue(Complex.getOne().equals(resultOffset1[0].getValue()));

        // Input |101> (5). Input value (Q2,Q1) = 10 = 2 (marked). Ancilla (Q0) = 1.
        // Expected output: |100> (4) (ancilla flipped 1 -> 0)
        Pair<Integer, Complex>[] resultOffset2 = oracleOffset.getTosAndItsCoefs(5);
        assertEquals(1, resultOffset2.length);
        assertEquals(4, resultOffset2[0].getKey()); // 5 ^ (1 << 0) = 101 ^ 001 = 100 = 4
        assertTrue(Complex.getOne().equals(resultOffset2[0].getValue()));

        // Input |010> (2). Input value (Q2,Q1) = 01 = 1 (not marked). Ancilla (Q0) = 0.
        // Expected output: |010> (2)
        Pair<Integer, Complex>[] resultOffset3 = oracleOffset.getTosAndItsCoefs(2);
        assertEquals(1, resultOffset3.length);
        assertEquals(2, resultOffset3[0].getKey());
        assertTrue(Complex.getOne().equals(resultOffset3[0].getValue()));
    }

    @Test
    void testApplyOracleGate() {
        // 2 input qubits (offset 0, size 2), 1 ancilla (index 2). Total 3 qubits (Q2, Q1, Q0)
        QubitRegister reg = new QubitRegister("regOracleApply", 3);
        Set<Integer> markedStates = new HashSet<>();
        markedStates.add(2); // Mark input state |10>
        OracleGate oracle = new OracleGate("ApplyOracle", reg, 0, 2, 2, markedStates);

        // Test 1: Initial state |010> (2). Input (Q1,Q0) = 10 = 2 (marked). Ancilla (Q2) = 0.
        // Expected final state |110> (6) (ancilla Q2 flipped 0->1)
        reg.getStates().clear();
        reg.getAmplitudes().clear();
        reg.getStates().set(2); // State |010>
        reg.getAmplitudes().set(2, Complex.getOne());
        oracle.apply();
        BitSet finalState1 = reg.getStates();
        assertEquals(1, finalState1.cardinality());
        assertTrue(finalState1.get(6)); // State |110>
        assertTrue(Complex.getOne().equals(reg.getAmplitudes().get(6)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);

        // Test 2: Initial state |011> (3). Input (Q1,Q0) = 11 = 3 (not marked). Ancilla (Q2) = 0.
        // Expected final state |011> (3)
        reg.getStates().clear();
        reg.getAmplitudes().clear();
        reg.getStates().set(3);
        reg.getAmplitudes().set(3, Complex.getOne());
        // Recreate gate because apply modifies internal state
        oracle = new OracleGate("ApplyOracle", reg, 0, 2, 2, markedStates);
        oracle.apply();
        BitSet finalState2 = reg.getStates();
        assertEquals(1, finalState2.cardinality());
        assertTrue(finalState2.get(3));
        assertTrue(Complex.getOne().equals(reg.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testApplyOracleGateOnSuperposition() {
        // 2 input qubits (offset 0, size 2), 1 ancilla (index 2). Total 3 qubits (Q2, Q1, Q0)
        QubitRegister reg = new QubitRegister("regOracleSuper", 3);
        Set<Integer> markedStates = new HashSet<>();
        markedStates.add(2); // Mark input state |10>
        OracleGate oracle = new OracleGate("SuperOracle", reg, 0, 2, 2, markedStates);

        // Initial state: (1/sqrt(2))(|100> + |011>)
        // |100> (4): Input=00=0 (not marked), Ancilla=1. Should not flip -> |100> (4)
        // |011> (3): Input=11=3 (not marked), Ancilla=0. Should not flip -> |011> (3)
        // Expected final state: (1/sqrt(2))(|100> + |011>)
        Complex amp = new Complex(1.0 / Math.sqrt(2.0));
        reg.getStates().clear();
        reg.getAmplitudes().clear();
        reg.getStates().set(4); // |100>
        reg.getAmplitudes().set(4, amp);
        reg.getStates().set(3); // |011>
        reg.getAmplitudes().set(3, amp);

        oracle.apply();

        BitSet finalState = reg.getStates();
        ChunkedComplexArray finalAmps = reg.getAmplitudes();

        assertEquals(2, finalState.cardinality());
        assertTrue(finalState.get(4)); // |100>
        assertTrue(finalState.get(3)); // |011>
        assertTrue(amp.equals(finalAmps.get(4)));
        assertTrue(amp.equals(finalAmps.get(3)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testToString() {
        QubitRegister reg = new QubitRegister("regToString", 1);
        Set<Integer> marked = new HashSet<>();
        OracleGate oracle = new OracleGate("MySpecialOracle", reg, 0, 0, 0, marked);
        assertEquals("MySpecialOracle", oracle.toString());
    }
} 