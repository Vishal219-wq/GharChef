package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCompletion: TextView
    private lateinit var progressCompletion: ProgressBar
    private lateinit var etEditName: EditText
    private lateinit var etEditUsername: EditText
    private lateinit var etEditMobile: EditText
    private lateinit var etEditAddress: EditText
    private lateinit var etEditCity: EditText
    private lateinit var etEditPincode: EditText
    private lateinit var etEditDietPref: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvCompletion = findViewById(R.id.tvCompletion)
        progressCompletion = findViewById(R.id.progressCompletion)
        etEditName = findViewById(R.id.etEditName)
        etEditUsername = findViewById(R.id.etEditUsername)
        etEditMobile = findViewById(R.id.etEditMobile)
        etEditAddress = findViewById(R.id.etEditAddress)
        etEditCity = findViewById(R.id.etEditCity)
        etEditPincode = findViewById(R.id.etEditPincode)
        etEditDietPref = findViewById(R.id.etEditDietPref)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Bottom navigation
        setupBottomNavigation()

        loadProfile()

        btnSave.setOnClickListener { saveProfile() }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, ActivityHome::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            // Already on Profile screen
        }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        tvEmail.text = auth.currentUser?.email ?: ""

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val username = doc.getString("username") ?: ""
                    val mobile = doc.getString("mobile") ?: ""
                    val address = doc.getString("address") ?: ""
                    val city = doc.getString("city") ?: ""
                    val pincode = doc.getString("pincode") ?: ""
                    val dietPref = doc.getString("dietPref") ?: ""

                    tvName.text = name.ifEmpty { "Your Name" }
                    etEditName.setText(name)
                    etEditUsername.setText(username)
                    etEditMobile.setText(mobile)
                    etEditAddress.setText(address)
                    etEditCity.setText(city)
                    etEditPincode.setText(pincode)
                    etEditDietPref.setText(dietPref)

                    calculateCompletion(name, username, mobile, address, city, pincode, dietPref)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateCompletion(vararg fields: String) {
        val filled = fields.count { it.isNotEmpty() }
        val percent = (filled * 100) / fields.size
        progressCompletion.progress = percent
        tvCompletion.text = "Profile $percent% complete"

        val color = when {
            percent < 40 -> android.graphics.Color.parseColor("#F44336")
            percent < 80 -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#4CAF50")
        }
        tvCompletion.setTextColor(color)
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val name = etEditName.text.toString().trim()
        val username = etEditUsername.text.toString().trim()
        val mobile = etEditMobile.text.toString().trim()
        val address = etEditAddress.text.toString().trim()
        val city = etEditCity.text.toString().trim()
        val pincode = etEditPincode.text.toString().trim()
        val dietPref = etEditDietPref.text.toString().trim()

        if (name.isEmpty()) {
            etEditName.error = "Name is required"
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "username" to username,
            "mobile" to mobile,
            "address" to address,
            "city" to city,
            "pincode" to pincode,
            "dietPref" to dietPref
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
                tvName.text = name
                calculateCompletion(name, username, mobile, address, city, pincode, dietPref)
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
                Toast.makeText(this, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}