package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Emulation;
import org.example.script.Command;
import org.example.script.Parser;
import org.example.translation.QuantumTranslator;
import org.example.translation.TranslatorFactory;
import org.example.qgantt.QGanttManager;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.example.syntax.SyntaxHighlighter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Main extends Application {

    private Emulation context = new Emulation();
    private Integer outputCounter = 1;
    private int currentLineIndex = -1;
    private List<String> commandList = new ArrayList<>();
    private Stack<Emulation> stateHistory = new Stack<>();
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
                    ex.printStackTrace();
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
                    stateHistory.clear();
                    outputHistory.clear();
                } catch (IOException ex) {
                    ex.printStackTrace();
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
                            stateHistory,
                            newOutputHistory,
                            commandList,
                            outputCounter,
                            currentLineIndex,
                            qganttManager.getStateHistory(),
                            qganttManager.getTransitions(),
                            qganttManager.getGateNames(),
                            qganttManager.getNumQubits()
                    );
                    oos.writeObject(appState);
                } catch (IOException ex) {
                    ex.printStackTrace();
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
                    stateHistory = appState.getStateHistory();
                    outputHistory = appState.getOutputHistory();
                    commandList = appState.getCommandList();
                    outputCounter = appState.getOutputCounter();
                    currentLineIndex = appState.getCurrentLineIndex();

                    commandInput.replaceText(String.join("\n", commandList));
                    stateHistory.push(context);

                    qganttManager.clear();
                    qganttManager.setNumQubits(appState.getNumQubits());
                    qganttManager.restoreHistory(appState.getQganttStateHistory(),
                            appState.getQganttTransitionHistory(), appState.getGateNamesHistory());

                    if (currentLineIndex >= 0 && currentLineIndex < commandList.size()) {
                        highlightCurrentLine(commandInput, currentLineIndex);
                    } else {
                        commandInput.deselect();
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
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
            outputCounter = 1;
            currentLineIndex = -1;
            context = new Emulation();
            stateHistory.clear();
            outputHistory.clear();
            qganttManager.clear();
            measurementOutput.clear();

            if (!commands.isEmpty()) {
                commandList = Arrays.asList(commands.split("\\r?\\n"));
                for (int i = 0; i < commandList.size(); i++) {
                    String command = commandList.get(i).trim();
                    if (!command.isEmpty()) {
                        currentLineIndex = i;
                        executeCommand(command, null, true);
                        highlightCurrentLine(commandInput, i);
                    }
                }
            }
        });

        stepForwardButton.setOnAction(e -> {
            String commands = commandInput.getText().trim();
            if (!commands.isEmpty()) {
                if (commandList.isEmpty() || !commands.equals(String.join("\n", commandList))) {
                    commandList = Arrays.asList(commands.split("\\r?\\n"));
                    currentLineIndex = -1;
                    context = new Emulation();
                    outputCounter = 1;
                    stateHistory.clear();
                    outputHistory.clear();
                }

                currentLineIndex++;
                while (currentLineIndex < commandList.size() &&
                        commandList.get(currentLineIndex).trim().isEmpty()) {
                    currentLineIndex++;
                }

                if (currentLineIndex < commandList.size()) {
                    String command = commandList.get(currentLineIndex).trim();
                    executeCommand(command, null, true);
                    highlightCurrentLine(commandInput, currentLineIndex);
                } else {
                    currentLineIndex--;
                }
            }
        });

        stepBackwardButton.setOnAction(e -> {
            if (!stateHistory.isEmpty()) {
                context = stateHistory.pop();
                outputCounter--;

                currentLineIndex--;
                while (currentLineIndex >= 0 &&
                        commandList.get(currentLineIndex).trim().isEmpty()) {
                    currentLineIndex--;
                }

                qganttManager.removeLastState();

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

    private void executeCommand(String command, TextArea outputArea, boolean saveState) {
        if (!command.isEmpty()) {
            String finalCommand = command;
            Platform.runLater(() -> {
                if (saveState) {
                    stateHistory.push(context.clone());
                }

                Command parsedCommand = Parser.parse(finalCommand);
                if (parsedCommand != null) {
                    String result = context.run(parsedCommand);

                    if (parsedCommand.getType() == Command.CommandType.MEASURE) {
                        measurementOutput.appendText(result + "\n");
                    }else if (parsedCommand.getType() == Command.CommandType.CREATE_REGISTER) {
                        int numQubits = parsedCommand.getArgumentAsInt("size");
                        qganttManager.setNumQubits(numQubits);
                        qganttManager.addState(context.getCurrentState());
                    } else if (parsedCommand.getType() == Command.CommandType.APPLY_GATE) {
                        String gateName = parsedCommand.getArgumentAsString("gate");
                        var trace = context.getLastGateTrace();
                        if (trace != null) {
                            qganttManager.addGateTrace(trace, gateName);
                        }
                    }

                    outputCounter++;
                } else {
                    measurementOutput.appendText("Ошибка: Неверный формат команды\n");
                }
            });
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