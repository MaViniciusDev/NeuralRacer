package org.MaViniciusDev.view;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.animation.AnimationTimer;
import java.io.InputStream;

public class Carro extends Group {
    // --- IMAGENS ESTÁTICAS (Cache) ---
    private static Image CACHED_CAR_IMAGE = null;
    private static Image CACHED_CRASH_IMAGE = null;

    // --- Variáveis PROTECTED ---
    protected double x, y;
    // Valores iniciais padrão, serão ajustados pela imagem
    protected double width = 20;
    protected double height = 10;
    protected double speed = 0;
    protected double angle = 0;

    protected final int[][] mapa;
    protected final int largura;
    protected final int altura;
    protected final double cellWidth;
    protected final double cellHeight;

    // Componentes Visuais
    protected Rectangle body;
    protected ImageView carView;
    private Rectangle wheel1, wheel2, wheel3, wheel4;
    private ImageView crashView;
    private double rotationOffset = 0;

    // Física
    private static final double ACCELERATION = 200.0;
    private static final double BRAKING_FORCE = 300.0;
    private static final double MAX_FORWARD_SPEED = 350.0;
    private static final double MAX_REVERSE_SPEED = -100.0;
    private static final double DRAG = 50.0;

    // Controle
    private double throttleInput = 0;
    private boolean braking = false;
    protected boolean destroyed = false;

    // Sensores
    protected final SensorSystem sensorSystem;

    public Carro(double startX, double startY, int[][] mapa, int largura, int altura, double cellWidth, double cellHeight) {
        this.x = startX;
        this.y = startY;
        this.mapa = mapa;
        this.largura = largura;
        this.altura = altura;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.sensorSystem = new SensorSystem(mapa, largura, altura, cellWidth, cellHeight);

        desenharCarro();
        setTranslateX(x);
        setTranslateY(y);
    }

    private void desenharCarro() {
        Image carImg = getCarImage();

        if (carImg != null) {
            setupCarImageView(carImg);
        } else {
            setupFallbackRectangle();
        }
        setupCrashView();
    }

    // --- SISTEMA DE CACHE ---
    private Image getCarImage() {
        if (CACHED_CAR_IMAGE != null) return CACHED_CAR_IMAGE;
        CACHED_CAR_IMAGE = tryLoadImage("car_top.png", "carro_top.png", "car.png");
        return CACHED_CAR_IMAGE;
    }

    private Image getCrashImage() {
        if (CACHED_CRASH_IMAGE != null) return CACHED_CRASH_IMAGE;
        CACHED_CRASH_IMAGE = tryLoadImage("explosao.gif", "crash.gif");
        return CACHED_CRASH_IMAGE;
    }

    private Image tryLoadImage(String... candidates) {
        for (String name : candidates) {
            try {
                InputStream is = getClass().getResourceAsStream("/images/" + name);
                if (is != null) return new Image(is);
            } catch (Exception e) { }
        }
        return null;
    }

    // --- CORREÇÃO DO TAMANHO DA IMAGEM AQUI ---
    private void setupCarImageView(Image carImg) {
        carView = new ImageView(carImg);
        carView.setPreserveRatio(true);

        double imgW = carImg.getWidth();
        double imgH = carImg.getHeight();

        // Define um tamanho alvo relativo ao tamanho das células do mapa.
        // O carro terá aproximadamente o tamanho de 2 células na sua maior dimensão.
        double targetDimension = Math.min(cellWidth, cellHeight) * 2.0;

        if (imgW > imgH) {
            // Imagem estilo paisagem (larga)
            carView.setFitWidth(targetDimension);
            // Atualiza as dimensões físicas do carro
            this.width = targetDimension;
            this.height = imgH * (targetDimension / imgW);
        } else {
            // Imagem estilo retrato (alta) ou quadrada
            carView.setFitHeight(targetDimension);
            // Atualiza as dimensões físicas do carro
            this.height = targetDimension;
            this.width = imgW * (targetDimension / imgH);
        }

        rotationOffset = 90.0; // Ajuste se necessário dependendo da sua imagem
        getChildren().add(carView);
    }

    private void setupFallbackRectangle() {
        // Atualiza dimensões físicas para o fallback também
        this.width = Math.min(cellWidth, cellHeight) * 1.5;
        this.height = this.width * 0.6;

        body = new Rectangle(width, height, Color.RED);
        body.setStroke(Color.BLACK);

        double wW = width * 0.2;
        double wH = height * 0.3;

        wheel1 = new Rectangle(wW, wH, Color.BLACK); wheel1.setTranslateX(width*0.1); wheel1.setTranslateY(-wH/2);
        wheel2 = new Rectangle(wW, wH, Color.BLACK); wheel2.setTranslateX(width*0.7); wheel2.setTranslateY(-wH/2);
        wheel3 = new Rectangle(wW, wH, Color.BLACK); wheel3.setTranslateX(width*0.1); wheel3.setTranslateY(height - wH/2);
        wheel4 = new Rectangle(wW, wH, Color.BLACK); wheel4.setTranslateX(width*0.7); wheel4.setTranslateY(height - wH/2);
        getChildren().addAll(body, wheel1, wheel2, wheel3, wheel4);
    }

    private void setupCrashView() {
        crashView = new ImageView();
        crashView.setVisible(false);
    }

    // --- Física e Controle ---
    public void setAngle(double angle) {
        this.angle = angle;
        setRotate(angle + rotationOffset);
    }

    public void applySteering(double deltaDegrees) {
        setAngle(this.angle + deltaDegrees);
    }

    public void setThrottle(double t) {
        if (destroyed) return;
        this.throttleInput = Math.max(-1, Math.min(1, t));
    }

    public void setBraking(boolean braking) {
        if (!destroyed) this.braking = braking;
    }

    public void update(double dt) {
        if (dt <= 0 || destroyed) return;

        double accel = 0;
        if (throttleInput != 0) accel = throttleInput * ACCELERATION;

        if (braking) {
            if (speed > 0) accel -= BRAKING_FORCE;
            else if (speed < 0) accel += BRAKING_FORCE;
        }

        if (speed > 0) accel -= DRAG;
        else if (speed < 0) accel += DRAG;

        speed += accel * dt;

        if (speed > MAX_FORWARD_SPEED) speed = MAX_FORWARD_SPEED;
        if (speed < MAX_REVERSE_SPEED) speed = MAX_REVERSE_SPEED;
        if (Math.abs(speed) < 5 && throttleInput == 0) speed = 0;

        moveCarOrDestroy(dt);
        sensorSystem.updateSensors(getCenterX(), getCenterY(), angle);
    }

    private void moveCarOrDestroy(double dt) {
        double rad = Math.toRadians(angle);
        double newX = x + speed * Math.cos(rad) * dt;
        double newY = y + speed * Math.sin(rad) * dt;

        if (!collides(newX, newY)) {
            x = newX;
            y = newY;
            setTranslateX(x);
            setTranslateY(y);
        } else {
            destroy();
        }
    }

    protected boolean collides(double px, double py) {
        // Verificação de colisão usando o centro do carro é mais segura
        double centerX = px + width / 2.0;
        double centerY = py + height / 2.0;

        int gridX = (int) (centerX / cellWidth);
        int gridY = (int) (centerY / cellHeight);

        if (gridX < 0 || gridX >= largura || gridY < 0 || gridY >= altura) return true;
        return mapa[gridY][gridX] == 1;
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
    }

    private void applyDestroyedVisuals() {
        if (carView != null) {
            ColorAdjust crashTint = new ColorAdjust();
            crashTint.setSaturation(-1.0);
            crashTint.setBrightness(-0.5);
            carView.setEffect(crashTint);
        } else if (body != null) {
            body.setFill(Color.DARKGRAY);
        }
    }

    private void showCrashGif() {
        Image crashImg = getCrashImage();
        if (crashImg == null) return;

        // Adiciona a explosão no painel pai para ficar sobre tudo
        if (getParent() instanceof javafx.scene.layout.Pane parent) {
            ImageView explosionInstance = new ImageView(crashImg);
            explosionInstance.setPreserveRatio(true);
            // Explosão 50% maior que o carro
            explosionInstance.setFitWidth(Math.max(width, height) * 1.5);

            // Centraliza a explosão na posição atual do carro
            double currentCenterX = getTranslateX() + width / 2.0;
            double currentCenterY = getTranslateY() + height / 2.0;

            explosionInstance.setLayoutX(currentCenterX - explosionInstance.getFitWidth() / 2.0);
            explosionInstance.setLayoutY(currentCenterY - explosionInstance.getBoundsInLocal().getHeight() / 2.0);

            parent.getChildren().add(explosionInstance);

            // Remove após 1.5s
            new AnimationTimer() {
                long start = -1;
                @Override
                public void handle(long now) {
                    if (start == -1) start = now;
                    if (now - start > 1_500_000_000L) {
                        parent.getChildren().remove(explosionInstance);
                        this.stop();
                    }
                }
            }.start();
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public double getCenterX() { return x + width / 2; }
    public double getCenterY() { return y + height / 2; }
    public boolean isDestroyed() { return destroyed; }
    public SensorSystem getSensorSystem() { return sensorSystem; }
    public double getSpeed() { return speed; }

    public void revive(double sx, double sy, double sa) {
        destroyed = false;
        x = sx; y = sy; angle = sa;
        speed = 0;
        setTranslateX(x); setTranslateY(y); setAngle(angle);
        setOpacity(1.0);
        if (carView != null) carView.setEffect(null);
        if (body != null) body.setFill(Color.RED);
    }
}