package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val etEmail          = findViewById<EditText>(R.id.etEmail)
        val etPassword       = findViewById<EditText>(R.id.etPassword)
        val btnLogin         = findViewById<Button>(R.id.btnLogin)
        val tvSignup         = findViewById<TextView>(R.id.tvSignup)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val ivToggle         = findViewById<android.widget.ImageView>(R.id.ivTogglePassword)

        // Password visibility toggle
        ivToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Signup spannable
        val signupText = "Don't have an account? Create Account"
        val spannable = SpannableString(signupText)
        val clickSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
            }
        }
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.orange_primary))
        spannable.setSpan(clickSpan, 23, signupText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(colorSpan, 23, signupText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignup.text = spannable
        tvSignup.movementMethod = LinkMovementMethod.getInstance()

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) { etEmail.error = "Please enter email"; return@setOnClickListener }
            if (password.isEmpty()) { etPassword.error = "Please enter password"; return@setOnClickListener }
            if (password.length < 6) { etPassword.error = "Min 6 characters"; return@setOnClickListener }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val role = doc.getString("role") ?: "user"
                            Toast.makeText(this,
                                if (role == "admin") "Welcome Admin! 🛡️" else "Welcome back! 👋",
                                Toast.LENGTH_SHORT).show()
                            if (role == "admin") {
                                startActivity(Intent(this, AdminDashboardActivity::class.java))
                            } else {
                                startActivity(Intent(this, ActivityHome::class.java))
                            }
                            finish()
                        }
                        .addOnFailureListener {
                            startActivity(Intent(this, ActivityHome::class.java))
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) { etEmail.error = "Enter your email first"; return@setOnClickListener }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { Toast.makeText(this, "Reset email sent! 📧", Toast.LENGTH_LONG).show() }
                .addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
}