package org.MaViniciusDev.view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class EditorMapa extends Application {
    private int[][] mapa;
    private int largura = 128, altura = 72;
    private boolean modoInicio = false;
    private boolean desenhando = false;
    private Canvas canvas;
    private GraphicsContext gc;
    private int tamanhoPincel = 1;
    private Integer inicioX = null;
    private Integer inicioY = null;

    @Override
    public void start(Stage primaryStage) {
        mapa = new int[altura][largura];
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                mapa[y][x] = 1;
            }
        }

        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();

        // Desenha o mapa inicial (tudo preto)
        redesenharMapa();

        canvas.setOnMousePressed(e -> {
            if (modoInicio) {
                // Define o ponto de início
                definirInicio(e.getX(), e.getY());
            } else {
                desenhando = true;
                desenharNaPosicao(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (desenhando && !modoInicio) {
                desenharNaPosicao(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(e -> {
            desenhando = false;
        });

        // Controles
        Button modoInicioBtn = new Button("Modo Desenho");
        modoInicioBtn.setOnAction(e -> {
            modoInicio = !modoInicio;
            modoInicioBtn.setText(modoInicio ? "Modo Início (Clique no mapa)" : "Modo Desenho");
        });

        Button salvarBtn = new Button("Salvar Mapa");
        salvarBtn.setOnAction(e -> salvarMapa());

        // Slider para tamanho do pincel
        Label labelPincel = new Label("Tamanho do Pincel: 1");
        Slider sliderPincel = new Slider(1, 10, 1);
        sliderPincel.setShowTickLabels(true);
        sliderPincel.setShowTickMarks(true);
        sliderPincel.setMajorTickUnit(1);
        sliderPincel.setBlockIncrement(1);
        sliderPincel.valueProperty().addListener((obs, oldVal, newVal) -> {
            tamanhoPincel = newVal.intValue();
            labelPincel.setText("Tamanho do Pincel: " + tamanhoPincel);
        });

        HBox controlePincel = new HBox(10, labelPincel, sliderPincel);
        controlePincel.setPadding(new Insets(10));

        HBox controles = new HBox(10, modoInicioBtn, salvarBtn);
        controles.setPadding(new Insets(10));

        VBox vbox = new VBox(canvas, controlePincel, controles);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        Scene scene = new Scene(vbox, 1280, 820);

        // Redimensiona o canvas quando a janela mudar de tamanho
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            redesenharMapa();
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue() - 100);
            redesenharMapa();
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Editor de Mapas");
        primaryStage.show();
    }

    private void definirInicio(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / largura;
        double cellHeight = canvas.getHeight() / altura;

        inicioX = (int) (mouseX / cellWidth);
        inicioY = (int) (mouseY / cellHeight);

        if (inicioX >= 0 && inicioX < largura && inicioY >= 0 && inicioY < altura) {
            redesenharMapa();
            System.out.println("Início definido em: (" + inicioX + ", " + inicioY + ")");
        }
    }

    private void redesenharMapa() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double cellWidth = canvas.getWidth() / largura;
        double cellHeight = canvas.getHeight() / altura;

        // Redesenha o mapa existente
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                if (mapa[y][x] == 0) {
                    gc.setFill(Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }

        // Desenha o ponto de início (verde)
        if (inicioX != null && inicioY != null) {
            gc.setFill(Color.LIME);
            gc.fillRect(inicioX * cellWidth, inicioY * cellHeight, cellWidth, cellHeight);

            // Adiciona um círculo no centro para melhor visualização
            gc.setFill(Color.DARKGREEN);
            gc.fillOval(inicioX * cellWidth + cellWidth * 0.25,
                    inicioY * cellHeight + cellHeight * 0.25,
                    cellWidth * 0.5,
                    cellHeight * 0.5);
        }
    }

    private void desenharNaPosicao(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / largura;
        double cellHeight = canvas.getHeight() / altura;

        int centerX = (int) (mouseX / cellWidth);
        int centerY = (int) (mouseY / cellHeight);

        // Desenha com o tamanho do pincel
        for (int dy = -tamanhoPincel + 1; dy < tamanhoPincel; dy++) {
            for (int dx = -tamanhoPincel + 1; dx < tamanhoPincel; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < largura && y >= 0 && y < altura) {
                    mapa[y][x] = 0;
                    gc.setFill(Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }

    private void salvarMapa() {
        if (inicioX == null || inicioY == null) {
            System.out.println("Por favor, defina o ponto de início primeiro!");
            return;
        }
        System.out.println("Mapa salvo! Início: (" + inicioX + ", " + inicioY + ")");
    }
}
