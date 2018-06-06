package com.hesky.bookmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Main application execution entry point
 */
public class App extends Application {
    private static final Logger LOG = getLogger(App.class);


    //start javafx application
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Starting app...");
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("printerScene.fxml")));
        primaryStage.setTitle("Book printer");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
