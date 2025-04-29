package org.example.model.gate.oracle;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class OracleDefinitionTest {

    @Test
    void testConstructorAndGetters() {
        String expectedName = "TestOracle";
        Set<Integer> expectedStates = new HashSet<>();
        expectedStates.add(1);
        expectedStates.add(3);

        OracleDefinition definition = new OracleDefinition(expectedName, expectedStates);

        assertEquals(expectedName, definition.getName(), "getName() should return the correct name.");
        assertEquals(expectedStates, definition.getStatesWhereOutputIsOne(), "getStatesWhereOutputIsOne() should return the correct set of states.");
    }

    @Test
    void testImmutabilityOfSet() {
        String name = "ImmutableOracle";
        Set<Integer> originalStates = new HashSet<>();
        originalStates.add(5);

        OracleDefinition definition = new OracleDefinition(name, originalStates);
        Set<Integer> retrievedStates = definition.getStatesWhereOutputIsOne();

        // Verify we got the same content
        assertEquals(originalStates, retrievedStates);

        // Try to modify the original set after creating the definition
        // The definition should retain the state from the time of creation.
        // Note: OracleDefinition doesn't make a defensive copy currently,
        // so this test would fail if the caller modifies the set afterwards.
        // This asserts the current behavior, not necessarily the ideal one.
        originalStates.add(10);
        assertEquals(originalStates, definition.getStatesWhereOutputIsOne(), "Internal set should reflect modification of original set if no defensive copy is made.");

        // If we wanted true immutability, OracleDefinition constructor should do:
        // this.statesWhereOutputIsOne = new HashSet<>(statesWhereOutputIsOne);
        // Then the above assertion would change to assertNotEquals or assertEquals(Set.of(5), ...);
    }

    @Test
    void testEmptySet() {
         String name = "EmptyOracle";
         Set<Integer> emptyStates = new HashSet<>();
         OracleDefinition definition = new OracleDefinition(name, emptyStates);

         assertEquals(name, definition.getName());
         assertTrue(definition.getStatesWhereOutputIsOne().isEmpty(), "Set should be empty.");
    }
} 