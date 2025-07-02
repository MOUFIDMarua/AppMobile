package com.marouua.beztamy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;

    public ExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.categoryTextView.setText(expense.getCategory());
        holder.amountTextView.setText(String.format("%.2f MAD", expense.getAmount()));
        if (expense.getDate() != null) {
            holder.dateTextView.setText(expense.getFormattedDate());
        } else {
            holder.dateTextView.setText("");
        }
        holder.noteTextView.setText(expense.getNote() != null ? expense.getNote() : "");
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, amountTextView, dateTextView, noteTextView;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.textViewCategory);
            amountTextView = itemView.findViewById(R.id.textViewAmount);
            dateTextView = itemView.findViewById(R.id.textViewDate);
            noteTextView = itemView.findViewById(R.id.textViewNote);
        }
    }
}
