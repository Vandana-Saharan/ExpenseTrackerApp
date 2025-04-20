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
import android.widget.ImageView
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.example.expensetrackerapp.PieChartMarkerView

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var buttonFilter: Button
    private lateinit var pieChart: PieChart
    private lateinit var imageNoData: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        auth = FirebaseAuth.getInstance()

        imageNoData = findViewById(R.id.imageNoData)

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
                    // Check budget alert after loading user data
                    checkBudgetAlert(uid)
                }
                .addOnFailureListener {
                    welcomeTextView.text = "Welcome to Expense Tracker Dashboard!"
                }
        }

        val buttonAddExpense = findViewById<Button>(R.id.buttonAddExpense)
        val buttonViewExpenses = findViewById<Button>(R.id.buttonViewExpenses)
        val buttonUserProfile = findViewById<Button>(R.id.buttonUserProfile)

        val buttonFilter = findViewById<Button>(R.id.buttonFilter)
        // Filter dialog
        val filters = resources.getStringArray(R.array.filter_options)
        var selectedFilterIndex = 1 // default Weekly
        buttonFilter.text = filters[selectedFilterIndex]
        buttonFilter.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Filter Expenses")
                .setSingleChoiceItems(filters, selectedFilterIndex) { dialog, which ->
                    selectedFilterIndex = which
                    val filter = filters[which]
                    buttonFilter.text = filter
                    loadPieChartData(filter)
                    dialog.dismiss()
                }
                .show()
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

    override fun onResume() {
        super.onResume()
        // Refresh chart data and budget alerts when returning from other activities
        val filter = findViewById<Button>(R.id.buttonFilter).text.toString()
        loadPieChartData(filter)
        auth.currentUser?.uid?.let { checkBudgetAlert(it) }
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false) // Hide labels on slices
        pieChart.setEntryLabelTextSize(0f)
        pieChart.setEntryLabelColor(Color.TRANSPARENT)
        pieChart.centerText = "Expense by Category"
        pieChart.setCenterTextSize(18f)
        pieChart.animateY(1000)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        // Display marker view on slice selection
        pieChart.marker = PieChartMarkerView(this, R.layout.marker_view)
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

        val categories = listOf("Food", "Grocery", "Shopping", "Rent", "Entertainment", "Laundry", "Bills", "Others")
        categories.forEach { categoryMap[it] = 0f }

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

                val validEntries = categoryMap.filterValues { it > 0f }.map { (category, amount) ->
                    PieEntry(amount, category)
                }

                if (validEntries.isEmpty()) {
                    pieChart.visibility = View.GONE
                    imageNoData.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvTotalExpense).text = "No expenses to display"
                } else {
                    pieChart.visibility = View.VISIBLE
                    imageNoData.visibility = View.GONE
                    updatePieChart(validEntries, categoryMap)
                }
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
            setDrawValues(false) // Hide value text on slices
        }

        pieChart.data = data
        pieChart.invalidate()

        val totalExpense = categoryMap.values.sum()
        findViewById<TextView>(R.id.tvTotalExpense).text = "Total: ₹%.2f".format(totalExpense)
    }
    
    private fun checkBudgetAlert(userId: String) {
        // Use coroutines to perform the Firestore operations
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get budget and limit from SharedPreferences
                val sharedPrefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
                val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
                val budget = sharedPrefs.getFloat("${currentMonth}_budget", -1f)
                val limit = sharedPrefs.getFloat("monthly_limit", -1f)
                
                // If budget or limit not set, return early
                if (budget < 0 || limit < 0) {
                    return@launch
                }
                
                // Calculate monthly expenses
                val monthlyExpense = fetchMonthlyExpenses(userId)
                val remaining = budget - monthlyExpense.toFloat()
                
                // Show alert if remaining amount is less than or equal to limit
                if (remaining <= limit) {
                    val message = if (remaining <= 0) {
                        "❗ You've exceeded your budget!"
                    } else {
                        "⚠️ Alert: Only ₹${String.format("%.2f", remaining)} remaining from your budget!"
                    }
                    
                    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this@DashboardActivity, android.R.color.holo_red_dark))
                        .setTextColor(Color.WHITE)
                        .setActionTextColor(ContextCompat.getColor(this@DashboardActivity, android.R.color.holo_blue_light))
                        .setAction("View Budget") { _: View ->
                            startActivity(Intent(this@DashboardActivity, UserProfileActivity::class.java))
                        }
                        .show()
                }
            } catch (e: Exception) {
                Log.e("BudgetAlert", "Error checking budget alert: ${e.message}")
            }
        }
    }
    
    private suspend fun fetchMonthlyExpenses(userId: String): Double {
        return try {
            val calendar = Calendar.getInstance()
            
            // Set start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.time.time

            // Set end of month
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.time.time

            val db = FirebaseFirestore.getInstance()
            val snapshot = withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(userId)
                    .collection("expenses")
                    .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                    .whereLessThanOrEqualTo("timestamp", endOfMonth)
                    .get()
                    .await()
            }

            val monthlyExpense = snapshot.documents.mapNotNull {
                it.getString("amount")?.toDoubleOrNull()
            }.sum()
            
            Log.d("BudgetAlert", "Monthly expenses: $monthlyExpense")
            monthlyExpense
            
        } catch (e: Exception) {
            Log.e("BudgetAlert", "Error fetching monthly expenses: ${e.message}")
            0.0
        }
    }
}