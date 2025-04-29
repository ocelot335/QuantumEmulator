package org.example.model.qubit;

import java.io.Serializable;
import java.util.HashMap;

public class ChunkedComplexArray implements Serializable, Cloneable {
    private final HashMap<Integer, Complex[]> innerArray;
    private final int CHUNK_SIZE = 32;

    public ChunkedComplexArray() {
        innerArray = new HashMap<>();
    }

    public void set(int id, Complex value) {
        if (!innerArray.containsKey(id / CHUNK_SIZE)) {
            Complex[] newChunk = new Complex[CHUNK_SIZE];
            for (int i = 0; i < CHUNK_SIZE; i++) {
                newChunk[i] = Complex.getZero();
            }
            innerArray.put(id / CHUNK_SIZE, newChunk);
        }
        Complex[] chunk = innerArray.get(id / CHUNK_SIZE);
        chunk[id % CHUNK_SIZE] = value;
        if (value.isZero()) {
            boolean isAllZero = true;
            for (int i = 0; i < CHUNK_SIZE; i++) {
                if (!chunk[i].isZero()) {
                    isAllZero = false;
                    break;
                }
            }
            if (isAllZero) {
                innerArray.remove(id / CHUNK_SIZE);
            }
        }
    }

    public void clear() {
        innerArray.clear();
    }

    public Complex get(int id) {
        if (!innerArray.containsKey(id / CHUNK_SIZE)) {
            return Complex.getZero();
        }
        return innerArray.get(id / CHUNK_SIZE)[id % CHUNK_SIZE];
    }
}
