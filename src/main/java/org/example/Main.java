package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Emulation;
import org.example.model.qubit.Complex;
import org.example.model.qubit.QubitRegister;
import org.example.qgantt.QGanttManager;
import org.example.script.Command;
import org.example.script.Parser;
import org.example.syntax.SyntaxHighlighter;
import org.example.translation.QuantumTranslator;
import org.example.translation.TranslatorFactory;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends Application {

    private Emulation context = new Emulation();
    private Integer versionCounter = 0;
    private int currentLineIndex = -1;
    private List<String> commandList = new ArrayList<>();
    private Stack<String> outputHistory = new Stack<>();
    private CodeArea commandInput;
    private CodeArea measurementOutput;
    private QGanttManager qganttManager;

    @Override
    public void start(Stage stage) {
        qganttManager = new QGanttManager();
        setupCodeArea();
        setupMeasurementOutput();

        Button executeAllButton = new Button("Выполнить все");
        Button stepForwardButton = new Button("Шаг вперёд");
        Button stepBackwardButton = new Button("Шаг назад");

        executeAllButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        stepForwardButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        stepBackwardButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        MenuBar menuBar = new MenuBar();

        Menu scriptMenu = new Menu("Скрипт");
        MenuItem saveScriptItem = new MenuItem("Сохранить скрипт");
        MenuItem loadScriptItem = new MenuItem("Загрузить скрипт");
        scriptMenu.getItems().addAll(saveScriptItem, loadScriptItem);

        Menu emulationMenu = new Menu("Эмуляция");
        MenuItem saveModelItem = new MenuItem("Сохранить модель");
        MenuItem loadModelItem = new MenuItem("Загрузить модель");
        emulationMenu.getItems().addAll(saveModelItem, loadModelItem);

        Menu translationMenu = new Menu("Трансляция");
        MenuItem qiskitItem = new MenuItem("Qiskit");
        MenuItem qsharpItem = new MenuItem("Q#");
        MenuItem cirqItem = new MenuItem("Cirq");
        translationMenu.getItems().addAll(qiskitItem, qsharpItem, cirqItem);

        menuBar.getMenus().addAll(scriptMenu, emulationMenu, translationMenu);

        saveScriptItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить скрипт");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.write(commandInput.getText());
                } catch (IOException ex) {
                    measurementOutput.appendText("Ошибка при сохранении скрипта: " + ex.getMessage() + "\n");
                }
            }
        });

        loadScriptItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Загрузить скрипт");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    commandInput.replaceText(content.toString());
                    commandList.clear();
                    currentLineIndex = -1;
                    outputHistory.clear();
                    versionCounter = 0;
                    qganttManager.clear();
                    measurementOutput.clear();
                } catch (IOException ex) {
                    measurementOutput.appendText("Ошибка при загрузке скрипта: " + ex.getMessage() + "\n");
                }
            }
        });

        saveModelItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить модель");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Model Files", "*.model")
            );
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                    Emulation currentState = context.clone();

                    Stack<String> newOutputHistory = new Stack<>();
                    newOutputHistory.addAll(outputHistory);

                    org.example.model.AppState appState = new org.example.model.AppState(
                            currentState,
                            newOutputHistory,
                            commandList,
                            versionCounter,
                            currentLineIndex,
                            currentState.getDefinedOracles(),
                            qganttManager.getRegistersData()
                    );
                    oos.writeObject(appState);
                    measurementOutput.appendText("Модель успешно сохранена\n");
                } catch (IOException ex) {
                    measurementOutput.appendText("Ошибка при сохранении модели: " + ex.getMessage() + "\n");
                }
            }
        });

        loadModelItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Загрузить модель");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Model Files", "*.model")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    org.example.model.AppState appState = (org.example.model.AppState) ois.readObject();

                    context = appState.getContext();
                    outputHistory = appState.getOutputHistory();
                    commandList = appState.getCommandList();
                    versionCounter = appState.getOutputCounter();
                    currentLineIndex = appState.getCurrentLineIndex();

                    commandInput.replaceText(String.join("\n", commandList));

                    qganttManager.clear();

                    qganttManager.restoreRegistersData(
                            appState.getQganttRegistersData()
                    );

                    qganttManager.updateDiagramToStep(versionCounter);

                    if (currentLineIndex >= 0 && currentLineIndex < commandList.size()) {
                        highlightCurrentLine(commandInput, currentLineIndex);
                    } else {
                        commandInput.deselect();
                    }
                    measurementOutput.appendText("Модель успешно загружена\n");
                } catch (IOException ex) {
                    measurementOutput.appendText("Ошибка при загрузке модели: " + ex.getMessage() + "\n");
                } catch (ClassNotFoundException ex) {
                    measurementOutput.appendText("Ошибка: Неверный формат файла модели\n");
                }
            }
        });

        qiskitItem.setOnAction(e -> {
            String result = translateToPlatform("Qiskit");
            saveTranslationResult(result, "Qiskit", stage);
        });

        qsharpItem.setOnAction(e -> {
            String result = translateToPlatform("Q#");
            saveTranslationResult(result, "Q#", stage);
        });

        cirqItem.setOnAction(e -> {
            String result = translateToPlatform("Cirq");
            saveTranslationResult(result, "Cirq", stage);
        });

        executeAllButton.setOnAction(e -> {
            String commands = commandInput.getText().trim();
            versionCounter = 0;
            currentLineIndex = -1;
            context = new Emulation();
            outputHistory.clear();
            qganttManager.clear();
            measurementOutput.clear();

            if (!commands.isEmpty()) {
                commandList = Arrays.asList(commands.split("\\r?\\n"));
                for (int i = 0; i < commandList.size(); i++) {
                    String command = commandList.get(i).trim();
                    if (!command.isEmpty()) {
                        currentLineIndex = i;
                        executeCommand(command);
                        highlightCurrentLine(commandInput, i);
                    }
                }
                if (currentLineIndex >= 0) {
                    highlightCurrentLine(commandInput, currentLineIndex);
                }
                qganttManager.updateDiagramToStep(versionCounter);
            }
        });

        stepForwardButton.setOnAction(e -> {
            String commands = commandInput.getText().trim();
            if (commands.isEmpty()) {
                return;
            }

            List<String> currentCommands = Arrays.asList(commands.split("\\r?\\n"));
            boolean scriptChanged = commandList.isEmpty() || !currentCommands.equals(commandList);

            if (scriptChanged) {
                commandList = currentCommands;
                currentLineIndex = -1;
                context = new Emulation();
                versionCounter = 0;
                outputHistory.clear();
                qganttManager.clear();
                commandInput.deselect();
                measurementOutput.clear();
            }

            int nextLineIndex = currentLineIndex + 1;
            while (nextLineIndex < commandList.size() &&
                    commandList.get(nextLineIndex).trim().isEmpty()) {
                nextLineIndex++;
            }

            if (nextLineIndex >= commandList.size()) {
                return;
            }

            int maxVersionInHistory = qganttManager.getMaxVersion();
            int targetVersion = versionCounter + 1;

            if (targetVersion > maxVersionInHistory) {
                currentLineIndex = nextLineIndex;
                String command = commandList.get(currentLineIndex).trim();

                int versionBeforeExecute = versionCounter;

                executeCommand(command);

                if (versionCounter > versionBeforeExecute) {
                    qganttManager.updateDiagramToStep(versionCounter);
                    highlightCurrentLine(commandInput, currentLineIndex);
                } else {
                    if (currentLineIndex >= 0) highlightCurrentLine(commandInput, currentLineIndex);
                }

            } else {
                currentLineIndex = nextLineIndex;
                versionCounter = targetVersion;
                qganttManager.updateDiagramToStep(versionCounter);
                highlightCurrentLine(commandInput, currentLineIndex);
            }
        });

        stepBackwardButton.setOnAction(e -> {
            if (versionCounter > 0) {
                int targetVersion = versionCounter - 1;

                int previousLineIndex = currentLineIndex - 1;
                while (previousLineIndex >= 0 && commandList.get(previousLineIndex).trim().isEmpty()) {
                    previousLineIndex--;
                }

                qganttManager.updateDiagramToStep(targetVersion);

                versionCounter = targetVersion;
                currentLineIndex = previousLineIndex;

                if (currentLineIndex >= 0) {
                    highlightCurrentLine(commandInput, currentLineIndex);
                } else {
                    commandInput.deselect();
                }
            }
        });

        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label inputLabel = new Label("Введите команды:");
        inputLabel.setStyle("-fx-font-weight: bold;");
        leftPanel.getChildren().addAll(inputLabel, commandInput);

        Label measurementLabel = new Label("Результаты измерений:");
        measurementLabel.setStyle("-fx-font-weight: bold;");
        leftPanel.getChildren().addAll(measurementLabel, measurementOutput);

        HBox controlButtons = new HBox(10);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.getChildren().addAll(executeAllButton, stepForwardButton, stepBackwardButton);
        leftPanel.getChildren().add(controlButtons);

        VBox.setVgrow(commandInput, Priority.ALWAYS);
        VBox.setVgrow(measurementOutput, Priority.ALWAYS);

        HBox mainContent = new HBox(5);
        mainContent.setPadding(new Insets(5));
        mainContent.getChildren().addAll(leftPanel, qganttManager.getWebView());
        HBox.setHgrow(qganttManager.getWebView(), Priority.ALWAYS);
        HBox.setHgrow(leftPanel, Priority.SOMETIMES);

        leftPanel.setPrefWidth(400);
        leftPanel.setMinWidth(300);
        qganttManager.getWebView().setPrefWidth(800);

        VBox root = new VBox(5, menuBar, mainContent);
        root.setPadding(new Insets(5));
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        qganttManager.getWebView().setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-color: white;");
        menuBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        root.setStyle("-fx-background-color: white;");

        scene.getStylesheets().add("data:text/css," + SyntaxHighlighter.getStyle());

        stage.setTitle("Quantum Emulator");
        stage.setMaximized(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    private void executeCommand(String command) {
        try {
            Command parsedCommand = Parser.parse(command);
            outputHistory.push(measurementOutput.getText());

            Map<String, Object> runResult = context.run(parsedCommand);
            String output = (String) runResult.getOrDefault("output", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> joinInfo = (Map<String, Object>) runResult.get("joinInfo");

            if (parsedCommand.getType() == Command.CommandType.MEASURE) {
                if (output != null && !output.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> operand = parsedCommand.getArguments();
                        String nominalRegisterName = (String) operand.get("register");
                        int nominalIndex = (int) operand.get("index");
                        int measurementResult = Integer.parseInt(output);

                        QubitRegister nominalReg = context.getNominalRegister(nominalRegisterName);
                        if (nominalReg != null) {
                            String targetRealRegisterName = nominalReg.getRealRegister().getName();
                            int measuredQubitRealIndex = nominalReg.getOffsetInRealRegister() + nominalIndex;
                            Map<Integer, Complex> finalState = context.getRealRegisterState(targetRealRegisterName);

                            versionCounter++;
                            qganttManager.addMeasurementStep(targetRealRegisterName, finalState, measuredQubitRealIndex, measurementResult, versionCounter);

                            measurementOutput.appendText("Измерение " + nominalRegisterName + "[" + nominalIndex + "] = " + output + "\n");
                        } else {
                            measurementOutput.appendText("Ошибка: Не найден номинальный регистр '" + nominalRegisterName + "' для обновления QGantt после MEASURE.\n");
                        }
                    } catch (Exception parseEx) {
                        measurementOutput.appendText("Ошибка обработки результата измерения для QGantt: " + parseEx.getMessage() + "\n");

                        measurementOutput.appendText(output + "\n");
                    }
                } else {
                    // Если output пуст, значит была ошибка внутри context.run(), она уже должна быть выведена
                    // Ничего не добавляем в QGantt и не меняем версию
                }
            } else if (parsedCommand.getType() == Command.CommandType.CREATE_REGISTER) {
                String realRegisterName = parsedCommand.getArgumentAsString("realRegisterName");
                int realRegisterSize = parsedCommand.getArgumentAsInt("realRegisterSize");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nominalSpecs = (List<Map<String, Object>>) parsedCommand.getArgument("nominalRegisters");

                qganttManager.createRegister(realRegisterName, realRegisterSize, nominalSpecs, null);

                QubitRegister realReg = context.getRealRegister(realRegisterName);
                if (realReg != null) {
                    Map<Integer, Complex> initialState = new HashMap<>();
                    initialState.put(0, Complex.getOne());
                    int currentVersionForState = versionCounter + 1;
                    qganttManager.addState(realRegisterName, initialState, currentVersionForState);
                    versionCounter++;
                } else {
                    measurementOutput.appendText("Ошибка: Не удалось найти реальный регистр '" + realRegisterName + "' после создания.\n");
                }
            } else if (parsedCommand.getType() == Command.CommandType.APPLY_GATE) {
                String gateName = parsedCommand.getArgumentAsString("gate");
                String targetRealRegisterName;

                if (joinInfo != null) {
                    String newRealRegName = (String) joinInfo.get("newRealRegName");
                    targetRealRegisterName = newRealRegName;

                    QubitRegister newRealReg = context.getRealRegister(newRealRegName);
                    if (newRealReg != null) {
                        List<Map<String, Object>> updatedNominalSpecs = context.getQubitRegisters().values().stream()
                                .filter(nr -> nr.getRealRegister() == newRealReg)
                                .sorted(Comparator.comparingInt(nr -> -nr.getOffsetInRealRegister())).map(nr -> {
                                    Map<String, Object> spec = new HashMap<>();
                                    spec.put("name", nr.getName());
                                    spec.put("size", nr.size());
                                    spec.put("offset", nr.getOffsetInRealRegister());
                                    return spec;
                                })
                                .collect(Collectors.toList());

                        @SuppressWarnings("unchecked")
                        List<String> oldRealRegNames = (List<String>) joinInfo.get("oldRealRegNames");
                        qganttManager.createRegister(newRealRegName, newRealReg.getRealSize(), updatedNominalSpecs, oldRealRegNames);

                        @SuppressWarnings("unchecked")
                        Map<Integer, Complex> initialJoinedState = (Map<Integer, Complex>) joinInfo.get("initialJoinedState");
                        Map<Integer, Complex> finalState = context.getRealRegisterState(newRealRegName);

                        int commonVersion = versionCounter + 1;
                        if (initialJoinedState != null) {
                            qganttManager.addState(newRealRegName, initialJoinedState, commonVersion);
                        } else {
                            measurementOutput.appendText("Предупреждение: Не удалось получить начальное состояние для объединенного регистра " + newRealRegName + "\n");
                            qganttManager.addState(newRealRegName, new HashMap<>(), commonVersion); // Добавляем пустое, чтобы версии не сдвигались
                        }

                        qganttManager.addGateTrace(newRealRegName, finalState, context.getLastGateTrace(), gateName, commonVersion); // Используем ту же версию

                        versionCounter++;
                    } else {
                        measurementOutput.appendText("Критическая ошибка: Не удалось найти новый реальный регистр '" + newRealRegName + "' после объединения.\n");
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> operandsData = (List<Map<String, Object>>) parsedCommand.getArgument("operands");
                    if (operandsData != null && !operandsData.isEmpty()) {
                        String firstNominalOperandName = (String) operandsData.get(0).get("register");
                        QubitRegister nominalReg = context.getNominalRegister(firstNominalOperandName);
                        if (nominalReg != null) {
                            targetRealRegisterName = nominalReg.getRealRegister().getName();
                            Map<Integer, Complex> finalState = context.getRealRegisterState(targetRealRegisterName);
                            int versionForFinal = versionCounter + 1;
                            qganttManager.addGateTrace(targetRealRegisterName, finalState, context.getLastGateTrace(), gateName, versionForFinal);
                            versionCounter++;
                        } else {
                            measurementOutput.appendText("Ошибка: Не удалось найти номинальный регистр '" + firstNominalOperandName + "' для обновления QGantt.\n");
                        }
                    }
                }

            } else if (parsedCommand.getType() == Command.CommandType.DEFINE_ORACLE_CSV) {
                // Эта команда определяет оракул, но не меняет состояние кубитов.
                // Просто выполняем ее в контексте (context.run уже был вызван).
            } else if (parsedCommand.getType() == Command.CommandType.APPLY_ORACLE) {
                if (joinInfo != null) {
                    measurementOutput.appendText("Критическая ошибка: Обнаружено объединение регистров при применении оракула!\n");
                    return;
                }

                String oracleName = parsedCommand.getArgumentAsString("oracleName");
                String inputNominalRegName = parsedCommand.getArgumentAsString("inputRegisterName");

                QubitRegister inputNominalReg = context.getNominalRegister(inputNominalRegName);
                if (inputNominalReg != null) {
                    String targetRealRegisterName = inputNominalReg.getRealRegister().getName();

                    Map<Integer, Complex> finalState = context.getRealRegisterState(targetRealRegisterName);

                    int versionForFinal = versionCounter + 1;
                    qganttManager.addGateTrace(targetRealRegisterName, finalState, context.getLastGateTrace(), oracleName, versionForFinal);
                    versionCounter++;
                } else {
                    measurementOutput.appendText("Ошибка: Не удалось найти номинальный регистр '" + inputNominalRegName + "' для обновления QGantt после APPLY_ORACLE.\n");
                }
            }
        } catch (Exception e) {
            measurementOutput.appendText("Ошибка: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void setupCodeArea() {
        commandInput = new CodeArea();
        commandInput.setWrapText(true);
        commandInput.setParagraphGraphicFactory(LineNumberFactory.get(commandInput));
        commandInput.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 14px;");
        commandInput.textProperty().addListener((obs, oldText, newText) -> {
            commandInput.setStyleSpans(0, SyntaxHighlighter.computeHighlighting(newText));
        });

        commandInput.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, new KeyEventHandler());
    }

    private static class KeyEventHandler implements javafx.event.EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.isControlDown()) {
                CodeArea commandInput = (CodeArea) event.getSource();
                switch (event.getCode()) {
                    case A:
                        // Ctrl+A - выделить всё
                        commandInput.selectAll();
                        event.consume();
                        break;
                    case C:
                        // Ctrl+C - копировать
                        if (commandInput.getSelectedText().length() > 0) {
                            final Clipboard clipboard = Clipboard.getSystemClipboard();
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(commandInput.getSelectedText());
                            clipboard.setContent(content);
                            event.consume();
                        }
                        break;
                    case V:
                        // Ctrl+V - вставить
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        if (clipboard.hasString()) {
                            commandInput.insertText(commandInput.getCaretPosition(), clipboard.getString());
                            event.consume();
                        }
                        break;
                    case X:
                        // Ctrl+X - вырезать
                        if (commandInput.getSelectedText().length() > 0) {
                            final Clipboard clipboard2 = Clipboard.getSystemClipboard();
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(commandInput.getSelectedText());
                            clipboard2.setContent(content);
                            commandInput.replaceSelection("");
                            event.consume();
                        }
                        break;
                }
            }
        }
    }

    private void setupMeasurementOutput() {
        measurementOutput = new CodeArea();
        measurementOutput.setWrapText(true);
        measurementOutput.setEditable(false);
        measurementOutput.setStyle("""
                -fx-font-family: 'Consolas', 'Monaco', monospace;
                -fx-font-size: 14px;
                -fx-background-color: white;
                -fx-text-fill: black;
                -fx-padding: 10px;
                """);

        measurementOutput.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case A:
                        // Ctrl+A - выделить всё
                        measurementOutput.selectAll();
                        event.consume();
                        break;
                    case C:
                        // Ctrl+C - копировать
                        if (measurementOutput.getSelectedText().length() > 0) {
                            final Clipboard clipboard = Clipboard.getSystemClipboard();
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(measurementOutput.getSelectedText());
                            clipboard.setContent(content);
                            event.consume();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void highlightCurrentLine(CodeArea textArea, int lineIndex) {
        Platform.runLater(() -> {
            try {
                String text = textArea.getText();
                String[] lines = text.split("\\r?\\n");
                if (lineIndex >= 0 && lineIndex < lines.length) {
                    int start = 0;
                    for (int i = 0; i < lineIndex; i++) {
                        start += lines[i].length() + 1;
                    }
                    int end = start + lines[lineIndex].length();

                    textArea.selectRange(start, end);
                    textArea.requestFocus();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<Command> parseCommandsFromText(String text) {
        List<Command> commands = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                Command command = Parser.parse(line);
                if (command != null) {
                    commands.add(command);
                }
            }
        }
        return commands;
    }

    public String translateToPlatform(String platform) {
        try {
            List<Command> commands = parseCommandsFromText(commandInput.getText());

            QuantumTranslator translator = TranslatorFactory.createTranslator(platform);

            return translator.translate(commands, context);
        } catch (Exception e) {
            return "Ошибка при трансляции: " + e.getMessage();
        }
    }

    private void saveTranslationResult(String code, String platform, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить " + platform + " код");

        String extension = switch (platform) {
            case "Q#" -> "*.qs";
            case "Qiskit", "Cirq" -> "*.py";
            default -> "*.*";
        };

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(platform + " Files", extension)
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            String filePath = file.getAbsolutePath();
            if (!filePath.endsWith(extension.substring(1))) {
                file = new File(filePath + extension.substring(1));
            }

            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(code);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}