package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.model.Emulation;

import java.util.Arrays;
import java.util.List;

public class Main extends Application {

    private Emulation context = new Emulation();
    private Integer outputCounter = 1;

    @Override
    public void start(Stage stage) {
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Текущее состояние...");
        outputArea.setPrefHeight(Screen.getPrimary().getBounds().getHeight() / 2);

        TextArea commandInput = new TextArea();
        commandInput.setPromptText("Введите команду...");

        Button executeButton = new Button("Выполнить");
        Integer i = 1;
        executeButton.setOnAction(e -> {
            String commands = commandInput.getText().trim();

            if (!commands.isEmpty()) {
                List<String> commandList = Arrays.asList(commands.split("\\r?\\n"));
                for (String command: commandList) {
                    command = command.trim();
                    String finalCommand = command;
                    Platform.runLater(() -> {
                        String result = context.run(finalCommand);
                        outputArea.appendText(outputCounter+":\n" + result + "\n");
                        outputCounter++;
                    });
                }
            }
        });


        HBox inputBox = new HBox(10, commandInput, executeButton);
        inputBox.setAlignment(Pos.TOP_LEFT);
        inputBox.setPadding(new Insets(10));
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        VBox root = new VBox(10, outputArea, inputBox);
        root.setPadding(new Insets(10));
        VBox.setVgrow(inputBox, Priority.ALWAYS);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Quantum Emulator");

        stage.setMaximized(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}