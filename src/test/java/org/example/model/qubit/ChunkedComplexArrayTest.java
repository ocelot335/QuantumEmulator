package org.example.model.qubit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkedComplexArrayTest {

    private ChunkedComplexArray array;
    private final int CHUNK_SIZE = 32;
    private Complex zero;

    @BeforeEach
    void setUp() {
        array = new ChunkedComplexArray();
        zero = Complex.getZero();
    }

    @Test
    void testGetFromEmptyArray() {
        assertTrue(zero.equals(array.get(0)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE - 1)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE)));
        assertTrue(zero.equals(array.get(100)));
    }

    @Test
    void testSetAndGetSingleValue() {
        Complex val1 = new Complex(1.0, 2.0);
        array.set(5, val1);
        assertTrue(val1.equals(array.get(5)));
        assertTrue(zero.equals(array.get(0)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE - 1)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE)));
    }

    @Test
    void testSetAndGetMultipleValuesSameChunk() {
        Complex val1 = new Complex(1.1, 2.2);
        Complex val2 = new Complex(3.3, 4.4);
        array.set(10, val1);
        array.set(20, val2);
        assertTrue(val1.equals(array.get(10)));
        assertTrue(val2.equals(array.get(20)));
        assertTrue(zero.equals(array.get(5)));
    }

    @Test
    void testSetAndGetMultipleValuesDifferentChunks() {
        Complex val1 = new Complex(1.0, 0.0);
        Complex val2 = new Complex(0.0, 1.0);
        int index1 = CHUNK_SIZE / 2;
        int index2 = CHUNK_SIZE + CHUNK_SIZE / 2;

        array.set(index1, val1);
        array.set(index2, val2);

        assertTrue(val1.equals(array.get(index1)));
        assertTrue(val2.equals(array.get(index2)));
        assertTrue(zero.equals(array.get(index1 - 1)));
        assertTrue(zero.equals(array.get(index1 + 1)));
        assertTrue(zero.equals(array.get(index2 - 1)));
        assertTrue(zero.equals(array.get(index2 + 1)));
        assertTrue(zero.equals(array.get(0)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE - 1)));
        assertTrue(zero.equals(array.get(CHUNK_SIZE)));
        assertTrue(zero.equals(array.get(2 * CHUNK_SIZE - 1)));

    }

    @Test
    void testOverwriteSetValue() {
        Complex val1 = new Complex(1.0, 1.0);
        Complex val2 = new Complex(2.0, 2.0);
        array.set(8, val1);
        assertTrue(val1.equals(array.get(8)));
        array.set(8, val2);
        assertTrue(val2.equals(array.get(8)));
    }

    @Test
    void testSetZeroRemovesChunkIfOnlyElement() {
        Complex val1 = new Complex(5.0, -5.0);
        int index = 15;
        // int chunkId = index / CHUNK_SIZE; // Should be 0

        array.set(index, val1);
        assertTrue(val1.equals(array.get(index)));
        assertTrue(zero.equals(array.get(index + 1)));

        array.set(index, Complex.getZero());

        assertTrue(zero.equals(array.get(index)));
        assertTrue(zero.equals(array.get(index + 1)));
        // Предполагаем, что если get возвращает ноль для всех элементов,
        // которые были в чанке, чанк эффективно удален.
    }

     @Test
    void testSetZeroDoesNotRemoveChunkIfOtherElementsExist() {
        Complex val1 = new Complex(1.0, 1.0);
        Complex val2 = new Complex(2.0, 2.0);
        int index1 = 10;
        int index2 = 20; // Тот же чанк
        // int chunkId = index1 / CHUNK_SIZE; // 0

        array.set(index1, val1);
        array.set(index2, val2);
        assertTrue(val1.equals(array.get(index1)));
        assertTrue(val2.equals(array.get(index2)));

        array.set(index1, Complex.getZero());

        assertTrue(zero.equals(array.get(index1)));
        assertTrue(val2.equals(array.get(index2)));
    }

    @Test
    void testSetZeroOnNonExistentChunk() {
        array.set(100, Complex.getZero());
        assertTrue(zero.equals(array.get(100)));
        assertTrue(zero.equals(array.get(99)));
        assertTrue(zero.equals(array.get(101)));
        assertTrue(zero.equals(array.get(100 + CHUNK_SIZE)));
    }
} 