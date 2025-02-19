package org.example.model.gate;

import lombok.Getter;
import org.example.model.qubit.Complex;

import java.util.HashMap;
import java.util.Map;

public class GateTrace {
    @Getter
    private Map<Integer, Map<Integer, Complex>> trace;

    public GateTrace() {
        trace = new HashMap<>();
    }

    public void addAmplitude(Integer stateFrom, Integer stateTo, Complex amplitude) {
        if(!trace.containsKey(stateFrom)) {
            trace.put(stateFrom, new HashMap<>());
        }
        Map<Integer, Complex> fromMap = trace.get(stateFrom);

        fromMap.put(stateTo, amplitude);
    }
}
