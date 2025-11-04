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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridLayout bingoGrid;
    private TextView drawnNumberText;
    private TextView usernameDisplay;
    private TextView scoreDisplay;
    private TextView coinsDisplay;
    private TextView resetsRemainingDisplay;
    private TextView coinTimerDisplay;
    private Button drawButton;
    private Button settingsButton;
    private Button restartButton;
    private List<Integer> drawnNumbers = new ArrayList<>();
    private Set<Integer> cardNumbers = new HashSet<>();
    private TextView[][] cells = new TextView[5][5];
    private boolean[][] marked = new boolean[5][5];
    private Random random = new Random();
    private DatabaseHelper dbHelper;
    private String username;
    private int coins;
    private int dailyResets;
    private String lastResetDate;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long coinTimerStart = 0;
    private long lastAddTime;
    private static final long COIN_INTERVAL_MS = 30000; // 0.5 min = 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

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

        dbHelper = new DatabaseHelper(this);

        // Retrieve username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        username = prefs.getString("username", "Guest");
        usernameDisplay.setText("Welcome, " + username + "!");

        if (!username.equals("Guest")) {
            int wins = dbHelper.getWins(username);
            scoreDisplay.setText("Wins: " + wins);
            coins = dbHelper.getCoins(username);
            coinsDisplay.setText("Coins: " + coins);
            updateResetInfo();
        } else {
            scoreDisplay.setText("Wins: 0");
            coins = 0; // Guest has no coins
            coinsDisplay.setText("Coins: 0");
            resetsRemainingDisplay.setText("Resets left: Unlimited (Guest)");
        }

        initializeBingoCard();
        setupDrawButton();

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        restartButton.setOnClickListener(v -> {
            restartGame();
        });
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
                    lastAddTime = now; // Reset to now after add
                }

                // Calculate remaining for display
                long remaining = COIN_INTERVAL_MS - (elapsedSinceLastAdd % COIN_INTERVAL_MS);
                coinTimerDisplay.setText("Next coin in: " + (remaining / 1000) + "s");

                timerHandler.postDelayed(this, 1000); // Update every second
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

        updateResetInfo(); // Refresh
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
        Toast.makeText(this, "Game restarted!", Toast.LENGTH_SHORT).show();
    }

    private void setupDrawButton() {
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (username.equals("Guest") || coins < 1) {
                    Toast.makeText(MainActivity.this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (drawnNumbers.size() >= 75) {
                    Toast.makeText(MainActivity.this, "All numbers drawn!", Toast.LENGTH_SHORT).show();
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
                            cells[row][col].setBackgroundColor(0xFF00FF00); // Green
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }

                // Check for Bingo
                if (checkForBingo()) {
                    Toast.makeText(MainActivity.this, "BINGO! You win!", Toast.LENGTH_LONG).show();
                    drawButton.setEnabled(false);

                    // Award 50 coins and increment wins
                    if (!username.equals("Guest")) {
                        dbHelper.incrementWins(username);
                        int newWins = dbHelper.getWins(username);
                        scoreDisplay.setText("Wins: " + newWins);

                        addCoin(50); // Uses addCoin to update DB and display
                    }
                }
            }
        });
    }

    // initializeBingoCard and checkForBingo remain the same

    private void initializeBingoCard() {
        // Generate unique numbers for each column
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
                card[row][col] = (row == 2 && col == 2) ? 0 : sorted.get(row); // Center is free (0)
            }
        }

        // Set up the grid UI
        bingoGrid.removeAllViews();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TextView cell = new TextView(this);
                int num = card[row][col];
                cell.setText(num == 0 ? "FREE" : String.valueOf(num));
                cell.setTextSize(20);
                cell.setPadding(16, 16, 16, 16);
                cell.setBackgroundColor(0xFFFFFFFF); // White background
                cell.setTextColor(0xFF000000); // Black text
                cell.setGravity(android.view.Gravity.CENTER);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(col, 1f);
                params.rowSpec = GridLayout.spec(row, 1f);
                bingoGrid.addView(cell, params);
                cells[row][col] = cell;
                if (num != 0) {
                    cardNumbers.add(num);
                } else {
                    marked[2][2] = true; // Free is marked
                    cells[2][2].setBackgroundColor(0xFF00FF00); // Green for marked
                }
            }
        }
    }

    private boolean checkForBingo() {
        // Check rows
        for (int row = 0; row < 5; row++) {
            boolean bingo = true;
            for (int col = 0; col < 5; col++) {
                if (!marked[row][col]) {
                    bingo = false;
                    break;
                }
            }
            if (bingo) return true;
        }

        // Check columns
        for (int col = 0; col < 5; col++) {
            boolean bingo = true;
            for (int row = 0; row < 5; row++) {
                if (!marked[row][col]) {
                    bingo = false;
                    break;
                }
            }
            if (bingo) return true;
        }

        // Check diagonals
        boolean diag1 = true;
        for (int i = 0; i < 5; i++) {
            if (!marked[i][i]) {
                diag1 = false;
                break;
            }
        }
        if (diag1) return true;

        boolean diag2 = true;
        for (int i = 0; i < 5; i++) {
            if (!marked[i][4 - i]) {
                diag2 = false;
                break;
            }
        }
        if (diag2) return true;

        return false;
    }

}

