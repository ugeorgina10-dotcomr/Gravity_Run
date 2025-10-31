package com.example.gravityrun;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;



public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crea la vista del juego y la muestra en pantalla
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resumeSensors(); // reanuda los sensores
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pauseSensors(); // pausa los sensores
        }
    }
}
