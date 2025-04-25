module org.example.qe {
    requires javafx.controls;
    requires javafx.web;
    requires javafx.base;
    requires javafx.graphics;
    requires org.fxmisc.richtext;
    requires static lombok;
    requires java.management;

    exports org.example;
    exports org.example.model;
    exports org.example.model.qubit;
    exports org.example.script;
    exports org.example.translation;
    exports org.example.qgantt;
    exports org.example.syntax;
    exports org.example.model.gate;
    exports org.example.model.gate.instances;
}