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
        db   = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etName     = findViewById<EditText>(R.id.etName)
        val etMobile   = findViewById<EditText>(R.id.etMobile)
        val etEmail    = findViewById<EditText>(R.id.etSignupEmail)
        val etPassword = findViewById<EditText>(R.id.etSignupPassword)
        val btnSignup  = findViewById<Button>(R.id.btnSignup)
        val tvLogin    = findViewById<TextView>(R.id.tvLogin)

        tvLogin.setOnClickListener { finish() }

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val name     = etName.text.toString().trim()
            val mobile   = etMobile.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                username.isEmpty() -> { etUsername.error = "Enter username"; return@setOnClickListener }
                name.isEmpty()     -> { etName.error = "Enter full name"; return@setOnClickListener }
                mobile.length != 10 -> { etMobile.error = "Enter valid 10-digit number"; return@setOnClickListener }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { etEmail.error = "Enter valid email"; return@setOnClickListener }
                password.length < 6 -> { etPassword.error = "Min 6 characters"; return@setOnClickListener }
            }

            btnSignup.isEnabled = false
            btnSignup.text = "Creating Account..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    val userMap = hashMapOf(
                        "uid"       to uid,
                        "username"  to username,
                        "name"      to name,
                        "mobile"    to mobile,
                        "email"     to email,
                        "role"      to "user",
                        "status"    to "active",
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created! 🎉", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ActivityHome::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        }
                        .addOnFailureListener { e ->
                            btnSignup.isEnabled = true
                            btnSignup.text = "Create Account"
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnSignup.isEnabled = true
                    btnSignup.text = "Create Account"
                    Toast.makeText(this, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}