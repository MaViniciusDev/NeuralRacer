package org.MaViniciusDev.view;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class Jogo {
    // Map data
    private int[][] mapa;
    private static final int LARGURA = 128;
    private static final int ALTURA = 72;

    // Canvas components
    private Canvas canvas;
    private GraphicsContext gc;
    private double cellWidth, cellHeight;

    // Game objects
    private Carro carro;
    private EditorMapa editorMapa;

    // Steering physics constants
    private static final double MAX_STEER_DEG_PER_SEC = 180.0;
    private static final double MIN_STEER_DEG_PER_SEC = 60.0;
    private static final double MIN_SPEED_FOR_STEERING = 10.0;

    public void setup(Stage stage, int[][] mapa, Integer inicioX, Integer inicioY, int direcao, EditorMapa editorMapa) {
        this.mapa = mapa;
        this.editorMapa = editorMapa;

        initializeCanvas();
        redesenharMapa();

        double startX = inicioX * cellWidth;
        double startY = inicioY * cellHeight;
        int angle = calculateInitialAngle(direcao);

        initializeCar(startX, startY, angle);

        Scene scene = createGameScene(startX, startY, angle);
        stage.setScene(scene);
        stage.setTitle("NeuralRacer - Jogo");
    }

    private void initializeCanvas() {
        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();
        cellWidth = canvas.getWidth() / LARGURA;
        cellHeight = canvas.getHeight() / ALTURA;
    }

    private int calculateInitialAngle(int direcao) {
        return switch (direcao) {
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
    }

    private void initializeCar(double startX, double startY, int angle) {
        carro = new Carro(startX, startY, mapa, LARGURA, ALTURA, cellWidth, cellHeight);
        carro.setSpeed(2);
        carro.setAngle(angle);
    }

    private Scene createGameScene(double startX, double startY, int angle) {
        Pane root = new Pane();
        root.getChildren().addAll(canvas, carro);

        Scene scene = new Scene(root, 1280, 720);

        setupInputHandlers(scene, startX, startY, angle);
        addRestartButton(root);

        return scene;
    }

    private void setupInputHandlers(Scene scene, double startX, double startY, int angle) {
        // Input state arrays
        final boolean[] up = {false};
        final boolean[] down = {false};
        final boolean[] left = {false};
        final boolean[] right = {false};
        final boolean[] brake = {false};

        // Timer references
        final AnimationTimer[] timerRef = new AnimationTimer[1];
        final long[] lastRef = new long[]{0};

        setupKeyPressedHandler(scene, up, down, left, right, brake, timerRef, lastRef, startX, startY, angle);
        setupKeyReleasedHandler(scene, up, down, left, right, brake);
        startGameLoop(timerRef, lastRef, up, down, left, right, brake);
    }

    private void setupKeyPressedHandler(Scene scene, boolean[] up, boolean[] down, boolean[] left, boolean[] right,
                                       boolean[] brake, AnimationTimer[] timerRef, long[] lastRef,
                                       double startX, double startY, int angle) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            KeyCode kc = e.getCode();

            // Movement keys
            if (kc == KeyCode.W || kc == KeyCode.UP) up[0] = true;
            if (kc == KeyCode.S || kc == KeyCode.DOWN) down[0] = true;
            if (kc == KeyCode.A || kc == KeyCode.LEFT) left[0] = true;
            if (kc == KeyCode.D || kc == KeyCode.RIGHT) right[0] = true;
            if (kc == KeyCode.SPACE) brake[0] = true;

            // ESC: Return to editor
            if (kc == KeyCode.ESCAPE) {
                if (timerRef[0] != null) timerRef[0].stop();
                editorMapa.voltarParaEditor();
            }

            // R: Quick restart
            if (kc == KeyCode.R) {
                restartGame(startX, startY, angle, up, down, left, right, brake, timerRef, lastRef);
            }
        });
    }

    private void setupKeyReleasedHandler(Scene scene, boolean[] up, boolean[] down, boolean[] left,
                                        boolean[] right, boolean[] brake) {
        scene.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            KeyCode kc = e.getCode();
            if (kc == KeyCode.W || kc == KeyCode.UP) up[0] = false;
            if (kc == KeyCode.S || kc == KeyCode.DOWN) down[0] = false;
            if (kc == KeyCode.A || kc == KeyCode.LEFT) left[0] = false;
            if (kc == KeyCode.D || kc == KeyCode.RIGHT) right[0] = false;
            if (kc == KeyCode.SPACE) brake[0] = false;
        });
    }

    private void restartGame(double startX, double startY, int angle, boolean[] up, boolean[] down,
                           boolean[] left, boolean[] right, boolean[] brake,
                           AnimationTimer[] timerRef, long[] lastRef) {
        carro.revive(startX, startY, angle);
        up[0] = down[0] = left[0] = right[0] = brake[0] = false;
        lastRef[0] = 0;
        if (timerRef[0] != null) timerRef[0].start();
    }

    private void addRestartButton(Pane root) {
        Button restartBtn = new Button("Restart (R)");
        restartBtn.setLayoutX(10);
        restartBtn.setLayoutY(10);
        root.getChildren().add(restartBtn);
    }

    private void startGameLoop(AnimationTimer[] timerRef, long[] lastRef, boolean[] up, boolean[] down,
                              boolean[] left, boolean[] right, boolean[] brake) {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastRef[0] == 0) {
                    lastRef[0] = now;
                    return;
                }

                double dt = (now - lastRef[0]) / 1_000_000_000.0;
                lastRef[0] = now;

                processInputs(up[0], down[0], left[0], right[0], brake[0], dt);

                carro.update(dt);

                if (carro.isDestroyed()) {
                    this.stop();
                }
            }
        };

        timerRef[0] = timer;
        timer.start();
    }

    private void processInputs(boolean up, boolean down, boolean left, boolean right, boolean brake, double dt) {
        // Throttle
        double throttle = 0.0;
        if (up) throttle += 1.0;
        if (down) throttle -= 1.0;
        carro.setThrottle(throttle);

        // Brake
        carro.setBraking(brake);

        // Steering
        double steeringRate = calculateSteeringRate();
        applySteeringInput(left, right, steeringRate, dt);
    }

    private double calculateSteeringRate() {
        double absSpeed = Math.abs(carro.getSpeed());

        if (absSpeed < MIN_SPEED_FOR_STEERING) {
            return MAX_STEER_DEG_PER_SEC;
        }

        double speedRatio = Math.min(1.0, absSpeed / carro.getMaxForwardSpeed());
        double t = Math.sqrt(speedRatio);
        return MAX_STEER_DEG_PER_SEC - (MAX_STEER_DEG_PER_SEC - MIN_STEER_DEG_PER_SEC) * t;
    }

    private void applySteeringInput(boolean left, boolean right, double steeringRate, double dt) {
        double steerSign = 0.0;
        if (left) steerSign -= 1.0;
        if (right) steerSign += 1.0;

        // Invert steering when moving backwards
        if (carro.getSpeed() < 0) {
            steerSign = -steerSign;
        }

        double steerDegThisFrame = steerSign * steeringRate * dt;
        if (steerDegThisFrame != 0) {
            carro.applySteering(steerDegThisFrame);
        }
    }

    private void redesenharMapa() {
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int y = 0; y < ALTURA; y++) {
            for (int x = 0; x < LARGURA; x++) {
                if (mapa[y][x] == 0) {
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }
}
