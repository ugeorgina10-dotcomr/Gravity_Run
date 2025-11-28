package com.example.gravityrun;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
        LEVEL_SELECT,
        RUNNING,
        PAUSED,
        WON,
        LOST
    }
    private GameState gameState = GameState.LEVEL_SELECT;

    private enum PlayerShape {
        CIRCLE, SQUARE, TRIANGLE
    }
    private PlayerShape playerShape = PlayerShape.CIRCLE;

    private int currentLevel = 0; // 0 significa en el menú de selección

    private final SensorManager sensorManager;
    private final Sensor accelerometer, proximity, lightSensor;

    // *** VARIABLES PARA EL CAMBIO CONSTANTE (NIVEL 3) ***
    private long frameCount = 0; // Contador de frames para la forma cambiante
    private final long SHAPE_CHANGE_INTERVAL = 30; // Cambia la forma cada 30 actualizaciones del sensor
    // *************************************************

    // --- VARIABLES PARA ESCALADO Y CENTRADO ---
    private final float DESIGN_WIDTH = 1010f;
    private final float DESIGN_HEIGHT = 1710f;

    private float scaleFactor = 1.0f;
    private float offsetX = 0;
    private float offsetY = 0;
    // ------------------------------------------

    // Coordenadas y tamaños originales que se escalarán
    private final float ORIGINAL_START_X = 60;
    private final float ORIGINAL_START_Y = 60;
    private final float ORIGINAL_RADIUS = 30;
    private final float ORIGINAL_SPEED = 2.5f;

    private float ballX, ballY;
    private float radius;
    private float movementSpeed;

    private static class Wall {
        RectF rect;
        public Wall(float left, float top, float right, float bottom) {
            this.rect = new RectF(left, top, right, bottom);
        }
    }
    private List<Wall> walls = new ArrayList<>();

    private RectF exitRect;
    private RectF[] levelButtons;

    private float maxX, maxY;

    // Colores por defecto para evitar NullPointer
    private int backgroundColor = Color.BLACK;
    private final int DEFAULT_WALL_COLOR = Color.DKGRAY;

    private final Paint paintBall, paintText, paintWalls, paintExit, paintMenu;

    public GameView(Context context) {
        super(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        paintBall = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBall.setColor(Color.RED);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintWalls = new Paint();
        paintExit = new Paint();
        paintMenu = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintExit.setColor(Color.GREEN);
        paintExit.setStyle(Paint.Style.FILL);
        paintExit.setShadowLayer(10.0f, 0.0f, 0.0f, Color.WHITE);

        resumeSensors();
    }

    // --- Lógica de Escalado y Centrado ---
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        maxX = w;
        maxY = h;

        float scaleX = w / DESIGN_WIDTH;
        float scaleY = h / DESIGN_HEIGHT;
        scaleFactor = Math.min(scaleX, scaleY);

        float finalWidth = DESIGN_WIDTH * scaleFactor;
        float finalHeight = DESIGN_HEIGHT * scaleFactor;
        offsetX = (w - finalWidth) / 2;
        offsetY = (h - finalHeight) / 2;

        radius = ORIGINAL_RADIUS * scaleFactor;
        movementSpeed = ORIGINAL_SPEED * scaleFactor;
        paintText.setTextSize(60 * scaleFactor);

        setupLevelSelectionMenu();

        if (gameState == GameState.LEVEL_SELECT) {
            loadLevel(0);
        }
    }

    private void setupLevelSelectionMenu() {
        levelButtons = new RectF[3];
        float buttonHeight = maxY / 6;
        float buttonWidth = maxX * 0.7f;
        float margin = maxY / 15;
        float startY = maxY / 3;

        for (int i = 0; i < 3; i++) {
            float top = startY + (i * (buttonHeight + margin));
            float bottom = top + buttonHeight;
            float left = (maxX - buttonWidth) / 2;
            float right = left + buttonWidth;
            levelButtons[i] = new RectF(left, top, right, bottom);
        }
    }

    // Ayudante para añadir paredes que aplica la escala y el offset de centrado
    private void addScaledWall(float l, float t, float r, float b) {
        walls.add(new Wall(
                (l * scaleFactor) + offsetX,
                (t * scaleFactor) + offsetY,
                (r * scaleFactor) + offsetX,
                (b * scaleFactor) + offsetY
        ));
    }

    private void loadLevel(int level) {
        currentLevel = level;
        walls.clear();
        paintWalls.setColor(DEFAULT_WALL_COLOR);

        // Reinicia el contador de frames al cargar un nivel
        frameCount = 0;

        // Posición inicial escalada y centrada
        ballX = (ORIGINAL_START_X * scaleFactor) + offsetX;
        ballY = (ORIGINAL_START_Y * scaleFactor) + offsetY;

        // Definiciones originales
        final float BORDER_WIDTH = 10;
        final float MAX_X_DESIGN = 1000 + BORDER_WIDTH;
        final float MAX_Y_DESIGN = 1700 + BORDER_WIDTH;
        final float WALL_THICKNESS = 20;

        // Variables de la meta (temporales, redefinidas en cada case)
        float exitLeft = 450;
        float exitRight = 650;
        float exitTop = 1700;
        float exitBottom = MAX_Y_DESIGN;

        // --- Configuración por Nivel ---
        switch (level) {
            case 1: // Nivel 1: Círculo Rojo (Original)
                backgroundColor = Color.parseColor("#A0522D"); // Marrón
                paintBall.setColor(Color.RED);
                playerShape = PlayerShape.CIRCLE;
                paintExit.setColor(Color.GREEN);

                // Laberinto Original
                addScaledWall(10, 100, 300, 100 + WALL_THICKNESS);
                addScaledWall(400, 100, 1000, 100 + WALL_THICKNESS);
                addScaledWall(100, 100, 100 + WALL_THICKNESS, 400);
                addScaledWall(100, 400, 500, 400 + WALL_THICKNESS);
                addScaledWall(600, 400, 1000, 400 + WALL_THICKNESS);
                addScaledWall(300, 500, 300 + WALL_THICKNESS, 800);
                addScaledWall(400, 600, 900, 600 + WALL_THICKNESS);
                addScaledWall(600, 200, 600 + WALL_THICKNESS, 600);
                addScaledWall(10, 800, 700, 800 + WALL_THICKNESS);
                addScaledWall(100, 900, 100 + WALL_THICKNESS, 1400);
                addScaledWall(200, 1000, 800, 1000 + WALL_THICKNESS);
                addScaledWall(500, 900, 500 + WALL_THICKNESS, 1200);
                addScaledWall(10, 1300, 400, 1300 + WALL_THICKNESS);
                addScaledWall(900, 1300, 900 + WALL_THICKNESS, 1600);
                break;

            case 2: // Nivel 2: Cuadrado Azul - Laberinto Espiral (Bordes inferiores cerrados)
                backgroundColor = Color.parseColor("#4682B4");
                paintBall.setColor(Color.BLUE);
                playerShape = PlayerShape.SQUARE;
                paintExit.setColor(Color.YELLOW);

                // Reubicar la meta al centro del laberinto (cerca de 500, 850)
                exitLeft = 400;
                exitRight = 600;
                exitTop = 800;
                exitBottom = 900;

                // Laberinto con más anillos de espiral concéntrica.
                // Anillo exterior (Grande, casi el borde)
                addScaledWall(100, 100, 900, 100 + WALL_THICKNESS);
                addScaledWall(900 - WALL_THICKNESS, 100 + WALL_THICKNESS, 900, 1500);
                addScaledWall(100, 1500 - WALL_THICKNESS, 900 - WALL_THICKNESS, 1500);
                addScaledWall(100, 200, 100 + WALL_THICKNESS, 1500 - WALL_THICKNESS);

                // Segundo anillo
                addScaledWall(200, 200, 800, 200 + WALL_THICKNESS);
                addScaledWall(800 - WALL_THICKNESS, 200 + WALL_THICKNESS, 800, 1400);
                addScaledWall(200, 1400 - WALL_THICKNESS, 800 - WALL_THICKNESS, 1400);
                addScaledWall(200, 300, 200 + WALL_THICKNESS, 1400 - WALL_THICKNESS);

                // Tercer anillo
                addScaledWall(300, 300, 700, 300 + WALL_THICKNESS);
                addScaledWall(700 - WALL_THICKNESS, 300 + WALL_THICKNESS, 700, 1300);
                addScaledWall(300, 1300 - WALL_THICKNESS, 700 - WALL_THICKNESS, 1300);
                addScaledWall(300, 400, 300 + WALL_THICKNESS, 1300 - WALL_THICKNESS);

                // Cuarto anillo (El que contiene el centro)
                addScaledWall(400, 400, 600, 400 + WALL_THICKNESS);
                addScaledWall(600 - WALL_THICKNESS, 400 + WALL_THICKNESS, 600, 1200);
                addScaledWall(400, 1200 - WALL_THICKNESS, 600 - WALL_THICKNESS, 1200);
                addScaledWall(400, 500, 400 + WALL_THICKNESS, 1200 - WALL_THICKNESS);

                // Muro extra para cerrarlo un poco más y que la salida esté clara.
                addScaledWall(500 - WALL_THICKNESS/2, 500, 500 + WALL_THICKNESS/2, 800);
                addScaledWall(500 - WALL_THICKNESS/2, 900, 500 + WALL_THICKNESS/2, 1100);
                break;

            case 3: // Nivel 3: Círculo/Cuadrado Cambiante
                backgroundColor = Color.parseColor("#4B0082"); // Índigo Oscuro
                paintBall.setColor(Color.MAGENTA);
                playerShape = PlayerShape.CIRCLE;
                paintExit.setColor(Color.RED);

                // La salida en el borde inferior.
                exitLeft = 450;
                exitRight = 650;
                exitTop = 1700;
                exitBottom = MAX_Y_DESIGN;

                // Laberinto similar al Nivel 1
                final float THIN_WALL = WALL_THICKNESS;

                // Fila 1 (por debajo del inicio)
                addScaledWall(10, 200, 300, 200 + THIN_WALL);
                addScaledWall(500, 200, 1000, 200 + THIN_WALL);

                // Pared vertical 1
                addScaledWall(400, 200 + THIN_WALL, 400 + THIN_WALL, 600);

                // Fila 2
                addScaledWall(100, 400, 400, 400 + THIN_WALL);
                addScaledWall(500, 400, 900, 400 + THIN_WALL);

                // Pared vertical 2
                addScaledWall(600, 400 + THIN_WALL, 600 + THIN_WALL, 800);

                // Fila 3
                addScaledWall(10, 600, 500, 600 + THIN_WALL);
                addScaledWall(700, 600, 1000, 600 + THIN_WALL);

                // Pared vertical 3
                addScaledWall(200, 600 + THIN_WALL, 200 + THIN_WALL, 1000);

                // Fila 4
                addScaledWall(300, 800, 700, 800 + THIN_WALL);
                addScaledWall(800, 800, 1000, 800 + THIN_WALL);

                // Pared vertical 4
                addScaledWall(800, 800 + THIN_WALL, 800 + THIN_WALL, 1200);

                // Fila 5
                addScaledWall(10, 1000, 200, 1000 + THIN_WALL);
                addScaledWall(300, 1000, 700, 1000 + THIN_WALL);
                addScaledWall(900, 1000, 1000, 1000 + THIN_WALL);

                // Pared vertical 5
                addScaledWall(500, 1000 + THIN_WALL, 500 + THIN_WALL, 1400);

                // Fila 6 (Cerca del fondo)
                addScaledWall(10, 1400, 400, 1400 + THIN_WALL);
                addScaledWall(600, 1400, 1000, 1400 + THIN_WALL);

                // Muros para forzar la salida (450-650)
                addScaledWall(700, 1500, 700 + THIN_WALL, 1700);
                addScaledWall(300, 1500, 300 + THIN_WALL, 1700);

                // Muro en la parte inferior para forzar la salida
                addScaledWall(700 + THIN_WALL, 1600, 1000, 1600 + THIN_WALL);
                addScaledWall(10, 1600, 300, 1600 + THIN_WALL);
                break;

            case 0: // Menú de Selección
                backgroundColor = Color.BLACK;
                paintBall.setColor(Color.GRAY);
                playerShape = PlayerShape.CIRCLE;
                walls.clear();
                exitRect = null;
                return;
        }

        // Crear Rectángulo de salida escalado
        exitRect = new RectF(
                (exitLeft * scaleFactor) + offsetX,
                (exitTop * scaleFactor) + offsetY,
                (exitRight * scaleFactor) + offsetX,
                (exitBottom * scaleFactor) + offsetY
        );

        // --- MUROS FIJOS (BORDES) ---
        addScaledWall(0, 0, BORDER_WIDTH, MAX_Y_DESIGN); // Izquierdo
        addScaledWall(1000, 0, MAX_X_DESIGN, MAX_Y_DESIGN); // Derecho
        addScaledWall(0, 0, MAX_X_DESIGN, BORDER_WIDTH); // Superior

        // Lógica del borde inferior (posición fija 1700/MAX_Y_DESIGN):
        final float BOTTOM_EDGE_Y_START = 1700;
        final float BOTTOM_EDGE_Y_END = MAX_Y_DESIGN;

        if (level == 1 || level == 2) {
            // Cierra el borde inferior por completo para los niveles con meta inferior o meta central.
            addScaledWall(0, BOTTOM_EDGE_Y_START, MAX_X_DESIGN, BOTTOM_EDGE_Y_END);
        } else if (level == 3) {
            // Nivel 3: Deja el hueco de salida en el centro inferior
            addScaledWall(0, BOTTOM_EDGE_Y_START, exitLeft, BOTTOM_EDGE_Y_END);
            addScaledWall(exitRight, BOTTOM_EDGE_Y_START, MAX_X_DESIGN, BOTTOM_EDGE_Y_END);
        }

        gameState = GameState.RUNNING;
        resumeSensors();
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
        if (exitRect == null) return false;
        RectF ballRect = new RectF(
                newX - radius,
                newY - radius,
                newX + radius,
                newY + radius
        );
        return RectF.intersects(ballRect, exitRect);
    }

    private void resetGame() {
        gameState = GameState.LEVEL_SELECT;
        loadLevel(0);
        invalidate();
    }

    private void drawPlayer(Canvas canvas) {
        switch (playerShape) {
            case SQUARE:
                float size = radius * 1.5f;
                canvas.drawRect(ballX - size / 2, ballY - size / 2, ballX + size / 2, ballY + size / 2, paintBall);
                break;
            case TRIANGLE:
                // Dibuja un triángulo equilátero
                Path trianglePath = new Path();
                trianglePath.moveTo(ballX, ballY - radius);
                trianglePath.lineTo(ballX - (float) (radius * Math.sqrt(3) / 2), ballY + radius / 2);
                trianglePath.lineTo(ballX + (float) (radius * Math.sqrt(3) / 2), ballY + radius / 2);
                trianglePath.close();
                canvas.drawPath(trianglePath, paintBall);
                break;
            case CIRCLE:
            default:
                canvas.drawCircle(ballX, ballY, radius, paintBall);
                break;
        }
    }

    private void drawLevelSelectMenu(Canvas canvas) {
        paintText.setColor(Color.WHITE);
        canvas.drawText("GRAVITY RUN", maxX / 2, maxY / 7, paintText);
        paintText.setTextSize(40 * scaleFactor);
        canvas.drawText("Elige tu Nivel", maxX / 2, maxY / 4, paintText);

        paintText.setTextSize(50 * scaleFactor);
        for (int i = 0; i < levelButtons.length; i++) {
            paintMenu.setColor(Color.parseColor("#282828"));
            paintMenu.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(levelButtons[i], 20, 20, paintMenu);

            paintMenu.setColor(Color.parseColor("#444444"));
            paintMenu.setStyle(Paint.Style.STROKE);
            paintMenu.setStrokeWidth(5);
            canvas.drawRoundRect(levelButtons[i], 20, 20, paintMenu);

            paintText.setColor(Color.WHITE);
            canvas.drawText("NIVEL " + (i + 1), levelButtons[i].centerX(), levelButtons[i].centerY() + (paintText.getTextSize() / 3), paintText);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(backgroundColor);

        if (gameState == GameState.LEVEL_SELECT) {
            drawLevelSelectMenu(canvas);
            return;
        }

        if (walls.isEmpty() || exitRect == null) {
            return;
        }

        for (Wall wall : walls) {
            canvas.drawRect(wall.rect, paintWalls);
        }

        if (exitRect != null) {
            canvas.drawRoundRect(exitRect, 10 * scaleFactor, 10 * scaleFactor, paintExit);
        }

        if (gameState == GameState.RUNNING || gameState == GameState.PAUSED || gameState == GameState.LOST) {
            drawPlayer(canvas);
        }

        String message = null;
        if (gameState == GameState.PAUSED) {
            message = "PAUSADO";
            paintText.setColor(Color.YELLOW);
        } else if (gameState == GameState.WON) {
            message = "¡GANASTE EL NIVEL " + currentLevel + "! Toca para continuar.";
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
                return true;
            } else if (gameState == GameState.LEVEL_SELECT) {
                for (int i = 0; i < levelButtons.length; i++) {
                    if (levelButtons[i].contains(event.getX(), event.getY())) {
                        loadLevel(i + 1);
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (gameState != GameState.RUNNING) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if (event.values[0] < event.sensor.getMaximumRange() && gameState == GameState.RUNNING) {
                    gameState = GameState.PAUSED;
                } else if (event.values[0] >= event.sensor.getMaximumRange() && gameState == GameState.PAUSED) {
                    gameState = GameState.RUNNING;
                }
            }
            invalidate();
            return;
        }

        if (walls.isEmpty()) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float xMovement = -event.values[0] * movementSpeed;
            float yMovement = event.values[1] * movementSpeed;

            float nextX = ballX + xMovement;
            float nextY = ballY + yMovement;

            // *** Lógica de Cambio de Forma Constante (NIVEL 3) ***
            if (currentLevel == 3) {
                frameCount++;
                if (frameCount % SHAPE_CHANGE_INTERVAL == 0) {
                    // Alterna entre Círculo y Cuadrado
                    if (playerShape == PlayerShape.CIRCLE) {
                        playerShape = PlayerShape.SQUARE;
                    } else {
                        playerShape = PlayerShape.CIRCLE;
                    }
                }
            }
            // *******************************************************

            if (checkExitReached(nextX, nextY)) {
                gameState = GameState.WON;
            }
            else if (checkWallCollision(nextX, nextY)) {
                // Colisión suave: permite deslizarse a lo largo de las paredes
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

            // LÍMITES AJUSTADOS (choca con el borde del laberinto)
            float mazeLeft = offsetX;
            float mazeRight = offsetX + (DESIGN_WIDTH * scaleFactor);
            float mazeTop = offsetY;

            if (ballX < mazeLeft + radius) ballX = mazeLeft + radius;
            if (ballX > mazeRight - radius) ballX = mazeRight - radius;
            if (ballY < mazeTop + radius) ballY = mazeTop + radius;

        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightValue = event.values[0];
            int defaultColor = (currentLevel == 1) ? Color.parseColor("#A0522D") :
                    (currentLevel == 2) ? Color.parseColor("#4682B4") :
                            Color.parseColor("#4B0082");
            backgroundColor = (lightValue < 10) ? Color.DKGRAY : defaultColor;
        }

        if (gameState == GameState.RUNNING || gameState == GameState.PAUSED) {
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