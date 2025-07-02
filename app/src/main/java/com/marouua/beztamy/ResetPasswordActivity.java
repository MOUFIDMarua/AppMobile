package com.marouua.beztamy;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.marouua.beztamy.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText resetEmailEditText;
    private Button resetButton;
    private TextView backToLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password); // Assuming your XML is saved as activity_reset_password.xml

        // Initialize views
        resetEmailEditText = findViewById(R.id.resetEmailEditText);
        resetButton = findViewById(R.id.resetButton);
        backToLoginText = findViewById(R.id.backToLoginText);

        // Set click listener for reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = resetEmailEditText.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(ResetPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                } else {
                    // Here you would typically implement your password reset logic
                    // For example, send a reset link to the email
                    Toast.makeText(ResetPasswordActivity.this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set click listener for back to login text
        backToLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish this activity to go back to login
                finish();
            }
        });
    }
}