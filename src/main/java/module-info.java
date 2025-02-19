module org.example.qe {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;

    opens org.example to javafx.fxml;
    exports org.example;
    exports org.example.model.gate;
    opens org.example.model.gate to javafx.fxml;
    exports org.example.script;
    opens org.example.script to javafx.fxml;
    exports org.example.model;
    opens org.example.model to javafx.fxml;
    exports org.example.model.qubit;
    opens org.example.model.qubit to javafx.fxml;
}