package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)

        val auth = FirebaseAuth.getInstance()
        val db   = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }

        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name  = doc.getString("name")  ?: "Admin"
                val email = doc.getString("email") ?: ""
                try {
                    findViewById<TextView>(R.id.tvAdminName).text  = name
                    findViewById<TextView>(R.id.tvAdminEmail).text = email
                } catch (_: Exception) {}
            }
    }
}

class AdminUserOrdersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user_orders)

        val userId   = intent.getStringExtra("userId")   ?: ""
        val userName = intent.getStringExtra("userName") ?: "User"

        try {
            findViewById<TextView>(R.id.tvUserOrdersTitle).text = "$userName's Orders"
        } catch (_: Exception) {}

        val db = FirebaseFirestore.getInstance()
        val rv = try { findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvUserOrders) } catch (_: Exception) { null }
        rv?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        if (userId.isNotEmpty()) {
            db.collection("orders").whereEqualTo("userId", userId).get()
                .addOnSuccessListener { result ->
                    val orders = result.documents.map { doc ->
                        val items = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList()
                        AdminOrder(
                            id        = doc.id,
                            userId    = userId,
                            items     = items,
                            total     = doc.getDouble("totalAmount") ?: 0.0,
                            status    = doc.getString("status") ?: "Confirmed",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.sortedByDescending { it.timestamp }

                    rv?.adapter = AdminOrdersAdapter(orders) {}
                }
        }

        try { findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() } } catch (_: Exception) {}
    }
}