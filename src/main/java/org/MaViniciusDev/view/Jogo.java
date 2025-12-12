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
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Jogo {
    // Classe interna para representar um checkpoint
    private static class Checkpoint {
        double x, y;
        double radius;

        Checkpoint(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        boolean isCarInside(double carX, double carY) {
            double dx = carX - x;
            double dy = carY - y;
            return (dx * dx + dy * dy) <= (radius * radius);
        }
    }

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

    // Lap system
    private int currentLap = 0;
    private static final int TOTAL_LAPS = 3;
    private double startLineX1, startLineY1, startLineX2, startLineY2;
    private boolean lastFrameAboveLine = true;
    private Label lapLabel;
    private Label wrongWayLabel;
    private boolean raceFinished = false;
    private boolean hasLeftStartZone = false;
    private boolean canCountLap = false; // Flag para garantir que só conta uma volta por cruzamento
    private double startX, startY;
    private double forwardDirX, forwardDirY; // Direção esperada de progresso
    private static final double MIN_DISTANCE_FROM_START = 100.0; // Distância mínima para considerar que saiu da zona inicial

    // Checkpoint system
    private java.util.List<Checkpoint> checkpoints;
    private int currentCheckpoint = 0;
    private int lastCheckpointPassed = -1;
    private static final int MIN_CHECKPOINTS = 4; // Mínimo de checkpoints para detectar direção

    public void setup(Stage stage, int[][] mapa, Integer inicioX, Integer inicioY, int direcao, EditorMapa editorMapa) {
        this.mapa = mapa;
        this.editorMapa = editorMapa;

        initializeCanvas();

        this.startX = inicioX * cellWidth;
        this.startY = inicioY * cellHeight;
        int angle = calculateInitialAngle(direcao);

        calculateStartLine(this.startX, this.startY, angle);
        generateCheckpoints(this.startX, this.startY, angle);
        redesenharMapa();
        drawStartLine();
        drawCheckpoints();

        initializeCar(this.startX, this.startY, angle);

        Scene scene = createGameScene(this.startX, this.startY, angle);
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

    private void calculateStartLine(double startX, double startY, int angle) {
        // A linha é perpendicular à direção do carro
        double perpendicularAngle = angle + 90;

        // Direção esperada de avanço (mesma direção inicial do carro)
        forwardDirX = Math.cos(Math.toRadians(angle));
        forwardDirY = Math.sin(Math.toRadians(angle));

        // Vetor unitário perpendicular
        double perpDirX = Math.cos(Math.toRadians(perpendicularAngle));
        double perpDirY = Math.sin(Math.toRadians(perpendicularAngle));

        // Encontrar a largura da pista no ponto inicial
        // Limitar a busca para não sair da pista
        double maxSearchDistance = Math.max(cellWidth, cellHeight) * 20; // Busca razoável
        double dist1 = findWallDistance(startX, startY, perpDirX, perpDirY, maxSearchDistance);
        double dist2 = findWallDistance(startX, startY, -perpDirX, -perpDirY, maxSearchDistance);

        // Reduzir um pouco as distâncias para garantir que a linha fica dentro da pista
        double margin = Math.min(cellWidth, cellHeight) * 0.5;
        dist1 = Math.max(0, dist1 - margin);
        dist2 = Math.max(0, dist2 - margin);

        // Posicionar os pontos da linha
        startLineX1 = startX - perpDirX * dist2;
        startLineY1 = startY - perpDirY * dist2;
        startLineX2 = startX + perpDirX * dist1;
        startLineY2 = startY + perpDirY * dist1;
    }

    private double findWallDistance(double startX, double startY, double dirX, double dirY, double maxDist) {
        double step = Math.min(cellWidth, cellHeight) / 2.0;
        double dist = 0;

        while (dist < maxDist) {
            dist += step;
            double testX = startX + dirX * dist;
            double testY = startY + dirY * dist;

            // Verificar se está fora dos limites ou bateu em parede
            int gridX = (int)(testX / cellWidth);
            int gridY = (int)(testY / cellHeight);

            if (gridX < 0 || gridX >= LARGURA || gridY < 0 || gridY >= ALTURA) {
                return dist - step;
            }

            if (mapa[gridY][gridX] == 1) {
                return dist - step;
            }
        }

        return maxDist;
    }

    private void drawStartLine() {
        gc.setStroke(Color.LIME);
        gc.setLineWidth(3);
        gc.strokeLine(startLineX1, startLineY1, startLineX2, startLineY2);

        // Desenhar padrão xadrez para linha de largada
        gc.setLineWidth(2);
        int segments = 10;
        for (int i = 0; i < segments; i++) {
            double t1 = (double) i / segments;
            double t2 = (double) (i + 1) / segments;

            double x1 = startLineX1 + (startLineX2 - startLineX1) * t1;
            double y1 = startLineY1 + (startLineY2 - startLineY1) * t1;
            double x2 = startLineX1 + (startLineX2 - startLineX1) * t2;
            double y2 = startLineY1 + (startLineY2 - startLineY1) * t2;

            if (i % 2 == 0) {
                gc.setStroke(Color.WHITE);
            } else {
                gc.setStroke(Color.BLACK);
            }
            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void generateCheckpoints(double startX, double startY, double startAngle) {
        checkpoints = new java.util.ArrayList<>();

        // Centralizar o ponto inicial na pista
        TrackPoint centerStart = findTrackCenter(startX, startY, startAngle);

        // Simular o trajeto a partir do ponto inicial
        double currentX = centerStart.x;
        double currentY = centerStart.y;
        double currentAngle = startAngle;

        int maxCheckpoints = 30; // Limite de checkpoints
        double minDistanceBetweenCheckpoints = MIN_DISTANCE_FROM_START;

        java.util.List<String> visitedRegions = new java.util.ArrayList<>();

        for (int i = 0; i < maxCheckpoints; i++) {
            // Avançar na direção atual
            double stepSize = minDistanceBetweenCheckpoints;
            double newX = currentX + Math.cos(Math.toRadians(currentAngle)) * stepSize;
            double newY = currentY + Math.sin(Math.toRadians(currentAngle)) * stepSize;

            // Verificar se a posição é válida (não está em parede)
            int gridX = (int)(newX / cellWidth);
            int gridY = (int)(newY / cellHeight);

            if (gridX < 0 || gridX >= LARGURA || gridY < 0 || gridY >= ALTURA) {
                break;
            }

            if (mapa[gridY][gridX] == 1) {
                // Bateu em parede, tentar outras direções
                boolean foundPath = false;
                for (int angleOffset = -90; angleOffset <= 90; angleOffset += 10) {
                    double testAngle = currentAngle + angleOffset;
                    double testX = currentX + Math.cos(Math.toRadians(testAngle)) * stepSize * 0.5;
                    double testY = currentY + Math.sin(Math.toRadians(testAngle)) * stepSize * 0.5;

                    int testGridX = (int)(testX / cellWidth);
                    int testGridY = (int)(testY / cellHeight);

                    if (testGridX >= 0 && testGridX < LARGURA && testGridY >= 0 && testGridY < ALTURA) {
                        if (mapa[testGridY][testGridX] == 0) {
                            newX = testX;
                            newY = testY;
                            currentAngle = testAngle;
                            foundPath = true;
                            break;
                        }
                    }
                }

                if (!foundPath) {
                    break;
                }
            }

            // Verificar se voltou ao início (completou o circuito)
            double distToStart = Math.sqrt(Math.pow(newX - startX, 2) + Math.pow(newY - startY, 2));
            if (i > 3 && distToStart < minDistanceBetweenCheckpoints * 0.8) {
                System.out.println("Circuito completo detectado!");
                break; // Completou o circuito
            }

            // Encontrar o centro da pista neste ponto
            TrackPoint trackPoint = findTrackCenter(newX, newY, currentAngle);

            // Criar região única baseada em grid maior
            int regionX = (int)(trackPoint.x / (cellWidth * 5));
            int regionY = (int)(trackPoint.y / (cellHeight * 5));
            String regionKey = regionX + "," + regionY;

            // Adicionar checkpoint apenas se não visitou essa região
            if (!visitedRegions.contains(regionKey)) {
                checkpoints.add(new Checkpoint(trackPoint.x, trackPoint.y, trackPoint.width / 2.0));
                visitedRegions.add(regionKey);

                // Atualizar posição e ângulo baseado no centro da pista
                currentX = trackPoint.x;
                currentY = trackPoint.y;

                // Ajustar o ângulo para seguir a direção da pista
                double newAngle = estimateTrackDirection(trackPoint.x, trackPoint.y, currentAngle);
                currentAngle = newAngle;
            } else {
                // Ainda atualizar a posição para não ficar preso
                currentX = newX;
                currentY = newY;
            }
        }

        System.out.println("Gerados " + checkpoints.size() + " checkpoints ao longo do circuito");
    }

    // Classe para armazenar informações sobre um ponto da pista
    private static class TrackPoint {
        double x, y;
        double width;

        TrackPoint(double x, double y, double width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    private TrackPoint findTrackCenter(double x, double y, double angle) {
        // Direção perpendicular ao movimento
        double perpAngle = angle + 90;
        double perpDirX = Math.cos(Math.toRadians(perpAngle));
        double perpDirY = Math.sin(Math.toRadians(perpAngle));

        // Encontrar as bordas da pista (paredes) nas duas direções perpendiculares
        double maxSearchDist = Math.max(canvas.getWidth(), canvas.getHeight()) / 4.0;
        double distLeft = findWallDistance(x, y, perpDirX, perpDirY, maxSearchDist);
        double distRight = findWallDistance(x, y, -perpDirX, -perpDirY, maxSearchDist);

        // Calcular o centro da pista
        double centerX = x + perpDirX * (distLeft - distRight) / 2.0;
        double centerY = y + perpDirY * (distLeft - distRight) / 2.0;

        // Verificar se o centro calculado está em área válida
        int gridX = (int)(centerX / cellWidth);
        int gridY = (int)(centerY / cellHeight);

        if (gridX >= 0 && gridX < LARGURA && gridY >= 0 && gridY < ALTURA && mapa[gridY][gridX] == 0) {
            // Centro válido
            double trackWidth = distLeft + distRight;
            return new TrackPoint(centerX, centerY, trackWidth);
        } else {
            // Centro inválido, usar posição original
            double trackWidth = distLeft + distRight;
            return new TrackPoint(x, y, trackWidth);
        }
    }

    private double estimateTrackDirection(double x, double y, double currentAngle) {
        // Testar várias direções para encontrar a que tem mais espaço livre à frente
        double bestAngle = currentAngle;
        double maxDistance = 0;

        double testDistance = MIN_DISTANCE_FROM_START * 2;

        for (int angleOffset = -45; angleOffset <= 45; angleOffset += 15) {
            double testAngle = currentAngle + angleOffset;
            double testDirX = Math.cos(Math.toRadians(testAngle));
            double testDirY = Math.sin(Math.toRadians(testAngle));

            // Verificar distância até parede nesta direção
            double dist = findWallDistance(x, y, testDirX, testDirY, testDistance);

            if (dist > maxDistance) {
                maxDistance = dist;
                bestAngle = testAngle;
            }
        }

        return bestAngle;
    }

    private void drawCheckpoints() {
        // Desenhar checkpoints (opcional, para debug)
        if (checkpoints == null || checkpoints.isEmpty()) {
            return;
        }

        gc.setLineWidth(2);
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);

            // Desenhar círculo preenchido no centro
            gc.setFill(Color.rgb(255, 255, 0, 0.3)); // Amarelo semi-transparente
            gc.fillOval(cp.x - 5, cp.y - 5, 10, 10);

            // Desenhar o raio de detecção (largura da pista)
            gc.setStroke(Color.YELLOW);
            gc.strokeOval(cp.x - cp.radius, cp.y - cp.radius, cp.radius * 2, cp.radius * 2);

            // Desenhar número do checkpoint
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            String text = String.valueOf(i);
            gc.strokeText(text, cp.x - 5, cp.y + 5);
            gc.fillText(text, cp.x - 5, cp.y + 5);
        }

        // Desenhar linha conectando os checkpoints para visualizar o caminho
        gc.setStroke(Color.rgb(255, 255, 0, 0.5));
        gc.setLineWidth(1);
        for (int i = 0; i < checkpoints.size() - 1; i++) {
            Checkpoint cp1 = checkpoints.get(i);
            Checkpoint cp2 = checkpoints.get(i + 1);
            gc.strokeLine(cp1.x, cp1.y, cp2.x, cp2.y);
        }
        // Conectar o último ao primeiro para fechar o circuito
        if (checkpoints.size() > 1) {
            Checkpoint first = checkpoints.get(0);
            Checkpoint last = checkpoints.get(checkpoints.size() - 1);
            gc.strokeLine(last.x, last.y, first.x, first.y);
        }
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
        addLapCounter(root);
        addWrongWayLabel(root);

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
        resetRace();
        if (timerRef[0] != null) timerRef[0].start();
    }

    private void addRestartButton(Pane root) {
        Button restartBtn = new Button("Restart (R)");
        restartBtn.setLayoutX(10);
        restartBtn.setLayoutY(10);
        root.getChildren().add(restartBtn);
    }

    private void addLapCounter(Pane root) {
        lapLabel = new Label("Volta: 0 / " + TOTAL_LAPS);
        lapLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lapLabel.setTextFill(Color.WHITE);
        lapLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 10px; -fx-background-radius: 5px;");
        lapLabel.setLayoutX(1050);
        lapLabel.setLayoutY(10);
        root.getChildren().add(lapLabel);
    }

    private void addWrongWayLabel(Pane root) {
        wrongWayLabel = new Label("CAMINHO ERRADO!");
        wrongWayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        wrongWayLabel.setTextFill(Color.RED);
        wrongWayLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-padding: 8px; -fx-background-radius: 6px;");
        wrongWayLabel.setLayoutX(480);
        wrongWayLabel.setLayoutY(60);
        wrongWayLabel.setVisible(false);
        root.getChildren().add(wrongWayLabel);
    }

    private void updateLapCounter() {
        if (lapLabel != null) {
            lapLabel.setText("Volta: " + currentLap + " / " + TOTAL_LAPS);
        }
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

                if (!raceFinished) {
                    processInputs(up[0], down[0], left[0], right[0], brake[0], dt);
                    carro.update(dt);
                    checkLineCrossing();
                    checkWrongWay();
                }

                if (carro.isDestroyed()) {
                    this.stop();
                }

                if (raceFinished) {
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

    private void checkLineCrossing() {
        double carX = carro.getTranslateX();
        double carY = carro.getTranslateY();

        // Calcular distância do ponto inicial
        double distanceFromStart = Math.sqrt(Math.pow(carX - startX, 2) + Math.pow(carY - startY, 2));

        // Verificar se o carro saiu da zona inicial
        if (!hasLeftStartZone && distanceFromStart > MIN_DISTANCE_FROM_START) {
            hasLeftStartZone = true;
            canCountLap = true; // Pode contar volta após sair da zona inicial
            System.out.println("Carro saiu da zona inicial!");
        }

        // Só verificar cruzamento se já saiu da zona inicial
        if (!hasLeftStartZone) {
            return;
        }

        // Verificar se o carro está sobre a linha de largada
        boolean isOnStartLine = isPointOnStartLine(carX, carY);

        // Verifica de qual lado da linha o carro está
        boolean isAboveLine = isPointAboveLine(carX, carY);

        // Se o carro cruzou a linha (mudou de lado) E está próximo da linha
        if (lastFrameAboveLine != isAboveLine && isOnStartLine) {
            // Apenas conta se o carro está se movendo para frente e se pode contar
            if (canCountLap && carro.getSpeed() > 20.0) { // Velocidade mínima para contar volta
                onLineCrossed();
                canCountLap = false; // Bloqueia contagem até sair da linha novamente
            }
            lastFrameAboveLine = isAboveLine;
        }

        // Atualizar o lado apenas se não está na linha
        if (!isOnStartLine && lastFrameAboveLine != isAboveLine) {
            lastFrameAboveLine = isAboveLine;
        }

        // Se está longe da linha, permite contar nova volta
        if (!canCountLap && distanceFromStart > MIN_DISTANCE_FROM_START) {
            double distFromLine = distanceToLine(carX, carY);
            if (distFromLine > MIN_DISTANCE_FROM_START * 0.5) {
                canCountLap = true; // Permite contar nova volta
            }
        }
    }

    private boolean isPointOnStartLine(double px, double py) {
        // Verificar se o ponto está próximo da linha de largada
        double distToLine = distanceToLine(px, py);

        // Também verificar se está dentro do segmento da linha (não nas extensões)
        double lineLength = Math.sqrt(
            Math.pow(startLineX2 - startLineX1, 2) +
            Math.pow(startLineY2 - startLineY1, 2)
        );

        // Projeção do ponto na linha
        double dx = startLineX2 - startLineX1;
        double dy = startLineY2 - startLineY1;
        double t = ((px - startLineX1) * dx + (py - startLineY1) * dy) / (lineLength * lineLength);

        // Verificar se está dentro do segmento (0 <= t <= 1) e próximo da linha
        double maxCarSize = Math.max(cellWidth, cellHeight) * 3;
        return t >= -0.1 && t <= 1.1 && distToLine < maxCarSize;
    }

    private double distanceToLine(double px, double py) {
        // Distância de um ponto a uma linha
        double A = startLineY2 - startLineY1;
        double B = startLineX1 - startLineX2;
        double C = startLineX2 * startLineY1 - startLineX1 * startLineY2;

        return Math.abs(A * px + B * py + C) / Math.sqrt(A * A + B * B);
    }

    private void checkWrongWay() {
        if (checkpoints == null || checkpoints.isEmpty()) {
            if (wrongWayLabel != null) {
                wrongWayLabel.setVisible(false);
            }
            return;
        }

        double carX = carro.getTranslateX();
        double carY = carro.getTranslateY();
        double speed = carro.getSpeed();

        // Verificar qual checkpoint está mais próximo
        int nearestCheckpoint = -1;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            double dx = carX - cp.x;
            double dy = carY - cp.y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestCheckpoint = i;
            }
        }

        // Detectar se está indo no caminho errado
        boolean wrongWay = false;

        if (Math.abs(speed) > 5.0 && hasLeftStartZone) {
            // Verificar se o checkpoint mais próximo é anterior ao último passado
            // Isso indica que está indo na direção errada
            if (lastCheckpointPassed >= 0) {
                int checkpointDiff = nearestCheckpoint - lastCheckpointPassed;

                // Normalizar para lidar com volta completa
                if (checkpointDiff < -checkpoints.size() / 2) {
                    checkpointDiff += checkpoints.size();
                } else if (checkpointDiff > checkpoints.size() / 2) {
                    checkpointDiff -= checkpoints.size();
                }

                // Se está se aproximando de checkpoints anteriores, está errado
                wrongWay = checkpointDiff < -1;
            }

            // Atualizar checkpoint atual se passou por ele
            if (nearestDistance < checkpoints.get(nearestCheckpoint).radius) {
                if (nearestCheckpoint != lastCheckpointPassed) {
                    int expectedNext = (lastCheckpointPassed + 1) % checkpoints.size();

                    // Verificar se passou pelo checkpoint correto
                    if (nearestCheckpoint == expectedNext || lastCheckpointPassed == -1) {
                        lastCheckpointPassed = nearestCheckpoint;
                        currentCheckpoint = nearestCheckpoint;
                        System.out.println("Checkpoint " + nearestCheckpoint + " atingido");
                    }
                }
            }
        }

        if (wrongWayLabel != null) {
            wrongWayLabel.setVisible(wrongWay);
        }
    }

    private boolean isPointAboveLine(double px, double py) {
        // Produto vetorial para determinar de qual lado da linha o ponto está
        double dx = startLineX2 - startLineX1;
        double dy = startLineY2 - startLineY1;
        double dpx = px - startLineX1;
        double dpy = py - startLineY1;

        double cross = dx * dpy - dy * dpx;
        return cross > 0;
    }

    private void onLineCrossed() {
        currentLap++;
        updateLapCounter();
        System.out.println("Volta " + currentLap + " completada!");

        if (currentLap >= TOTAL_LAPS) {
            onRaceFinished();
        }
    }

    private void onRaceFinished() {
        raceFinished = true;
        System.out.println("Corrida finalizada! Parabéns!");
        showVictoryScreen();
    }

    private void showVictoryScreen() {
        Pane parent = (Pane) canvas.getParent();

        // Overlay escuro
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(0, 0, 1280, 720);
        overlay.setFill(Color.color(0, 0, 0, 0.8));

        // Texto de vitória
        Label victoryLabel = new Label("CORRIDA COMPLETA!");
        victoryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        victoryLabel.setTextFill(Color.LIME);
        victoryLabel.setLayoutX(400);
        victoryLabel.setLayoutY(250);

        Label subLabel = new Label("Você completou " + TOTAL_LAPS + " voltas!");
        subLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 24));
        subLabel.setTextFill(Color.WHITE);
        subLabel.setLayoutX(450);
        subLabel.setLayoutY(320);

        // Botões
        Button restartButton = new Button("Reiniciar Corrida (R)");
        restartButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        restartButton.setLayoutX(520);
        restartButton.setLayoutY(400);
        restartButton.setOnAction(e -> {
            parent.getChildren().removeAll(overlay, victoryLabel, subLabel, restartButton);
            resetRace();
        });

        parent.getChildren().addAll(overlay, victoryLabel, subLabel, restartButton);
    }

    private void resetRace() {
        currentLap = 0;
        raceFinished = false;
        lastFrameAboveLine = true;
        hasLeftStartZone = false;
        canCountLap = false;
        currentCheckpoint = 0;
        lastCheckpointPassed = -1;
        if (wrongWayLabel != null) {
            wrongWayLabel.setVisible(false);
        }
        updateLapCounter();
    }
}
