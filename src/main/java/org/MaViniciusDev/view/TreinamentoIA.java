package org.MaViniciusDev.view;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.MaViniciusDev.ia.CerebroGenetico;

import java.util.ArrayList;
import java.util.List;

/**
 * TreinamentoIA class handles the genetic algorithm training visualization for AI cars.
 * It displays multiple cars evolving in real-time on the canvas.
 */
public class TreinamentoIA {

    // --- HYPERPARAMETERS ---
    private int targetPopulation = 100;
    private double currentMutationRate = 0.05;
    private double timeScale = 1.0;
    private boolean showSensors = false;

    // --- CONFIGURATION CONSTANTS ---
    private static final int GRID_WIDTH = 128;
    private static final int GRID_HEIGHT = 72;
    private static final double CANVAS_WIDTH = 1280.0;
    private static final double CANVAS_HEIGHT = 720.0;
    private static final double MIN_DISTANCE_FROM_START = 100.0;
    private static final double INITIAL_TIME_REMAINING = 6.0;
    private static final double TIME_BONUS_PER_CHECKPOINT = 4.0;
    private static final double MAX_TIME_REMAINING = 15.0;
    private static final double FITNESS_PER_CHECKPOINT = 1000.0;
    private static final double FITNESS_PER_LAP = 5000.0;
    private static final double SLOWNESS_PENALTY_FACTOR = 2.0;
    private static final int LAPS_TO_WIN = 3;
    private static final int GENERATIONS_WITHOUT_IMPROVEMENT_THRESHOLD = 5;
    private static final double MUTATION_BOOST_RATE = 0.20;

    // --- UI COMPONENTS ---
    private StackPane root;
    private Pane gameContainer;
    private Canvas canvas;
    private GraphicsContext gc;
    private Label infoLabel;
    private Label winLabel;

    // --- GAME STATE ---
    private int[][] map;
    private double startX, startY;
    private int startDirection;
    private double startLineX1, startLineY1, startLineX2, startLineY2;

    private List<Checkpoint> checkpoints;
    private List<AICar> population;
    private int generation = 1;
    private int aliveCars;
    private boolean training = true;

    private double bestFitnessHistory = 0;
    private int generationsWithoutImprovement = 0;

    private AnimationTimer timer;
    private EditorMapa editorReference;

    // Cell dimensions for grid calculations
    private double cellWidth, cellHeight;

    /**
     * Sets up the training scene with the given parameters.
     */
    public void setup(Stage stage, int[][] map, Integer startGridX, Integer startGridY, int direction, EditorMapa editor) {
        this.map = map;
        this.editorReference = editor;

        // Calculate cell dimensions based on fixed canvas size
        this.cellWidth = CANVAS_WIDTH / GRID_WIDTH;
        this.cellHeight = CANVAS_HEIGHT / GRID_HEIGHT;

        this.startX = startGridX * cellWidth;
        this.startY = startGridY * cellHeight;
        this.startDirection = direction;
        start(stage);
    }

    /**
     * Initializes and starts the training UI.
     */
    public void start(Stage stage) {
        initializeUI();
        initializeGameLogic();
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
        stage.setScene(scene);
        startGameLoop();
    }

    /**
     * Initializes the JavaFX UI components.
     */
    private void initializeUI() {
        root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        gameContainer = new Pane();
        gameContainer.setPrefSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        gameContainer.setMaxSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        gameContainer.setMinSize(CANVAS_WIDTH, CANVAS_HEIGHT);

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        infoLabel = new Label("Generation: 1");
        infoLabel.setTextFill(Color.WHITE);
        infoLabel.setFont(new Font("Consolas", 16));
        infoLabel.setLayoutX(10);
        infoLabel.setLayoutY(10);

        winLabel = new Label("SUCCESS! (" + LAPS_TO_WIN + " LAPS)");
        winLabel.setTextFill(Color.LIME);
        winLabel.setFont(new Font("Arial Black", 40));
        winLabel.setLayoutX(300);
        winLabel.setLayoutY(300);
        winLabel.setVisible(false);

        gameContainer.getChildren().addAll(canvas, infoLabel, winLabel);

        VBox controlPanel = createControlPanel();
        StackPane.setAlignment(controlPanel, Pos.CENTER_RIGHT);

        root.getChildren().addAll(gameContainer, controlPanel);
    }

    /**
     * Creates the control panel with sliders and buttons.
     */
    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: rgba(30, 30, 30, 0.9); -fx-border-color: #444; -fx-border-width: 0 0 0 2;");
        panel.setMaxWidth(250);
        panel.setMinWidth(250);

        Label title = new Label("AI Lab");
        title.setTextFill(Color.CYAN);
        title.setFont(new Font("Arial Bold", 18));

        // Speed slider
        Label speedLabel = new Label("Speed: 1.0x");
        speedLabel.setTextFill(Color.WHITE);
        Slider speedSlider = new Slider(0.1, 5.0, 1.0);
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            timeScale = val.doubleValue();
            speedLabel.setText(String.format("Speed: %.1fx", timeScale));
        });

        // Mutation slider
        Label mutationLabel = new Label("Mutation: 5%");
        mutationLabel.setTextFill(Color.WHITE);
        Slider mutationSlider = new Slider(0, 0.5, 0.05);
        mutationSlider.valueProperty().addListener((obs, old, val) -> {
            currentMutationRate = val.doubleValue();
            mutationLabel.setText(String.format("Mutation: %.0f%%", currentMutationRate * 100));
        });

        // Population slider
        Label populationLabel = new Label("Next Population: 100");
        populationLabel.setTextFill(Color.WHITE);
        Slider populationSlider = new Slider(10, 300, 100);
        populationSlider.valueProperty().addListener((obs, old, val) -> {
            targetPopulation = val.intValue();
            populationLabel.setText("Next Population: " + targetPopulation);
        });

        // Sensors checkbox
        CheckBox sensorsCheckBox = new CheckBox("Show Sensors");
        sensorsCheckBox.setTextFill(Color.WHITE);
        sensorsCheckBox.setSelected(showSensors);
        sensorsCheckBox.selectedProperty().addListener((obs, old, val) -> showSensors = val);

        // Kill all button
        Button killAllButton = new Button("💀 Kill All");
        killAllButton.setMaxWidth(Double.MAX_VALUE);
        killAllButton.setStyle("-fx-background-color: #800; -fx-text-fill: white;");
        killAllButton.setOnAction(e -> population.forEach(AICar::destroy));

        // Back button
        Button backButton = new Button("Back to Editor");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> {
            training = false;
            if (timer != null) timer.stop();
            editorReference.voltarParaEditor();
        });

        panel.getChildren().addAll(
                title, new Separator(),
                speedLabel, speedSlider,
                mutationLabel, mutationSlider,
                populationLabel, populationSlider,
                new Separator(), sensorsCheckBox,
                new Separator(), killAllButton, backButton
        );

        return panel;
    }

    /**
     * Initializes game logic components like checkpoints and population.
     */
    private void initializeGameLogic() {
        int angle = getAngleFromDirection(startDirection);
        calculateStartLine(startX, startY, angle);
        generateCheckpoints(startX, startY, angle);
        initializePopulation();
    }

    /**
     * Initializes the population of AI cars.
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < targetPopulation; i++) {
            AICar car = new AICar(startX, startY, map, GRID_WIDTH, GRID_HEIGHT, cellWidth, cellHeight);
            car.setAngle(getAngleFromDirection(startDirection));
            car.setOpacity(0.5);
            gameContainer.getChildren().add(car);
            population.add(car);
        }
        aliveCars = targetPopulation;
    }

    /**
     * Starts the main game loop using AnimationTimer.
     */
    private void startGameLoop() {
        timer = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (!training) return;
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double rawDt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                double dt = Math.min(rawDt, 0.1) * timeScale;

                updateGame(dt);
                render();
            }
        };
        timer.start();
    }

    /**
     * Updates the game state for the current frame.
     */
    private void updateGame(double dt) {
        redrawBackground();
        drawVisuals();

        boolean allDead = true;
        aliveCars = 0;
        double bestFitness = 0;
        int maxLaps = 0;
        AICar bestCar = null;

        for (AICar car : population) {
            if (!car.isDestroyed()) {
                allDead = false;
                aliveCars++;

                car.control(dt);
                car.updateFitness();

                if (car.fitness > bestFitness) {
                    bestFitness = car.fitness;
                    bestCar = car;
                }
                if (car.laps > maxLaps) maxLaps = car.laps;

                if (car.laps >= LAPS_TO_WIN) {
                    training = false;
                    winLabel.setVisible(true);
                    winLabel.setText("AI WINS! GEN " + generation);
                    timer.stop();
                }
            }
        }

        if (showSensors && bestCar != null) {
            bestCar.getSensorSystem().drawSensors(gc, bestCar.getCenterX(), bestCar.getCenterY(), bestCar.getAngle());
        }

        infoLabel.setText(String.format("Gen: %d | Alive: %d | Fit: %.0f | Laps: %d/%d\nMutation: %.0f%% | Pop: %d",
                generation, aliveCars, bestFitness, maxLaps, LAPS_TO_WIN, currentMutationRate * 100, population.size()));

        if (training && allDead) {
            evolveNextGeneration();
        }
    }

    /**
     * Evolves the population to the next generation.
     */
    private void evolveNextGeneration() {
        population.sort((c1, c2) -> Double.compare(c2.fitness, c1.fitness));
        AICar best = population.get(0);

        System.out.println("Gen " + generation + " | Best Fit: " + (int) best.fitness + " | Mutation: " + (int) (currentMutationRate * 100) + "%");

        if (best.fitness > bestFitnessHistory) {
            bestFitnessHistory = best.fitness;
            generationsWithoutImprovement = 0;
        } else {
            generationsWithoutImprovement++;
        }

        double effectiveMutation = currentMutationRate;
        if (generationsWithoutImprovement > GENERATIONS_WITHOUT_IMPROVEMENT_THRESHOLD && currentMutationRate < MUTATION_BOOST_RATE) {
            effectiveMutation = MUTATION_BOOST_RATE;
            System.out.println(">> Auto-Boost Mutation!");
        }

        List<AICar> newPopulation = new ArrayList<>();

        // Elitism: keep top 2
        newPopulation.add(createChild(population.get(0).brain, cellWidth, cellHeight));
        newPopulation.add(createChild(population.get(1).brain, cellWidth, cellHeight));

        while (newPopulation.size() < targetPopulation) {
            AICar parent1 = tournamentSelection();
            AICar parent2 = tournamentSelection();
            CerebroGenetico childBrain = crossover(parent1.brain, parent2.brain);
            childBrain.mutar(effectiveMutation);
            newPopulation.add(createChild(childBrain, cellWidth, cellHeight));
        }

        gameContainer.getChildren().removeIf(node -> node instanceof AICar);

        population = newPopulation;
        for (AICar car : population) {
            car.setOpacity(0.5);
            gameContainer.getChildren().add(car);
        }
        generation++;
    }

    /**
     * Creates a new AI car with the given brain.
     */
    private AICar createChild(CerebroGenetico brain, double cw, double ch) {
        AICar child = new AICar(startX, startY, map, GRID_WIDTH, GRID_HEIGHT, cw, ch);
        child.setAngle(getAngleFromDirection(startDirection));
        child.brain = new CerebroGenetico(brain.getCromossomo().clone());
        return child;
    }

    /**
     * Performs tournament selection to choose a parent.
     */
    private AICar tournamentSelection() {
        int idx1 = (int) (Math.random() * population.size());
        int idx2 = (int) (Math.random() * population.size());
        int idx3 = (int) (Math.random() * population.size());
        AICar c1 = population.get(idx1);
        AICar c2 = population.get(idx2);
        AICar c3 = population.get(idx3);
        AICar best = c1;
        if (c2.fitness > best.fitness) best = c2;
        if (c3.fitness > best.fitness) best = c3;
        return best;
    }

    /**
     * Performs crossover between two brains.
     */
    private CerebroGenetico crossover(CerebroGenetico p1, CerebroGenetico p2) {
        boolean[] genes1 = p1.getCromossomo();
        boolean[] genes2 = p2.getCromossomo();
        boolean[] newGenes = new boolean[genes1.length];
        for (int i = 0; i < genes1.length; i++) {
            newGenes[i] = Math.random() < 0.5 ? genes1[i] : genes2[i];
        }
        return new CerebroGenetico(newGenes);
    }

    /**
     * Redraws the background map.
     */
    private void redrawBackground() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double cw = canvas.getWidth() / GRID_WIDTH;
        double ch = canvas.getHeight() / GRID_HEIGHT;
        gc.setFill(Color.WHITE);
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (map[y][x] == 0) gc.fillRect(x * cw, y * ch, cw, ch);
            }
        }
    }

    /**
     * Draws additional visuals like the start line.
     */
    private void drawVisuals() {
        gc.setStroke(Color.LIME);
        gc.setLineWidth(3);
        gc.strokeLine(startLineX1, startLineY1, startLineX2, startLineY2);
        gc.setLineWidth(1);
        gc.setStroke(Color.WHITE);
        gc.strokeLine(startLineX1, startLineY1, startLineX2, startLineY2);
    }

    /**
     * Renders the current frame (placeholder for additional rendering).
     */
    private void render() {
        // Additional rendering logic can be added here if needed
    }

    // --- HELPER CLASSES ---

    private static class Checkpoint {
        double x, y, radius;

        Checkpoint(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    private static class TrackPoint {
        double x, y, width;

        TrackPoint(double x, double y, double width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    // --- TRACK GENERATION METHODS ---

    private void generateCheckpoints(double startX, double startY, double startAngle) {
        checkpoints = new ArrayList<>();
        TrackPoint centerStart = findTrackCenter(startX, startY, startAngle);
        double currentX = centerStart.x;
        double currentY = centerStart.y;
        double currentAngle = startAngle;
        int maxCheckpoints = 60;
        List<String> visitedRegions = new ArrayList<>();

        for (int i = 0; i < maxCheckpoints; i++) {
            double newX = currentX + Math.cos(Math.toRadians(currentAngle)) * MIN_DISTANCE_FROM_START;
            double newY = currentY + Math.sin(Math.toRadians(currentAngle)) * MIN_DISTANCE_FROM_START;
            int gridX = (int) (newX / cellWidth);
            int gridY = (int) (newY / cellHeight);

            if (gridX < 0 || gridX >= GRID_WIDTH || gridY < 0 || gridY >= GRID_HEIGHT || map[gridY][gridX] == 1) {
                boolean foundPath = false;
                for (int angleOffset = -90; angleOffset <= 90; angleOffset += 10) {
                    double testAngle = currentAngle + angleOffset;
                    double testX = currentX + Math.cos(Math.toRadians(testAngle)) * MIN_DISTANCE_FROM_START * 0.6;
                    double testY = currentY + Math.sin(Math.toRadians(testAngle)) * MIN_DISTANCE_FROM_START * 0.6;
                    int tgX = (int) (testX / cellWidth);
                    int tgY = (int) (testY / cellHeight);
                    if (tgX >= 0 && tgX < GRID_WIDTH && tgY >= 0 && tgY < GRID_HEIGHT && map[tgY][tgX] == 0) {
                        newX = testX;
                        newY = testY;
                        currentAngle = testAngle;
                        foundPath = true;
                        break;
                    }
                }
                if (!foundPath) break;
            }
            if (i > 4) {
                double distToStart = Math.sqrt(Math.pow(newX - startX, 2) + Math.pow(newY - startY, 2));
                if (distToStart < MIN_DISTANCE_FROM_START * 0.8) break;
            }
            TrackPoint trackPoint = findTrackCenter(newX, newY, currentAngle);
            int regionX = (int) (trackPoint.x / (cellWidth * 3));
            int regionY = (int) (trackPoint.y / (cellHeight * 3));
            String regionKey = regionX + "," + regionY;
            if (!visitedRegions.contains(regionKey)) {
                checkpoints.add(new Checkpoint(trackPoint.x, trackPoint.y, trackPoint.width / 2.0));
                visitedRegions.add(regionKey);
                currentX = trackPoint.x;
                currentY = trackPoint.y;
            } else {
                currentX = newX;
                currentY = newY;
            }
        }
    }

    private TrackPoint findTrackCenter(double x, double y, double angle) {
        double perpAngle = angle + 90;
        double perpDirX = Math.cos(Math.toRadians(perpAngle));
        double perpDirY = Math.sin(Math.toRadians(perpAngle));
        double maxSearch = 200;
        double distLeft = findWallDistance(x, y, perpDirX, perpDirY, maxSearch);
        double distRight = findWallDistance(x, y, -perpDirX, -perpDirY, maxSearch);
        double centerX = x + perpDirX * (distLeft - distRight) / 2.0;
        double centerY = y + perpDirY * (distLeft - distRight) / 2.0;
        double width = distLeft + distRight;
        return new TrackPoint(centerX, centerY, width);
    }

    private void calculateStartLine(double startX, double startY, int angle) {
        double perpendicularAngle = angle + 90;
        double perpDirX = Math.cos(Math.toRadians(perpendicularAngle));
        double perpDirY = Math.sin(Math.toRadians(perpendicularAngle));
        double maxSearchDistance = Math.max(cellWidth, cellHeight) * 20;
        double dist1 = findWallDistance(startX, startY, perpDirX, perpDirY, maxSearchDistance);
        double dist2 = findWallDistance(startX, startY, -perpDirX, -perpDirY, maxSearchDistance);
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
            int gridX = (int) (testX / cellWidth);
            int gridY = (int) (testY / cellHeight);
            if (gridX < 0 || gridX >= GRID_WIDTH || gridY < 0 || gridY >= GRID_HEIGHT) return dist - step;
            if (map[gridY][gridX] == 1) return dist - step;
        }
        return maxDist;
    }

    private int getAngleFromDirection(int dir) {
        return switch (dir) {
            case 0 -> 270;
            case 1 -> 315;
            case 2 -> 0;
            case 3 -> 45;
            case 4 -> 90;
            case 5 -> 135;
            case 6 -> 180;
            case 7 -> 225;
            default -> 0;
        };
    }

    // --- AI CAR CLASS ---

    /**
     * AICar represents an AI-controlled car with genetic brain.
     */
    public class AICar extends Carro {
        public CerebroGenetico brain;
        public double fitness = 0;
        public int laps = 0;
        public int nextCheckpointIndex = 0;
        public int totalCheckpointsPassed = 0;
        private double timeRemaining = INITIAL_TIME_REMAINING;
        private double lastX, lastY;
        private double distanceTraveled = 0;
        private double totalLifetime = 0;

        public AICar(double x, double y, int[][] m, int l, int a, double cw, double ch) {
            super(x, y, m, l, a, cw, ch);
            this.brain = new CerebroGenetico();
            this.lastX = x;
            this.lastY = y;
        }

        /**
         * Controls the car based on AI decisions.
         */
        public void control(double dt) {
            timeRemaining -= dt;
            totalLifetime += dt;
            if (timeRemaining <= 0) {
                destroy();
                return;
            }

            double[] inputs = getSensorSystem().getFuzzySnapshot();
            double desiredAngle = brain.processarDirecao(inputs);
            double desiredAcceleration = brain.processarAceleracao(inputs);

            if (desiredAcceleration == 0.0) desiredAcceleration = 0.5;
            if (desiredAcceleration < 0) {
                this.setThrottle(0);
                this.setBraking(true);
            } else {
                this.setBraking(false);
                this.setThrottle(desiredAcceleration);
            }

            double steeringLimit = 180.0 * dt;
            double actualSteering = Math.max(-steeringLimit, Math.min(steeringLimit, desiredAngle));
            this.applySteering(actualSteering);
            super.update(dt);
        }

        /**
         * Updates the fitness score based on progress.
         */
        public void updateFitness() {
            if (checkpoints == null || checkpoints.isEmpty()) return;
            Checkpoint nextCP = checkpoints.get(nextCheckpointIndex);
            double distToNext = Math.sqrt(Math.pow(getX() - nextCP.x, 2) + Math.pow(getY() - nextCP.y, 2));
            if (distToNext < nextCP.radius) {
                nextCheckpointIndex++;
                totalCheckpointsPassed++;
                fitness += FITNESS_PER_CHECKPOINT;
                timeRemaining += TIME_BONUS_PER_CHECKPOINT;
                if (timeRemaining > MAX_TIME_REMAINING) timeRemaining = MAX_TIME_REMAINING;
                if (nextCheckpointIndex >= checkpoints.size()) {
                    nextCheckpointIndex = 0;
                    laps++;
                    fitness += FITNESS_PER_LAP;
                }
            }
            double dx = getX() - lastX;
            double dy = getY() - lastY;
            double frameDistance = Math.sqrt(dx * dx + dy * dy);
            double rad = Math.toRadians(getAngle());
            if (dx * Math.cos(rad) + dy * Math.sin(rad) > 0) {
                distanceTraveled += frameDistance;
            } else {
                distanceTraveled -= frameDistance;
            }
            double slownessPenalty = totalLifetime * SLOWNESS_PENALTY_FACTOR;
            this.fitness = (totalCheckpointsPassed * 2000) + Math.max(0, distanceTraveled) - slownessPenalty;
            this.lastX = getX();
            this.lastY = getY();
        }
    }
}

