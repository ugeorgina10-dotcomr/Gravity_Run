package com.example.gravityrun;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GameView extends View implements SensorEventListener {

    private enum GameState {
        RUNNING, PAUSED, WON, LOST
    }
    private GameState gameState = GameState.RUNNING;

    private final SensorManager sensorManager;
    private final Sensor accelerometer, proximity, lightSensor;

    private final float START_X = 60;
    private final float START_Y = 60;
    private float ballX = START_X, ballY = START_Y;
    private final float radius = 30;

    private static class Wall {
        RectF rect;
        public Wall(float left, float top, float right, float bottom) {
            this.rect = new RectF(left, top, right, bottom);
        }
    }
    private List<Wall> walls;

    private RectF exitRect;
    private final float movementSpeed = 2.5f;

    private float maxX, maxY;

    // Colores fijos para el nivel único (se restauran los valores originales)
    private final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#A0522D"); // Marrón madera
    private final int DEFAULT_WALL_COLOR = Color.DKGRAY; // Gris oscuro

    private int backgroundColor; // El color de fondo actual (puede cambiar por el sensor de luz)
    private int wallColor;       // El color de las paredes (fijo)

    private final Paint paintBall, paintText, paintWalls, paintExit;

    public GameView(Context context) {
        super(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        paintBall = new Paint();
        paintBall.setColor(Color.RED);
        paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(80);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintWalls = new Paint();
        paintExit = new Paint();

        // Estilo visual mejorado para la salida y las paredes
        paintExit.setColor(Color.GREEN);
        paintExit.setStyle(Paint.Style.FILL);
        paintExit.setShadowLayer(10.0f, 0.0f, 0.0f, Color.WHITE); // Sombra blanca

        setupWalls(); // Configura el único laberinto al inicio.

        resumeSensors();
    }

    // Configura los muros, colores y la salida para el único nivel.
    private void setupWalls() {
        walls = new ArrayList<>();
        ballX = START_X; // Reinicia la posición de la bola
        ballY = START_Y;

        wallColor = DEFAULT_WALL_COLOR;
        backgroundColor = DEFAULT_BACKGROUND_COLOR;

        paintWalls.setColor(wallColor);

        final float BORDER_WIDTH = 10;
        final float MAX_X = 1000 + BORDER_WIDTH;
        final float MAX_Y = 1700 + BORDER_WIDTH;
        final float WALL_THICKNESS = 20;

        // Definición de la salida.
        final float EXIT_LEFT = 450;
        final float EXIT_RIGHT = 650;
        final float EXIT_TOP = 1700;
        final float EXIT_BOTTOM = MAX_Y;
        exitRect = new RectF(EXIT_LEFT, EXIT_TOP, EXIT_RIGHT, EXIT_BOTTOM);

        // Muros de Borde (con hueco para la salida)
        walls.add(new Wall(0, 0, BORDER_WIDTH, MAX_Y)); // Izquierdo
        walls.add(new Wall(1000, 0, MAX_X, MAX_Y)); // Derecho
        walls.add(new Wall(0, 0, MAX_X, BORDER_WIDTH)); // Superior

        // Muros Inferiores (crean el hueco de la salida)
        walls.add(new Wall(0, EXIT_TOP, EXIT_LEFT, EXIT_BOTTOM)); // Inferior Izquierda al hueco
        walls.add(new Wall(EXIT_RIGHT, EXIT_TOP, MAX_X, EXIT_BOTTOM)); // Inferior Derecha al hueco

        // Laberinto Único (el diseño original)
        walls.add(new Wall(10, 100, 300, 100 + WALL_THICKNESS));
        walls.add(new Wall(400, 100, 1000, 100 + WALL_THICKNESS));
        walls.add(new Wall(100, 100, 100 + WALL_THICKNESS, 400));
        walls.add(new Wall(100, 400, 500, 400 + WALL_THICKNESS));
        walls.add(new Wall(600, 400, 1000, 400 + WALL_THICKNESS));
        walls.add(new Wall(300, 100, 300 + WALL_THICKNESS, 200));
        walls.add(new Wall(300, 300, 300 + WALL_THICKNESS, 400));
        walls.add(new Wall(300, 500, 300 + WALL_THICKNESS, 800));
        walls.add(new Wall(400, 600, 900, 600 + WALL_THICKNESS));
        walls.add(new Wall(600, 200, 600 + WALL_THICKNESS, 600));
        walls.add(new Wall(10, 800, 700, 800 + WALL_THICKNESS));
        walls.add(new Wall(800, 800, 1000, 800 + WALL_THICKNESS));
        walls.add(new Wall(100, 900, 100 + WALL_THICKNESS, 1400));
        walls.add(new Wall(200, 1000, 800, 1000 + WALL_THICKNESS));
        walls.add(new Wall(500, 900, 500 + WALL_THICKNESS, 1200));
        walls.add(new Wall(10, 1300, 400, 1300 + WALL_THICKNESS));
        walls.add(new Wall(500, 1300, 900, 1300 + WALL_THICKNESS));
        walls.add(new Wall(900, 1300, 900 + WALL_THICKNESS, 1600));

        // Muros de camino final, asegurando que el camino lleve a la salida
        walls.add(new Wall(10, 1600, 450, 1600 + WALL_THICKNESS)); // Muro que llega al borde izquierdo del hueco de salida
        walls.add(new Wall(650, 1600, 1000, 1600 + WALL_THICKNESS)); // Muro que llega al borde derecho del hueco de salida

        gameState = GameState.RUNNING;
        invalidate();
    }

    private boolean checkWallCollision(float newX, float newY) {
        for (Wall wall : walls) {
            RectF ballRect = new RectF(
                    newX - radius,
                    newY - radius,
                    newX + radius,
                    newY + radius
            );
            if (RectF.intersects(ballRect, wall.rect)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkExitReached(float newX, float newY) {
        RectF ballRect = new RectF(
                newX - radius,
                newY - radius,
                newX + radius,
                newY + radius
        );
        return RectF.intersects(ballRect, exitRect);
    }

    // Reinicia el juego, volviendo al estado inicial.
    private void resetGame() {
        setupWalls();
        gameState = GameState.RUNNING; // Vuelve al estado de juego
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        maxX = getWidth();
        maxY = getHeight();

        canvas.drawColor(backgroundColor);

        for (Wall wall : walls) {
            canvas.drawRect(wall.rect, paintWalls);
        }

        if (gameState != GameState.WON) {
            // Dibuja la salida con un borde redondeado para destacarla
            canvas.drawRoundRect(exitRect, 10, 10, paintExit);
        }

        if (gameState == GameState.RUNNING || gameState == GameState.PAUSED || gameState == GameState.LOST) {
            canvas.drawCircle(ballX, ballY, radius, paintBall);
        }

        String message = null;
        if (gameState == GameState.PAUSED) {
            message = "PAUSADO (Cubre Proximidad para Reanudar)";
            paintText.setColor(Color.YELLOW);
        } else if (gameState == GameState.WON) {
            message = "¡GANASTE! Toca para empezar de nuevo.";
            paintText.setColor(Color.GREEN);
            pauseSensors();
        } else if (gameState == GameState.LOST) {
            message = "¡GAME OVER! Toca para reiniciar";
            paintText.setColor(Color.RED);
        }

        if (message != null) {
            canvas.drawText(message, maxX / 2, maxY / 2, paintText);
        }

        if (gameState == GameState.RUNNING) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameState == GameState.WON || gameState == GameState.LOST) {
                resetGame();
                resumeSensors(); // Asegura que los sensores se reanuden después de ganar/perder
                return true;
            }
        }
        return super.onTouchEvent(event);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < event.sensor.getMaximumRange()) {
                if (gameState == GameState.RUNNING) {
                    gameState = GameState.PAUSED;
                }
            } else {
                if (gameState == GameState.PAUSED) {
                    gameState = GameState.RUNNING;
                }
            }
        }

        if (gameState != GameState.RUNNING) {
            invalidate();
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float xMovement = -event.values[0] * movementSpeed;
            float yMovement = event.values[1] * movementSpeed;

            float nextX = ballX + xMovement;
            float nextY = ballY + yMovement;

            if (checkExitReached(nextX, nextY)) {
                gameState = GameState.WON;
            }
            else if (checkWallCollision(nextX, nextY)) {
                if (!checkWallCollision(nextX, ballY)) {
                    ballX = nextX;
                }
                if (!checkWallCollision(ballX, nextY)) {
                    ballY = nextY;
                }
            } else {
                ballX = nextX;
                ballY = nextY;
            }

            if (ballX < radius) ballX = radius;
            if (ballY < radius) ballY = radius;
            if (ballX > maxX - radius) ballX = maxX - radius;
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightValue = event.values[0];
            // Si hay poca luz, usa color oscuro; de lo contrario, usa el color de fondo predeterminado.
            backgroundColor = (lightValue < 10) ? Color.DKGRAY : DEFAULT_BACKGROUND_COLOR;
        }
        if (gameState != GameState.RUNNING) {
            invalidate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void resumeSensors() {
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (proximity != null)
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        if (lightSensor != null)
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void pauseSensors() {
        sensorManager.unregisterListener(this);
    }
}
