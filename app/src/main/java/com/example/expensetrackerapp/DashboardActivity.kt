package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetrackerapp.ui.theme.ExpenseTrackerAppTheme
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth  // Firebase Authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        setContent {
            ExpenseTrackerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLogout = {
                            auth.signOut() // Log out the user
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Close DashboardActivity
                        },
                        onAddExpense = {
                            val intent = Intent(this, AddExpenseActivity::class.java)
                            startActivity(intent) // Open AddExpenseActivity
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onAddExpense: () -> Unit
) {
    val context = LocalContext.current  // ✅ Get the current context

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Welcome to Expense Tracker Dashboard!")
        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Add Expense Button
        Button(onClick = onAddExpense) {
            Text("Add Expense")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Logout Button
        Button(onClick = onLogout) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ View Expenses Button (Fixed)
        Button(
            onClick = {
                val intent = Intent(context, ViewExpensesActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Expenses")
        }
    }
}

