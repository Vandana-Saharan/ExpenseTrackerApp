package com.example.expensetrackerapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private var expensesList = mutableListOf<Expense>()
    private lateinit var textNoExpenses: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnShowFilterDialog: Button

    private val months = arrayOf(
        "All", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val years = (Calendar.getInstance().get(Calendar.YEAR) downTo 2000).map { it.toString() }
    private val categories = arrayOf("All","Food", "Grocery", "Shopping", "Rent", "Entertainment", "Laundry", "bills","Others")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        btnShowFilterDialog = findViewById(R.id.btnShowFilterDialog)

        textNoExpenses = findViewById(R.id.textNoExpenses)

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

        btnShowFilterDialog.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_expense, null)
        val spinnerMonth = dialogView.findViewById<Spinner>(R.id.spinnerDialogMonth)
        val spinnerYear = dialogView.findViewById<Spinner>(R.id.spinnerDialogYear)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerDialogCategory)

        spinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spinnerYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        AlertDialog.Builder(this)
            .setTitle("Filter Expenses")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val monthIndex = spinnerMonth.selectedItemPosition
                val year = spinnerYear.selectedItem.toString().toInt()
                val category = spinnerCategory.selectedItem.toString()

                applyFilter(monthIndex, year, category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyFilter(monthIndex: Int, year: Int, category: String) {
        val calendar = Calendar.getInstance()

        val startDate: Date
        val endDate: Date

        if (monthIndex == 0) {
            // "All" month selected → entire year
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            startDate = calendar.time

            calendar.set(Calendar.MONTH, Calendar.DECEMBER)
            calendar.set(Calendar.DAY_OF_MONTH, 31)
            endDate = calendar.time
        } else {
            // Specific month selected
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthIndex - 1) // -1 because "All" is at index 0
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            startDate = calendar.time

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            endDate = calendar.time
        }

        val uid = auth.currentUser?.uid ?: return
        var query = db.collection("users")
            .document(uid)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", startDate.time)
            .whereLessThanOrEqualTo("timestamp", endDate.time)

        if (category != "All") {
            query = query.whereEqualTo("category", category)
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val filteredExpenses = result.documents.mapNotNull { doc ->
                    val expense = doc.toObject(Expense::class.java)
                    expense?.apply { id = doc.id }
                }
                expensesList.clear()
                expensesList.addAll(filteredExpenses)
                expenseAdapter.updateList(expensesList)


                textNoExpenses.visibility = if (expensesList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("ExpenseFilter", "Failed to filter expenses: ${e.message}")
                Toast.makeText(this, "Failed to filter expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    expense?.id = doc.id
                    expense
                }
                expensesList.clear()
                expensesList.addAll(expenses)
                expenseAdapter.updateList(expensesList)

                textNoExpenses.visibility = if (expensesList.isEmpty()) View.VISIBLE else View.GONE
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
