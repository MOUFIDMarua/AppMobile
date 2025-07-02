package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditsoldeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextView currentBalanceText;
    private EditText newBalanceInput;
    private Button saveBalanceButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editsolde);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI components
        currentBalanceText = findViewById(R.id.currentBalanceText);
        newBalanceInput = findViewById(R.id.newBalanceInput);
        saveBalanceButton = findViewById(R.id.saveBalanceButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        // Verify user authentication
        if (user == null) {
            Toast.makeText(this, "Utilisateur non authentifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up Toolbar and Drawer
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupDrawerContent();
        updateNavHeader();

        // Fetch and display current balance
        fetchCurrentBalance();

        // Set up save button listener
        saveBalanceButton.setOnClickListener(v -> saveNewBalance());

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchCurrentBalance() {
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    Double balance = document.getDouble("balance");
                    if (balance != null) {
                        currentBalanceText.setText(String.format(Locale.getDefault(), "Solde actuel : %,.2f DH", balance));
                    } else {
                        currentBalanceText.setText("Solde actuel : 0.00 DH");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors du chargement du solde", Toast.LENGTH_SHORT).show();
                    currentBalanceText.setText("Solde actuel : 0.00 DH");
                });
    }

    private void saveNewBalance() {
        String newBalanceStr = newBalanceInput.getText().toString().trim();
        if (newBalanceStr.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nouveau solde", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double newBalance = Double.parseDouble(newBalanceStr);
            if (newBalance < 0) {
                Toast.makeText(this, "Le solde ne peut pas être négatif", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("balance", newBalance);

            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Solde mis à jour avec succès", Toast.LENGTH_SHORT).show();
                        fetchCurrentBalance(); // Refresh displayed balance
                        finish(); // Return to previous activity
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur lors de la mise à jour du solde", Toast.LENGTH_SHORT).show();
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Veuillez entrer un nombre valide", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_email);

        emailTextView.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        nameTextView.setText(name != null ? name : "Utilisateur");
                    } else {
                        nameTextView.setText("Utilisateur");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement données utilisateur", Toast.LENGTH_SHORT).show();
                    nameTextView.setText("Utilisateur");
                });
    }

    private void setupDrawerContent() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Navigation vers Tableau de bord", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DashboardActivity.class));
            } else if (id == R.id.nav_add_expense) {
                Toast.makeText(this, "Navigation vers Ajouter une dépense", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AddExpenseActivity.class));
            } else if (id == R.id.nav_revenu) {
                Toast.makeText(this, "Navigation vers Ajouter un revenu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AddRevenueActivity.class));
            } else if (id == R.id.nav_add_income) {
                Toast.makeText(this, "Navigation vers Ajouter Category", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AddIncomeActivity.class));
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Vous êtes déjà sur Modifier le Solde", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "Historique des transactions", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Déconnexion", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public boolean onNavigationItemSelected(android.view.MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavHeader();
    }
}