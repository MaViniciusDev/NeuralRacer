package org.MaViniciusDev.view;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Jogo {
    private int[][] mapa;
    private final int largura = 128;
    private final int altura = 72;
    private Integer inicioX, inicioY;
    private int direcao;
    private Canvas canvas;
    private GraphicsContext gc;
    private Carro carro;
    private double cellWidth, cellHeight;

    public void setup(Stage stage, int[][] mapa, Integer inicioX, Integer inicioY, int direcao) {
        this.mapa = mapa;
        this.inicioX = inicioX;
        this.inicioY = inicioY;
        this.direcao = direcao;

        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();
        cellWidth = canvas.getWidth() / largura;
        cellHeight = canvas.getHeight() / altura;

        redesenharMapa();

        double startX = inicioX * cellWidth;
        double startY = inicioY * cellHeight;
        int angle = switch (direcao) {
            case 0 -> 270; // cima
            case 1 -> 315; // cima-direita
            case 2 -> 0;   // direita
            case 3 -> 45;  // baixo-direita
            case 4 -> 90;  // baixo
            case 5 -> 135; // baixo-esquerda
            case 6 -> 180; // esquerda
            case 7 -> 225; // cima-esquerda
            default -> 0;
        };

        carro = new Carro(startX, startY, mapa, largura, altura, cellWidth, cellHeight);
        carro.setSpeed(2);
        carro.setAngle(angle);

        Pane root = new Pane();
        root.getChildren().addAll(canvas, carro);

        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.setTitle("NeuralRacer - Jogo");

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                carro.updatePosition();
            }
        };
        timer.start();
    }

    private void redesenharMapa() {
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (mapa[y][x] == 0) {
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }
}
