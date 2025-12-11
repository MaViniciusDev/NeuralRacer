package org.MaViniciusDev.view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class EditorMapa extends Application {
    private int[][] mapa;
    private final int largura = 128;
    private final int altura = 72;
    private boolean modoInicio = false;
    private boolean desenhando = false;
    private Canvas canvas;
    private GraphicsContext gc;
    private int tamanhoPincel = 1;
    private Integer inicioX = null;
    private Integer inicioY = null;
    private int direcao = 0; // 0: cima, 1: cima-direita, 2: direita, 3: baixo-direita, 4: baixo, 5: baixo-esquerda, 6: esquerda, 7: cima-esquerda
    private boolean selecionandoDirecao = false;
    private double currentMouseX, currentMouseY;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
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
            if (modoInicio && !selecionandoDirecao) {
                // Define o ponto de início
                definirInicio(e.getX(), e.getY());
            } else if (selecionandoDirecao) {
                // Define a direção
                definirDirecao(e.getX(), e.getY());
            } else {
                desenhando = true;
                desenharNaPosicao(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseMoved(e -> {
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (selecionandoDirecao) {
                redesenharMapa();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (desenhando && !modoInicio) {
                desenharNaPosicao(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(event -> desenhando = false);

        // Controles
        Button modoInicioBtn = new Button("Modo Desenho");
        modoInicioBtn.setOnAction(event -> {
            modoInicio = !modoInicio;
            modoInicioBtn.setText(modoInicio ? "Modo Início (Clique no mapa)" : "Modo Desenho");
        });

        Button salvarBtn = new Button("Salvar Mapa");
        salvarBtn.setOnAction(event -> salvarMapa());

        Button iniciarJogoBtn = new Button("Iniciar Jogo");
        iniciarJogoBtn.setOnAction(event -> {
            if (inicioX != null && inicioY != null) {
                new Jogo().setup(stage, mapa, inicioX, inicioY, direcao);
            } else {
                System.out.println("Defina o ponto de início primeiro!");
            }
        });

        // Slider para tamanho do pincel
        Label labelPincel = new Label("Tamanho do Pincel: 1");
        Slider sliderPincel = new Slider(1, 10, 1);
        sliderPincel.setShowTickLabels(true);
        sliderPincel.setShowTickMarks(true);
        sliderPincel.setMajorTickUnit(1);
        sliderPincel.setBlockIncrement(1);
        sliderPincel.valueProperty().addListener((observable, oldValue, newValue) -> {
            tamanhoPincel = newValue.intValue();
            labelPincel.setText("Tamanho do Pincel: " + tamanhoPincel);
        });

        HBox controlePincel = new HBox(10, labelPincel, sliderPincel);
        controlePincel.setPadding(new Insets(10));

        HBox controles = new HBox(10, modoInicioBtn, salvarBtn, iniciarJogoBtn);
        controles.setPadding(new Insets(10));

        VBox vbox = new VBox(canvas, controlePincel, controles);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        Scene scene = new Scene(vbox, 1280, 820);

        // Redimensiona o canvas quando a janela mudar de tamanho
        scene.widthProperty().addListener((obs, old, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            redesenharMapa();
        });

        scene.heightProperty().addListener((obs, old, newVal) -> {
            canvas.setHeight(newVal.doubleValue() - 100);
            redesenharMapa();
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Editor de Mapas");
        primaryStage.show();

        // Adiciona listener para teclas de direção
        scene.setOnKeyPressed(event -> {
            if (inicioX != null && inicioY != null) {
                switch (event.getCode()) {
                    case UP -> direcao = 0;
                    case RIGHT -> direcao = 1;
                    case DOWN -> direcao = 2;
                    case LEFT -> direcao = 3;
                }
                redesenharMapa();
            }
        });
    }

    private void definirInicio(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / largura;
        double cellHeight = canvas.getHeight() / altura;

        int x = (int) (mouseX / cellWidth);
        int y = (int) (mouseY / cellHeight);

        if (x >= 0 && x < largura && y >= 0 && y < altura && mapa[y][x] == 0) {
            inicioX = x;
            inicioY = y;
            selecionandoDirecao = true;
            redesenharMapa();
            System.out.println("Início definido em: (" + inicioX + ", " + inicioY + "). Agora selecione a direção.");
        } else {
            System.out.println("Posição inválida para início. Deve ser uma pista.");
        }
    }

    private void definirDirecao(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / largura;
        double cellHeight = canvas.getHeight() / altura;
        double cx = inicioX * cellWidth + cellWidth / 2;
        double cy = inicioY * cellHeight + cellHeight / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;

        double angle = Math.atan2(dy, dx);
        double deg = Math.toDegrees(angle);

        if (deg >= -22.5 && deg < 22.5) {
            direcao = 2; // direita
        } else if (deg >= 22.5 && deg < 67.5) {
            direcao = 3; // baixo-direita
        } else if (deg >= 67.5 && deg < 112.5) {
            direcao = 4; // baixo
        } else if (deg >= 112.5 && deg < 157.5) {
            direcao = 5; // baixo-esquerda
        } else if (deg >= 157.5 || deg < -157.5) {
            direcao = 6; // esquerda
        } else if (deg >= -157.5 && deg < -112.5) {
            direcao = 7; // cima-esquerda
        } else if (deg >= -112.5 && deg < -67.5) {
            direcao = 0; // cima
        } else {
            direcao = 1; // cima-direita
        }

        selecionandoDirecao = false;
        redesenharMapa();
        System.out.println("Direção definida: " + switch (direcao) {
            case 0 -> "cima";
            case 1 -> "cima-direita";
            case 2 -> "direita";
            case 3 -> "baixo-direita";
            case 4 -> "baixo";
            case 5 -> "baixo-esquerda";
            case 6 -> "esquerda";
            case 7 -> "cima-esquerda";
            default -> "desconhecida";
        });
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

            // Desenha a seta de direção ou preview
            double cx = inicioX * cellWidth + cellWidth / 2;
            double cy = inicioY * cellHeight + cellHeight / 2;
            if (selecionandoDirecao) {
                // Desenha uma linha do centro até o mouse
                gc.setStroke(Color.RED);
                gc.setLineWidth(2);
                gc.strokeLine(cx, cy, currentMouseX, currentMouseY);
            } else {
                gc.setStroke(Color.BLUE);
                gc.setLineWidth(4);
                double arrowLength = cellWidth;
                double arrowAngle = switch (direcao) {
                    case 0 -> 270.0; // cima
                    case 1 -> 315.0; // cima-direita
                    case 2 -> 0.0;   // direita
                    case 3 -> 45.0;  // baixo-direita
                    case 4 -> 90.0;  // baixo
                    case 5 -> 135.0; // baixo-esquerda
                    case 6 -> 180.0; // esquerda
                    case 7 -> 225.0; // cima-esquerda
                    default -> 0.0;
                };
                double endX = cx + arrowLength * Math.cos(Math.toRadians(arrowAngle));
                double endY = cy + arrowLength * Math.sin(Math.toRadians(arrowAngle));
                gc.strokeLine(cx, cy, endX, endY);
                // Desenha a ponta da seta
                double dirX = endX - cx;
                double dirY = endY - cy;
                double len = Math.sqrt(dirX * dirX + dirY * dirY);
                dirX /= len;
                dirY /= len;
                double arrowSize = cellWidth * 0.15;
                double leftX = endX - arrowSize * (dirX * Math.cos(Math.PI / 4) - dirY * Math.sin(Math.PI / 4));
                double leftY = endY - arrowSize * (dirX * Math.sin(Math.PI / 4) + dirY * Math.cos(Math.PI / 4));
                double rightX = endX - arrowSize * (dirX * Math.cos(-Math.PI / 4) - dirY * Math.sin(-Math.PI / 4));
                double rightY = endY - arrowSize * (dirX * Math.sin(-Math.PI / 4) + dirY * Math.cos(-Math.PI / 4));
                gc.strokeLine(endX, endY, leftX, leftY);
                gc.strokeLine(endX, endY, rightX, rightY);
            }
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

                if (x >= 0 && x < largura && y >= 0 && y < altura && x > 0 && x < largura - 1 && y > 0 && y < altura - 1) {
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
        String dirStr = switch (direcao) {
            case 0 -> "cima";
            case 1 -> "cima-direita";
            case 2 -> "direita";
            case 3 -> "baixo-direita";
            case 4 -> "baixo";
            case 5 -> "baixo-esquerda";
            case 6 -> "esquerda";
            case 7 -> "cima-esquerda";
            default -> "desconhecida";
        };
        System.out.println("Mapa salvo! Início: (" + inicioX + ", " + inicioY + "), Direção: " + dirStr);
    }
}
