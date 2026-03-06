package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // Already logged in — check role
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val role = doc.getString("role") ?: "user"
                        if (role == "admin") {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else {
                            startActivity(Intent(this, ActivityHome::class.java))
                        }
                        finish()
                    }
                    .addOnFailureListener {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }, 2000)
    }
}