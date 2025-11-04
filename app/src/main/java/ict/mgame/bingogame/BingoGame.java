package ict.mgame.bingogame;

import android.content.Context;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BingoGame {
    private List<Integer> drawnNumbers = new ArrayList<>();
    private Set<Integer> cardNumbers = new HashSet<>();
    private TextView[][] cells = new TextView[5][5];
    private boolean[][] marked = new boolean[5][5];
    private Random random = new Random();
    private GridLayout bingoGrid;
    private TextView drawnNumberText;
    private Context context;

    public BingoGame(Context context, GridLayout bingoGrid, TextView drawnNumberText) {
        this.context = context;
        this.bingoGrid = bingoGrid;
        this.drawnNumberText = drawnNumberText;
    }

    /**
     * Loads a saved bingo card state into the UI, including marked cells.
     *
     * @param card   The 5x5 array representing the bingo card numbers.
     * @param marked The 5x5 boolean array indicating which cells are marked.
     */
    public void loadBingoCard(int[][] card, boolean[][] marked) {
        bingoGrid.removeAllViews();
        cardNumbers.clear();
        setupCardUI(card, marked);
        this.marked = marked;
    }

    /**
     * Initializes a new random bingo card and sets up the UI.
     */
    public void initializeBingoCard() {
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

    /**
     * Sets up the bingo card UI in the GridLayout using the provided card and marked states.
     *
     * @param card   The 5x5 array of card numbers.
     * @param marked The 5x5 boolean array for marked cells.
     */
    private void setupCardUI(int[][] card, boolean[][] marked) {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TextView cell = new TextView(context);
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

    public boolean checkForBingo() {
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

    /**
     * Performs a draw action: generates a new number, updates the drawn text, and marks the card if matched.
     * Assumes caller has checked coins and other conditions.
     *
     * @param coins        Current coins (not used here, but passed for context).
     * @param dbHelper     Database helper (not used here).
     * @param username     Username (not used here).
     * @param coinsDisplay Coins display TextView (not used here).
     */
    public void performDraw(int coins, DatabaseHelper dbHelper, String username, TextView coinsDisplay) {
        if (coins < 1 || drawnNumbers.size() >= 75) {
            return; // Assume caller handles toasts
        }

        // Deduct coin (caller updates DB and display)

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
    }

    /**
     * Resets and restarts the game by clearing state and initializing a new card.
     */
    public void performRestart() {
        drawnNumbers.clear();
        cardNumbers.clear();
        marked = new boolean[5][5];
        drawnNumberText.setText("Drawn Number");
        initializeBingoCard();
    }

    public int[][] getCard() {
        int[][] card = new int[5][5];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                String text = cells[row][col].getText().toString();
                card[row][col] = "FREE".equals(text) ? 0 : Integer.parseInt(text);
            }
        }
        return card;
    }

    public List<Integer> getDrawnNumbers() {
        return drawnNumbers;
    }

    public boolean[][] getMarked() {
        return marked;
    }

    /**
     * Sets whether the draw action is enabled (e.g., after a win).
     *
     * @param enabled True to enable drawing, false to disable.
     */
    public void setDrawEnabled(boolean enabled) {
        // If needed, expose for bingo check
    }
}