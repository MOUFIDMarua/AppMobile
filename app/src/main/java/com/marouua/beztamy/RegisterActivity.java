package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText firstNameEditText, lastNameEditText, emailEditText, phoneEditText,
            passwordEditText, confirmPasswordEditText, initialBalanceEditText;
    private Button registerButton;
    private TextView loginRedirectText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1) Find views
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        initialBalanceEditText = findViewById(R.id.initialBalanceEditText); // Nouveau champ
        registerButton = findViewById(R.id.registerButton);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // 2) Register click
        registerButton.setOnClickListener(v -> registerUser());

        // 3) Redirect to login
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String pwd = passwordEditText.getText().toString().trim();
        String confirm = confirmPasswordEditText.getText().toString().trim();
        String balanceStr = initialBalanceEditText.getText().toString().trim();

        // 4) Validations
        if (TextUtils.isEmpty(firstName)
                || TextUtils.isEmpty(lastName)
                || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(pwd)
                || TextUtils.isEmpty(confirm)
                || TextUtils.isEmpty(balanceStr)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwd.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pwd.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return;
        }

        // Valider le solde initial
        double initialBalance;
        try {
            initialBalance = Double.parseDouble(balanceStr);
            if (initialBalance < 0) {
                Toast.makeText(this, "Le solde initial ne peut pas être négatif", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Solde initial invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5) Create user in Auth
        mAuth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 6) Save profile in Firestore
                            Map<String, Object> data = new HashMap<>();
                            data.put("firstName", firstName);
                            data.put("lastName", lastName);
                            data.put("email", email);
                            data.put("phone", phone);
                            data.put("balance", initialBalance); // Solde initial saisi par l'utilisateur

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(data)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                                        // Envoie directement vers le Dashboard
                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                "Erreur enregistrement profil: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this,
                                "Échec création compte: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}