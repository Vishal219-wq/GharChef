package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvResults: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar

    private val allItems = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        db        = FirebaseFirestore.getInstance()
        auth      = FirebaseAuth.getInstance()
        rvResults = findViewById(R.id.rvSearchResults)
        tvEmpty   = findViewById(R.id.tvSearchEmpty)
        etSearch  = findViewById(R.id.etSearchQuery)
        progressBar = findViewById(R.id.progressBar)

        rvResults.layoutManager = LinearLayoutManager(this)

        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Load all menu items upfront for fast in-memory search
        loadAllItems()

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                if (q.length >= 1) searchItems(q) else showAll()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etSearch.requestFocus()
    }

    private fun loadAllItems() {
        progressBar.visibility = View.VISIBLE
        db.collection("menu").whereEqualTo("available", true).get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allItems.clear()
                for (doc in result.documents) {
                    val name = doc.getString("name") ?: ""
                    if (name.isNotEmpty()) {
                        allItems.add(MenuItem(
                            id          = doc.id,
                            name        = name,
                            description = doc.getString("description") ?: "",
                            price       = doc.getDouble("price")       ?: 0.0,
                            imageUrl    = doc.getString("imageUrl")    ?: "",
                            category    = doc.getString("category")    ?: "",
                            prepTime    = doc.getString("prepTime")    ?: "20 mins",
                            rating      = doc.getDouble("rating")      ?: 4.0,
                            available   = doc.getBoolean("available")  ?: true
                        ))
                    }
                }
                showAll()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load items", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAll() {
        updateResults(allItems)
    }

    private fun searchItems(query: String) {
        val results = allItems.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
        updateResults(results)
    }

    private fun updateResults(results: List<MenuItem>) {
        if (results.isEmpty()) {
            tvEmpty.visibility   = View.VISIBLE
            rvResults.visibility = View.GONE
        } else {
            tvEmpty.visibility   = View.GONE
            rvResults.visibility = View.VISIBLE
            rvResults.adapter = SearchResultsAdapter(results) { addToCart(it) }
        }
    }

    private fun addToCart(item: MenuItem) {
        val uid = auth.currentUser?.uid ?: run {
            startActivity(Intent(this, LoginActivity::class.java)); return
        }
        val cartRef = db.collection("carts").document(uid).collection("items")
        cartRef.whereEqualTo("name", item.name).get()
            .addOnSuccessListener { result ->
                if (result.documents.isNotEmpty()) {
                    val doc = result.documents[0]
                    val qty = (doc.getLong("quantity") ?: 1).toInt()
                    doc.reference.update("quantity", qty + 1)
                        .addOnSuccessListener { Toast.makeText(this, "${item.name} updated 🛒", Toast.LENGTH_SHORT).show() }
                } else {
                    cartRef.add(hashMapOf("name" to item.name, "price" to item.price, "imageUrl" to item.imageUrl, "quantity" to 1))
                        .addOnSuccessListener { Toast.makeText(this, "${item.name} added! 🛒", Toast.LENGTH_SHORT).show() }
                }
            }
    }
}

class SearchResultsAdapter(
    private val items: List<MenuItem>,
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivSearchItemImage)
        val tvName:  TextView  = view.findViewById(R.id.tvSearchItemName)
        val tvPrice: TextView  = view.findViewById(R.id.tvSearchItemPrice)
        val tvDesc:  TextView  = view.findViewById(R.id.tvSearchItemDesc)
        val tvTime:  TextView  = view.findViewById(R.id.tvSearchItemTime)
        val btnAdd:  Button    = view.findViewById(R.id.btnSearchAddCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text  = item.name
        holder.tvPrice.text = "₹%.0f".format(item.price)
        holder.tvDesc.text  = item.description.ifEmpty { item.category.replace("_", " ").replaceFirstChar { it.uppercase() } }
        holder.tvTime.text  = "⏱ ${item.prepTime}"
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder).centerCrop().into(holder.ivImage)
        }
        holder.btnAdd.setOnClickListener { onAddToCart(item) }
    }

    override fun getItemCount() = items.size
}