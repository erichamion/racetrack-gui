package com.erichamion.racetrack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("racetrack.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Racetrack");
        Scene scene = new Scene(root, 500, 275);
        primaryStage.setScene(scene);
        primaryStage.show();

        RacetrackFXMLController windowController = fxmlLoader.getController();
        windowController.setScene(scene);

    }


    public static void main(String[] args) {
        launch(args);
    }


}
