package org.MaViniciusDev.view;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Carro extends Group {
    private double x, y;
    private double width = 20;
    private double height = 10;
    private double speed = 0;
    private double angle = 0; // in degrees
    private int[][] mapa;
    private int largura, altura;
    private double cellWidth, cellHeight;

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
        // Corpo do carro
        Rectangle body = new Rectangle(width, height);
        body.setFill(Color.RED);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1);

        // Rodas
        Rectangle wheel1 = new Rectangle(3, 5);
        wheel1.setFill(Color.BLACK);
        wheel1.setTranslateX(2);
        wheel1.setTranslateY(2);

        Rectangle wheel2 = new Rectangle(3, 5);
        wheel2.setFill(Color.BLACK);
        wheel2.setTranslateX(width - 5);
        wheel2.setTranslateY(2);

        Rectangle wheel3 = new Rectangle(3, 5);
        wheel3.setFill(Color.BLACK);
        wheel3.setTranslateX(2);
        wheel3.setTranslateY(height - 7);

        Rectangle wheel4 = new Rectangle(3, 5);
        wheel4.setFill(Color.BLACK);
        wheel4.setTranslateX(width - 5);
        wheel4.setTranslateY(height - 7);

        getChildren().addAll(body, wheel1, wheel2, wheel3, wheel4);
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        setTranslateX(x);
        setTranslateY(y);
    }

    public void setAngle(double angle) {
        this.angle = angle;
        setRotate(angle);
    }

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

    public void updatePosition() {
        double newX = x + speed * Math.cos(Math.toRadians(angle));
        double newY = y + speed * Math.sin(Math.toRadians(angle));
        if (!collides(newX, newY)) {
            x = newX;
            y = newY;
            setTranslateX(x);
            setTranslateY(y);
        } else {
            speed = 0; // para o carro ao colidir
            System.out.println("Colisão detectada! Carro parado.");
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
