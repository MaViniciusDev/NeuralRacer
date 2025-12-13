package org.MaViniciusDev.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Arrays;

public class SensorSystem {
    // Sensor configuration
    private static final int NUM_SENSORS = 5;

    // --- ALTERAÇÃO: VISÃO DE LONGO ALCANCE (300px) ---
    private static final double MAX_SENSOR_RANGE = 300.0;

    private static final double[] SENSOR_ANGLES = {-45, -22.5, 0, 22.5, 45};

    private final double[] sensorReadings = new double[NUM_SENSORS];

    private final int[][] mapa;
    private final int largura;
    private final int altura;
    private final double cellWidth;
    private final double cellHeight;

    public enum DistanceLevel {
        MUITO_PERTO, PERTO, MEDIO, LONGE, MUITO_LONGE
    }

    public enum SteeringLevel {
        MUITO_ESQUERDA, ESQUERDA, CENTRO, DIREITA, MUITO_DIREITA
    }

    public SensorSystem(int[][] mapa, int largura, int altura, double cellWidth, double cellHeight) {
        this.mapa = mapa;
        this.largura = largura;
        this.altura = altura;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        Arrays.fill(sensorReadings, 100.0);
    }

    public void updateSensors(double carX, double carY, double carAngle) {
        for (int i = 0; i < NUM_SENSORS; i++) {
            double sensorAngle = carAngle + SENSOR_ANGLES[i];
            sensorReadings[i] = castRay(carX, carY, sensorAngle);
        }
    }

    private double castRay(double startX, double startY, double angle) {
        double dirX = Math.cos(Math.toRadians(angle));
        double dirY = Math.sin(Math.toRadians(angle));

        double step = Math.min(cellWidth, cellHeight) / 2.0;
        double distance = 0;

        while (distance < MAX_SENSOR_RANGE) {
            distance += step;
            double testX = startX + dirX * distance;
            double testY = startY + dirY * distance;

            int gridX = (int)(testX / cellWidth);
            int gridY = (int)(testY / cellHeight);

            if (gridX < 0 || gridX >= largura || gridY < 0 || gridY >= altura) {
                return (distance / MAX_SENSOR_RANGE) * 100.0;
            }

            if (mapa[gridY][gridX] == 1) {
                return (distance / MAX_SENSOR_RANGE) * 100.0;
            }
        }
        return 100.0;
    }

    public double getMembership(double value, DistanceLevel level) {
        return switch (level) {
            case MUITO_PERTO -> trapezoid(value, 0, 0, 10, 20);
            case PERTO -> triangle(value, 15, 27.5, 40);
            case MEDIO -> triangle(value, 35, 50, 65);
            case LONGE -> triangle(value, 60, 72.5, 85);
            case MUITO_LONGE -> trapezoid(value, 80, 90, 100, 100);
        };
    }

    private double triangle(double x, double a, double b, double c) {
        if (x <= a || x >= c) return 0.0;
        if (x == b) return 1.0;
        if (x < b) return (x - a) / (b - a);
        return (c - x) / (c - b);
    }

    private double trapezoid(double x, double a, double b, double c, double d) {
        if (x <= a || x >= d) return 0.0;
        if (x >= b && x <= c) return 1.0;
        if (x < b) return (x - a) / (b - a);
        return (d - x) / (d - c);
    }

    public double calculateFuzzySteeringAngle() { return 0.0; }

    public void drawSensors(GraphicsContext gc, double carX, double carY, double carAngle) {
        for (int i = 0; i < NUM_SENSORS; i++) {
            double sensorAngle = carAngle + SENSOR_ANGLES[i];
            double distance = (sensorReadings[i] / 100.0) * MAX_SENSOR_RANGE;

            double dirX = Math.cos(Math.toRadians(sensorAngle));
            double dirY = Math.sin(Math.toRadians(sensorAngle));

            double endX = carX + dirX * distance;
            double endY = carY + dirY * distance;

            double normalized = sensorReadings[i] / 100.0;
            Color sensorColor = Color.color(1.0 - normalized, normalized, 0, 0.5);

            gc.setStroke(sensorColor);
            gc.setLineWidth(1);
            gc.strokeLine(carX, carY, endX, endY);

            gc.setFill(sensorColor);
            gc.fillOval(endX - 2, endY - 2, 4, 4);
        }
    }

    public double[] getSensorReadings() { return sensorReadings.clone(); }

    public double[] getFuzzySnapshot() {
        double[] snapshot = new double[25];
        int index = 0;
        for (int i = 0; i < NUM_SENSORS; i++) {
            double leitura = sensorReadings[i];
            snapshot[index++] = getMembership(leitura, DistanceLevel.MUITO_PERTO);
            snapshot[index++] = getMembership(leitura, DistanceLevel.PERTO);
            snapshot[index++] = getMembership(leitura, DistanceLevel.MEDIO);
            snapshot[index++] = getMembership(leitura, DistanceLevel.LONGE);
            snapshot[index++] = getMembership(leitura, DistanceLevel.MUITO_LONGE);
        }
        return snapshot;
    }
}