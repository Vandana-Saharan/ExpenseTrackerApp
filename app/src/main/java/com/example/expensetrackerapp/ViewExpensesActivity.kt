package com.example.expensetrackerapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.model.Expense
import com.google.firebase.firestore.FirebaseFirestore

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private var expensesList = mutableListOf<Expense>()
    private lateinit var spinnerSortDate: Spinner
    private lateinit var spinnerSortAmount: Spinner
    private lateinit var spinnerSortCategory: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(expensesList)
        recyclerView.adapter = expenseAdapter

        fetchExpenses()

        val btnSortByDate: Button = findViewById(R.id.btnSortByDate)
        val btnSortByAmount: Button = findViewById(R.id.btnSortByAmount)
        val btnSortByCategory: Button = findViewById(R.id.btnSortByCategory)

        spinnerSortDate = findViewById(R.id.spinnerSortDate)
        spinnerSortAmount = findViewById(R.id.spinnerSortAmount)
        spinnerSortCategory = findViewById(R.id.spinnerSortCategory)

        setupSpinners()

        btnSortByDate.setOnClickListener { toggleSpinner(spinnerSortDate) }
        btnSortByAmount.setOnClickListener { toggleSpinner(spinnerSortAmount) }
        btnSortByCategory.setOnClickListener { toggleSpinner(spinnerSortCategory) }
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

    private fun setupSpinners() {
        val dateOptions = arrayOf("Select", "By Month", "By Week", "By Year")
        val amountOptions = arrayOf("Select", "Low to High", "High to Low")
        val categoryOptions = arrayOf("Select", "Food", "Transport", "Shopping", "Bills", "Others")

        setupSpinner(spinnerSortDate, dateOptions) { sortByDate(it) }
        setupSpinner(spinnerSortAmount, amountOptions) { sortByAmount(it) }
        setupSpinner(spinnerSortCategory, categoryOptions) { sortByCategory(it) }
    }

    private fun setupSpinner(spinner: Spinner, options: Array<String>, onItemSelected: (String) -> Unit) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) onItemSelected(options[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinner.visibility = View.GONE
    }

    private fun toggleSpinner(spinner: Spinner) {
        spinner.visibility = if (spinner.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun sortByDate(option: String) {
        when (option) {
            "By Month" -> expensesList.sortBy { it.date }
            "By Week" -> expensesList.sortBy { it.date }
            "By Year" -> expensesList.sortBy { it.date }
        }
        expenseAdapter.notifyDataSetChanged()
    }

    private fun sortByAmount(option: String) {
        when (option) {
            "Low to High" -> expensesList.sortBy { it.amount }
            "High to Low" -> expensesList.sortByDescending { it.amount }
        }
        expenseAdapter.notifyDataSetChanged()
    }

    private fun sortByCategory(option: String) {
        expensesList.sortBy { it.category }
        expenseAdapter.notifyDataSetChanged()
    }
}