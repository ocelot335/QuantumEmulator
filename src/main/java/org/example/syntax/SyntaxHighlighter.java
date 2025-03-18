package org.example.syntax;

import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private static final Set<String> OPERATIONS = new HashSet<>(Set.of(
        "CR","CQ","H", "X", "Y", "Z", "S", "T",
        "SWAP", "INC", "DEC", "NOT",
        "CX","CNOT", "CCNOT", "CY", "CZ", "CH", "CS", "CT"
    ));

    private static final String OPERATION_PATTERN = String.join("|", OPERATIONS);
    private static final Pattern PATTERN = Pattern.compile(
        "\\b(" + OPERATION_PATTERN + ")\\b"
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while(matcher.find()) {
            String styleClass = "operation";
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);

        return spansBuilder.create();
    }

    public static String getStyle() {
        return ".operation { -fx-fill: #cc3300; }";
    }
} 