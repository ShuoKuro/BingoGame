package ict.mgame.bingogame;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button enterButton;
    private Button registerButton;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // welcomeText is found but unused; remove if not needed
        // TextView welcomeText = findViewById(R.id.welcome_text);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        enterButton = findViewById(R.id.enter_button);
        registerButton = findViewById(R.id.register_button);

        dbHelper = new DatabaseHelper(this);

        // Insert default user if not exists (for testing)
        if (!dbHelper.userExists("admin")) {
            dbHelper.insertUser("admin", "password");
        }

        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dbHelper.validateLogin(username, password)) {
                    // Store in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.apply();

                    // Go to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Show dialog
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Login Failed")
                            .setMessage("Your data is incorrect!! Enter again!")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "bingo.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(@Nullable Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS users");
            onCreate(db);
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
            db.insert("users", null, values);
        }

        public boolean validateLogin(String username, String password) {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query("users", new String[]{"username"}, "username = ? AND password = ?", new String[]{username, password}, null, null, null);
            boolean valid = cursor.getCount() > 0;
            cursor.close();
            return valid;
        }
    }
}