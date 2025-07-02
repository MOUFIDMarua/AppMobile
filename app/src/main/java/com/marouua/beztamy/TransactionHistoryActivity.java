package com.marouua.beztamy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "TransactionHistory";

    private RecyclerView transactionsRecyclerView;
    private TextView noTransactionsText;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        noTransactionsText = findViewById(R.id.noTransactionsText);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_history);

        db = FirebaseFirestore.getInstance();

        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList); // Fixed: Pass transactionList as the second parameter
        transactionsRecyclerView.setAdapter(adapter);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchTransactions();
    }

    private void fetchTransactions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db.collection("transactions")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = new Transaction();
                            transaction.setId(doc.getId());
                            transaction.setUserId(doc.getString("userId"));
                            String type = doc.getString("type");
                            transaction.setType(type != null ? type : "unknown");
                            transaction.setIncomeType(doc.getString("incomeType"));
                            transaction.setCategoryName(doc.getString("categoryName"));
                            transaction.setCategoryId(doc.getString("categoryId"));
                            transaction.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0.0);

                            Object dateObj = doc.get("date");
                            if (dateObj instanceof com.google.firebase.Timestamp) {
                                transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                            } else if (dateObj instanceof String) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy à HH:mm:ss 'UTC'Z", Locale.FRENCH);
                                    Date date = sdf.parse((String) dateObj);
                                    transaction.setDate(date);
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to parse date string: " + dateObj + " for doc: " + doc.getId(), e);
                                    transaction.setDate(null);
                                }
                            } else {
                                Log.w(TAG, "Unexpected date type: " + (dateObj != null ? dateObj.getClass().getName() : "null") + " for doc: " + doc.getId());
                                transaction.setDate(null);
                            }

                            transactionList.add(transaction);
                            Log.d(TAG, "Transaction fetched: " + transaction.getCategoryName() + ", " + transaction.getAmount());
                        } catch (Exception e) {
                            Log.w(TAG, "Error mapping transaction for doc: " + doc.getId(), e);
                        }
                    }

                    if (transactionList.isEmpty()) {
                        noTransactionsText.setVisibility(View.VISIBLE);
                        transactionsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noTransactionsText.setVisibility(View.GONE);
                        transactionsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setTransactions(transactionList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement transactions: " + e.getMessage(), e);
                    Toast.makeText(this, "Erreur chargement transactions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    noTransactionsText.setVisibility(View.VISIBLE);
                    transactionsRecyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        if (id == R.id.nav_home) {
            intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            // Already on this activity
        } else if (id == R.id.nav_add_expense) {
            intent = new Intent(this, AddExpenseActivity.class);
            startActivity(intent);
        }  else if (id == R.id.nav_settings)  {
            startActivity(new Intent(this, EditsoldeActivity.class));
        }else if (id == R.id.nav_revenu) {
            intent = new Intent(this, AddRevenueActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_add_income) {
            intent = new Intent(this, AddIncomeActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTransactions();
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