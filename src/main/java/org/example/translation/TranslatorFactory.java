package org.example.translation;

public class TranslatorFactory {
    public static QuantumTranslator createTranslator(String platform) {
        return switch (platform) {
            case "Qiskit" -> new QiskitTranslator();
            case "Q#" -> new QSharpTranslator();
            case "Cirq" -> new CirqTranslator();
            default -> throw new IllegalArgumentException("Неподдерживаемая платформа: " + platform);
        };
    }
} 