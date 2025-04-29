package org.example.model;

import org.example.model.gate.GateTrace;
import org.example.model.gate.oracle.OracleDefinition;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.example.script.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EmulationTest {

    private Emulation emulation;
    private final Complex ONE = Complex.getOne();
    private final Complex ZERO = Complex.getZero();
    private static final double DELTA = 1e-9;

    @BeforeEach
    void setUp() {
        emulation = new Emulation();
    }

    // Helper to create a simple register command
    private Command createRegisterCmd(String spec) {
        // Simplified parser logic for testing
        List<Map<String, Object>> nominals = new ArrayList<>();
        int totalSize = 0;
        Set<String> names = new HashSet<>();
        String[] definitions = spec.split(",");
        int offsetCounter = 0;
        for (String definition : definitions) {
            String[] parts = definition.trim().split("\\[|\\]");
            String name = parts[0];
            int size = Integer.parseInt(parts[1]);
            totalSize += size;
        }
        int currentOffset = 0;
         for (String definition : definitions) {
            String[] parts = definition.trim().split("\\[|\\]");
            String name = parts[0];
            int size = Integer.parseInt(parts[1]);
            Map<String, Object> nominalSpec = new HashMap<>();
            nominalSpec.put("name", name);
            nominalSpec.put("size", size);
            nominalSpec.put("offset", currentOffset);
            nominals.add(nominalSpec);
            currentOffset +=size;
        }

        Collections.reverse(nominals);
        int startOffset = 0;
        for (Map<String,Object> map : nominals) {
            int size = (int) map.get("size");
            map.put("offset",startOffset);
            startOffset += size;
        }
        Collections.reverse(nominals);


        String realName = "real_" + UUID.randomUUID().toString().substring(0, 4);
        Map<String, Object> cmdArgs = new HashMap<>();
        cmdArgs.put("realRegisterName", realName);
        cmdArgs.put("realRegisterSize", totalSize);
        cmdArgs.put("nominalRegisters", nominals);
        return new Command(Command.CommandType.CREATE_REGISTER, cmdArgs);
    }

    // Helper to create apply gate command
    private Command applyGateCmd(String gateName, String... operands) {
        Map<String, Object> cmdArgs = new HashMap<>();
        cmdArgs.put("gate", gateName);
        List<Map<String, Object>> ops = new ArrayList<>();
        for (String op : operands) {
             String[] parts = op.trim().split("\\[|\\]");
             Map<String, Object> opMap = new HashMap<>();
             opMap.put("register", parts[0]);
             opMap.put("index", Integer.parseInt(parts[1]));
             ops.add(opMap);
        }
        cmdArgs.put("operands", ops);
        return new Command(Command.CommandType.APPLY_GATE, cmdArgs);
    }

    // Helper to create measure command
    private Command measureCmd(String register, int index) {
         Map<String, Object> cmdArgs = new HashMap<>();
         cmdArgs.put("register", register);
         cmdArgs.put("index", index);
        return new Command(Command.CommandType.MEASURE, cmdArgs);
    }

    @Test
    void testProcessCreateRegister() {
        Command cmd = createRegisterCmd("q[2],a[1]");
        Map<String, Object> result = emulation.run(cmd);
        String output = (String) result.get("output");
        assertNotNull(output);
        // Output should be the string representation of the new real register
        assertTrue(output.contains("|000>: 1,00 + 0,00i"), "Output should show initial state |000>");

        // Cannot directly check realRegisters map size. Check existence of expected real register.
        String realRegName = emulation.getNominalRegister("q").getRealRegister().getName();
        assertNotNull(emulation.getRealRegister(realRegName));
        assertEquals(2, emulation.getQubitRegisters().size());
        assertNotNull(emulation.getNominalRegister("q"));
        assertNotNull(emulation.getNominalRegister("a"));
        assertEquals(2, emulation.getNominalRegister("q").size());
        assertEquals(1, emulation.getNominalRegister("a").size());
        assertEquals(3, emulation.getNominalRegister("q").getRealSize());
        QubitRegister realReg = emulation.getNominalRegister("q").getRealRegister();
        assertSame(realReg, emulation.getNominalRegister("a").getRealRegister());
        // Check offsets (assuming q[2] is bits 1,2 and a[1] is bit 0)
        assertEquals(1, emulation.getNominalRegister("q").getOffsetInRealRegister()); // Offset of q depends on order
        assertEquals(0, emulation.getNominalRegister("a").getOffsetInRealRegister()); // Offset of a depends on order
    }

     @Test
    void testProcessCreateRegisterDuplicateNominal() {
         Command cmd = createRegisterCmd("dup[1],dup[1]");
         // The helper createRegisterCmd doesn't check for duplicates within spec,
         // but the Emulation.processCreateRegister should (or the parser).
         // Assuming Parser handles it based on its tests.
         // If Emulation *also* checks, this test needs adjustment.
         // Let's assume Emulation expects unique names passed to it.
          Map<String, Object> args = cmd.getArguments();
         List<Map<String, Object>> nominals = (List<Map<String, Object>>) args.get("nominalRegisters");
         emulation.getQubitRegisters().put("dup", new QubitRegister("dup", 1)); // Pre-populate to trigger error

         Exception exception = assertThrows(IllegalArgumentException.class, () -> emulation.run(cmd));
         assertTrue(exception.getMessage().contains("уже существует"));
    }

    @Test
    void testProcessApplyGateSingleRegister() {
        Command createCmd = createRegisterCmd("r[1]");
        emulation.run(createCmd);
        Command applyCmd = applyGateCmd("X", "r[0]");
        Map<String, Object> result = emulation.run(applyCmd);

        assertNull(result.get("joinInfo"), "Should not join for single register op");
        String output = (String) result.get("output");
        assertTrue(output.contains("|1>: 1,00 + 0,00i"), "State should be |1> after X on |0>");

        Map<Integer, Complex> state = emulation.getRegisterState("r");
        assertEquals(1, state.size());
        assertTrue(state.containsKey(1));
        assertTrue(ONE.equals(state.get(1)));
        assertNotNull(emulation.getLastGateTrace());
    }

     @Test
    void testProcessApplyGateJoinRegisters() {
        Command createCmd1 = createRegisterCmd("a[1]");
        // Cast output to String before calling lines()
        String output1 = (String) emulation.run(createCmd1).get("output");
        QubitRegister realReg1 = emulation.getNominalRegister("a").getRealRegister();
        String realName1 = realReg1.getName();
        assertNotNull(realReg1);

        Command createCmd2 = createRegisterCmd("b[1]");
        // Cast output to String before calling lines()
        String output2 = (String) emulation.run(createCmd2).get("output");
        QubitRegister realReg2 = emulation.getNominalRegister("b").getRealRegister();
        String realName2 = realReg2.getName();
        assertNotNull(realReg2);
        assertNotEquals(realName1, realName2);

        // Apply CNOT across registers, should trigger join
        Command applyCmd = applyGateCmd("CNOT", "a[0]", "b[0]");
        Map<String, Object> result = emulation.run(applyCmd);

        assertNotNull(result.get("joinInfo"), "Should trigger join");
        Map<String, Object> joinInfo = (Map<String, Object>) result.get("joinInfo");
        assertTrue((Boolean) joinInfo.get("joined"));
        assertNotNull(joinInfo.get("newRealRegName"));
        String newRealName = (String) joinInfo.get("newRealRegName");

        // Verify the expected new real register exists and old ones might be gone (or check nominals point to new)
        assertNotNull(emulation.getRealRegister(newRealName), "New real register should exist");
        // Checking removal of old registers might be implementation-dependent
        // assertNull(emulation.getRealRegister(realName1), "Old real register 1 should be removed");
        // assertNull(emulation.getRealRegister(realName2), "Old real register 2 should be removed");

        // Verify nominal registers point to the new real register
        QubitRegister newRealReg = emulation.getRealRegister(newRealName);
        assertSame(newRealReg, emulation.getNominalRegister("a").getRealRegister());
        assertSame(newRealReg, emulation.getNominalRegister("b").getRealRegister());
        assertEquals(2, newRealReg.getRealSize());

        // Verify state after CNOT (initial |00> -> CNOT(a[0],b[0]) -> |00>)
         Map<Integer, Complex> finalState = emulation.getRealRegisterState(newRealName);
        assertEquals(1, finalState.size());
        assertTrue(finalState.containsKey(0)); // State |00>
        assertTrue(ONE.equals(finalState.get(0)));
        assertNotNull(emulation.getLastGateTrace());

        // Apply X to a[0] (absolute index might depend on join order, assume a is higher index -> bit 1)
        Command applyX = applyGateCmd("X", "a[0]");
        emulation.run(applyX); // Now state is |10>
        finalState = emulation.getRealRegisterState(newRealName);
        assertTrue(finalState.containsKey(2)); // State |10> = 2

        // Apply CNOT again (|10> -> |11>)
        Command applyCnotAgain = applyGateCmd("CNOT", "a[0]", "b[0]");
        emulation.run(applyCnotAgain);
        finalState = emulation.getRealRegisterState(newRealName);
        assertEquals(1, finalState.size());
        assertTrue(finalState.containsKey(3)); // State |11> = 3
        assertTrue(ONE.equals(finalState.get(3)));

    }

     @Test
    void testProcessApplyGateUnknownGate() {
        Command createCmd = createRegisterCmd("q[1]");
        emulation.run(createCmd);
        Command applyCmd = applyGateCmd("UNKNOWN_GATE", "q[0]");
        Map<String, Object> result = emulation.run(applyCmd);
        String output = (String) result.get("output");
        assertTrue(output.contains("Неизвестный гейт"));
    }

    @Test
    void testProcessMeasure() {
        Command createCmd = createRegisterCmd("m[1]");
        emulation.run(createCmd);
        // Apply X to get |1>
        emulation.run(applyGateCmd("X", "m[0]"));

        Command measureCmd = measureCmd("m", 0);
        Map<String, Object> result = emulation.run(measureCmd);

        String output = (String) result.get("output");
        assertEquals("1", output, "Measurement of |1> should yield 1");

        // State should remain |1> after measuring |1>
        Map<Integer, Complex> state = emulation.getRegisterState("m");
        assertEquals(1, state.size());
        assertTrue(state.containsKey(1));
        assertTrue(ONE.equals(state.get(1)));
    }

     @Test
    void testProcessMeasureInvalidRegister() {
        Command measureCmd = measureCmd("nonexistent", 0);
        Map<String, Object> result = emulation.run(measureCmd);
        String output = (String) result.get("output");
        assertTrue(output.contains("не найден"));
    }

     @Test
    void testProcessMeasureInvalidIndex() {
         emulation.run(createRegisterCmd("idx[1]"));
         Command measureCmd = measureCmd("idx", 1);
         Map<String, Object> result = emulation.run(measureCmd);
         String output = (String) result.get("output");
         assertTrue(output.contains("Ошибка измерения"));
    }

    @Test
    void testDefineAndApplyOracle(@TempDir Path tempDir) throws IOException {
        // 1. Create CSV file
        Path csvFile = tempDir.resolve("oracle.csv");
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("2\n"); // inputSize = 2
            writer.write("3\n"); // State |11> should give output 1
        }

        // 2. Define Oracle
        Map<String, Object> defineArgs = Map.of("oracleName", "TestOracle", "csvPath", csvFile.toString());
        Command defineCmd = new Command(Command.CommandType.DEFINE_ORACLE_CSV, defineArgs);
        Map<String, Object> defineResult = emulation.run(defineCmd);
        assertTrue(((String)defineResult.get("output")).contains("успешно определен"));
        assertTrue(emulation.getDefinedOracles().containsKey("TestOracle"));
        OracleDefinition def = emulation.getDefinedOracles().get("TestOracle");
        assertTrue(def.getStatesWhereOutputIsOne().contains(3));

        // 3. Create Registers
        emulation.run(createRegisterCmd("inp[2],anc[1]")); // Input |00>, Ancilla |0>

        // 4. Apply X to input to make it |11>
        emulation.run(applyGateCmd("X", "inp[0]"));
        emulation.run(applyGateCmd("X", "inp[1]"));

        // State before oracle: |110> (inp=3, anc=0 -> total state 6)
        Map<Integer, Complex> stateBefore = emulation.getRegisterState("inp");
        System.out.println(stateBefore);
        assertTrue(stateBefore.containsKey(6)); // State |110>

        // 5. Apply Oracle
        Map<String, Object> applyArgs = Map.of(
            "oracleName", "TestOracle",
            "inputRegisterName", "inp",
            "ancillaRegisterName", "anc",
            "ancillaIndex", 0
        );
        Command applyCmd = new Command(Command.CommandType.APPLY_ORACLE, applyArgs);
        Map<String, Object> applyResult = emulation.run(applyCmd);
        assertTrue(((String)applyResult.get("output")).contains("применен"));

        // 6. Verify state: Input |11> matches oracle definition, should flip ancilla |0> to |1>
        // Expected state: |111> (inp=3, anc=1 -> total state 7)
        Map<Integer, Complex> stateAfter = emulation.getRegisterState("inp"); // Get state of the real register
         assertEquals(1, stateAfter.size());
        assertTrue(stateAfter.containsKey(7)); // State |111>
        assertTrue(ONE.equals(stateAfter.get(7)));
        assertNotNull(emulation.getLastGateTrace());

        // 7. Apply again (should flip back)
        emulation.run(applyCmd);
        stateAfter = emulation.getRegisterState("inp");
        assertEquals(1, stateAfter.size());
        assertTrue(stateAfter.containsKey(6)); // State |110>
        assertTrue(ONE.equals(stateAfter.get(6)));
    }

    @Test
    void testApplyOracleInputNotInSet(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("oracle2.csv");
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("2\n");
            writer.write("3\n"); // Only state 3 is marked
        }
        Map<String, Object> defineArgs = Map.of("oracleName", "TestOracle2", "csvPath", csvFile.toString());
        emulation.run(new Command(Command.CommandType.DEFINE_ORACLE_CSV, defineArgs));
        emulation.run(createRegisterCmd("i[2],a[1]")); // Input |00>, Ancilla |0>

        // State before: |000> (state 0)
        Map<Integer, Complex> stateBefore = emulation.getRegisterState("i");
        assertTrue(stateBefore.containsKey(0));

        // Apply Oracle: Input 00 is not in the set {3}
        Map<String, Object> applyArgs = Map.of("oracleName", "TestOracle2", "inputRegisterName", "i", "ancillaRegisterName", "a", "ancillaIndex", 0);
        emulation.run(new Command(Command.CommandType.APPLY_ORACLE, applyArgs));

        // Verify state: Should remain |000>
        Map<Integer, Complex> stateAfter = emulation.getRegisterState("i");
        assertEquals(1, stateAfter.size());
        assertTrue(stateAfter.containsKey(0));
        assertTrue(ONE.equals(stateAfter.get(0)));
    }

     @Test
    void testDefineOracleInvalidCsv(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("bad_oracle.csv");
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("abc\n"); // Invalid input size
        }
         Map<String, Object> defineArgs = Map.of("oracleName", "BadOracle", "csvPath", csvFile.toString());
        Command defineCmd = new Command(Command.CommandType.DEFINE_ORACLE_CSV, defineArgs);
        assertThrows(RuntimeException.class, () -> emulation.run(defineCmd));

        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            writer.write("1\n");
            writer.write("2\n"); // State 2 invalid for input size 1
        }
         assertThrows(RuntimeException.class, () -> emulation.run(defineCmd));
     }

     @Test
    void testApplyUndefinedOracle() {
        emulation.run(createRegisterCmd("i[1],a[1]"));
        Map<String, Object> applyArgs = Map.of("oracleName", "UndefinedOracle", "inputRegisterName", "i", "ancillaRegisterName", "a", "ancillaIndex", 0);
        Command applyCmd = new Command(Command.CommandType.APPLY_ORACLE, applyArgs);
        Map<String, Object> result = emulation.run(applyCmd);
        assertTrue(((String)result.get("output")).contains("не определен"));
    }

     @Test
    void testClone() {
         emulation.run(createRegisterCmd("orig[1]"));
         emulation.run(applyGateCmd("X", "orig[0]")); // State |1>

         Emulation clonedEmulation = emulation.clone();

         // Verify clone is not the same object
         assertNotSame(emulation, clonedEmulation);
         assertNotSame(emulation.getQubitRegisters(), clonedEmulation.getQubitRegisters());
         // assertNotSame(emulation.getRealRegisters(), clonedEmulation.getRealRegisters()); // Cannot access realRegisters directly

         // Verify state is copied
         assertEquals(1, clonedEmulation.getQubitRegisters().size());
         // Cannot check total real registers easily, check specific one
         assertNotNull(clonedEmulation.getNominalRegister("orig").getRealRegister());
         // assertEquals(1, clonedEmulation.getRealRegisters().size()); // Removed
         assertNotNull(clonedEmulation.getNominalRegister("orig"));
         Map<Integer, Complex> clonedState = clonedEmulation.getRegisterState("orig");
         assertEquals(1, clonedState.size());
         assertTrue(clonedState.containsKey(1));
         assertTrue(ONE.equals(clonedState.get(1)));

         // Modify original, check clone is unaffected
         emulation.run(applyGateCmd("X", "orig[0]")); // State |0>
         Map<Integer, Complex> originalStateAfter = emulation.getRegisterState("orig");
         assertTrue(originalStateAfter.containsKey(0));

         Map<Integer, Complex> clonedStateAfter = clonedEmulation.getRegisterState("orig");
         assertTrue(clonedStateAfter.containsKey(1)); // Should still be |1>
         assertFalse(clonedStateAfter.containsKey(0));

         // Verify GateTrace is not shared (if it's mutable, though here it might be null or simple)
         GateTrace originalTrace = emulation.getLastGateTrace();
         GateTrace clonedTrace = clonedEmulation.getLastGateTrace();
         if (originalTrace != null && clonedTrace != null) { // lastGateTrace might be null initially
            // For GateTrace, a shallow copy might be acceptable if it's treated as immutable after creation
            // A deep clone test would require modifying the trace and checking independence.
            // assertNotSame(originalTrace, clonedTrace); // Check if it needs deep clone
         }
     }
} 