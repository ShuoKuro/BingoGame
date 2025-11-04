package ict.mgame.bingogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private GridLayout bingoGrid;
    private TextView drawnNumberText, usernameDisplay, scoreDisplay, coinsDisplay, resetsRemainingDisplay, coinTimerDisplay;

    // Buttons
    private Button drawButton, settingsButton, restartButton;

    // Game state
    private List<Integer> drawnNumbers = new ArrayList<>();
    private Set<Integer> cardNumbers = new HashSet<>();
    private TextView[][] cells = new TextView[5][5];
    private boolean[][] marked = new boolean[5][5];
    private Random random = new Random();

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

        if (!username.equals("Guest")) {
            loadUserData();
            loadOrInitializeGameState();
        } else {
            setupGuestMode();
        }

        setupButtons();
    }

    private void initializeViews() {
        bingoGrid = findViewById(R.id.bingo_grid);
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

    private void loadUserData() {
        int wins = dbHelper.getWins(username);
        scoreDisplay.setText("Wins: " + wins);
        coins = dbHelper.getCoins(username);
        coinsDisplay.setText("Coins: " + coins);
        updateResetInfo();
    }

    private void loadOrInitializeGameState() {
        DatabaseHelper.GameState state = dbHelper.getGameState(username);
        if (state != null) {
            loadBingoCard(state.card, state.marked);
            drawnNumbers = state.drawnNumbers;
            if (!drawnNumbers.isEmpty()) {
                drawnNumberText.setText("Drawn: " + drawnNumbers.get(drawnNumbers.size() - 1));
            }
            if (checkForBingo()) {
                drawButton.setEnabled(false);
            }
        } else {
            initializeBingoCard();
            saveGameState();
        }
    }

    private void setupGuestMode() {
        scoreDisplay.setText("Wins: 0");
        coins = 0;
        coinsDisplay.setText("Coins: 0");
        resetsRemainingDisplay.setText("Resets left: Unlimited (Guest)");
        coinTimerDisplay.setText("");
        initializeBingoCard();
    }

    private void setupButtons() {
        setupDrawButton();

        settingsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        restartButton.setOnClickListener(v -> restartGame());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!username.equals("Guest")) {
            startCoinTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCoinTimer();
        if (!username.equals("Guest")) {
            saveGameState();
        }
    }

    private void loadBingoCard(int[][] card, boolean[][] marked) {
        bingoGrid.removeAllViews();
        cardNumbers.clear();
        setupCardUI(card, marked);
        this.marked = marked;
    }

    private void initializeBingoCard() {
        int[][] card = generateCard();
        bingoGrid.removeAllViews();
        setupCardUI(card, marked); // marked is already reset
    }

    private int[][] generateCard() {
        int[][] card = new int[5][5];
        for (int col = 0; col < 5; col++) {
            Set<Integer> columnNumbers = new HashSet<>();
            int min = col * 15 + 1;
            int max = min + 14;
            while (columnNumbers.size() < 5) {
                int num = random.nextInt(15) + min;
                if (!columnNumbers.contains(num)) {
                    columnNumbers.add(num);
                }
            }
            List<Integer> sorted = new ArrayList<>(columnNumbers);
            java.util.Collections.sort(sorted);
            for (int row = 0; row < 5; row++) {
                card[row][col] = (row == 2 && col == 2) ? 0 : sorted.get(row);
            }
        }
        return card;
    }

    private void setupCardUI(int[][] card, boolean[][] marked) {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TextView cell = new TextView(this);
                int num = card[row][col];
                cell.setText(num == 0 ? "FREE" : String.valueOf(num));
                cell.setTextSize(20);
                cell.setPadding(16, 16, 16, 16);
                cell.setTextColor(0xFF000000);
                cell.setGravity(Gravity.CENTER);
                cell.setBackgroundColor(marked[row][col] ? 0xFF00FF00 : 0xFFFFFFFF);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(col, 1f);
                params.rowSpec = GridLayout.spec(row, 1f);
                bingoGrid.addView(cell, params);
                cells[row][col] = cell;
                if (num != 0) {
                    cardNumbers.add(num);
                } else if (num == 0) {
                    marked[2][2] = true;
                }
            }
        }
    }

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

    private void stopCoinTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void addCoin(int amount) {
        coins += amount;
        dbHelper.updateCoins(username, coins);
        coinsDisplay.setText("Coins: " + coins);
        Toast.makeText(this, "+" + amount + " coin!", Toast.LENGTH_SHORT).show();
    }

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

    private void restartGame() {
        if (username.equals("Guest")) {
            performRestart();
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
        performRestart();
    }

    private void performRestart() {
        drawnNumbers.clear();
        cardNumbers.clear();
        marked = new boolean[5][5];
        drawnNumberText.setText("Drawn Number");
        drawButton.setEnabled(true);
        initializeBingoCard();
        saveGameState();
        Toast.makeText(this, "Game restarted!", Toast.LENGTH_SHORT).show();
    }

    private void saveGameState() {
        if (username.equals("Guest")) return;
        int[][] card = new int[5][5];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                String text = cells[row][col].getText().toString();
                card[row][col] = "FREE".equals(text) ? 0 : Integer.parseInt(text);
            }
        }
        dbHelper.updateGameState(username, card, drawnNumbers, marked);
    }

    private void setupDrawButton() {
        drawButton.setOnClickListener(v -> {
            if (username.equals("Guest") || coins < 1) {
                Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (drawnNumbers.size() >= 75) {
                Toast.makeText(this, "All numbers drawn!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Deduct coin
            coins--;
            dbHelper.updateCoins(username, coins);
            coinsDisplay.setText("Coins: " + coins);

            int newNumber;
            do {
                newNumber = random.nextInt(75) + 1;
            } while (drawnNumbers.contains(newNumber));

            drawnNumbers.add(newNumber);
            drawnNumberText.setText("Drawn: " + newNumber);

            // Mark if on card
            boolean found = false;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    if (cells[row][col].getText().toString().equals(String.valueOf(newNumber))) {
                        marked[row][col] = true;
                        cells[row][col].setBackgroundColor(0xFF00FF00);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            saveGameState();

            // Check for Bingo
            if (checkForBingo()) {
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
        });
    }

    private boolean checkForBingo() {
        // Rows
        for (int row = 0; row < 5; row++) {
            if (isLineMarked(row, -1, true)) return true;
        }
        // Columns
        for (int col = 0; col < 5; col++) {
            if (isLineMarked(-1, col, false)) return true;
        }
        // Diagonals
        boolean diag1 = true, diag2 = true;
        for (int i = 0; i < 5; i++) {
            if (!marked[i][i]) diag1 = false;
            if (!marked[i][4 - i]) diag2 = false;
        }
        return diag1 || diag2;
    }

    private boolean isLineMarked(int row, int col, boolean isRow) {
        for (int i = 0; i < 5; i++) {
            if (isRow ? !marked[row][i] : !marked[i][col]) {
                return false;
            }
        }
        return true;
    }
}