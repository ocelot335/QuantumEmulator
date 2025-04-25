package org.example.qgantt;

import javafx.scene.web.WebView;
import lombok.Getter;
import org.example.model.gate.GateTrace;
import org.example.model.qubit.Complex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QGanttManager {
    @Getter
    private final WebView webView;
    private final Map<String, RegisterHistoryData> registersData;

    private static class RegisterHistoryData implements java.io.Serializable {
        final List<Map<String, Complex>> stateHistory;
        final List<Integer> stateVersions;
        final List<Map<String, Map<String, Complex>>> transitions;
        final List<String> gateNames;
        int numQubits;
        List<Map<String, Object>> nominalSpecs;
        List<String> parentRegisterNames;

        public RegisterHistoryData() {
            this.stateHistory = new ArrayList<>();
            this.stateVersions = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.gateNames = new ArrayList<>();
            this.numQubits = 0;
            this.nominalSpecs = new ArrayList<>();
            this.parentRegisterNames = new ArrayList<>();
        }
    }

    public QGanttManager() {
        this.webView = new WebView();
        this.registersData = new HashMap<>();
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

    public void createRegister(String realRegisterName, int numQubits, List<Map<String, Object>> nominalSpecs, List<String> parentNames) {
        if (!registersData.containsKey(realRegisterName)) {
            RegisterHistoryData registerData = new RegisterHistoryData();
            registerData.numQubits = numQubits;
            if (nominalSpecs != null) {
                registerData.nominalSpecs = new ArrayList<>(nominalSpecs);
            }
            if (parentNames != null) {
                registerData.parentRegisterNames = new ArrayList<>(parentNames);
            }
            registersData.put(realRegisterName, registerData);
        } else {
            RegisterHistoryData existingData = registersData.get(realRegisterName);
            if (nominalSpecs != null) {
                existingData.nominalSpecs = new ArrayList<>(nominalSpecs);
            }
            existingData.numQubits = numQubits;
            if (existingData.parentRegisterNames.isEmpty() && parentNames != null) {
                existingData.parentRegisterNames = new ArrayList<>(parentNames);
            }
        }
    }

    public void addState(String registerName, Map<Integer, Complex> states, int currentVersion) {
        if (!registersData.containsKey(registerName)) {
            System.err.println("Предупреждение: Попытка добавить состояние для регистра '" + registerName + "', который не был создан через createRegister. NumQubits может быть неверным.");
            createRegister(registerName, 0, null, null);
        }

        RegisterHistoryData registerData = registersData.get(registerName);
        Map<String, Complex> stateMap = new HashMap<>();

        if (registerData.numQubits <= 0 && !states.isEmpty()) {
            System.err.println("Предупреждение: numQubits не был установлен для регистра '" + registerName + "'. Пытаемся определить...");
            int maxState = states.keySet().stream().max(Integer::compareTo).orElse(0);
            int requiredBits = 0;
            if (maxState > 0) {
                requiredBits = Integer.SIZE - Integer.numberOfLeadingZeros(maxState);
            } else if (maxState == 0) {
                requiredBits = 1;
            }
            registerData.numQubits = Math.max(1, requiredBits);
            System.err.println("Определен numQubits=" + registerData.numQubits + " для регистра '" + registerName + "'");
        }

        if (!states.isEmpty()) {
            if (registerData.numQubits <= 0) {
                System.err.println("Критическая ошибка: Невозможно определить numQubits для регистра '" + registerName + "' для форматирования состояния.");
            } else {
                for (Map.Entry<Integer, Complex> entry : states.entrySet()) {
                    if (entry.getValue().equals(Complex.getZero())) {
                        continue;
                    }
                    if (entry.getKey() >= (1 << registerData.numQubits)) {
                        System.err.println("Ошибка: Состояние " + entry.getKey() + " выходит за пределы размера регистра " + registerName + " (numQubits=" + registerData.numQubits + ")");
                        continue;
                    }
                    String binaryState = String.format("%" + registerData.numQubits + "s",
                            Integer.toBinaryString(entry.getKey())).replace(' ', '0');
                    stateMap.put(binaryState, entry.getValue());
                }
            }
        }

        registerData.stateHistory.add(stateMap);
        registerData.stateVersions.add(currentVersion);
    }

    public void addGateTrace(String registerName, Map<Integer, Complex> finalStateMapInt, GateTrace trace, String gateName, int currentVersion) {
        if (!registersData.containsKey(registerName)) {
            System.err.println("Ошибка: Попытка добавить gate trace для несуществующего регистра: " + registerName);
            return;
        }

        RegisterHistoryData registerData = registersData.get(registerName);
        Map<String, Complex> finalStateMapString = new HashMap<>();
        Map<String, Map<String, Complex>> transitionMapForVis = new HashMap<>();

        if (finalStateMapInt != null && !finalStateMapInt.isEmpty()) {
            if (registerData.numQubits == 0) {
                int maxState = finalStateMapInt.keySet().stream().max(Integer::compareTo).orElse(0);
                while ((1 << registerData.numQubits) <= maxState) {
                    registerData.numQubits++;
                }
            }
            for (Map.Entry<Integer, Complex> entry : finalStateMapInt.entrySet()) {
                if (entry.getValue().equals(Complex.getZero())) {
                    continue;
                }
                String binaryState = String.format("%" + registerData.numQubits + "s",
                        Integer.toBinaryString(entry.getKey())).replace(' ', '0');
                finalStateMapString.put(binaryState, entry.getValue());
            }
        }

        System.out.println("QGantt: Received finalStateMapInt for " + registerName + " v" + currentVersion + ": " + finalStateMapInt); // Лог 3.1
        System.out.println("QGantt: Converted finalStateMapString for " + registerName + " v" + currentVersion + ": " + finalStateMapString); // Лог 3.2

        Map<String, Complex> previousStateMap = registerData.stateHistory.isEmpty() ?
                new HashMap<>() :
                registerData.stateHistory.get(registerData.stateHistory.size() - 1);
        transitionMapForVis.clear();
        if (trace != null && trace.getTrace() != null) {
            for (Map.Entry<Integer, Map<Integer, Complex>> fromEntry : trace.getTrace().entrySet()) {
                String fromStateBinary = String.format("%" + registerData.numQubits + "s",
                        Integer.toBinaryString(fromEntry.getKey())).replace(' ', '0');
                if (!previousStateMap.containsKey(fromStateBinary)) continue;

                Map<String, Complex> currentTransitions = new HashMap<>();
                for (Map.Entry<Integer, Complex> toEntry : fromEntry.getValue().entrySet()) {
                    if (toEntry.getValue().equals(Complex.getZero())) {
                        continue;
                    }
                    String toStateBinary = String.format("%" + registerData.numQubits + "s",
                            Integer.toBinaryString(toEntry.getKey())).replace(' ', '0');
                    currentTransitions.put(toStateBinary, toEntry.getValue());
                }
                if (!currentTransitions.isEmpty()) {
                    transitionMapForVis.put(fromStateBinary, currentTransitions);
                }
            }
        }

        registerData.stateHistory.add(finalStateMapString);
        registerData.stateVersions.add(currentVersion);
        registerData.transitions.add(transitionMapForVis);
        registerData.gateNames.add(gateName);
    }

    public void addMeasurementStep(String registerName, Map<Integer, Complex> finalStateMapInt, int measuredQubitRealIndex, int measurementResult, int currentVersion) {
        if (!registersData.containsKey(registerName)) {
            System.err.println("Ошибка: Попытка добавить шаг измерения для несуществующего регистра: " + registerName);
            return;
        }

        RegisterHistoryData registerData = registersData.get(registerName);
        Map<String, Complex> finalStateMapString = new HashMap<>();
        Map<String, Map<String, Complex>> measurementTransitions = new HashMap<>();

        if (finalStateMapInt != null && !finalStateMapInt.isEmpty()) {
            if (registerData.numQubits == 0) {
                int maxState = finalStateMapInt.keySet().stream().max(Integer::compareTo).orElse(0);
                int requiredBits = (maxState == 0) ? 1 : Integer.SIZE - Integer.numberOfLeadingZeros(maxState);
                registerData.numQubits = Math.max(1, requiredBits);
            }
            for (Map.Entry<Integer, Complex> entry : finalStateMapInt.entrySet()) {
                if (entry.getValue().equals(Complex.getZero())) {
                    continue;
                }
                if (entry.getKey() >= (1 << registerData.numQubits)) {
                    System.err.println("Ошибка: Состояние " + entry.getKey() + " выходит за пределы размера регистра " + registerName + " (numQubits=" + registerData.numQubits + ") при измерении.");
                    continue;
                }
                String binaryState = String.format("%" + registerData.numQubits + "s",
                        Integer.toBinaryString(entry.getKey())).replace(' ', '0');
                finalStateMapString.put(binaryState, entry.getValue());
            }
        }

        registerData.stateHistory.add(finalStateMapString);
        registerData.stateVersions.add(currentVersion);

        Map<String, Complex> previousStateMap = registerData.stateHistory.size() > 1 ?
                registerData.stateHistory.get(registerData.stateHistory.size() - 2) :
                new HashMap<>();

        for (String fromStateBinary : previousStateMap.keySet()) {
            try {
                int fromStateInt = Integer.parseInt(fromStateBinary, 2);
                if (((fromStateInt >> measuredQubitRealIndex) & 1) == measurementResult) {
                    Map<String, Complex> toMap = new HashMap<>();
                    toMap.put(fromStateBinary, Complex.getOne());
                    measurementTransitions.put(fromStateBinary, toMap);
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга состояния '" + fromStateBinary + "' при построении переходов измерения.");
            }
        }

        registerData.transitions.add(measurementTransitions);

        String gateNameWithIndex = "M[" + measuredQubitRealIndex + "]";
        registerData.gateNames.add(gateNameWithIndex);
    }

    private void updateDiagram() {
        int maxVersion = registersData.values().stream()
                .flatMapToInt(data -> data.stateVersions.stream().mapToInt(Integer::intValue))
                .max().orElse(0);
        updateDiagram(maxVersion);
    }

    private void updateDiagram(int targetVersion) {
        System.out.println("[QGantt Update] Target Version: " + targetVersion);
        if (targetVersion <= 0) {
            System.out.println("[QGantt Update] Clearing diagram for targetVersion <= 0.");
            String script = "updateMultipleQGantt({});";
            javafx.application.Platform.runLater(() -> {
                try {
                    webView.getEngine().executeScript(script);
                } catch (Exception e) {
                    System.err.println("Ошибка выполнения скрипта JavaScript для очистки: " + e.getMessage());
                }
            });
            return;
        }

        StringBuilder registersJson = new StringBuilder("{");
        boolean firstRegister = true;

        for (Map.Entry<String, RegisterHistoryData> entry : registersData.entrySet()) {
            String realRegisterName = entry.getKey();
            RegisterHistoryData registerData = entry.getValue();
            System.out.println("[QGantt Update] Checking register: " + realRegisterName + " (Versions: " + registerData.stateVersions + ")");

            boolean hasStatesAtTargetVersion = false;
            int minVersion = Integer.MAX_VALUE;
            for (int version : registerData.stateVersions) {
                if (version <= targetVersion) {
                    hasStatesAtTargetVersion = true;
                }
                if (version < minVersion) {
                    minVersion = version;
                }
            }
            System.out.println("[QGantt Update] Register: " + realRegisterName + ", Min Version: " + (minVersion == Integer.MAX_VALUE ? "N/A" : minVersion) + ", Has States <= Target: " + hasStatesAtTargetVersion);

            // Добавляем регистр в JSON только если у него есть состояния для этой версии
            if (hasStatesAtTargetVersion) {
                System.out.println("[QGantt Update] Including register: " + realRegisterName);
                if (!firstRegister) {
                    registersJson.append(",");
                }

                registersJson.append("'").append(realRegisterName).append("':{");
                registersJson.append("'states':").append(convertStatesToJavaScript(registerData.stateHistory, registerData.stateVersions, targetVersion)).append(",");
                registersJson.append("'transitions':").append(convertTransitionsToJavaScript(registerData.transitions, registerData.stateVersions, targetVersion)).append(",");
                registersJson.append("'gateNames':").append(convertGateNamesToJavaScript(registerData.gateNames, registerData.transitions, registerData.stateVersions, targetVersion)).append(",");
                registersJson.append("'numQubits':").append(registerData.numQubits).append(",");
                registersJson.append("'nominalSpecs':").append(convertNominalSpecsToJavaScript(registerData.nominalSpecs)).append(",");
                registersJson.append("'parents':").append(convertListStringToJson(registerData.parentRegisterNames));
                registersJson.append("}");

                firstRegister = false;
            }
        }

        registersJson.append("}");

        String script = "updateMultipleQGantt(" + registersJson + ");";
        System.out.println("[QGantt Update] Executing script: " + script.substring(0, Math.min(script.length(), 500)) + "..."); // Лог скрипта (укороченный)
        javafx.application.Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript(script);
            } catch (Exception e) {
                System.err.println("Ошибка выполнения скрипта JavaScript: " + e.getMessage());
            }
        });
    }

    private String convertStatesToJavaScript(List<Map<String, Complex>> states, List<Integer> versions, int targetVersion) {
        StringBuilder json = new StringBuilder("[");
        boolean firstState = true;
        for (int i = 0; i < states.size(); i++) {
            if (versions.get(i) <= targetVersion) { // Фильтруем по версии
                if (!firstState) json.append(",");
                json.append("{");

                Map<String, Complex> stateMap = states.get(i);
                boolean firstEntry = true;
                for (Map.Entry<String, Complex> entry : stateMap.entrySet()) {
                    if (!firstEntry) json.append(",");

                    json.append("'").append(entry.getKey()).append("':'")
                            .append(entry.getValue().toString()).append("'");
                    firstEntry = false;
                }

                json.append("}");
                firstState = false;
            }
        }
        json.append("]");
        return json.toString();
    }

    private String convertTransitionsToJavaScript(List<Map<String, Map<String, Complex>>> transitions, List<Integer> stateVersions, int targetVersion) {
        StringBuilder json = new StringBuilder("[");
        boolean firstTransition = true;
        for (int i = 0; i < transitions.size(); i++) {
            if (i + 1 < stateVersions.size() && stateVersions.get(i + 1) <= targetVersion) {
                if (!firstTransition) json.append(",");
                json.append("{");

                Map<String, Map<String, Complex>> transitionMap = transitions.get(i);
                boolean firstEntry = true;
                for (Map.Entry<String, Map<String, Complex>> entry : transitionMap.entrySet()) {
                    if (!firstEntry) json.append(",");
                    json.append("'").append(entry.getKey()).append("':{");

                    boolean innerFirst = true;
                    for (Map.Entry<String, Complex> toState : entry.getValue().entrySet()) {
                        if (!innerFirst) json.append(",");

                        json.append("'").append(toState.getKey()).append("':'")
                                .append(toState.getValue().toString()).append("'");
                        innerFirst = false;
                    }

                    json.append("}");
                    firstEntry = false;
                }

                json.append("}");
                firstTransition = false;
            }
        }
        json.append("]");
        return json.toString();
    }

    private String convertGateNamesToJavaScript(List<String> gateNames, List<Map<String, Map<String, Complex>>> transitions,
                                                List<Integer> stateVersions, int targetVersion) {
        StringBuilder json = new StringBuilder("[");
        boolean firstGate = true;

        for (int i = 0; i < gateNames.size(); i++) {
            if (i + 1 < stateVersions.size() && stateVersions.get(i + 1) <= targetVersion) {
                if (!firstGate) json.append(",");
                String gateName = gateNames.get(i);
                json.append("{name:'").append(gateName).append("'");

                if (i < transitions.size()) {
                    Map<String, Map<String, Complex>> transitionMap = transitions.get(i);
                    if (transitionMap != null && !transitionMap.isEmpty()) {
                        String fromState = transitionMap.keySet().iterator().next();
                        Map<String, Complex> toStates = transitionMap.get(fromState);
                        if (toStates != null && !toStates.isEmpty()) {
                            String toState = toStates.keySet().iterator().next();
                            json.append(",indices:[");
                            boolean firstIndex = true;
                            int len = Math.min(fromState.length(), toState.length());
                            for (int j = 0; j < len; j++) {
                                if (fromState.charAt(j) != toState.charAt(j)) {
                                    if (!firstIndex) json.append(",");
                                    json.append(j);
                                    firstIndex = false;
                                }
                            }
                            json.append("]");
                        } else {
                            json.append(",indices:[]");
                        }
                    } else {
                        json.append(",indices:[]");
                    }
                } else {
                    json.append(",indices:[]");
                }
                json.append("}");
                firstGate = false;
            }
        }
        json.append("]");
        return json.toString();
    }

    private String convertNominalSpecsToJavaScript(List<Map<String, Object>> specs) {
        if (specs == null || specs.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        boolean firstSpec = true;
        for (Map<String, Object> spec : specs) {
            if (!firstSpec) json.append(",");
            json.append("{");
            json.append("'name':'").append(spec.getOrDefault("name", "")).append("',");
            json.append("'size':").append(spec.getOrDefault("size", 0)).append(",");
            json.append("'offset':").append(spec.getOrDefault("offset", 0));
            json.append("}");
            firstSpec = false;
        }
        json.append("]");
        return json.toString();
    }

    public void clear() {
        registersData.clear();
        updateDiagram(0);
    }

    public void clearRegister(String registerName) {
        if (registersData.containsKey(registerName)) {
            RegisterHistoryData registerData = new RegisterHistoryData();
            registerData.numQubits = registersData.get(registerName).numQubits;
            registersData.put(registerName, registerData);
            updateDiagram();
        }
    }

    public Map<String, Object> getRegistersData() {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, RegisterHistoryData> entry : registersData.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public void restoreRegistersData(Map<String, Object> data) {
        registersData.clear();
        if (data == null) return;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String registerName = entry.getKey();
            if (entry.getValue() instanceof RegisterHistoryData) {
                registersData.put(registerName, (RegisterHistoryData) entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                try {
                    Map<String, Object> registerDataMap = (Map<String, Object>) entry.getValue();
                    RegisterHistoryData registerData = new RegisterHistoryData();

                    if (registerDataMap.containsKey("stateHistory") && registerDataMap.get("stateHistory") instanceof List) {
                        registerData.stateHistory.addAll((List<Map<String, Complex>>) registerDataMap.get("stateHistory"));
                    }
                    if (registerDataMap.containsKey("transitions") && registerDataMap.get("transitions") instanceof List) {
                        registerData.transitions.addAll((List<Map<String, Map<String, Complex>>>) registerDataMap.get("transitions"));
                    }
                    if (registerDataMap.containsKey("gateNames") && registerDataMap.get("gateNames") instanceof List) {
                        registerData.gateNames.addAll((List<String>) registerDataMap.get("gateNames"));
                    }
                    if (registerDataMap.containsKey("numQubits") && registerDataMap.get("numQubits") instanceof Integer) {
                        registerData.numQubits = (int) registerDataMap.get("numQubits");
                    }

                    if (registerDataMap.containsKey("stateVersions") && registerDataMap.get("stateVersions") instanceof List) {
                        registerData.stateVersions.addAll((List<Integer>) registerDataMap.get("stateVersions"));
                    } else {
                        for (int i = 0; i < registerData.stateHistory.size(); ++i)
                            registerData.stateVersions.add(i + 1);
                    }

                    if (registerDataMap.containsKey("nominalSpecs") && registerDataMap.get("nominalSpecs") instanceof List) {
                        try {
                            registerData.nominalSpecs.addAll((List<Map<String, Object>>) registerDataMap.get("nominalSpecs"));
                        } catch (ClassCastException cce) {
                            System.err.println("Ошибка каста nominalSpecs для регистра '" + registerName + "'.");
                        }
                    } else {
                        if (registerData.numQubits > 0) {
                            Map<String, Object> defaultSpec = new HashMap<>();
                            defaultSpec.put("name", registerName);
                            defaultSpec.put("size", registerData.numQubits);
                            defaultSpec.put("offset", 0);
                            registerData.nominalSpecs.add(defaultSpec);
                        }
                    }

                    if (registerDataMap.containsKey("parentRegisterNames") && registerDataMap.get("parentRegisterNames") instanceof List) {
                        try {
                            registerData.parentRegisterNames.addAll((List<String>) registerDataMap.get("parentRegisterNames"));
                        } catch (ClassCastException cce) {
                            System.err.println("Ошибка каста parentRegisterNames для регистра '" + registerName + "'.");
                        }
                    }

                    registersData.put(registerName, registerData);
                } catch (Exception e) {
                    System.err.println("Ошибка восстановления данных регистра '" + registerName + "' из старого формата: " + e.getMessage());
                }
            }
        }
    }

    public void updateDiagramToStep(int step) {
        updateDiagram(step);
    }

    public int getMaxVersion() {
        int maxVersion = 0;
        for (RegisterHistoryData data : registersData.values()) {
            if (!data.stateVersions.isEmpty()) {
                int registerMax = data.stateVersions.stream().max(Integer::compareTo).orElse(0);
                if (registerMax > maxVersion) {
                    maxVersion = registerMax;
                }
            }
        }
        return maxVersion;
    }

    private String convertListStringToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String item : list) {
            if (!first) json.append(",");
            json.append("'").append(item.replace("'", "\\'")).append("'");
            first = false;
        }
        json.append("]");
        return json.toString();
    }
}