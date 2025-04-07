package com.example.expensetrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val listener: OnExpenseActionListener
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    interface OnExpenseActionListener {
        fun onEdit(expense: Expense)
        fun onDelete(expense: Expense)
    }

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvExpenseName)
        val amount: TextView = itemView.findViewById(R.id.tvExpenseAmount)
        val category: TextView = itemView.findViewById(R.id.tvExpenseCategory)
        val date: TextView = itemView.findViewById(R.id.tvExpenseDate)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
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

        holder.btnEdit.setOnClickListener {
            listener.onEdit(expense)
        }

        holder.btnDelete.setOnClickListener {
            listener.onDelete(expense)
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun updateList(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }
}