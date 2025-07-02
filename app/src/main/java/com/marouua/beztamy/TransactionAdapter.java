package com.marouua.beztamy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactions;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction
                , parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (transactions == null || position < 0 || position >= transactions.size()) return;

        Transaction transaction = transactions.get(position);

        holder.categoryText.setText(transaction.getCategoryName() != null ? transaction.getCategoryName() : "N/A");
        holder.amountText.setText(String.format(Locale.getDefault(), "%.2f DH", transaction.getAmount()));

        if (transaction.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.dateText.setText(sdf.format(transaction.getDate()));
        } else {
            holder.dateText.setText("N/A");
        }

        String type = transaction.getType();
        String incomeType = transaction.getIncomeType();
        if ("revenue".equals(type)) {
            holder.typeText.setText("Revenu" + (incomeType != null ? " - " + incomeType : ""));
            holder.arrowIcon.setImageResource(R.drawable.ic_arrow_up_green);
        } else if ("expense".equals(type)) {
            holder.typeText.setText("Dépense");
            holder.arrowIcon.setImageResource(R.drawable.ic_arrow_down_red);
        } else {
            holder.typeText.setText("Inconnu");
            holder.arrowIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        }

        // Set click listeners
        holder.editIcon.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditTransactionActivity.class);
            intent.putExtra("transaction_id", transaction.getId());
            intent.putExtra("category", transaction.getCategoryName());
            intent.putExtra("amount", transaction.getAmount());
            intent.putExtra("date", transaction.getDate() != null ?
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(transaction.getDate()) : "");
            intent.putExtra("type", transaction.getType());
            intent.putExtra("income_type", transaction.getIncomeType());
            context.startActivity(intent);
            Toast.makeText(context, "Modifier la transaction: " + transaction.getId(), Toast.LENGTH_SHORT).show();
        });

        holder.deleteIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmer la suppression")
                    .setMessage("Êtes-vous sûr de vouloir supprimer cette transaction ?")
                    .setPositiveButton("Oui", (dialog, which) -> deleteTransaction(position))
                    .setNegativeButton("Non", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void deleteTransaction(int position) {
        if (position < 0 || position >= transactions.size()) {
            Toast.makeText(context, "Position invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction transaction = transactions.get(position);
        if (transaction == null || transaction.getId() == null) {
            Toast.makeText(context, "Transaction invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        String transactionId = transaction.getId();
        db.collection("transactions").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    transactions.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, transactions.size());
                    Toast.makeText(context, "Transaction supprimée", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Erreur lors de la suppression: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TransactionAdapter", "Delete failed", e);
                });
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText, amountText, dateText, typeText;
        ImageView arrowIcon, editIcon, deleteIcon;

        TransactionViewHolder(View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(R.id.categoryText);
            amountText = itemView.findViewById(R.id.amountText);
            dateText = itemView.findViewById(R.id.dateText);
            typeText = itemView.findViewById(R.id.typeText);
            arrowIcon = itemView.findViewById(R.id.arrowIcon);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}