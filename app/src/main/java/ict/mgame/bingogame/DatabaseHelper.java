package ict.mgame.bingogame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bingo.db";
    private static final int DATABASE_VERSION = 3; // Bumped for new columns

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, wins INTEGER DEFAULT 0, coins INTEGER DEFAULT 20, daily_resets INTEGER DEFAULT 0, last_reset_date TEXT DEFAULT '')");
    }

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
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"username"}, "username = ?", new String[]{username}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

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

    public void updateDailyResets(String username, int newResets, String newDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("daily_resets", newResets);
        values.put("last_reset_date", newDate);
        db.update("users", values, "username = ?", new String[]{username});
    }
}