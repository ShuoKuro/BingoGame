package ict.mgame.bingogame;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BingoGame extends AppCompatActivity {

    private GridLayout bingoGrid;
    private TextView drawnNumberText;
    private Button drawButton;
    private List<Integer> drawnNumbers = new ArrayList<>();
    private Set<Integer> cardNumbers = new HashSet<>();
    private TextView[][] cells = new TextView[5][5];
    private boolean[][] marked = new boolean[5][5];
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bingo_game_layout);

        bingoGrid = findViewById(R.id.bingo_grid);
        drawnNumberText = findViewById(R.id.drawn_number);
        drawButton = findViewById(R.id.draw_button);

        initializeBingoCard();
        setupDrawButton();
    }

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

    private void setupDrawButton() {
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawnNumbers.size() >= 75) {
                    Toast.makeText(BingoGame.this, "All numbers drawn!", Toast.LENGTH_SHORT).show();
                    return;
                }

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
                    Toast.makeText(BingoGame.this, "BINGO! You win!", Toast.LENGTH_LONG).show();
                    drawButton.setEnabled(false);
                }
            }
        });
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