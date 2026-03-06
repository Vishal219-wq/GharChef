package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvTotalOrders: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvRecentOrders: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSeedData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvTotalOrders  = findViewById(R.id.tvTotalOrders)
        tvTotalUsers   = findViewById(R.id.tvTotalUsers)
        tvTotalItems   = findViewById(R.id.tvTotalItems)
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        tvRecentOrders = findViewById(R.id.tvRecentOrders)
        progressBar    = findViewById(R.id.progressBar)
        btnSeedData    = findViewById(R.id.btnSeedData)

        setupBottomNavigation()
        loadStats()

        btnSeedData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Seed Sample Data")
                .setMessage("This will add 14 sample menu items to Firestore. Only do this once. Continue?")
                .setPositiveButton("Seed Now") { _, _ ->
                    btnSeedData.isEnabled = false
                    btnSeedData.text = "Seeding..."
                    FirestoreSeeder.seedAll(db)
                    Toast.makeText(this, "Seeding started! Check Logcat for TAG=SEEDER", Toast.LENGTH_LONG).show()
                    btnSeedData.postDelayed({
                        btnSeedData.isEnabled = true
                        btnSeedData.text = "Seed Sample Data"
                        loadStats()
                    }, 5000)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener { }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadStats() {
        progressBar.visibility = View.VISIBLE

        db.collection("orders").get()
            .addOnSuccessListener { orders ->
                tvTotalOrders.text = orders.size().toString()
                var revenue = 0.0
                for (doc in orders.documents) revenue += doc.getDouble("totalAmount") ?: 0.0
                tvTotalRevenue.text = "₹%.2f".format(revenue)

                val recent = orders.documents.sortedByDescending { it.getLong("timestamp") ?: 0L }.take(5)
                val sb = StringBuilder()
                for (doc in recent) {
                    sb.appendLine("#${doc.id.takeLast(6).uppercase()}  •  ₹%.0f".format(doc.getDouble("totalAmount") ?: 0.0) + "  •  ${doc.getString("status") ?: "Pending"}")
                }
                tvRecentOrders.text = if (sb.isEmpty()) "No orders yet" else sb.toString().trim()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                tvRecentOrders.text = "Error: ${e.message}"
            }

        db.collection("users").get().addOnSuccessListener { tvTotalUsers.text = it.size().toString() }
        db.collection("menu").get().addOnSuccessListener { tvTotalItems.text = it.size().toString() }
    }
}