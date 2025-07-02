package com.marouua.beztamy;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DashboardActivity";

    private TextView balanceTextView, revenueTextView, expenseTextView;
    private PieChart expensePieChart;
    private BarChart revenueBarChart;
    private LineChart lineChart;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private LinearLayout notificationBanner;
    private TextView notificationText;
    private ImageButton notificationCloseButton;
    private FloatingActionButton addExpenseButton;
    private Spinner monthSpinner, yearSpinner;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Double> revenueCategories = new HashMap<>();
    private Map<String, Double> expenseCategories = new HashMap<>();
    private Map<String, Double> revenueByDate = new HashMap<>();
    private Map<String, Double> expenseByDate = new HashMap<>();
    private double totalRevenue = 0.0;
    private double totalExpense = 0.0;
    private List<String> dates = new ArrayList<>();
    private int selectedMonth = -1;
    private int selectedYear = -1;
    private boolean hasShownBalanceAlert = false; // To show alert only once per session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize UI components
        balanceTextView = findViewById(R.id.balanceAmountText);
        revenueTextView = findViewById(R.id.revenueAmountText);
        expenseTextView = findViewById(R.id.expenseAmountText);
        expensePieChart = findViewById(R.id.expensePieChart);
        revenueBarChart = findViewById(R.id.revenueBarChart);
        lineChart = findViewById(R.id.lineChart);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        notificationBanner = findViewById(R.id.notificationBanner);
        notificationText = findViewById(R.id.notificationText);
        notificationCloseButton = findViewById(R.id.notificationCloseButton);
        addExpenseButton = findViewById(R.id.addExpenseButton);
        monthSpinner = findViewById(R.id.monthSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);

        // Verify UI components
        if (balanceTextView == null || revenueTextView == null || expenseTextView == null ||
                expensePieChart == null || revenueBarChart == null || lineChart == null ||
                notificationBanner == null || notificationText == null || notificationCloseButton == null ||
                monthSpinner == null || yearSpinner == null) {
            Log.e(TAG, "Required UI component is null. Check activity_dashboard.xml");
            Toast.makeText(this, "Erreur: Composants d'interface manquants", Toast.LENGTH_LONG).show();
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

        // Set up Notification Close Button
        notificationCloseButton.setOnClickListener(v -> hideNotification());

        // Set up FAB
        addExpenseButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AddExpenseActivity.class));
        });

        // Set up FAB long-click to edit a transaction (temporary test)
        addExpenseButton.setOnLongClickListener(v -> {
            editTransaction("testTransactionId");
            return true;
        });

        // Set up Spinners for filtering
        setupFilterSpinners();

        // Fetch data
        fetchFinancialData();
    }

    private void setupFilterSpinners() {
        // Month Spinner
        List<String> months = new ArrayList<>();
        months.add("Tous les mois");
        for (int i = 1; i <= 12; i++) {
            months.add(new SimpleDateFormat("MMMM", Locale.getDefault()).format(getDateForMonth(i)));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        // Year Spinner
        List<String> years = new ArrayList<>();
        years.add("Toutes les années");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Set listeners for spinners
        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position == 0 ? -1 : position; // -1 for "Tous les mois"
                fetchFinancialData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMonth = -1;
            }
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = position == 0 ? -1 : Integer.parseInt(years.get(position)); // -1 for "Toutes les années"
                fetchFinancialData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedYear = -1;
            }
        });
    }

    private java.util.Date getDateForMonth(int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month - 1);
        return cal.getTime();
    }

    private void editTransaction(String transactionId) {
        Intent intent = new Intent(this, EditTransactionActivity.class);
        intent.putExtra("transactionId", transactionId);
        startActivity(intent);
        Log.d(TAG, "Launching EditTransactionActivity with transactionId: " + transactionId);
    }

    private void showNotification(String message) {
        notificationText.setText(message);
        notificationBanner.setVisibility(View.VISIBLE);
        Log.d(TAG, "Showing notification: " + message);
        notificationBanner.postDelayed(this::hideNotification, 5000);
    }

    private void hideNotification() {
        notificationBanner.setVisibility(View.GONE);
        Log.d(TAG, "Notification hidden");
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

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("firstName");
                        nameTextView.setText(name != null ? name : "Utilisateur");
                    } else {
                        nameTextView.setText("Utilisateur");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement données utilisateur", e);
                    showNotification("Erreur chargement données utilisateur");
                });
    }

    private void setupDrawerContent() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Vous êtes déjà sur Tableau de bord", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_add_expense) {
                startActivity(new Intent(this, AddExpenseActivity.class));
            } else if (id == R.id.nav_revenu) {
                startActivity(new Intent(this, AddRevenueActivity.class));
            } else if (id == R.id.nav_add_income) {
                startActivity(new Intent(this, AddIncomeActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, EditsoldeActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void fetchFinancialData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Fetch balance
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    Double balance = document.getDouble("balance");
                    double balanceValue = balance != null ? balance : 0.0;
                    balanceTextView.setText(String.format(Locale.getDefault(), "%,.2f DH", balanceValue));

                    // Check if balance is <= 500 and show alert if not already shown
                    if (balanceValue <= 500 && !hasShownBalanceAlert) {
                        showLowBalanceAlert(balanceValue);
                        hasShownBalanceAlert = true;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement solde", e);
                    showNotification("Erreur chargement solde");
                });

        // Fetch transactions for totals, charts, and line graph
        db.collection("transactions")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    revenueCategories.clear();
                    expenseCategories.clear();
                    revenueByDate.clear();
                    expenseByDate.clear();
                    totalRevenue = 0.0;
                    totalExpense = 0.0;
                    dates.clear();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                    for (QueryDocumentSnapshot doc : query) {
                        String catName = doc.getString("categoryName");
                        Double amount = doc.getDouble("amount");
                        String type = doc.getString("type");
                        java.util.Date date = doc.getDate("date");

                        if (catName == null || amount == null || type == null || date == null) continue;

                        // Apply month and year filter
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        int transactionMonth = cal.get(Calendar.MONTH) + 1; // 1-12
                        int transactionYear = cal.get(Calendar.YEAR);

                        if (selectedMonth != -1 && selectedMonth != transactionMonth) continue;
                        if (selectedYear != -1 && selectedYear != transactionYear) continue;

                        Log.d(TAG, "Transaction: catName=" + catName + ", amount=" + amount + ", type=" + type + ", date=" + date);

                        String dateStr = sdf.format(date);
                        if (!dates.contains(dateStr)) {
                            dates.add(dateStr);
                        }

                        // Update totals and categories
                        if (type.equals("revenue")) {
                            totalRevenue += amount;
                            revenueCategories.put(catName, revenueCategories.getOrDefault(catName, 0.0) + amount);
                            revenueByDate.merge(dateStr, amount, Double::sum);
                        } else if (type.equals("expense")) {
                            totalExpense += amount;
                            expenseCategories.put(catName, expenseCategories.getOrDefault(catName, 0.0) + amount);
                            expenseByDate.merge(dateStr, amount, Double::sum);
                        }
                    }

                    // Sort dates in ascending chronological order
                    Collections.sort(dates, new Comparator<String>() {
                        @Override
                        public int compare(String date1, String date2) {
                            try {
                                return sdf.parse(date1).compareTo(sdf.parse(date2));
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing dates for sorting: " + e.getMessage());
                                return 0;
                            }
                        }
                    });

                    // Update totals in cards
                    revenueTextView.setText(String.format(Locale.getDefault(), "%,.2f DH", totalRevenue));
                    expenseTextView.setText(String.format(Locale.getDefault(), "%,.2f DH", totalExpense));

                    // Populate Expense PieChart
                    ArrayList<PieEntry> expenseEntries = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : expenseCategories.entrySet()) {
                        expenseEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                    }

                    PieDataSet expenseDataSet = new PieDataSet(expenseEntries, "Dépenses");
                    expenseDataSet.setColors(new int[]{
                            R.color.pie_chart_orange,
                            R.color.pie_chart_amber,
                            R.color.pie_chart_green,
                            R.color.pie_chart_blue,
                            R.color.pie_chart_pink,
                            R.color.pie_chart_red,
                            R.color.pie_chart_grey,
                            R.color.pie_chart_white,
                    }, this);
                    expenseDataSet.setValueTextSize(12f);
                    expenseDataSet.setValueTextColor(android.graphics.Color.WHITE);

                    PieData expensePieData = new PieData(expenseDataSet);
                    expensePieChart.setData(expensePieData);
                    expensePieChart.getDescription().setEnabled(false);
                    expensePieChart.setCenterText("Dépenses");
                    expensePieChart.setCenterTextSize(14f);
                    expensePieChart.setHoleRadius(40f);
                    expensePieChart.setTransparentCircleRadius(45f);
                    expensePieChart.animateY(1000);
                    expensePieChart.invalidate();

                    // Populate Revenue BarChart
                    ArrayList<BarEntry> revenueEntries = new ArrayList<>();
                    ArrayList<String> categoryLabels = new ArrayList<>(revenueCategories.keySet());
                    for (int i = 0; i < categoryLabels.size(); i++) {
                        String category = categoryLabels.get(i);
                        revenueEntries.add(new BarEntry(i, revenueCategories.get(category).floatValue()));
                    }

                    BarDataSet revenueDataSet = new BarDataSet(revenueEntries, "Revenus");
                    revenueDataSet.setColors(new int[]{
                            R.color.pie_chart_orange,
                            R.color.pie_chart_amber,
                            R.color.pie_chart_green,
                            R.color.pie_chart_blue,
                            R.color.pie_chart_pink,
                            R.color.pie_chart_red,
                            R.color.pie_chart_grey,
                            R.color.pie_chart_white,
                    }, this);
                    revenueDataSet.setValueTextSize(12f);
                    revenueDataSet.setValueTextColor(android.graphics.Color.BLACK);

                    BarData barData = new BarData(revenueDataSet);
                    barData.setBarWidth(0.5f);
                    revenueBarChart.setData(barData);
                    revenueBarChart.getDescription().setEnabled(false);

                    XAxis xAxis = revenueBarChart.getXAxis();
                    xAxis.setEnabled(true);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);
                    xAxis.setLabelCount(categoryLabels.size());
                    xAxis.setGranularity(1f);
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = (int) value;
                            if (index >= 0 && index < categoryLabels.size()) {
                                return categoryLabels.get(index);
                            }
                            return "";
                        }
                    });

                    revenueBarChart.getAxisLeft().setAxisMinimum(0f);
                    revenueBarChart.getAxisRight().setEnabled(false);
                    revenueBarChart.getLegend().setEnabled(false);
                    revenueBarChart.animateY(1000);
                    revenueBarChart.invalidate();

                    // Populate LineChart for Revenue and Expenses over Time
                    ArrayList<Entry> revenueLineEntries = new ArrayList<>();
                    ArrayList<Entry> expenseLineEntries = new ArrayList<>();
                    for (int i = 0; i < dates.size(); i++) {
                        String date = dates.get(i);
                        float revenue = revenueByDate.getOrDefault(date, 0.0).floatValue();
                        float expense = expenseByDate.getOrDefault(date, 0.0).floatValue();
                        revenueLineEntries.add(new Entry(i, revenue));
                        expenseLineEntries.add(new Entry(i, expense));
                    }

                    LineDataSet revenueLineSet = new LineDataSet(revenueLineEntries, "Revenus");
                    revenueLineSet.setColor(getResources().getColor(R.color.card_revenue_color));
                    revenueLineSet.setValueTextColor(getResources().getColor(R.color.textColorPrimary));
                    revenueLineSet.setLineWidth(2f);

                    LineDataSet expenseLineSet = new LineDataSet(expenseLineEntries, "Dépenses");
                    expenseLineSet.setColor(getResources().getColor(R.color.card_expense_color));
                    expenseLineSet.setValueTextColor(getResources().getColor(R.color.textColorPrimary));
                    expenseLineSet.setLineWidth(2f);

                    LineData lineData = new LineData(revenueLineSet, expenseLineSet);
                    lineChart.setData(lineData);
                    lineChart.getDescription().setEnabled(false);
                    lineChart.getLegend().setEnabled(true);
                    lineChart.getAxisLeft().setDrawGridLines(false);
                    lineChart.getAxisRight().setEnabled(false);
                    lineChart.getXAxis().setDrawGridLines(false);
                    lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = (int) value;
                            if (index >= 0 && index < dates.size()) {
                                return dates.get(index);
                            }
                            return "";
                        }
                    });
                    lineChart.getXAxis().setLabelRotationAngle(45f);
                    lineChart.invalidate();

                    Log.d(TAG, "Sorted Dates: " + dates);
                    Log.d(TAG, "Expense Chart Data: " + expenseCategories);
                    Log.d(TAG, "Revenue Chart Data: " + revenueCategories);
                    Log.d(TAG, "Revenue by Date: " + revenueByDate);
                    Log.d(TAG, "Expense by Date: " + expenseByDate);

                    if (expenseCategories.isEmpty()) {
                        showNotification("Aucune donnée pour le graphique des dépenses");
                    }
                    if (revenueCategories.isEmpty()) {
                        showNotification("Aucune donnée pour le graphique des revenus");
                    }
                    if (revenueByDate.isEmpty() && expenseByDate.isEmpty()) {
                        showNotification("Aucune donnée pour le graphique des revenus/dépenses par temps");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement transactions", e);
                    showNotification("Erreur chargement données graphiques");
                });
    }

    private void showLowBalanceAlert(double balance) {
        // Inflate custom layout for the alert
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_low_balance, null);

        TextView titleText = dialogView.findViewById(R.id.alertTitle);
        TextView messageText = dialogView.findViewById(R.id.alertMessage);

        titleText.setText("Alerte Solde Faible");
        messageText.setText(String.format(Locale.getDefault(), "Votre solde est de %.2f DH, ce qui est inférieur ou égal à 500 DH.", balance));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false);

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.RED));
        alertDialog.show();

        // Ensure text is readable on red background
        titleText.setTextColor(Color.WHITE);
        messageText.setTextColor(Color.WHITE);
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
        fetchFinancialData();
        hasShownBalanceAlert = false; // Reset alert flag on resume
    }
}