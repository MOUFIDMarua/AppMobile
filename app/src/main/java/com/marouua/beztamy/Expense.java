package com.marouua.beztamy;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class Expense {
    private String id;
    private String category;
    private double amount;
    private Date date;
    private String note;
    private String userId;

    public Expense() {
        // Constructeur vide requis pour Firestore
    }

    public Expense(String category, double amount, Date date, String note, String userId) {
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.userId = userId;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Exclude
    public String getFormattedDate() {
        if (date == null) return "N/A";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    @Exclude
    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%.2f MAD", amount);
    }
}
