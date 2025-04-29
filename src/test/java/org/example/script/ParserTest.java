package org.example.script;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    void testParseCreateRegisterValid() {
        String line = "CR regA[3], regB[1]";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.CREATE_REGISTER, command.getType());

        Map<String, Object> args = command.getArguments();
        assertEquals("real_reg_0", args.get("realRegisterName")); // Assuming counter starts at 0
        assertEquals(4, args.get("realRegisterSize"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nominals = (List<Map<String, Object>>) args.get("nominalRegisters");
        assertNotNull(nominals);
        assertEquals(2, nominals.size());

        // Note: Parsing order might affect offsets, assuming regB is parsed second
        Map<String, Object> specA = nominals.get(0);
        assertEquals("regA", specA.get("name"));
        assertEquals(3, specA.get("size"));
        assertEquals(1, specA.get("offset")); // 4 - 3 = 1

        Map<String, Object> specB = nominals.get(1);
        assertEquals("regB", specB.get("name"));
        assertEquals(1, specB.get("size"));
        assertEquals(0, specB.get("offset")); // 1 - 1 = 0
    }

    @Test
    void testParseCreateRegisterSingle() {
        String line = "CR myReg[5]";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.CREATE_REGISTER, command.getType());
        assertEquals(5, command.getArgumentAsInt("realRegisterSize"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nominals = (List<Map<String, Object>>) command.getArgument("nominalRegisters");
        assertEquals(1, nominals.size());
        assertEquals("myReg", nominals.get(0).get("name"));
        assertEquals(5, nominals.get(0).get("size"));
        assertEquals(0, nominals.get(0).get("offset"));
    }

    @Test
    void testParseCreateRegisterInvalidSyntax() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR reg"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR reg[abc]"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR reg[-1]"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR reg[3], reg[2]")); // Duplicate name
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR "));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR reg1[1] reg2[1]")); // Missing comma
    }

    @Test
    void testParseApplyGateValid() {
        String line = "H q[0]";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.APPLY_GATE, command.getType());
        assertEquals("H", command.getArgumentAsString("gate"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> operands = (List<Map<String, Object>>) command.getArgument("operands");
        assertEquals(1, operands.size());
        assertEquals("q", operands.get(0).get("register"));
        assertEquals(0, operands.get(0).get("index"));

        line = "CNOT ctrl[1] target[0]"; // CNOT is not explicitly listed but should parse as APPLY_GATE
        command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.APPLY_GATE, command.getType());
        assertEquals("CNOT", command.getArgumentAsString("gate"));
        operands = (List<Map<String, Object>>) command.getArgument("operands");
        assertEquals(2, operands.size());
        assertEquals("ctrl", operands.get(0).get("register"));
        assertEquals(1, operands.get(0).get("index"));
        assertEquals("target", operands.get(1).get("register"));
        assertEquals(0, operands.get(1).get("index"));
    }

    @Test
    void testParseApplyGateInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("X ")); // No operands
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("X q")); // Invalid operand format
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("X q[a]")); // Invalid index
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("X q[-1]")); // Invalid index
    }

    @Test
    void testParseMeasureValid() {
        String line = "M result[5]";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.MEASURE, command.getType());
        assertEquals("result", command.getArgumentAsString("register"));
        assertEquals(5, command.getArgumentAsInt("index"));
    }

    @Test
    void testParseMeasureInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("M "));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("M result"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("M result[a]"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("M result[1][2]"));
    }

     @Test
    void testParseDefineOracleCsvValid() {
        String line = "DEFINE_ORACLE_CSV MyOracle \"data/my_oracle.csv\"";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.DEFINE_ORACLE_CSV, command.getType());
        assertEquals("MyOracle", command.getArgumentAsString("oracleName"));
        assertEquals("data/my_oracle.csv", command.getArgumentAsString("csvPath"));
    }

    @Test
    void testParseDefineOracleCsvInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("DEFINE_ORACLE_CSV MyOracle")); // Missing path
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("DEFINE_ORACLE_CSV MyOracle path")); // Path not quoted
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("DEFINE_ORACLE_CSV \"path\"")); // Missing name
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("DEFINE_ORACLE_CSV Name \"\"")); // Empty path
    }

    @Test
    void testParseApplyOracleValid() {
        String line = "APPLY_ORACLE SearchOracle InputReg -> AncillaReg[0]";
        Command command = Parser.parse(line);
        assertNotNull(command);
        assertEquals(Command.CommandType.APPLY_ORACLE, command.getType());
        assertEquals("SearchOracle", command.getArgumentAsString("oracleName"));
        assertEquals("InputReg", command.getArgumentAsString("inputRegisterName"));
        assertEquals("AncillaReg", command.getArgumentAsString("ancillaRegisterName"));
        assertEquals(0, command.getArgumentAsInt("ancillaIndex"));
    }

     @Test
    void testParseApplyOracleInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("APPLY_ORACLE Name Input")); // Missing -> and ancilla
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("APPLY_ORACLE Name Input -> Ancilla")); // Missing ancilla index
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("APPLY_ORACLE Name -> Ancilla[0]")); // Missing input
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("APPLY_ORACLE Name Input -> Ancilla[a]")); // Invalid index
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("APPLY_ORACLE Name Input -> AncillaReg [-1]")); // Invalid index
    }

    @Test
    void testParseCommentAndEmpty() {
        assertNull(Parser.parse(""));
        assertNull(Parser.parse("  "));
        assertNull(Parser.parse("# This is a comment"));
        assertNull(Parser.parse("  # Another comment"));
    }

    @Test
    void testParseUnknownCommand() {
         // Test cases that don't match any known command structure
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("UNKNOWN_CMD arg1"));
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("MEASURE_ALL regs")); // similar but not exact
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("CR X Y Z")); // CR needs size
        assertThrows(IllegalArgumentException.class, () -> Parser.parse("123 start")); // starts with number
    }
} 