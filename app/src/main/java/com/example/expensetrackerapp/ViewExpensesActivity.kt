package com.example.expensetrackerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.firestore.FirebaseFirestore

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private var expensesList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses) // Make sure this is correct!

        recyclerView = findViewById(R.id.recyclerViewExpenses) // Find RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(expensesList)
        recyclerView.adapter = expenseAdapter

        fetchExpenses()
    }

    private fun fetchExpenses() {
        FirebaseFirestore.getInstance().collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { it.toObject(Expense::class.java) }
                expensesList.clear()
                expensesList.addAll(expenses)
                expenseAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}
