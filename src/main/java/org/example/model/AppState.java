package org.example.model;

import lombok.Data;
import org.example.model.Emulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Data
public class AppState implements Serializable {
    private Emulation context;
    private Stack<Emulation> stateHistory;
    private Stack<String> outputHistory;
    private List<String> commandList;
    private Integer outputCounter;
    private int currentLineIndex;

    public AppState(Emulation context, Stack<Emulation> stateHistory, Stack<String> outputHistory,
                    List<String> commandList, Integer outputCounter, int currentLineIndex) {
        this.context = context;
        this.stateHistory = stateHistory;
        this.outputHistory = outputHistory;
        this.commandList = new ArrayList<>(commandList); // Копируем список
        this.outputCounter = outputCounter;
        this.currentLineIndex = currentLineIndex;
    }

    // Геттеры для восстановления состояния
    public Emulation getContext() {
        return context;
    }

    public Stack<Emulation> getStateHistory() {
        return stateHistory;
    }

    public Stack<String> getOutputHistory() {
        return outputHistory;
    }

    public List<String> getCommandList() {
        return commandList;
    }

    public Integer getOutputCounter() {
        return outputCounter;
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }
}