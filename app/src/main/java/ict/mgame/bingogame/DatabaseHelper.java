package ict.mgame.bingogame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bingo.db";
    private static final int DATABASE_VERSION = 4; // Bumped for game state columns

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time. Creates the users table with all columns.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, wins INTEGER DEFAULT 0, coins INTEGER DEFAULT 20, daily_resets INTEGER DEFAULT 0, last_reset_date TEXT DEFAULT '', card_state TEXT DEFAULT '', drawn_state TEXT DEFAULT '', marked_state TEXT DEFAULT '')");
    }

    /**
     * Called when the database needs to be upgraded. Adds new columns based on version changes.
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE users ADD COLUMN wins INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE users ADD COLUMN coins INTEGER DEFAULT 20");
            db.execSQL("ALTER TABLE users ADD COLUMN daily_resets INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE users ADD COLUMN last_reset_date TEXT DEFAULT ''");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE users ADD COLUMN card_state TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE users ADD COLUMN drawn_state TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE users ADD COLUMN marked_state TEXT DEFAULT ''");
        }
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"username"}, "username = ?", new String[]{username}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Inserts a new user into the database with the given username and password, initializing other fields to defaults.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     */
    public void insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("wins", 0);
        values.put("coins", 20); // Start with 20 coins
        values.put("daily_resets", 0);
        values.put("last_reset_date", "");
        db.insert("users", null, values);
    }

    public boolean validateLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"username"}, "username = ? AND password = ?", new String[]{username, password}, null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    /**
     * Updates the password for the specified user in the database.
     *
     * @param username    The username of the user to update.
     * @param newPassword The new password to set.
     */
    public void updatePassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        db.update("users", values, "username = ?", new String[]{username});
    }

    public int getWins(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"wins"}, "username = ?", new String[]{username}, null, null, null);
        int wins = 0;
        if (cursor.moveToFirst()) {
            wins = cursor.getInt(0);
        }
        cursor.close();
        return wins;
    }

    /**
     * Increments the win count for the specified user by 1.
     *
     * @param username The username of the user to increment wins for.
     */
    public void incrementWins(String username) {
        int currentWins = getWins(username);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("wins", currentWins + 1);
        db.update("users", values, "username = ?", new String[]{username});
    }

    public int getCoins(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"coins"}, "username = ?", new String[]{username}, null, null, null);
        int coins = 0;
        if (cursor.moveToFirst()) {
            coins = cursor.getInt(0);
        }
        cursor.close();
        return coins;
    }

    /**
     * Updates the coin count for the specified user in the database.
     *
     * @param username The username of the user to update.
     * @param newCoins The new coin amount to set.
     */
    public void updateCoins(String username, int newCoins) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("coins", newCoins);
        db.update("users", values, "username = ?", new String[]{username});
    }

    public int getDailyResets(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"daily_resets"}, "username = ?", new String[]{username}, null, null, null);
        int resets = 0;
        if (cursor.moveToFirst()) {
            resets = cursor.getInt(0);
        }
        cursor.close();
        return resets;
    }

    public String getLastResetDate(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"last_reset_date"}, "username = ?", new String[]{username}, null, null, null);
        String date = "";
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        return date;
    }

    /**
     * Updates the daily reset count and last reset date for the specified user.
     *
     * @param username  The username of the user to update.
     * @param newResets The new reset count to set.
     * @param newDate   The new last reset date to set.
     */
    public void updateDailyResets(String username, int newResets, String newDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("daily_resets", newResets);
        values.put("last_reset_date", newDate);
        db.update("users", values, "username = ?", new String[]{username});
    }

    /**
     * Updates the game state (card, drawn numbers, marked cells) for the specified user in the database.
     *
     * @param username     The username of the user to update.
     * @param card         The current bingo card state.
     * @param drawnNumbers The list of drawn numbers.
     * @param marked       The marked cells state.
     */
    public void updateGameState(String username, int[][] card, List<Integer> drawnNumbers, boolean[][] marked) {
        String cardStr = serializeCard(card);
        String drawnStr = serializeList(drawnNumbers);
        String markedStr = serializeMarked(marked);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("card_state", cardStr);
        values.put("drawn_state", drawnStr);
        values.put("marked_state", markedStr);
        db.update("users", values, "username = ?", new String[]{username});
    }

    public GameState getGameState(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"card_state", "drawn_state", "marked_state"}, "username = ?", new String[]{username}, null, null, null);
        if (cursor.moveToFirst()) {
            String cardStr = cursor.getString(0);
            String drawnStr = cursor.getString(1);
            String markedStr = cursor.getString(2);
            cursor.close();
            if (cardStr.isEmpty()) {
                return null; // No saved state
            }
            int[][] card = deserializeCard(cardStr);
            List<Integer> drawn = deserializeList(drawnStr);
            boolean[][] markedArr = deserializeMarked(markedStr);
            return new GameState(card, drawn, markedArr);
        }
        cursor.close();
        return null;
    }

    private String serializeCard(int[][] card) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                if (sb.length() > 0) sb.append(",");
                sb.append(card[row][col]);
            }
        }
        return sb.toString();
    }

    private int[][] deserializeCard(String str) {
        if (str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length != 25) return null; // Invalid
        int[][] card = new int[5][5];
        try {
            int index = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    card[row][col] = Integer.parseInt(parts[index++]);
                }
            }
            return card;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String serializeList(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int num : list) {
            if (sb.length() > 0) sb.append(",");
            sb.append(num);
        }
        return sb.toString();
    }

    private List<Integer> deserializeList(String str) {
        List<Integer> list = new ArrayList<>();
        if (str.isEmpty()) return list;
        String[] parts = str.split(",");
        for (String part : parts) {
            list.add(Integer.parseInt(part));
        }
        return list;
    }

    private String serializeMarked(boolean[][] marked) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                if (sb.length() > 0) sb.append(",");
                sb.append(marked[row][col] ? "1" : "0");
            }
        }
        return sb.toString();
    }

    private boolean[][] deserializeMarked(String str) {
        if (str.isEmpty()) return new boolean[5][5];
        String[] parts = str.split(",");
        boolean[][] marked = new boolean[5][5];
        int index = 0;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                marked[row][col] = "1".equals(parts[index++]);
            }
        }
        return marked;
    }

    public static class GameState {
        public int[][] card;
        public List<Integer> drawnNumbers;
        public boolean[][] marked;

        public GameState(int[][] card, List<Integer> drawnNumbers, boolean[][] marked) {
            this.card = card;
            this.drawnNumbers = drawnNumbers;
            this.marked = marked;
        }
    }
}