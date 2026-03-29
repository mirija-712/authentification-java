module com.example.authentification_front {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.authentification_front to javafx.fxml;
    exports com.example.authentification_front;
}