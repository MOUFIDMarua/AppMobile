package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddIncomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AddIncomeActivity";

    private TextInputEditText typeNameEditText;
    private Spinner typeSpinner;
    private Button saveButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String> typeOptions = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        // Initialize UI components
        typeNameEditText = findViewById(R.id.typeNameEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        saveButton = findViewById(R.id.saveButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        // Verify UI components
        if (typeSpinner == null) {
            Log.e(TAG, "typeSpinner is null. Check activity_add_income.xml for R.id.typeSpinner");
            Toast.makeText(this, "Erreur: Spinner type non trouvé", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (typeNameEditText == null || saveButton == null) {
            Log.e(TAG, "Required UI component is null. Check activity_add_income.xml");
            Toast.makeText(this, "Erreur: Composants d'interface manquants", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up Toolbar and Drawer
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajouter un Type");
        } else {
            Log.e(TAG, "SupportActionBar is null after setSupportActionBar");
            Toast.makeText(this, "Erreur: ActionBar non configuré", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupDrawerContent();
        updateNavHeader();

        // Configure Spinner with options
        typeOptions.add("Sélectionnez le type");
        typeOptions.add("Revenu");
        typeOptions.add("Dépense");
        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                typeOptions
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(spinnerAdapter);

        // Save button
        saveButton.setOnClickListener(v -> saveType());
    }

    private void updateNavHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_email);

        emailTextView.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");
        nameTextView.setText(user.getDisplayName() != null ? user.getDisplayName() : "Utilisateur");

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
                    Log.e(TAG, "Erreur chargement données utilisateur", e);
                    Toast.makeText(this, "Erreur chargement données utilisateur", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDrawerContent() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Navigation vers Tableau de bord", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_add_expense) {
                Toast.makeText(this, "Navigation vers Ajouter une dépense", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, AddExpenseActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_revenu) {
                Toast.makeText(this, "Navigation vers Ajouter un revenu", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, AddRevenueActivity.class);
                startActivity(intent);
            }
            else if (id == R.id.nav_settings)  {
                startActivity(new Intent(this, EditsoldeActivity.class));
            }else if (id == R.id.nav_add_income) {
                Toast.makeText(this, "Vous êtes déjà sur Ajouter Category", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "Historique des transactions", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, TransactionHistoryActivity.class);
                startActivity(intent);

            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Déconnexion", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

     
    }

    private void saveType() {
        String typeName = typeNameEditText.getText().toString().trim();
        String selectedType = (String) typeSpinner.getSelectedItem();

        if (typeName.isEmpty() || selectedType == null || selectedType.equals("Sélectionnez le type")) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionName = selectedType.equals("Revenu") ? "revenue_types" : "categories";
        Map<String, Object> typeData = new HashMap<>();
        typeData.put("name", typeName);

        db.collection(collectionName)
                .add(typeData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Type added to " + collectionName + " with ID: " + docRef.getId());
                    Toast.makeText(this, "Type ajouté avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de l'ajout du type", e);
                    Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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