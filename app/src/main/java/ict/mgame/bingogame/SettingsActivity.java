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

public class SettingsActivity extends AppCompatActivity {

    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmNewPasswordEditText;
    private Button changePasswordButton;
    private Button logoutButton;
    private DatabaseHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        currentPasswordEditText = findViewById(R.id.current_password);
        newPasswordEditText = findViewById(R.id.new_password);
        confirmNewPasswordEditText = findViewById(R.id.confirm_new_password);
        changePasswordButton = findViewById(R.id.change_password_button);
        logoutButton = findViewById(R.id.logout_button);

        dbHelper = new DatabaseHelper(this);

        // Get current username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        username = prefs.getString("username", null);
        if (username == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPw = currentPasswordEditText.getText().toString().trim();
                String newPw = newPasswordEditText.getText().toString().trim();
                String confirmNewPw = confirmNewPasswordEditText.getText().toString().trim();

                if (currentPw.isEmpty() || newPw.isEmpty() || confirmNewPw.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!dbHelper.validateLogin(username, currentPw)) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("Verification Failed")
                            .setMessage("Current password is incorrect!")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                if (!newPw.equals(confirmNewPw)) {
                    Toast.makeText(SettingsActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                dbHelper.updatePassword(username, newPw);

                // Update SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("password", newPw);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();

                // Clear fields
                currentPasswordEditText.setText("");
                newPasswordEditText.setText("");
                confirmNewPasswordEditText.setText("");
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Go to LoginActivity
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
                finishAffinity(); // Close all activities
            }
        });
    }
}