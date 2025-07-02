package com.marouua.beztamy;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class EditTransactionActivity extends AppCompatActivity {

    private static final String TAG = "EditTransactionActivity";

    private EditText categoryInput, amountInput, dateInput;
    private Spinner typeSpinner, incomeTypeSpinner;
    private Button updateButton;
    private FirebaseFirestore db;
    private String transactionId;
    private ArrayAdapter<String> incomeTypeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        // Initialize UI components
        categoryInput = findViewById(R.id.categoryInput);
        amountInput = findViewById(R.id.amountInput);
        dateInput = findViewById(R.id.dateInput);
        typeSpinner = findViewById(R.id.typeSpinner);
        incomeTypeSpinner = findViewById(R.id.incomeTypeSpinner);
        updateButton = findViewById(R.id.updateButton);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set up type Spinner
        String[] types = {"revenue", "dépense"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Initialize incomeType Spinner with an empty adapter
        List<String> initialIncomeTypes = new ArrayList<>();
        incomeTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, initialIncomeTypes);
        incomeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        incomeTypeSpinner.setAdapter(incomeTypeAdapter);

        // Show/hide and populate incomeTypeSpinner based on type selection
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                if ("revenue".equals(selectedType)) {
                    fetchAndSetSpinnerData("revenue_types");
                    incomeTypeSpinner.setVisibility(View.VISIBLE);
                } else if ("dépense".equals(selectedType)) {
                    fetchAndSetSpinnerData("depense_types");
                    incomeTypeSpinner.setVisibility(View.VISIBLE);
                } else {
                    incomeTypeSpinner.setVisibility(View.GONE);
                    incomeTypeAdapter.clear();
                    incomeTypeAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                incomeTypeSpinner.setVisibility(View.GONE);
                incomeTypeAdapter.clear();
                incomeTypeAdapter.notifyDataSetChanged();
            }
        });

        // Set up DatePicker for dateInput
        dateInput.setOnClickListener(v -> showDatePickerDialog());

        // Load transaction data from Intent
        transactionId = getIntent().getStringExtra("transaction_id");
        String category = getIntent().getStringExtra("category");
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String date = getIntent().getStringExtra("date");
        String type = getIntent().getStringExtra("type");
        String incomeType = getIntent().getStringExtra("income_type");

        // Pre-fill the form
        categoryInput.setText(category);
        amountInput.setText(String.format(Locale.getDefault(), "%.2f", amount));
        dateInput.setText(date);

        if (type != null) {
            int typePosition = typeAdapter.getPosition(type);
            typeSpinner.setSelection(typePosition >= 0 ? typePosition : 0);
            if ("revenue".equals(type) || "dépense".equals(type)) {
                fetchAndSetSpinnerData(type.equals("revenue") ? "revenue_types" : "depense_types");
                incomeTypeSpinner.setVisibility(View.VISIBLE);
                if (incomeType != null) {
                    incomeTypeAdapter.add(incomeType);
                    int incomeTypePosition = incomeTypeAdapter.getPosition(incomeType);
                    incomeTypeSpinner.setSelection(incomeTypePosition >= 0 ? incomeTypePosition : 0);
                }
            }
        }

        // Set up Update Button
        updateButton.setOnClickListener(v -> updateTransaction());
    }

    private void fetchAndSetSpinnerData(String collectionName) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> types = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("name"); // Adjust field name based on your Firestore structure
                        if (type != null) {
                            types.add(type);
                        }
                    }
                    incomeTypeAdapter.clear();
                    incomeTypeAdapter.addAll(types);
                    incomeTypeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching " + collectionName + " data: " + e.getMessage(), e);
                    Toast.makeText(this, "Erreur chargement des types: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    incomeTypeAdapter.clear();
                    incomeTypeAdapter.notifyDataSetChanged();
                });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthSelected, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(yearSelected, monthSelected, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    dateInput.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void updateTransaction() {
        String category = categoryInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString();
        String incomeType = type.equals("revenue") ? incomeTypeSpinner.getSelectedItem().toString() : null;

        if (category.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non authentifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        java.util.Date date;
        try {
            date = sdf.parse(dateStr);
        } catch (Exception e) {
            Toast.makeText(this, "Format de date invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", user.getUid());
        transaction.put("type", type);
        if ("revenue".equals(type)) {
            transaction.put("incomeType", incomeType);
        } else {
            transaction.put("incomeType", null); // Clear incomeType if not revenu
        }
        transaction.put("categoryName", category);
        transaction.put("amount", amount);
        transaction.put("date", date);

        db.collection("transactions").document(transactionId)
                .set(transaction)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction mise à jour: " + transactionId);
                    Toast.makeText(this, "Transaction mise à jour avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur lors de la mise à jour de la transaction", e);
                    Toast.makeText(this, "Erreur lors de la mise à jour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}