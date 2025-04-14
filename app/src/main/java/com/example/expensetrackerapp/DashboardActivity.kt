package com.example.expensetrackerapp

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import java.util.Calendar
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var filterSpinner: Spinner
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        auth = FirebaseAuth.getInstance()

        val welcomeTextView = findViewById<TextView>(R.id.dashboardWelcomeText)
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "User"
                        welcomeTextView.text = "Welcome, $name to your  Dashboard!"
                    } else {
                        welcomeTextView.text = "Welcome to Expense Tracker Dashboard!"
                    }
                }
                .addOnFailureListener {
                    welcomeTextView.text = "Welcome to Expense Tracker Dashboard!"
                }
        }

        val buttonAddExpense = findViewById<Button>(R.id.buttonAddExpense)
        val buttonViewExpenses = findViewById<Button>(R.id.buttonViewExpenses)
        val buttonUserProfile = findViewById<Button>(R.id.buttonUserProfile)

        // Initialize Spinner
        filterSpinner = findViewById(R.id.filterSpinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            filterSpinner.adapter = adapter
        }

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val filter = parent.getItemAtPosition(position).toString()
                loadPieChartData(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // PieChart setup
        pieChart = findViewById(R.id.pieChart)
        setupPieChart()

        buttonAddExpense.setOnClickListener {
            checkLoginAndProceed {
                startActivity(Intent(this, AddExpenseActivity::class.java))
            }
        }

        buttonViewExpenses.setOnClickListener {
            checkLoginAndProceed {
                startActivity(Intent(this, ViewExpensesActivity::class.java))
            }
        }

        buttonUserProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.centerText = "Expense by Category"
        pieChart.setCenterTextSize(18f)
        pieChart.animateY(1000)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
    }

    private fun checkLoginAndProceed(action: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showLoginRequiredDialog()
        } else {
            action()
        }
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Login Required")
            .setMessage("Please login or sign up to perform this action.")
            .setPositiveButton("Login / Signup") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadPieChartData(filter: String) {
        val entries = ArrayList<PieEntry>()
        val categoryMap = mutableMapOf<String, Float>()

        // Defined categories
        val categories = listOf("Food", "Grocery", "Shopping", "Rent", "Entertainment", "Laundry", "Bills", "Others")
        categories.forEach { categoryMap[it] = 0f }

        // Set time filter
        val calendar = Calendar.getInstance()
        val now = calendar.time
        when (filter) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            "Monthly" -> calendar.add(Calendar.MONTH, -1)
            "Yearly" -> calendar.add(Calendar.YEAR, -1)
        }
        val startDate = calendar.time

        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid

        // Correct path: users/{uid}/expenses
        db.collection("users").document(uid).collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", startDate.time)
            .whereLessThanOrEqualTo("timestamp", now.time)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("category") ?: "Others"
                    val amountStr = document.get("amount").toString()
                    val amount = amountStr.toFloatOrNull() ?: 0f

                    categoryMap[category] = categoryMap[category]?.plus(amount) ?: amount
                }

                // Add only non-zero values
                val validEntries = categoryMap.filterValues { it > 0f }.map { (category, amount) ->
                    PieEntry(amount, category)
                }

                updatePieChart(validEntries, categoryMap)
            }
            .addOnFailureListener { e ->
                Log.e("PieChartError", "Error loading chart data", e)
                Toast.makeText(this, "Failed to load chart data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePieChart(entries: List<PieEntry>, categoryMap: Map<String, Float>) {
        val dataSet = PieDataSet(entries, "Expenses").apply {
            colors = listOf(
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_food),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_grocery),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_shopping),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_rent),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_entertainment),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_laundry),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_bills),
                ContextCompat.getColor(this@DashboardActivity, R.color.pie_color_others)
            )
        }

        val data = PieData(dataSet).apply {
            setDrawValues(true)
            setValueTextSize(12f)
            setValueTextColor(Color.BLACK)
        }

        pieChart.data = data
        pieChart.invalidate()

        val totalExpense = categoryMap.values.sum()
        findViewById<TextView>(R.id.tvTotalExpense).text = "Total: ₹%.2f".format(totalExpense)
    }
}
