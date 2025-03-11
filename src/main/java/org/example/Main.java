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
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.model.Emulation;
import org.example.script.Command;
import org.example.script.Parser;
import org.example.translation.QiskitTranslator;

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
    private List<Command> commandsToTranslate = new ArrayList<>();
    private TextArea commandInput;
    private TextArea outputArea;

    @Override
    public void start(Stage stage) {
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Текущее состояние...");
        outputArea.setPrefHeight(Screen.getPrimary().getBounds().getHeight() / 2);

        commandInput = new TextArea();
        commandInput.setPromptText("Введите команды (каждая команда с новой строки)...");

        Button executeAllButton = new Button("Выполнить все");
        Button stepForwardButton = new Button("Шаг вперёд");
        Button stepBackwardButton = new Button("Шаг назад");

        executeAllButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        stepForwardButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        stepBackwardButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

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
                    outputArea.appendText("Ошибка при сохранении файла: " + ex.getMessage() + "\n");
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
                    commandInput.setText(content.toString());
                    commandList.clear();
                    currentLineIndex = -1;
                    stateHistory.clear();
                    outputHistory.clear();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    outputArea.appendText("Ошибка при загрузке файла: " + ex.getMessage() + "\n");
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
                    String currentOutput = outputArea.getText();

                    Stack<String> newOutputHistory = new Stack<>();
                    newOutputHistory.addAll(outputHistory);
                    newOutputHistory.push(currentOutput);
                    System.out.println(newOutputHistory);
                    org.example.model.AppState appState = new org.example.model.AppState(currentState, stateHistory, newOutputHistory, commandList, outputCounter, currentLineIndex);
                    oos.writeObject(appState);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    outputArea.appendText("Ошибка при сохранении модели: " + ex.getMessage() + "\n");
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

                    commandInput.setText(String.join("\n", commandList));
                    stateHistory.push(context);
                    if (!outputHistory.isEmpty()) {
                        outputArea.setText(outputHistory.pop());
                    } else {
                        outputArea.setText("");
                    }

                    if (currentLineIndex >= 0 && currentLineIndex < commandList.size()) {
                        highlightCurrentLine(commandInput, currentLineIndex);
                    } else {
                        commandInput.deselect();
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                    outputArea.appendText("Ошибка при загрузке модели: " + ex.getMessage() + "\n");
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
            outputArea.setText("");
            outputCounter = 1;
            currentLineIndex = -1;
            context = new Emulation();
            stateHistory.clear();
            outputHistory.clear();

            if (!commands.isEmpty()) {
                commandList = Arrays.asList(commands.split("\\r?\\n"));
                for (int i = 0; i < commandList.size(); i++) {
                    String command = commandList.get(i).trim();
                    if (!command.isEmpty()) {
                        currentLineIndex = i;
                        executeCommand(command, outputArea, true);
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
                    outputArea.setText("");
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
                    executeCommand(command, outputArea, true);
                    highlightCurrentLine(commandInput, currentLineIndex);
                } else {
                    currentLineIndex--;
                }
            }
        });

        stepBackwardButton.setOnAction(e -> {
            if (!stateHistory.isEmpty() && !outputHistory.isEmpty()) {
                context = stateHistory.pop();
                String previousOutput = outputHistory.pop();
                outputArea.setText(previousOutput);
                outputCounter--;

                currentLineIndex--;
                while (currentLineIndex >= 0 &&
                        commandList.get(currentLineIndex).trim().isEmpty()) {
                    currentLineIndex--;
                }

                if (currentLineIndex >= 0) {
                    highlightCurrentLine(commandInput, currentLineIndex);
                } else {
                    commandInput.deselect();
                }
            }
        });

        HBox controlButtons = new HBox(10, executeAllButton, stepForwardButton, stepBackwardButton);
        controlButtons.setAlignment(Pos.CENTER);

        VBox inputBox = new VBox(10, commandInput, controlButtons);
        inputBox.setAlignment(Pos.TOP_LEFT);
        inputBox.setPadding(new Insets(10));
        VBox.setVgrow(commandInput, Priority.ALWAYS);

        VBox root = new VBox(10, menuBar, outputArea, inputBox);
        root.setPadding(new Insets(10));
        VBox.setVgrow(inputBox, Priority.ALWAYS);

        Scene scene = new Scene(root);
        stage.setScene(scene);
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
                    outputHistory.push(outputArea.getText());
                }

                String result = context.run(finalCommand);

                outputArea.appendText(outputCounter + ":\n" + result + "\n");
                outputCounter++;
            });
        }
    }

    private void highlightCurrentLine(TextArea textArea, int lineIndex) {
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
        String text = commandInput.getText();
        List<Command> commands = parseCommandsFromText(text);
        
        if (platform.equals("Qiskit")) {
            return new QiskitTranslator().translate(commands, context);
        } else if (platform.equals("Q#")) {
            return "Q# транслятор пока не реализован";
        } else if (platform.equals("Cirq")) {
            return "Cirq транслятор пока не реализован";
        }
        return "Неподдерживаемая платформа: " + platform;
    }

    private void saveTranslationResult(String code, String platform, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить " + platform + " код");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Python Files", "*.py")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(code);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}