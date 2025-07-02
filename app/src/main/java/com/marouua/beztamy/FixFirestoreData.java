package com.marouua.beztamy;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FixFirestoreData {

    private static final String TAG = "FixFirestoreData";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void convertTransactionDates() {
        db.collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Object dateObj = doc.get("date");
                        if (dateObj instanceof String) {
                            String dateStr = (String) dateObj;
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy à HH:mm:ss 'UTC'Z", Locale.FRENCH);
                                Date date = sdf.parse(dateStr);
                                db.collection("transactions").document(doc.getId())
                                        .update("date", date)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated date for: " + doc.getId()))
                                        .addOnFailureListener(e -> Log.e(TAG, "Error updating date: " + e.getMessage(), e));
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error for date: " + dateStr + " in doc: " + doc.getId(), e);
                            }
                        } else if (dateObj instanceof com.google.firebase.Timestamp) {
                            Log.d(TAG, "Date already a Timestamp in doc: " + doc.getId());
                        } else {
                            Log.w(TAG, "Unexpected date type: " + (dateObj != null ? dateObj.getClass().getName() : "null") + " in doc: " + doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching transactions: " + e.getMessage(), e));
    }
}