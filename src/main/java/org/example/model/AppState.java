package org.example.model;

import lombok.Data;
import org.example.model.gate.oracle.OracleDefinition;

import java.io.Serializable;
import java.util.*;

@Data
public class AppState implements Serializable {
    private Emulation context;
    private Map<String, OracleDefinition> definedOracles;
    private Stack<String> outputHistory;
    private List<String> commandList;
    private Integer outputCounter;
    private int currentLineIndex;
    private Map<String, Object> qganttRegistersData;

    public AppState(Emulation context, Stack<String> outputHistory,
                    List<String> commandList, Integer outputCounter, int currentLineIndex,
                    Map<String, OracleDefinition> definedOracles,
                    Map<String, Object> qganttRegistersData) {
        this.context = context;
        this.outputHistory = outputHistory;
        this.commandList = new ArrayList<>(commandList);
        this.outputCounter = outputCounter;
        this.currentLineIndex = currentLineIndex;
        this.definedOracles = new HashMap<>(definedOracles);
        this.qganttRegistersData = qganttRegistersData;
    }
}