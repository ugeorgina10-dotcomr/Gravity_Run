package com.example.gravityrun;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path; // Necesario para dibujar el triángulo
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View implements SensorEventListener {

    private enum GameState {
        RUNNING, PAUSED, WON, LOST
    }
    private GameState gameState = GameState.RUNNING;

    private int currentLevel = 1; // Controla el nivel actual
    private final int TOTAL_LEVELS = 3; // Número total de niveles

    private final SensorManager sensorManager;
    private final Sensor accelerometer, proximity, lightSensor;

    private float boardOffsetX = 0; // Desplazamiento X para centrar el laberinto
    private float boardOffsetY = 0; // Desplazamiento Y para centrar el laberinto

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

    // Dimensiones Fijas del Tablero del Laberinto
    private final float BOARD_WIDTH = 1000;
    private final float BOARD_HEIGHT = 1700;
    private final float BORDER_WIDTH = 10;
    private final float MAX_BOARD_X = BOARD_WIDTH + BORDER_WIDTH;
    private final float MAX_BOARD_Y = BOARD_HEIGHT + BORDER_WIDTH;
    private final float WALL_THICKNESS = 20;


    // Colores de la Interfaz (Base para cambiar con el nivel)
    private final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#A0522D"); // Marrón madera
    private final int DEFAULT_WALL_COLOR = Color.DKGRAY; // Gris oscuro
    private final int DEFAULT_BALL_COLOR = Color.RED;

    private int backgroundColor;
    private int wallColor;
    private int ballColor;

    private final Paint paintBall, paintText, paintWalls, paintExit;
    private final Path trianglePath = new Path(); // Objeto para dibujar el triángulo

    public GameView(Context context) {
        super(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        paintBall = new Paint();
        paintBall.setColor(DEFAULT_BALL_COLOR);
        paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(80); // Tamaño base
        paintText.setTextAlign(Paint.Align.CENTER);

        paintWalls = new Paint();
        paintExit = new Paint();

        paintExit.setColor(Color.GREEN);
        paintExit.setStyle(Paint.Style.FILL);
        paintExit.setShadowLayer(10.0f, 0.0f, 0.0f, Color.WHITE);

        setupWalls();

        resumeSensors();
    }

    // Clase auxiliar para definir los datos de cada nivel
    private static class LevelData {
        public List<Wall> walls;
        public int wallColor;
        public int ballColor;
        public RectF exitRect;

        public LevelData(List<Wall> walls, int wallColor, int ballColor, RectF exitRect) {
            this.walls = walls;
            this.wallColor = wallColor;
            this.ballColor = ballColor;
            this.exitRect = exitRect;
        }
    }

    private LevelData getLevelData(int level) {
        List<Wall> levelWalls = new ArrayList<>();
        int levelWallColor = DEFAULT_WALL_COLOR;
        int levelBallColor = DEFAULT_BALL_COLOR;
        RectF levelExitRect;

        // Definición de la salida (la misma posición relativa para todos)
        final float EXIT_LEFT = 450;
        final float EXIT_RIGHT = 650;
        final float EXIT_TOP = BOARD_HEIGHT;
        final float EXIT_BOTTOM = MAX_BOARD_Y;
        levelExitRect = new RectF(EXIT_LEFT, EXIT_TOP, EXIT_RIGHT, EXIT_BOTTOM);

        // Muros de Borde (siempre los mismos con el hueco de salida)
        levelWalls.add(new Wall(0, 0, BORDER_WIDTH, MAX_BOARD_Y)); // Izquierdo
        levelWalls.add(new Wall(BOARD_WIDTH, 0, MAX_BOARD_X, MAX_BOARD_Y)); // Derecho
        levelWalls.add(new Wall(0, 0, MAX_BOARD_X, BORDER_WIDTH)); // Superior

        // Muros Inferiores (crean el hueco de la salida)
        levelWalls.add(new Wall(0, EXIT_TOP, EXIT_LEFT, EXIT_BOTTOM)); // Inferior Izquierda al hueco
        levelWalls.add(new Wall(EXIT_RIGHT, EXIT_TOP, MAX_BOARD_X, EXIT_BOTTOM)); // Inferior Derecha al hueco

        // --- Diseño Específico de Cada Nivel ---
        switch (level) {
            case 1: // Nivel 1: Círculo (Rojo)
                levelBallColor = Color.RED;
                levelWallColor = Color.parseColor("#4CAF50"); // Verde
                // Laberinto Único (el diseño original)
                levelWalls.add(new Wall(10, 100, 300, 100 + WALL_THICKNESS));
                levelWalls.add(new Wall(400, 100, 1000, 100 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 100, 100 + WALL_THICKNESS, 400));
                levelWalls.add(new Wall(100, 400, 500, 400 + WALL_THICKNESS));
                levelWalls.add(new Wall(600, 400, 1000, 400 + WALL_THICKNESS));
                levelWalls.add(new Wall(300, 100, 300 + WALL_THICKNESS, 200));
                levelWalls.add(new Wall(300, 300, 300 + WALL_THICKNESS, 400));
                levelWalls.add(new Wall(300, 500, 300 + WALL_THICKNESS, 800));
                levelWalls.add(new Wall(400, 600, 900, 600 + WALL_THICKNESS));
                levelWalls.add(new Wall(600, 200, 600 + WALL_THICKNESS, 600));
                levelWalls.add(new Wall(10, 800, 700, 800 + WALL_THICKNESS));
                levelWalls.add(new Wall(800, 800, 1000, 800 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 900, 100 + WALL_THICKNESS, 1400));
                levelWalls.add(new Wall(200, 1000, 800, 1000 + WALL_THICKNESS));
                levelWalls.add(new Wall(500, 900, 500 + WALL_THICKNESS, 1200));
                levelWalls.add(new Wall(10, 1300, 400, 1300 + WALL_THICKNESS));
                levelWalls.add(new Wall(500, 1300, 900, 1300 + WALL_THICKNESS));
                levelWalls.add(new Wall(900, 1300, 900 + WALL_THICKNESS, 1600));
                // Muros de camino final, asegurando que el camino lleve a la salida
                levelWalls.add(new Wall(10, 1600, 450, 1600 + WALL_THICKNESS));
                levelWalls.add(new Wall(650, 1600, 1000, 1600 + WALL_THICKNESS));
                break;
            case 2: // Nivel 2: Cuadrado (Azul)
                levelBallColor = Color.BLUE;
                levelWallColor = Color.parseColor("#FF9800"); // Naranja
                // Laberinto Cuadrado - Diseño Abierto (Ejemplo)
                levelWalls.add(new Wall(100, 100, 900, 100 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 100, 100 + WALL_THICKNESS, 1500));
                levelWalls.add(new Wall(900, 100, 900 + WALL_THICKNESS, 1500));
                levelWalls.add(new Wall(200, 300, 800, 300 + WALL_THICKNESS));
                levelWalls.add(new Wall(200, 500, 800, 500 + WALL_THICKNESS));
                levelWalls.add(new Wall(200, 700, 800, 700 + WALL_THICKNESS));
                levelWalls.add(new Wall(200, 900, 800, 900 + WALL_THICKNESS));
                levelWalls.add(new Wall(200, 1100, 800, 1100 + WALL_THICKNESS));
                levelWalls.add(new Wall(200, 1300, 800, 1300 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 1500, 900, 1500 + WALL_THICKNESS));
                levelWalls.add(new Wall(400, 1500, 400 + WALL_THICKNESS, 1600));
                levelWalls.add(new Wall(600, 1500, 600 + WALL_THICKNESS, 1600));
                break;
            case 3: // Nivel 3: Triángulo (Amarillo)
                levelBallColor = Color.YELLOW;
                levelWallColor = Color.parseColor("#9C27B0"); // Púrpura
                // Laberinto Triángulo - Diseño Complejo (Ejemplo)
                levelWalls.add(new Wall(500, 100, 500 + WALL_THICKNESS, 1600));
                levelWalls.add(new Wall(10, 800, 400, 800 + WALL_THICKNESS));
                levelWalls.add(new Wall(600, 800, 990, 800 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 100, 900, 100 + WALL_THICKNESS));
                levelWalls.add(new Wall(100, 100, 100 + WALL_THICKNESS, 1600));
                levelWalls.add(new Wall(900, 100, 900 + WALL_THICKNESS, 1600));
                levelWalls.add(new Wall(100, 1600, 900, 1600 + WALL_THICKNESS));

                // Bloques en el centro
                levelWalls.add(new Wall(200, 200, 300, 300));
                levelWalls.add(new Wall(700, 200, 800, 300));
                levelWalls.add(new Wall(200, 1400, 300, 1500));
                levelWalls.add(new Wall(700, 1400, 800, 1500));
                levelWalls.add(new Wall(400, 400, 600, 600));
                levelWalls.add(new Wall(400, 1000, 600, 1200));
                break;
            default:
                // Si el nivel es mayor al total, vuelve al nivel 1.
                return getLevelData(1);
        }
        return new LevelData(levelWalls, levelWallColor, levelBallColor, levelExitRect);
    }

    // Configura los muros, colores y la salida para el nivel actual.
    private void setupWalls() {
        LevelData data = getLevelData(currentLevel);
        walls = data.walls;
        exitRect = data.exitRect;
        wallColor = data.wallColor;
        ballColor = data.ballColor;

        ballX = START_X; // Reinicia la posición de la bola
        ballY = START_Y;

        paintWalls.setColor(wallColor);
        paintBall.setColor(ballColor);
        backgroundColor = DEFAULT_BACKGROUND_COLOR; // Se restaura (o lo cambia el sensor de luz)

        gameState = GameState.RUNNING;
        invalidate();
    }

    private boolean checkWallCollision(float newX, float newY) {
        // Aplica el offset de centralización a la posición de la bola para la colisión
        RectF ballRect = new RectF(
                newX - radius + boardOffsetX,
                newY - radius + boardOffsetY,
                newX + radius + boardOffsetX,
                newY + radius + boardOffsetY
        );
        for (Wall wall : walls) {
            if (RectF.intersects(ballRect, wall.rect)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkExitReached(float newX, float newY) {
        // Aplica el offset de centralización a la posición de la bola para la colisión
        RectF ballRect = new RectF(
                newX - radius + boardOffsetX,
                newY - radius + boardOffsetY,
                newX + radius + boardOffsetX,
                newY + radius + boardOffsetY
        );
        return RectF.intersects(ballRect, exitRect);
    }

    // Reinicia el juego, al nivel 1 si se pierde, o al siguiente si se gana.
    private void resetGame() {
        if (gameState == GameState.WON && currentLevel < TOTAL_LEVELS) {
            currentLevel++;
        } else {
            currentLevel = 1; // Reinicia al nivel 1 si ganó el último o perdió
        }

        setupWalls();
        gameState = GameState.RUNNING;
        resumeSensors();
        invalidate();
    }

    // Nuevo método para dibujar la bola según el nivel
    private void drawBall(Canvas canvas, float x, float y) {
        switch (currentLevel) {
            case 1: // Círculo
                canvas.drawCircle(x, y, radius, paintBall);
                break;
            case 2: // Cuadrado
                canvas.drawRect(x - radius, y - radius, x + radius, y + radius, paintBall);
                break;
            case 3: // Triángulo (equilátero, punta hacia arriba)
                trianglePath.reset();
                trianglePath.moveTo(x, y - radius); // Punta superior
                trianglePath.lineTo(x + (float)(radius * Math.sqrt(3)), y + radius / 2); // Esquina inferior derecha
                trianglePath.lineTo(x - (float)(radius * Math.sqrt(3)), y + radius / 2); // Esquina inferior izquierda
                trianglePath.close();
                canvas.drawPath(trianglePath, paintBall);
                break;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        maxX = getWidth();
        maxY = getHeight();

        // 1. Calcular Desplazamiento para Centralizar el Tablero
        boardOffsetX = (maxX - MAX_BOARD_X) / 2;
        boardOffsetY = (maxY - MAX_BOARD_Y) / 2;

        canvas.drawColor(backgroundColor);

        // 2. Aplicar Desplazamiento a Todo el Dibujo del Laberinto
        canvas.save();
        canvas.translate(boardOffsetX, boardOffsetY);

        for (Wall wall : walls) {
            canvas.drawRect(wall.rect, paintWalls);
        }

        if (gameState != GameState.WON || currentLevel < TOTAL_LEVELS) {
            // Dibuja la salida, si no es el último nivel completado
            canvas.drawRoundRect(exitRect, 10, 10, paintExit);
        }

        // Dibuja el nivel actual
        paintText.setTextSize(50); // Tamaño más pequeño para el texto de nivel
        paintText.setColor(Color.WHITE);
        canvas.drawText("Nivel: " + currentLevel, MAX_BOARD_X / 2, 50, paintText);

        if (gameState == GameState.RUNNING || gameState == GameState.PAUSED || gameState == GameState.LOST) {
            // Dibuja la bola en sus coordenadas **sin** el offset, ya que el Canvas ya está trasladado
            drawBall(canvas, ballX, ballY);
        }

        canvas.restore(); // Restaura el Canvas a la posición original (sin offset)


        // 3. Dibujar Mensajes Centrados en la Pantalla Completa
        String message = null;
        if (gameState == GameState.PAUSED) {
            message = "PAUSADO (Cubre Proximidad para Reanudar)";
            paintText.setColor(Color.YELLOW);
            paintText.setTextSize(60);
        } else if (gameState == GameState.WON) {
            if (currentLevel == TOTAL_LEVELS) {
                message = "¡FELICITACIONES, GANASTE EL JUEGO!";
                paintText.setColor(Color.CYAN);
                paintText.setTextSize(70); // Pequeño para que quepa bien
            } else {
                message = "¡NIVEL " + currentLevel + " COMPLETADO! Toca para el Nivel " + (currentLevel + 1);
                paintText.setColor(Color.GREEN);
                paintText.setTextSize(60);
            }
            pauseSensors();
        } else if (gameState == GameState.LOST) {
            message = "¡GAME OVER! Toca para reiniciar al Nivel 1";
            paintText.setColor(Color.RED);
            paintText.setTextSize(60);
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

            // La verificación de colisión ahora debe usar la posición **sin** offset
            if (checkExitReached(nextX, nextY)) {
                gameState = GameState.WON;
            }
            // Verifica la colisión de la bola con la pared, incluyendo el offset del tablero
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

            // Mantenemos la bola dentro de los límites del laberinto (sin el offset)
            if (ballX < radius) ballX = radius;
            if (ballY < radius) ballY = radius;
            if (ballX > MAX_BOARD_X - radius) ballX = MAX_BOARD_X - radius;
            if (ballY > MAX_BOARD_Y - radius) ballY = MAX_BOARD_Y - radius;
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