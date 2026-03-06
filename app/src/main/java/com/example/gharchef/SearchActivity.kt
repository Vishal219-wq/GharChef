package com.example.gharchef

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvResults = findViewById(R.id.rvSearchResults)
        tvNoResults = findViewById(R.id.tvNoResults)
        progressBar = findViewById(R.id.progressBar)
        etSearch = findViewById(R.id.etSearchQuery)

        rvResults.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val query = intent.getStringExtra("QUERY") ?: ""
        etSearch.setText(query)
        if (query.isNotEmpty()) searchItems(query)

        etSearch.setOnEditorActionListener { _, _, _ ->
            val q = etSearch.text.toString().trim()
            if (q.isNotEmpty()) searchItems(q)
            true
        }
    }

    private fun searchItems(query: String) {
        progressBar.visibility = View.VISIBLE
        tvNoResults.visibility = View.GONE

        db.collection("products")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                if (result.isEmpty) {
                    tvNoResults.visibility = View.VISIBLE
                    rvResults.visibility = View.GONE
                } else {
                    tvNoResults.visibility = View.GONE
                    rvResults.visibility = View.VISIBLE
                    val items = result.documents.mapNotNull { doc ->
                        MenuItemData(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            cookingTime = doc.getString("cookingTime") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    }
                    rvResults.adapter = MenuAdapter(items) { item ->
                        val uid = auth.currentUser?.uid ?: return@MenuAdapter
                        val cartItem = hashMapOf(
                            "name" to item.name, "price" to item.price,
                            "imageUrl" to item.imageUrl, "quantity" to 1
                        )
                        db.collection("carts").document(uid)
                            .collection("items").document(item.id)
                            .set(cartItem)
                            .addOnSuccessListener {
                                Toast.makeText(this, "${item.name} added to cart!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
            }
    }
}