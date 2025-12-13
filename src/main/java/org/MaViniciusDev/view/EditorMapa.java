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
    // Map dimensions
    private static final int LARGURA = 128;
    private static final int ALTURA = 72;

    // Map data
    private int[][] mapa;

    // Canvas components
    private Canvas canvas;
    private GraphicsContext gc;

    // Editor state
    private boolean modoInicio = false;
    private boolean desenhando = false;
    private int tamanhoPincel = 1;

    // Start position and direction
    private Integer inicioX = null;
    private Integer inicioY = null;
    private int direcao = 0;
    private boolean selecionandoDirecao = false;
    private double currentMouseX, currentMouseY;

    // Stage reference
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        initializeMap();
        setupCanvas();
        setupMouseHandlers();

        Scene scene = createScene();
        setupKeyboardHandlers(scene);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Editor de Mapas");
        primaryStage.show();
    }

    private void initializeMap() {
        mapa = new int[ALTURA][LARGURA];
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                mapa[y][x] = 1;
            }
        }
    }

    private void setupCanvas() {
        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();
        redesenharMapa();
    }

    private void setupMouseHandlers() {
        canvas.setOnMousePressed(e -> {
            if (modoInicio && !selecionandoDirecao) {
                definirInicio(e.getX(), e.getY());
            } else if (selecionandoDirecao) {
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

        canvas.setOnMouseReleased(_ -> desenhando = false);
    }

    private Scene createScene() {
        VBox vbox = new VBox(canvas, createBrushControl(), createButtonControls());
        VBox.setVgrow(canvas, Priority.ALWAYS);

        Scene scene = new Scene(vbox, 1280, 820);

        scene.widthProperty().addListener((_, _, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            redesenharMapa();
        });

        scene.heightProperty().addListener((_, _, newVal) -> {
            canvas.setHeight(newVal.doubleValue() - 100);
            redesenharMapa();
        });

        return scene;
    }

    private HBox createBrushControl() {
        Label labelPincel = new Label("Tamanho do Pincel: 1");
        Slider sliderPincel = new Slider(1, 10, 1);
        sliderPincel.setShowTickLabels(true);
        sliderPincel.setShowTickMarks(true);
        sliderPincel.setMajorTickUnit(1);
        sliderPincel.setBlockIncrement(1);
        sliderPincel.valueProperty().addListener((_, _, newValue) -> {
            tamanhoPincel = newValue.intValue();
            labelPincel.setText("Tamanho do Pincel: " + tamanhoPincel);
        });

        HBox controlePincel = new HBox(10, labelPincel, sliderPincel);
        controlePincel.setPadding(new Insets(10));
        return controlePincel;
    }

    private HBox createButtonControls() {
        Button modoInicioBtn = new Button("Modo Desenho");
        modoInicioBtn.setOnAction(_ -> {
            modoInicio = !modoInicio;
            modoInicioBtn.setText(modoInicio ? "Modo Início (Clique no mapa)" : "Modo Desenho");
        });

        Button salvarBtn = new Button("Salvar Mapa");
        salvarBtn.setOnAction(_ -> salvarMapa());

        Button reiniciarBtn = new Button("Reiniciar Mapa");
        reiniciarBtn.setOnAction(_ -> reiniciarMapa());

        Button iniciarJogoBtn = new Button("Jogar Manualmente 🎮");
        iniciarJogoBtn.setOnAction(_ -> iniciarJogo());

        // --- BOTÃO ADICIONADO ---
        Button treinarIABtn = new Button("Treinar IA 🧬");
        treinarIABtn.setOnAction(_ -> iniciarTreinamento());

        HBox controles = new HBox(10, modoInicioBtn, salvarBtn, reiniciarBtn, iniciarJogoBtn, treinarIABtn);
        controles.setPadding(new Insets(10));
        return controles;
    }

    private void setupKeyboardHandlers(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (inicioX != null && inicioY != null) {
                switch (event.getCode()) {
                    case UP -> direcao = 0;
                    case RIGHT -> direcao = 2;
                    case DOWN -> direcao = 4;
                    case LEFT -> direcao = 6;
                }
                redesenharMapa();
            }
        });
    }

    public void voltarParaEditor() {
        stage.setScene(createScene());
        setupKeyboardHandlers(stage.getScene());
        redesenharMapa();
    }

    private void iniciarJogo() {
        if (inicioX != null && inicioY != null) {
            new Jogo().setup(stage, mapa, inicioX, inicioY, direcao, this);
        } else {
            System.out.println("Defina o ponto de início primeiro!");
        }
    }

    // --- MÉTODO NOVO ---
    private void iniciarTreinamento() {
        if (inicioX != null && inicioY != null) {
            System.out.println("Iniciando Algoritmo Genético...");
            new TreinamentoIA().setup(stage, mapa, inicioX, inicioY, direcao, this);
        } else {
            System.out.println("⚠️ ERRO: Defina o ponto de início (Modo Início) antes de treinar!");
        }
    }

    private String getDirecaoString(int dir) {
        return switch (dir) {
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
    }

    private void definirInicio(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / LARGURA;
        double cellHeight = canvas.getHeight() / ALTURA;

        int x = (int) (mouseX / cellWidth);
        int y = (int) (mouseY / cellHeight);

        if (isPosicaoValida(x, y) && mapa[y][x] == 0) {
            inicioX = x;
            inicioY = y;
            selecionandoDirecao = true;
            redesenharMapa();
            System.out.println("Início definido em: (" + inicioX + ", " + inicioY + "). Agora selecione a direção.");
        } else {
            System.out.println("Posição inválida para início. Deve ser uma pista.");
        }
    }

    private boolean isPosicaoValida(int x, int y) {
        return x >= 0 && x < LARGURA && y >= 0 && y < ALTURA;
    }

    private void definirDirecao(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / LARGURA;
        double cellHeight = canvas.getHeight() / ALTURA;

        double cx = inicioX * cellWidth + cellWidth / 2;
        double cy = inicioY * cellHeight + cellHeight / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;

        direcao = calcularDirecaoDoAngulo(dx, dy);
        selecionandoDirecao = false;
        redesenharMapa();

        System.out.println("Direção definida: " + getDirecaoString(direcao));
    }

    private int calcularDirecaoDoAngulo(double dx, double dy) {
        double angle = Math.atan2(dy, dx);
        double deg = Math.toDegrees(angle);

        if (deg >= -22.5 && deg < 22.5) {
            return 2;
        } else if (deg >= 22.5 && deg < 67.5) {
            return 3;
        } else if (deg >= 67.5 && deg < 112.5) {
            return 4;
        } else if (deg >= 112.5 && deg < 157.5) {
            return 5;
        } else if (deg >= 157.5 || deg < -157.5) {
            return 6;
        } else if (deg >= -157.5 && deg < -112.5) {
            return 7;
        } else if (deg >= -112.5 && deg < -67.5) {
            return 0;
        } else {
            return 1;
        }
    }

    private void redesenharMapa() {
        double cellWidth = canvas.getWidth() / LARGURA;
        double cellHeight = canvas.getHeight() / ALTURA;

        desenharFundo();
        desenharPistas(cellWidth, cellHeight);

        if (inicioX != null && inicioY != null) {
            desenharPontoInicio(cellWidth, cellHeight);
            desenharIndicadorDirecao(cellWidth, cellHeight);
        }
    }

    private void desenharFundo() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void desenharPistas(double cellWidth, double cellHeight) {
        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                if (mapa[y][x] == 0) {
                    gc.setFill(Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }

    private void desenharPontoInicio(double cellWidth, double cellHeight) {
        gc.setFill(Color.LIME);
        gc.fillRect(inicioX * cellWidth, inicioY * cellHeight, cellWidth, cellHeight);

        gc.setFill(Color.DARKGREEN);
        gc.fillOval(
                inicioX * cellWidth + cellWidth * 0.25,
                inicioY * cellHeight + cellHeight * 0.25,
                cellWidth * 0.5,
                cellHeight * 0.5
        );
    }

    private void desenharIndicadorDirecao(double cellWidth, double cellHeight) {
        double cx = inicioX * cellWidth + cellWidth / 2;
        double cy = inicioY * cellHeight + cellHeight / 2;

        if (selecionandoDirecao) {
            desenharLinhaPreview(cx, cy);
        } else {
            desenharSetaDirecao(cx, cy, cellWidth);
        }
    }

    private void desenharLinhaPreview(double cx, double cy) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeLine(cx, cy, currentMouseX, currentMouseY);
    }

    private void desenharSetaDirecao(double cx, double cy, double cellWidth) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(4);

        double arrowAngle = getAnguloFromDirecao(direcao);

        double endX = cx + cellWidth * Math.cos(Math.toRadians(arrowAngle));
        double endY = cy + cellWidth * Math.sin(Math.toRadians(arrowAngle));

        gc.strokeLine(cx, cy, endX, endY);
        desenharPontaDaSeta(cx, cy, endX, endY, cellWidth);
    }

    private double getAnguloFromDirecao(int dir) {
        return switch (dir) {
            case 0 -> 270.0;
            case 1 -> 315.0;
            case 2 -> 0.0;
            case 3 -> 45.0;
            case 4 -> 90.0;
            case 5 -> 135.0;
            case 6 -> 180.0;
            case 7 -> 225.0;
            default -> 0.0;
        };
    }

    private void desenharPontaDaSeta(double cx, double cy, double endX, double endY, double cellWidth) {
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

    private void desenharNaPosicao(double mouseX, double mouseY) {
        double cellWidth = canvas.getWidth() / LARGURA;
        double cellHeight = canvas.getHeight() / ALTURA;

        int centerX = (int) (mouseX / cellWidth);
        int centerY = (int) (mouseY / cellHeight);

        for (int dy = -tamanhoPincel + 1; dy < tamanhoPincel; dy++) {
            for (int dx = -tamanhoPincel + 1; dx < tamanhoPincel; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (isPosicaoDesenhavelValida(x, y)) {
                    mapa[y][x] = 0;
                    gc.setFill(Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight,
                            cellWidth, cellHeight);
                }
            }
        }
    }

    private boolean isPosicaoDesenhavelValida(int x, int y) {
        return x > 0 && x < LARGURA - 1 && y > 0 && y < ALTURA - 1;
    }

    private void salvarMapa() {
        if (inicioX == null || inicioY == null) {
            System.out.println("Por favor, defina o ponto de início primeiro!");
            return;
        }

        System.out.println("Mapa salvo! Início: (" + inicioX + ", " + inicioY +
                "), Direção: " + getDirecaoString(direcao));
    }

    private void reiniciarMapa() {
        initializeMap();
        inicioX = null;
        inicioY = null;
        direcao = 0;
        selecionandoDirecao = false;
        modoInicio = false;
        redesenharMapa();
        System.out.println("Mapa reiniciado!");
    }
}