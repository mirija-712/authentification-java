module com.example.authentification_front {
	requires com.example.authentification.common;
	requires javafx.controls;
	requires javafx.fxml;
	requires java.net.http;
	requires com.google.gson;
	requires jdk.httpserver;

	opens com.example.authentification_front to javafx.fxml;
	exports com.example.authentification_front;
	exports com.example.authentification_front.api;
	exports com.example.authentification_front.policy;
}
