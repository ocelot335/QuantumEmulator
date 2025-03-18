package org.example.model;

import lombok.Data;
import org.example.model.qubit.Complex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@Data
public class AppState implements Serializable {
    private Emulation context;
    private Stack<Emulation> stateHistory;
    private Stack<String> outputHistory;
    private List<String> commandList;
    private Integer outputCounter;
    private int currentLineIndex;
    private List<Map<String, Complex>> qganttStateHistory;
    private List<Map<String, Map<String, Complex>>> qganttTransitionHistory;
    private List<String> gateNamesHistory;
    private int numQubits;

    public AppState(Emulation context, Stack<Emulation> stateHistory, Stack<String> outputHistory,
                    List<String> commandList, Integer outputCounter, int currentLineIndex,
                    List<Map<String, Complex>> qganttStateHistory,
                    List<Map<String, Map<String, Complex>>> qganttTransitionHistory, List<String> gateNamesHistory, int numQubits) {
        this.context = context;
        this.stateHistory = stateHistory;
        this.outputHistory = outputHistory;
        this.commandList = new ArrayList<>(commandList);
        this.outputCounter = outputCounter;
        this.currentLineIndex = currentLineIndex;
        this.qganttStateHistory = new ArrayList<>(qganttStateHistory);
        this.qganttTransitionHistory = new ArrayList<>(qganttTransitionHistory);
        this.gateNamesHistory = new ArrayList<>(gateNamesHistory);
        this.numQubits = numQubits;
    }
}