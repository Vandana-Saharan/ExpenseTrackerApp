package com.example.expensetrackerapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private var expensesList = mutableListOf<Expense>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(this)

        expenseAdapter = ExpenseAdapter(expensesList, object : ExpenseAdapter.OnExpenseActionListener {
            override fun onEdit(expense: Expense) {
                showEditDialog(expense)
            }

            override fun onDelete(expense: Expense) {
                deleteExpense(expense)
            }
        })

        recyclerView.adapter = expenseAdapter

        fetchExpenses()
    }

    private fun fetchExpenses() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { doc ->
                    val expense = doc.toObject(Expense::class.java)
                    expense?.id = doc.id // Store document ID for update/delete
                    expense
                }
                expensesList.clear()
                expensesList.addAll(expenses)
                expenseAdapter.updateList(expensesList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteExpense(expense: Expense) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("expenses")
            .document(expense.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show()
                fetchExpenses()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(expense: Expense) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_expense, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etEditAmount)
        val etCategory = dialogView.findViewById<EditText>(R.id.etEditCategory)

        etName.setText(expense.name)
        etAmount.setText(expense.amount)
        etCategory.setText(expense.category)

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedName = etName.text.toString().trim()
                val updatedAmount = etAmount.text.toString().trim()
                val updatedCategory = etCategory.text.toString().trim()

                if (updatedName.isNotEmpty() && updatedAmount.isNotEmpty()) {
                    updateExpense(expense.id, updatedName, updatedAmount, updatedCategory)
                } else {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateExpense(id: String, name: String, amount: String, category: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "name" to name,
            "amount" to amount,
            "category" to category
        )

        db.collection("users")
            .document(uid)
            .collection("expenses")
            .document(id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                fetchExpenses()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update expense", Toast.LENGTH_SHORT).show()
            }
    }
}