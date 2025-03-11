package org.example.translation;

import org.example.model.Emulation;
import org.example.script.Command;

import java.util.List;

public interface Translator {
    String translate(List<Command> commands, Emulation context);
    String getFileExtension();
    String getPlatformName();
}
