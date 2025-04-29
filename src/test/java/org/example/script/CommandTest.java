package org.example.script;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

    @Test
    void testConstructorAndGetters() {
        Command.CommandType expectedType = Command.CommandType.APPLY_GATE;
        Map<String, Object> expectedArgs = new HashMap<>();
        expectedArgs.put("gate", "H");
        expectedArgs.put("targetRegister", "q");
        expectedArgs.put("targetIndex", 0);

        Command command = new Command(expectedType, expectedArgs);

        assertEquals(expectedType, command.getType(), "getType() should return the correct command type.");
        assertEquals(expectedArgs, command.getArguments(), "getArguments() should return the correct arguments map.");
    }

    @Test
    void testGetArgumentMethods() {
        Map<String, Object> args = new HashMap<>();
        args.put("stringArg", "testValue");
        args.put("intArg", 123);
        args.put("otherArg", new Object());

        Command command = new Command(Command.CommandType.MEASURE, args);

        assertEquals("testValue", command.getArgument("stringArg"), "getArgument() should return the correct object.");
        assertEquals(123, command.getArgument("intArg"), "getArgument() should return the correct object.");
        assertNotNull(command.getArgument("otherArg"), "getArgument() should return the correct object.");
        assertNull(command.getArgument("nonExistentArg"), "getArgument() should return null for non-existent key.");

        assertEquals("testValue", command.getArgumentAsString("stringArg"), "getArgumentAsString() should return the correct String.");
        assertEquals(123, command.getArgumentAsInt("intArg"), "getArgumentAsInt() should return the correct Integer.");

        // Test potential ClassCastExceptions (though current usage might prevent this)
        assertThrows(ClassCastException.class, () -> command.getArgumentAsString("intArg"),
                     "getArgumentAsString() should throw ClassCastException for wrong type.");
        assertThrows(ClassCastException.class, () -> command.getArgumentAsInt("stringArg"),
                     "getArgumentAsInt() should throw ClassCastException for wrong type.");
    }

    @Test
    void testToString() {
        Map<String, Object> args = new HashMap<>();
        args.put("gate", "X");
        args.put("target", "r[0]");
        Command command = new Command(Command.CommandType.APPLY_GATE, args);

        String expectedString = "Command{type=APPLY_GATE, arguments={gate=X, target=r[0]}}";
        assertEquals(expectedString, command.toString(), "toString() should return the expected string representation.");
    }
} 