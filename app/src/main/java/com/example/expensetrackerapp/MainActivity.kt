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
import androidx.compose.ui.unit.dp
import com.example.expensetrackerapp.ui.theme.ExpenseTrackerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "User",
                        modifier = Modifier.padding(innerPadding),
                        navigateToLogin = {
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)  // ✅ Switch to LoginActivity
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, navigateToLogin: () -> Unit) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Welcome to Expense Tracker, $name!")
        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Navigate to LoginActivity
        Button(onClick = navigateToLogin) {
            Text("Go to Login")
        }
    }
}
