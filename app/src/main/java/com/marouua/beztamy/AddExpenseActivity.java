package com.marouua.beztamy;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AddExpenseActivity";

    private EditText amountEditText, dateEditText;
    private Spinner categorySpinner;
    private Button saveButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;
    private List<String> categoryNames = new ArrayList<>();
    private Map<String, String> nameToIdMap = new HashMap<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String transactionIdToEdit = null;
    private Double originalAmount = null; // To store the original amount when editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_expense);

        // Gestion des insets (bords d'écran)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Initialisation des vues
        amountEditText = findViewById(R.id.amountEditText);
        dateEditText = findViewById(R.id.dateEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        // Configurer la Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Activer le bouton hamburger
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Configurer le contenu du tiroir
        setupDrawerContent();
        updateNavHeader();

        // Configurer le DatePicker
        setupDatePicker();

        // Configurer le Spinner
        categoryNames.add("Sélectionnez une catégorie");
        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Charger les catégories
        loadCategories();

        // Vérifier si édition de transaction
        transactionIdToEdit = getIntent().getStringExtra("transaction_id");
        if (transactionIdToEdit != null) {
            loadTransactionData(transactionIdToEdit);
        }

        // Gérer le clic sur Enregistrer
        saveButton.setOnClickListener(v -> saveTransaction());
    }

    private void updateNavHeader() {
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Récupérer la vue du header
        View headerView = navigationView.getHeaderView(0);
        TextView nameTextView = headerView.findViewById(R.id.nav_header_name);
        TextView emailTextView = headerView.findViewById(R.id.nav_header_email);

        // Mettre à jour l'email depuis FirebaseAuth
        emailTextView.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");

        // Récupérer le nom depuis Firestore
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
                Toast.makeText(this, "Vous êtes déjà sur Ajouter une dépense", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_revenu) {
                Toast.makeText(this, "Navigation vers Ajouter un revenu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AddRevenueActivity.class));
            } else if (id == R.id.nav_add_income) {
                Toast.makeText(this, "Navigation vers Ajouter Category", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AddIncomeActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, EditsoldeActivity.class));
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

    private void setupDatePicker() {
        dateEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (DatePicker view, int year, int month, int day) -> {
                        calendar.set(year, month, day);
                        String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(calendar.getTime());
                        dateEditText.setText(formatted);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void loadCategories() {
        db.collection("categories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    categoryNames.subList(1, categoryNames.size()).clear();
                    nameToIdMap.clear();

                    for (var doc : snapshot) {
                        String name = doc.getString("name");
                        String id = doc.getId();
                        if (name != null) {
                            categoryNames.add(name);
                            nameToIdMap.put(name, id);
                        }
                    }

                    spinnerAdapter.notifyDataSetChanged();

                    if (categoryNames.size() == 1) {
                        Toast.makeText(this, "Aucune catégorie trouvée", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement catégories", e);
                    Toast.makeText(this, "Erreur chargement catégories", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString().trim();
        String dateStr = dateEditText.getText().toString().trim();
        String selected = (String) categorySpinner.getSelectedItem();

        if (amountStr.isEmpty() || dateStr.isEmpty() || selected == null
                || selected.equals("Sélectionnez une catégorie")) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Le montant doit être supérieur à 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Date date;
        try {
            date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
        } catch (Exception ex) {
            Toast.makeText(this, "Date invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setDate(date);
        tx.setCategoryId(nameToIdMap.get(selected));
        tx.setUserId(userId);
        tx.setCategoryName(selected);
        tx.setType("expense");

        Log.d(TAG, "Saving transaction: amount=" + amount + ", categoryId=" + tx.getCategoryId() +
                ", userId=" + userId + ", type=" + tx.getType());

        var col = db.collection("transactions");
        if (transactionIdToEdit == null) {
            // Ajout d'une nouvelle dépense
            col.add(tx)
                    .addOnSuccessListener(doc -> {
                        Log.d(TAG, "Transaction saved with ID: " + doc.getId());
                        // Subtract the amount from the balance
                        updateBalance(amount, false);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de l'ajout: " + e.getMessage(), e);
                        Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Édition d'une dépense existante
            col.document(transactionIdToEdit)
                    .set(tx)
                    .addOnSuccessListener(u -> {
                        Log.d(TAG, "Transaction updated with ID: " + transactionIdToEdit);
                        // Adjust the balance: add back the original amount, then subtract the new amount
                        adjustBalanceForEdit(originalAmount, amount);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de la modification: " + e.getMessage(), e);
                        Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateBalance(double amount, boolean isRevenue) {
        DocumentReference userRef = db.collection("users").document(user.getUid());
        db.runTransaction(transaction -> {
            Double currentBalance = transaction.get(userRef).getDouble("balance");
            if (currentBalance == null) currentBalance = 0.0;

            double newBalance = isRevenue ? currentBalance + amount : currentBalance - amount;
            if (newBalance < 0) {
                throw new IllegalStateException("Le solde ne peut pas être négatif");
            }
            transaction.update(userRef, "balance", newBalance);
            return newBalance;
        }).addOnSuccessListener(newBalance -> {
            Toast.makeText(this, "Dépense ajoutée et solde mis à jour", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("négatif")) {
                Toast.makeText(this, "Erreur : Solde insuffisant pour cette dépense", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour du solde", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void adjustBalanceForEdit(double originalAmount, double newAmount) {
        DocumentReference userRef = db.collection("users").document(user.getUid());
        db.runTransaction(transaction -> {
            Double currentBalance = transaction.get(userRef).getDouble("balance");
            if (currentBalance == null) currentBalance = 0.0;

            // Add back the original amount (since this is an expense, it was previously subtracted)
            double tempBalance = currentBalance + originalAmount;
            // Subtract the new amount
            double newBalance = tempBalance - newAmount;
            if (newBalance < 0) {
                throw new IllegalStateException("Le solde ne peut pas être négatif");
            }
            transaction.update(userRef, "balance", newBalance);
            return newBalance;
        }).addOnSuccessListener(newBalance -> {
            Toast.makeText(this, "Dépense modifiée et solde mis à jour", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("négatif")) {
                Toast.makeText(this, "Erreur : Solde insuffisant pour cette dépense", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Erreur lors de la mise à jour du solde", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactionData(String id) {
        db.collection("transactions").document(id).get()
                .addOnSuccessListener(doc -> {
                    Transaction t = doc.toObject(Transaction.class);
                    if (t != null) {
                        amountEditText.setText(String.valueOf(t.getAmount()));
                        originalAmount = t.getAmount(); // Store the original amount for balance adjustment
                        if (t.getDate() != null) {
                            dateEditText.setText(
                                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .format(t.getDate())
                            );
                        }

                        if (t.getCategoryId() != null) {
                            db.collection("categories").document(t.getCategoryId()).get()
                                    .addOnSuccessListener(cat -> {
                                        String name = cat.getString("name");
                                        if (name != null) {
                                            int idx = categoryNames.indexOf(name);
                                            if (idx >= 0) categorySpinner.setSelection(idx);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Erreur chargement catégorie: " + e.getMessage(), e);
                                        Toast.makeText(this, "Erreur chargement catégorie", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement transaction: " + e.getMessage(), e);
                    Toast.makeText(this, "Erreur chargement transaction", Toast.LENGTH_SHORT).show();
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