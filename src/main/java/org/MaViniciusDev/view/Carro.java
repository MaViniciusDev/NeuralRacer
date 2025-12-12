package org.MaViniciusDev.view;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;

public class Carro extends Group {
    // Position and dimensions
    private double x, y;
    private double width = 20;
    private double height = 10;

    // Physics state
    private double speed = 0;
    private double angle = 0;

    // Map collision detection
    private final int[][] mapa;
    private final int largura;
    private final int altura;
    private final double cellWidth;
    private final double cellHeight;

    // Visual components
    private Rectangle body;
    private ImageView carView;
    private Rectangle wheel1, wheel2, wheel3, wheel4;
    private ImageView crashView;
    private double rotationOffset = 0;

    // Physics parameters
    private static final double ACCELERATION = 200.0;
    private static final double BRAKING_FORCE = 300.0;
    private static final double MAX_FORWARD_SPEED = 300.0;
    private static final double MAX_REVERSE_SPEED = -100.0;
    private static final double DRAG = 40.0;

    // Control state
    private double throttleInput = 0;
    private boolean braking = false;
    private boolean destroyed = false;

    public Carro(double startX, double startY, int[][] mapa, int largura, int altura, double cellWidth, double cellHeight) {
        this.x = startX;
        this.y = startY;
        this.mapa = mapa;
        this.largura = largura;
        this.altura = altura;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        desenharCarro();
        setTranslateX(x);
        setTranslateY(y);
    }

    private void desenharCarro() {
        Image carImg = tryLoadCarImage();

        if (carImg != null) {
            setupCarImageView(carImg);
        } else {
            setupFallbackRectangle();
        }

        setupCrashView();
    }

    private Image loadImageFromCandidates(String... candidates) {
        for (String name : candidates) {
            InputStream is = getClass().getResourceAsStream("/images/" + name);
            if (is != null) {
                return new Image(is);
            }

            java.io.File f = new java.io.File("images/" + name);
            if (f.exists()) {
                return new Image("file:images/" + name);
            }
        }

        return null;
    }

    private Image tryLoadCarImage() {
        return loadImageFromCandidates("car_top.png", "carro_top.png", "car.png");
    }

    private void setupCarImageView(Image carImg) {
        carView = new ImageView(carImg);
        carView.setPreserveRatio(true);

        double imgW = carImg.getWidth();
        double imgH = carImg.getHeight();
        double cellMax = Math.max(cellWidth, cellHeight);
        double scaleMultiplier = 2.7;
        double targetMax = cellMax * scaleMultiplier;

        if (imgW >= imgH) {
            carView.setFitWidth(targetMax);
            double resultingH = imgH * (targetMax / imgW);
            this.width = targetMax;
            this.height = resultingH;
        } else {
            carView.setFitHeight(targetMax);
            double resultingW = imgW * (targetMax / imgH);
            this.height = targetMax;
            this.width = resultingW;
        }

        rotationOffset = 90.0;
        getChildren().add(carView);
    }

    private void setupFallbackRectangle() {
        body = new Rectangle(width, height);
        body.setFill(Color.RED);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1);

        wheel1 = new Rectangle(3, 5);
        wheel1.setFill(Color.BLACK);
        wheel1.setTranslateX(2);
        wheel1.setTranslateY(2);

        wheel2 = new Rectangle(3, 5);
        wheel2.setFill(Color.BLACK);
        wheel2.setTranslateX(width - 5);
        wheel2.setTranslateY(2);

        wheel3 = new Rectangle(3, 5);
        wheel3.setFill(Color.BLACK);
        wheel3.setTranslateX(2);
        wheel3.setTranslateY(height - 7);

        wheel4 = new Rectangle(3, 5);
        wheel4.setFill(Color.BLACK);
        wheel4.setTranslateX(width - 5);
        wheel4.setTranslateY(height - 7);

        getChildren().addAll(body, wheel1, wheel2, wheel3, wheel4);
    }

    private void setupCrashView() {
        crashView = new ImageView();
        crashView.setVisible(false);
        crashView.setMouseTransparent(true);
        crashView.setManaged(false);
        crashView.setRotate(0);
    }

    // Position and angle setters

    public void setAngle(double angle) {
        this.angle = angle;
        setRotate(angle + rotationOffset);
    }

    public void applySteering(double deltaDegrees) {
        setAngle(this.angle + deltaDegrees);
    }

    // Getters

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getMaxForwardSpeed() {
        return MAX_FORWARD_SPEED;
    }

    // Control inputs
    public void setThrottle(double t) {
        if (destroyed) return;
        if (t > 1) t = 1;
        if (t < -1) t = -1;
        this.throttleInput = t;
    }

    public void setBraking(boolean braking) {
        if (!destroyed) {
            this.braking = braking;
        }
    }

    // State checks
    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        if (destroyed) return;

        destroyed = true;
        speed = 0;
        throttleInput = 0;
        braking = true;

        applyDestroyedVisuals();
        showCrashGif();

        setMouseTransparent(true);
        System.out.println("Carro destruído!");
    }

    private void applyDestroyedVisuals() {
        if (carView != null) {
            ColorAdjust crashTint = new ColorAdjust();
            crashTint.setSaturation(-1.0);
            crashTint.setBrightness(-0.6);
            carView.setEffect(crashTint);
        } else {
            Color crashGray = Color.web("#050505");
            if (body != null) body.setFill(crashGray);
            if (wheel1 != null) wheel1.setFill(crashGray);
            if (wheel2 != null) wheel2.setFill(crashGray);
            if (wheel3 != null) wheel3.setFill(crashGray);
            if (wheel4 != null) wheel4.setFill(crashGray);
        }
    }

    private void showCrashGif() {
        try {
            if (crashView.getImage() == null) {
                Image img = loadCrashGifImage();
                if (img != null) {
                    configureCrashGif(img);
                } else {
                    System.err.println("Crash GIF não encontrado em resources/images/ ou images/ (candidatos: explosao.gif)");
                }
            }

            if (crashView.getImage() != null) {
                displayCrashGif();
            }
        } catch (Exception ex) {
            System.err.println("Não foi possível carregar o GIF de colisão: " + ex.getMessage());
        }
    }

    private Image loadCrashGifImage() {
        return loadImageFromCandidates("explosao.gif");
    }

    private void configureCrashGif(Image img) {
        crashView.setImage(img);
        crashView.setPreserveRatio(true);
        crashView.setFitWidth(width * 1.5);
        crashView.setFitHeight(height * 1.5);
        crashView.setTranslateX(-width * 0.25);
        crashView.setTranslateY(-height * 0.25);
    }

    private void displayCrashGif() {
        if (!(getParent() instanceof javafx.scene.layout.Pane parent)) {
            if (!getChildren().contains(crashView)) {
                getChildren().add(crashView);
            }
            crashView.setVisible(true);
            crashView.toFront();
            return;
        }

        Platform.runLater(() -> {
            try {
                positionAndShowCrashGif(parent);
                scheduleGifHiding(parent);
            } catch (Exception ex) {
                System.err.println("Erro ao posicionar crashView: " + ex.getMessage());
            }
        });
    }

    private void positionAndShowCrashGif(javafx.scene.layout.Pane parent) {
        Image imgLocal = crashView.getImage();
        double imgW = imgLocal.getWidth();
        double imgH = imgLocal.getHeight();

        double carW = (carView != null) ? carView.getBoundsInLocal().getWidth() : body.getBoundsInLocal().getWidth();
        double carH = (carView != null) ? carView.getBoundsInLocal().getHeight() : body.getBoundsInLocal().getHeight();

        double carMax = Math.max(carW, carH);
        double scaleTarget = 1.2;
        double targetMax = carMax * scaleTarget;

        double fitW, fitH;
        if (imgW >= imgH) {
            fitW = targetMax;
            fitH = imgH * (fitW / imgW);
        } else {
            fitH = targetMax;
            fitW = imgW * (fitH / imgH);
        }

        crashView.setFitWidth(fitW);
        crashView.setFitHeight(fitH);

        javafx.geometry.Point2D center = this.localToParent(carW / 2.0, carH / 2.0);
        double layoutX = center.getX() - (fitW / 2.0);
        double layoutY = center.getY() - (fitH / 2.0);

        crashView.setLayoutX(layoutX);
        crashView.setLayoutY(layoutY);

        if (!parent.getChildren().contains(crashView)) {
            parent.getChildren().add(crashView);
        }

        crashView.setVisible(true);
        crashView.toFront();
    }

    private void scheduleGifHiding(javafx.scene.layout.Pane parent) {
        new AnimationTimer() {
            private final long startTime = System.nanoTime();
            private static final long HIDE_AFTER_NANOS = 2_500_000_000L;

            @Override
            public void handle(long now) {
                if (now - startTime >= HIDE_AFTER_NANOS) {
                    crashView.setVisible(false);
                    parent.getChildren().remove(crashView);
                    this.stop();
                }
            }
        }.start();
    }

    public void revive(double startX, double startY, double startAngle) {
        destroyed = false;
        setOpacity(1.0);
        setMouseTransparent(false);

        this.x = startX;
        this.y = startY;
        setTranslateX(x);
        setTranslateY(y);
        setAngle(startAngle);

        this.speed = 0;
        this.throttleInput = 0;
        this.braking = false;

        restoreOriginalVisuals();
        hideCrashGif();
    }

    private void restoreOriginalVisuals() {
        if (carView != null) {
            carView.setEffect(null);
        } else {
            if (body != null) body.setFill(Color.RED);
            if (wheel1 != null) wheel1.setFill(Color.BLACK);
            if (wheel2 != null) wheel2.setFill(Color.BLACK);
            if (wheel3 != null) wheel3.setFill(Color.BLACK);
            if (wheel4 != null) wheel4.setFill(Color.BLACK);
        }
    }

    private void hideCrashGif() {
        if (crashView != null) {
            crashView.setVisible(false);
            if (crashView.getParent() instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) crashView.getParent()).getChildren().remove(crashView);
            }
        }
    }

    public void update(double dt) {
        if (dt <= 0 || destroyed) return;

        double accel = calculateAcceleration();
        applyPhysics(accel, dt);
        moveCarOrDestroy(dt);
    }

    private double calculateAcceleration() {
        double accel = 0;

        if (throttleInput > 0) {
            accel = throttleInput * ACCELERATION;
        } else if (throttleInput < 0) {
            accel = throttleInput * ACCELERATION;
        }

        if (braking) {
            if (speed > 0) {
                accel -= BRAKING_FORCE;
            } else if (speed < 0) {
                accel += BRAKING_FORCE;
            }
        }

        if (speed > 0) {
            accel -= DRAG;
        } else if (speed < 0) {
            accel += DRAG;
        }

        return accel;
    }

    private void applyPhysics(double accel, double dt) {
        speed += accel * dt;

        if (speed > MAX_FORWARD_SPEED) speed = MAX_FORWARD_SPEED;
        if (speed < MAX_REVERSE_SPEED) speed = MAX_REVERSE_SPEED;
    }

    private void moveCarOrDestroy(double dt) {
        double newX = x + speed * Math.cos(Math.toRadians(angle)) * dt;
        double newY = y + speed * Math.sin(Math.toRadians(angle)) * dt;

        if (!collides(newX, newY)) {
            x = newX;
            y = newY;
            setTranslateX(x);
            setTranslateY(y);
        } else {
            destroy();
        }
    }


    private boolean collides(double px, double py) {
        int left = (int) (px / cellWidth);
        int right = (int) ((px + width) / cellWidth);
        int top = (int) (py / cellHeight);
        int bottom = (int) ((py + height) / cellHeight);

        for (int i = Math.max(0, left); i <= Math.min(largura - 1, right); i++) {
            for (int j = Math.max(0, top); j <= Math.min(altura - 1, bottom); j++) {
                if (mapa[j][i] == 1) {
                    return true;
                }
            }
        }

        return false;
    }
}
