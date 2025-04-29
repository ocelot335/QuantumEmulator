package org.example.model.gate;

import org.example.model.qubit.Complex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GateTraceTest {

    private GateTrace gateTrace;
    private final Complex ONE = Complex.getOne();
    private final Complex ZERO = Complex.getZero();
    private final Complex I = new Complex(0, 1);
    private final Complex HALF = new Complex(0.5);

    @BeforeEach
    void setUp() {
        gateTrace = new GateTrace();
    }

    @Test
    void testConstructor() {
        assertNotNull(gateTrace.getTrace(), "Trace map should not be null after construction.");
        assertTrue(gateTrace.getTrace().isEmpty(), "Trace map should be empty after construction.");
    }

    @Test
    void testAddSingleAmplitude() {
        // Add trace: 0 -> 1 with amplitude 1.0
        gateTrace.addAmplitude(0, 1, ONE);

        Map<Integer, Map<Integer, Complex>> trace = gateTrace.getTrace();
        assertEquals(1, trace.size(), "Trace map should contain one entry for stateFrom=0.");
        assertTrue(trace.containsKey(0), "Trace map should contain the key for stateFrom=0.");

        Map<Integer, Complex> fromMap = trace.get(0);
        assertNotNull(fromMap, "Inner map for stateFrom=0 should not be null.");
        assertEquals(1, fromMap.size(), "Inner map for stateFrom=0 should contain one entry.");
        assertTrue(fromMap.containsKey(1), "Inner map should contain the key for stateTo=1.");
        assertTrue(ONE.equals(fromMap.get(1)), "Amplitude for 0->1 should be ONE.");
    }

    @Test
    void testAddMultipleAmplitudesSameFrom() {
        // Add trace: 2 -> 3 with amplitude i
        // Add trace: 2 -> 0 with amplitude 0.5
        gateTrace.addAmplitude(2, 3, I);
        gateTrace.addAmplitude(2, 0, HALF);

        Map<Integer, Map<Integer, Complex>> trace = gateTrace.getTrace();
        assertEquals(1, trace.size(), "Trace map should still contain one entry for stateFrom=2.");
        assertTrue(trace.containsKey(2), "Trace map should contain the key for stateFrom=2.");

        Map<Integer, Complex> fromMap = trace.get(2);
        assertNotNull(fromMap);
        assertEquals(2, fromMap.size(), "Inner map for stateFrom=2 should contain two entries.");
        assertTrue(fromMap.containsKey(3), "Inner map should contain key for stateTo=3.");
        assertTrue(I.equals(fromMap.get(3)), "Amplitude for 2->3 should be I.");
        assertTrue(fromMap.containsKey(0), "Inner map should contain key for stateTo=0.");
        assertTrue(HALF.equals(fromMap.get(0)), "Amplitude for 2->0 should be HALF.");
    }

    @Test
    void testAddMultipleAmplitudesDifferentFrom() {
        // Add trace: 0 -> 1 with amplitude 1.0
        // Add trace: 3 -> 2 with amplitude i
        gateTrace.addAmplitude(0, 1, ONE);
        gateTrace.addAmplitude(3, 2, I);

        Map<Integer, Map<Integer, Complex>> trace = gateTrace.getTrace();
        assertEquals(2, trace.size(), "Trace map should contain entries for stateFrom=0 and stateFrom=3.");
        assertTrue(trace.containsKey(0), "Trace map should contain key stateFrom=0.");
        assertTrue(trace.containsKey(3), "Trace map should contain key stateFrom=3.");

        // Check stateFrom=0
        Map<Integer, Complex> fromMap0 = trace.get(0);
        assertNotNull(fromMap0);
        assertEquals(1, fromMap0.size());
        assertTrue(fromMap0.containsKey(1));
        assertTrue(ONE.equals(fromMap0.get(1)));

        // Check stateFrom=3
        Map<Integer, Complex> fromMap3 = trace.get(3);
        assertNotNull(fromMap3);
        assertEquals(1, fromMap3.size());
        assertTrue(fromMap3.containsKey(2));
        assertTrue(I.equals(fromMap3.get(2)));
    }

    @Test
    void testOverwriteAmplitude() {
        // Add trace: 5 -> 6 with amplitude 1.0
        gateTrace.addAmplitude(5, 6, ONE);
        // Add trace: 5 -> 6 again, but with amplitude i (should overwrite)
        gateTrace.addAmplitude(5, 6, I);

        Map<Integer, Map<Integer, Complex>> trace = gateTrace.getTrace();
        assertEquals(1, trace.size());
        assertTrue(trace.containsKey(5));

        Map<Integer, Complex> fromMap = trace.get(5);
        assertNotNull(fromMap);
        assertEquals(1, fromMap.size(), "Inner map should still only contain one entry for stateTo=6.");
        assertTrue(fromMap.containsKey(6));
        assertTrue(I.equals(fromMap.get(6)), "Amplitude for 5->6 should be overwritten to I.");
    }
} 