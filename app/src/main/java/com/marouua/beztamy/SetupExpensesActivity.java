package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SetupExpensesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private EditText categoryEditText, amountEditText;
    private Spinner categorySpinner;
    private Button saveButton, deleteButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String transactionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_expenses);

        // Initialisation des vues
        initViews();

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Toolbar + drawer
        setupToolbar();
        setupNavigationDrawer();

        // Spinner
        setupCategorySpinner();

        // Intent reçu pour modification
        handleIntentIfEditing();

        saveButton.setOnClickListener(v -> saveExpense());
        deleteButton.setOnClickListener(v -> deleteTransaction());
    }

    private void initViews() {
        categoryEditText = findViewById(R.id.categoryEditText);
        amountEditText = findViewById(R.id.amountEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton menuButton = new ImageButton(this);
        menuButton.setImageResource(R.drawable.ic_menu);
        menuButton.setBackgroundResource(android.R.color.transparent);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT,
                Gravity.START);
        toolbar.addView(menuButton, params);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                categoryEditText.setVisibility(selected.equals("Autre") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                categoryEditText.setVisibility(View.GONE);
            }
        });
    }

    private void handleIntentIfEditing() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("transactionId")) {
            transactionId = intent.getStringExtra("transactionId");
            amountEditText.setText(intent.getStringExtra("amount"));

            String category = intent.getStringExtra("category");
            categorySpinner.setSelection(getSpinnerIndex(categorySpinner, category));

            if (category.equalsIgnoreCase("Autre")) {
                categoryEditText.setVisibility(View.VISIBLE);
                categoryEditText.setText(intent.getStringExtra("customCategory"));
            }

            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private void saveExpense() {
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String category = selectedCategory.equals("Autre") ?
                categoryEditText.getText().toString().trim() : selectedCategory;
        String amountStr = amountEditText.getText().toString().trim();

        if (category.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> expense = new HashMap<>();
            expense.put("category", category);
            expense.put("amount", amount);
            expense.put("userId", userId);
            expense.put("date", new Date());

            if (transactionId != null) {
                db.collection("transactions")
                        .document(transactionId)
                        .set(expense)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Transaction mise à jour", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                db.collection("transactions")
                        .add(expense)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Dépense enregistrée", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTransaction() {
        if (transactionId != null) {
            db.collection("transactions")
                    .document(transactionId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Transaction supprimée", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur suppression: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(this, DashboardActivity.class));
        } else if (id == R.id.nav_add_expense) {
            startActivity(new Intent(this, SetupExpensesActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
