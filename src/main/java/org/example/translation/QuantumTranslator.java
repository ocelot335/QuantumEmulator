package org.example.translation;

import org.example.model.Emulation;
import org.example.script.Command;

import java.util.List;

public interface QuantumTranslator {
    String translate(List<Command> commands, Emulation context);
} 