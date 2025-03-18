package org.example.qgantt;

import javafx.scene.web.WebView;
import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QGanttManager {
    private final WebView webView;
    private final List<Map<String, Complex>> stateHistory;
    private final List<Map<String, Map<String, Complex>>> transitions;
    private final List<String> gateNames;
    private int numQubits = 0;

    public QGanttManager() {
        this.webView = new WebView();
        this.stateHistory = new ArrayList<>();
        this.transitions = new ArrayList<>();
        this.gateNames = new ArrayList<>();
        initializeWebView();
    }

    private void initializeWebView() {
        String htmlUrl = getClass().getResource("/qgantt.html").toExternalForm();

        webView.setPrefWidth(800);
        webView.setPrefHeight(600);
        webView.setMinWidth(400);
        webView.setMinHeight(300);

        webView.getEngine().load(htmlUrl);
    }

    public WebView getWebView() {
        return webView;
    }

    public void addState(Map<Integer, Complex> states) {
        Map<String, Complex> stateMap = new HashMap<>();

        if (states.isEmpty()) {
            return;
        }

        if (states.size() == 1 && states.containsKey(0)) {
            int maxState = states.keySet().iterator().next();
            while ((1 << numQubits) <= maxState) {
                numQubits++;
            }
        }
        for (Map.Entry<Integer, Complex> entry : states.entrySet()) {
            if (entry.getValue().equals(Complex.getZero())) {
                continue;
            }
            String binaryState = String.format("%" + numQubits + "s",
                    Integer.toBinaryString(entry.getKey())).replace(' ', '0');
            stateMap.put(binaryState, entry.getValue());
        }

        stateHistory.add(stateMap);
        updateDiagram();
    }

    public void addGateTrace(GateTrace trace, String gateName) {
        Map<String, Complex> stateMap = new HashMap<>();
        Map<String, Map<String, Complex>> transitionMap = new HashMap<>();

        for (Map.Entry<Integer, Map<Integer, Complex>> entry : trace.getTrace().entrySet()) {
            for (Map.Entry<Integer, Complex> targetState : entry.getValue().entrySet()) {
                if (targetState.getValue().equals(Complex.getZero())) {
                    continue;
                }
                String toStateBinary = String.format("%" + numQubits + "s",
                        Integer.toBinaryString(targetState.getKey())).replace(' ', '0');
                if (stateMap.containsKey(toStateBinary)) {
                    Complex existingAmplitude = stateMap.get(toStateBinary);
                    Complex newAmplitude = targetState.getValue();
                    Complex sum = existingAmplitude.add(newAmplitude);
                    if (!sum.equals(Complex.getZero())) {
                        stateMap.put(toStateBinary, sum);
                    } else {
                        stateMap.remove(toStateBinary);
                    }
                } else {
                    stateMap.put(toStateBinary, targetState.getValue());
                }
            }
        }

        for (Map.Entry<Integer, Map<Integer, Complex>> entry : trace.getTrace().entrySet()) {
            String fromState = String.format("%" + numQubits + "s",
                    Integer.toBinaryString(entry.getKey())).replace(' ', '0');

            Map<String, Complex> toStates = new HashMap<>();
            for (Map.Entry<Integer, Complex> targetState : entry.getValue().entrySet()) {
                if (targetState.getValue().equals(Complex.getZero())) {
                    continue;
                }
                String toStateBinary = String.format("%" + numQubits + "s",
                        Integer.toBinaryString(targetState.getKey())).replace(' ', '0');
                toStates.put(toStateBinary, targetState.getValue());
            }

            if (!toStates.isEmpty()) {
                transitionMap.put(fromState, toStates);
            }
        }

        stateHistory.add(stateMap);
        transitions.add(transitionMap);
        gateNames.add(gateName);
        updateDiagram();
    }

    private void updateDiagram() {
        String script = "updateQGantt(" + convertToJavaScript(stateHistory) + ", " +
                convertTransitionsToJavaScript() + ", " +
                convertGateNamesToJavaScript() +
                ");";

        webView.getEngine().executeScript(script);
    }

    private String convertToJavaScript(List<Map<String, Complex>> data) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) json.append(",");
            json.append("{");

            Map<String, Complex> stateMap = data.get(i);
            boolean first = true;
            for (Map.Entry<String, Complex> entry : stateMap.entrySet()) {
                if (!first) json.append(",");
                json.append("'").append(entry.getKey()).append("':'")
                        .append(entry.getValue().toString()).append("'");
                first = false;
            }

            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    private String convertTransitionsToJavaScript() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < transitions.size(); i++) {
            if (i > 0) json.append(",");
            json.append("{");

            Map<String, Map<String, Complex>> transitionMap = transitions.get(i);
            boolean first = true;
            for (Map.Entry<String, Map<String, Complex>> entry : transitionMap.entrySet()) {
                if (!first) json.append(",");
                json.append("'").append(entry.getKey()).append("':{");

                boolean innerFirst = true;
                for (Map.Entry<String, Complex> toState : entry.getValue().entrySet()) {
                    if (!innerFirst) json.append(",");
                    json.append("'").append(toState.getKey()).append("':'")
                            .append(toState.getValue().toString()).append("'");
                    innerFirst = false;
                }

                json.append("}");
                first = false;
            }

            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    private String convertGateNamesToJavaScript() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < gateNames.size(); i++) {
            if (i > 0) json.append(",");
            String gateName = gateNames.get(i);
            json.append("{name:'").append(gateName).append("'");

            Map<String, Map<String, Complex>> transitionMap = transitions.get(i);
            if (transitionMap != null && !transitionMap.isEmpty()) {
                String fromState = transitionMap.keySet().iterator().next();
                Map<String, Complex> toStates = transitionMap.get(fromState);
                if (!toStates.isEmpty()) {
                    String toState = toStates.keySet().iterator().next();
                    json.append(",indices:[");

                    if (gateName.startsWith("SWAP")) {
                        List<Integer> swapIndices = new ArrayList<>();
                        for (int j = 0; j < fromState.length(); j++) {
                            if (fromState.charAt(j) != toState.charAt(j)) {
                                swapIndices.add(j);
                            }
                        }
                        if (!swapIndices.isEmpty()) {
                            json.append(swapIndices.get(0));
                            if (swapIndices.size() > 1) {
                                json.append(",").append(swapIndices.get(1));
                            }
                        }
                    } else if (gateName.startsWith("C")) {
                        boolean first = true;
                        for (int j = 0; j < fromState.length(); j++) {
                            if (fromState.charAt(j) != toState.charAt(j)) {
                                if (!first) json.append(",");
                                json.append(j);
                                first = false;
                            }
                        }
                    } else {
                        boolean first = true;
                        for (int j = 0; j < fromState.length(); j++) {
                            if (fromState.charAt(j) != toState.charAt(j)) {
                                if (!first) json.append(",");
                                json.append(j);
                                first = false;
                            }
                        }
                    }
                    json.append("]");
                }
            }
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    public void clear() {
        stateHistory.clear();
        transitions.clear();
        gateNames.clear();
        numQubits = 0;
        updateDiagram();
    }

    public void setNumQubits(int n) {
        this.numQubits = n;
    }

    public List<Map<String, Complex>> getStateHistory() {
        return new ArrayList<>(stateHistory);
    }

    public List<Map<String, Map<String, Complex>>> getTransitions() {
        return new ArrayList<>(transitions);
    }

    public List<String> getGateNames() {
        return new ArrayList<>(gateNames);
    }

    public int getNumQubits() {
        return numQubits;
    }

    public void restoreHistory(List<Map<String, Complex>> stateHistory,
                               List<Map<String, Map<String, Complex>>> transitionsHistory,
                               List<String> gateNamesHistory) {
        this.stateHistory.clear();
        this.stateHistory.addAll(stateHistory);

        transitions.clear();
        transitions.addAll(transitionsHistory);

        gateNames.clear();
        gateNames.addAll(gateNamesHistory);
        updateDiagram();
    }

    public void removeLastState() {
        if (!stateHistory.isEmpty()) {
            stateHistory.remove(stateHistory.size() - 1);
            if (!transitions.isEmpty()) {
                transitions.remove(transitions.size() - 1);
            }
            if (!gateNames.isEmpty()) {
                gateNames.remove(gateNames.size() - 1);
            }
            updateDiagram();
        }
    }
} 