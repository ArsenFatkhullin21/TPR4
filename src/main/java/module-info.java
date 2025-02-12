module ru.arsen.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.arsen.demo to javafx.fxml;
    exports ru.arsen.demo;
}