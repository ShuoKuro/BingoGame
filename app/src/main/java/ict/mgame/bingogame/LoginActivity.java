package ict.mgame.bingogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
}