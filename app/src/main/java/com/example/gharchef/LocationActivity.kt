package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class LocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
       val b1 = findViewById<Button>(R.id.b1)
        b1.setOnClickListener {
            startActivity(Intent(this, ActivityHome::class.java)) // ← changed HomeActivity to ActivityHome
            finish()
        }


    }
}