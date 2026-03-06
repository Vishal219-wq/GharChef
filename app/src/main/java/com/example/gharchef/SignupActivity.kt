package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etName = findViewById<EditText>(R.id.etName)
        val etMobile = findViewById<EditText>(R.id.etMobile)
        val etEmail = findViewById<EditText>(R.id.etSignupEmail)
        val etPassword = findViewById<EditText>(R.id.etSignupPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnSignup.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val name = etName.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // ── Validation ──
            when {
                username.isEmpty() -> {
                    etUsername.error = "Enter username"
                    etUsername.requestFocus()
                    return@setOnClickListener
                }
                name.isEmpty() -> {
                    etName.error = "Enter full name"
                    etName.requestFocus()
                    return@setOnClickListener
                }
                mobile.isEmpty() || mobile.length != 10 -> {
                    etMobile.error = "Enter valid 10-digit mobile number"
                    etMobile.requestFocus()
                    return@setOnClickListener
                }
                email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Enter valid email"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    etPassword.error = "Password must be at least 6 characters"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            // ── Disable button to prevent double click ──
            btnSignup.isEnabled = false
            btnSignup.text = "Creating Account..."

            // ── Step 1: Create user in Firebase Auth ──
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->

                    val uid = authResult.user?.uid ?: ""

                    // ── Step 2: Save user data in Firestore ──
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "username" to username,
                        "name" to name,
                        "mobile" to mobile,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users")
                        .document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()

                            // ── Step 3: Go to Home ──
                            val intent = Intent(this, ActivityHome::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnSignup.isEnabled = true
                            btnSignup.text = "Create Account"
                            Toast.makeText(this, "Failed to save data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnSignup.isEnabled = true
                    btnSignup.text = "Create Account"
                    Toast.makeText(this, "Signup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}

