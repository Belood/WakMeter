module com.wakfu {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    opens com.wakfu to javafx.fxml;
    opens com.wakfu.data to com.fasterxml.jackson.databind;
    exports com.wakfu;
    exports com.wakfu.data;
}