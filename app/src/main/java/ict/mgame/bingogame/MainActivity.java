package ict.mgame.bingogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private TextView drawnNumberText, usernameDisplay, scoreDisplay, coinsDisplay, resetsRemainingDisplay, coinTimerDisplay;

    // Buttons
    private Button drawButton, settingsButton, restartButton;

    // Database and user data
    private DatabaseHelper dbHelper;
    private String username;
    private int coins, dailyResets;
    private String lastResetDate;

    // Timer for coins
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long lastAddTime;
    private static final long COIN_INTERVAL_MS = 30000; // 30 seconds

    // Game handler
    private BingoGame bingoGame;

    /**
     * Called when the activity is first created. Sets up the UI, loads user data, and initializes the game state.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        initializeViews();
        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        username = prefs.getString("username", "Guest");
        usernameDisplay.setText("Welcome, " + username + "!");

        bingoGame = new BingoGame(this, findViewById(R.id.bingo_grid), drawnNumberText);

        if (!username.equals("Guest")) {
            loadUserData();
            loadOrInitializeGameState();
        } else {
            setupGuestMode();
        }

        setupButtons();
    }

    /**
     * Initializes all UI views by finding them via their IDs.
     */
    private void initializeViews() {
        drawnNumberText = findViewById(R.id.drawn_number);
        usernameDisplay = findViewById(R.id.username_display);
        scoreDisplay = findViewById(R.id.score_display);
        coinsDisplay = findViewById(R.id.coins_display);
        resetsRemainingDisplay = findViewById(R.id.resets_remaining);
        coinTimerDisplay = findViewById(R.id.coin_timer);
        drawButton = findViewById(R.id.draw_button);
        settingsButton = findViewById(R.id.settings_button);
        restartButton = findViewById(R.id.restart_button);
    }

    /**
     * Loads user-specific data from the database, such as wins and coins, and updates the UI displays.
     */
    private void loadUserData() {
        int wins = dbHelper.getWins(username);
        scoreDisplay.setText("Wins: " + wins);
        coins = dbHelper.getCoins(username);
        coinsDisplay.setText("Coins: " + coins);
        updateResetInfo();
    }

    /**
     * Loads the saved game state if available; otherwise, initializes a new game state and saves it.
     */
    private void loadOrInitializeGameState() {
        DatabaseHelper.GameState state = dbHelper.getGameState(username);
        if (state != null) {
            bingoGame.loadBingoCard(state.card, state.marked);
            if (!state.drawnNumbers.isEmpty()) {
                drawnNumberText.setText("Drawn: " + state.drawnNumbers.get(state.drawnNumbers.size() - 1));
            }
            if (bingoGame.checkForBingo()) {
                drawButton.setEnabled(false);
            }
        } else {
            bingoGame.initializeBingoCard();
            saveGameState();
        }
    }

    /**
     * Configures the UI for guest mode, setting default values and initializing a new bingo card.
     */
    private void setupGuestMode() {
        scoreDisplay.setText("Wins: 0");
        coins = 0;
        coinsDisplay.setText("Coins: 0");
        resetsRemainingDisplay.setText("Resets left: Unlimited (Guest)");
        coinTimerDisplay.setText("");
        bingoGame.initializeBingoCard();
    }

    /**
     * Sets up click listeners for all buttons in the activity.
     */
    private void setupButtons() {
        drawButton.setOnClickListener(v -> drawNumber());

        settingsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        restartButton.setOnClickListener(v -> restartGame());
    }

    /**
     * Called when the activity is becoming visible to the user. Starts the coin timer if not in guest mode.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!username.equals("Guest")) {
            startCoinTimer();
        }
    }

    /**
     * Called when the activity is no longer visible to the user. Stops the coin timer and saves game state if not in guest mode.
     */
    @Override
    protected void onPause() {
        super.onPause();
        stopCoinTimer();
        if (!username.equals("Guest")) {
            saveGameState();
        }
    }

    /**
     * Handles the draw number action, including coin deduction, number generation, marking the card, and checking for bingo.
     */
    private void drawNumber() {
        if (username.equals("Guest") || coins < 1) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bingoGame.getDrawnNumbers().size() >= 75) {
            Toast.makeText(this, "All numbers drawn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deduct coin
        coins--;
        dbHelper.updateCoins(username, coins);
        coinsDisplay.setText("Coins: " + coins);

        bingoGame.performDraw(coins, dbHelper, username, coinsDisplay);

        saveGameState();

        if (bingoGame.checkForBingo()) {
            Toast.makeText(this, "BINGO! You win!", Toast.LENGTH_LONG).show();
            drawButton.setEnabled(false);

            if (!username.equals("Guest")) {
                dbHelper.incrementWins(username);
                int newWins = dbHelper.getWins(username);
                scoreDisplay.setText("Wins: " + newWins);
                addCoin(50);
            }
            saveGameState();
        }
    }

    /**
     * Starts the timer for passively adding coins every interval.
     */
    private void startCoinTimer() {
        lastAddTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long elapsedSinceLastAdd = now - lastAddTime;
                if (elapsedSinceLastAdd >= COIN_INTERVAL_MS) {
                    addCoin(1);
                    lastAddTime = now;
                }
                long remaining = COIN_INTERVAL_MS - (elapsedSinceLastAdd % COIN_INTERVAL_MS);
                coinTimerDisplay.setText("Next coin in: " + (remaining / 1000) + "s");
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Stops the coin addition timer.
     */
    private void stopCoinTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * Adds a specified amount of coins to the user's total, updates the database, and refreshes the UI.
     *
     * @param amount The number of coins to add.
     */
    private void addCoin(int amount) {
        coins += amount;
        dbHelper.updateCoins(username, coins);
        coinsDisplay.setText("Coins: " + coins);
        Toast.makeText(this, "+" + amount + " coin!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the daily reset information based on the current date and database values.
     */
    private void updateResetInfo() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        lastResetDate = dbHelper.getLastResetDate(username);
        if (!currentDate.equals(lastResetDate)) {
            dailyResets = 0;
            dbHelper.updateDailyResets(username, 0, currentDate);
        } else {
            dailyResets = dbHelper.getDailyResets(username);
        }
        resetsRemainingDisplay.setText("Resets left: " + (5 - dailyResets));
    }

    /**
     * Handles the restart game action, checking daily limits and performing the restart if allowed.
     */
    private void restartGame() {
        if (username.equals("Guest")) {
            bingoGame.performRestart();
            return;
        }
        updateResetInfo();
        if (dailyResets >= 5) {
            Toast.makeText(this, "No more resets today!", Toast.LENGTH_SHORT).show();
            return;
        }
        dailyResets++;
        dbHelper.updateDailyResets(username, dailyResets, lastResetDate);
        resetsRemainingDisplay.setText("Resets left: " + (5 - dailyResets));
        bingoGame.performRestart();
        saveGameState();
        Toast.makeText(this, "Game restarted!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Saves the current game state to the database if not in guest mode.
     */
    private void saveGameState() {
        if (username.equals("Guest")) return;
        dbHelper.updateGameState(username, bingoGame.getCard(), bingoGame.getDrawnNumbers(), bingoGame.getMarked());
    }
}