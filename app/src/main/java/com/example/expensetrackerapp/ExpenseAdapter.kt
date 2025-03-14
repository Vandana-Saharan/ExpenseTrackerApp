package com.example.expensetrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense

class ExpenseAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvExpenseName)
        val amount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        val category: TextView = itemView.findViewById(R.id.tvExpenseCategory)
        val date: TextView = itemView.findViewById(R.id.tvExpenseDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expense_item, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.name.text = "Name: ${expense.name}"
        holder.amount.text = "Amount: ₹${expense.amount}"
        holder.category.text = "Category: ${expense.category}"
        holder.date.text = "Date: ${expense.date}"
    }

    override fun getItemCount(): Int = expenses.size

    fun updateList(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }
}
