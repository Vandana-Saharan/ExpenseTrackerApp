package com.example.expensetrackerapp

import android.os.Bundle
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(expensesList)
        recyclerView.adapter = expenseAdapter

        fetchExpenses()
    }

    private fun fetchExpenses() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your expenses", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { it.toObject(Expense::class.java) }
                expensesList.clear()
                expensesList.addAll(expenses)
                expenseAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}