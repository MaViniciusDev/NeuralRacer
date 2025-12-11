package org.MaViniciusDev.main;

import javafx.application.Application;
import javafx.stage.Stage;
import org.MaViniciusDev.view.EditorMapa;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Inicia o editor de mapas
        new EditorMapa().start(primaryStage);
    }

}
