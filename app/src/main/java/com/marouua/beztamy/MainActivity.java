package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mAuth.setLanguageCode("fr");

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        // ✅ changé de Button à TextView
        TextView signupText = findViewById(R.id.signupText); // ✅ correspond au TextView cliquable

        loginButton.setOnClickListener(v -> loginUser());

        signupText.setOnClickListener(v -> {
            // ✅ Lancer l'activité d'inscription
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Échec: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
