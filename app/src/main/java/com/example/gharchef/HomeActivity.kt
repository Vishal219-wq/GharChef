package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ActivityHome : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvPopular: RecyclerView
    private lateinit var rvRecommended: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGreeting: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvPopular = findViewById(R.id.rvPopular)
        rvRecommended = findViewById(R.id.rvRecommended)
        progressBar = findViewById(R.id.progressBar)
        tvGreeting = findViewById(R.id.tvGreeting)

        rvPopular.layoutManager = LinearLayoutManager(this)
        rvRecommended.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Set greeting based on time
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        tvGreeting.text = when {
            hour < 12 -> "Good Morning! 🌅"
            hour < 17 -> "Good Afternoon! ☀️"
            else -> "Good Evening! 🌙"
        }

        /* ===========================
           BOTTOM NAVIGATION
        ============================ */
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // Already here
        }
        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        /* ===========================
           SEARCH ACTION
        ============================ */
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.setOnEditorActionListener { _, _, _ ->
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("QUERY", query)
                startActivity(intent)
            }
            true
        }

        /* ===========================
           CATEGORY CHIPS
        ============================ */
        val categoryGroup = findViewById<RadioGroup>(R.id.categoryGroup)
        categoryGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.chipAll) return@setOnCheckedChangeListener
            val (name, key) = when (checkedId) {
                R.id.chipIndian -> Pair("Indian", "north_indian")
                R.id.chipRegional -> Pair("Regional", "east_indian")
                R.id.chipFastCooking -> Pair("Fast Cooking", "street_food")
                else -> return@setOnCheckedChangeListener
            }
            val intent = Intent(this, CategoryMenuActivity::class.java)
            intent.putExtra("CATEGORY_NAME", name)
            intent.putExtra("CATEGORY_KEY", key)
            startActivity(intent)
            // Reset chip selection after launching
            categoryGroup.check(R.id.chipAll)
        }

        /* ===========================
           LOAD DYNAMIC DATA
        ============================ */
        loadPopularDishes()
        loadRecommendedDishes()

        /* ===========================
           LOAD USER GREETING
        ============================ */
        loadUserName()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                if (name.isNotEmpty()) {
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour < 12 -> "Good Morning"
                        hour < 17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }
                    tvGreeting.text = "$greeting, ${name.split(" ").first()}! 👋"
                }
            }
    }

    private fun loadPopularDishes() {
        progressBar.visibility = View.VISIBLE
        db.collection("menu")
            .whereEqualTo("available", true)
            .whereEqualTo("popular", true)
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                val items = mutableListOf<MenuItem>()
                for (doc in result.documents) {
                    val item = MenuItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        prepTime = doc.getString("prepTime") ?: "20 mins",
                        rating = doc.getDouble("rating") ?: 4.0
                    )
                    if (item.name.isNotEmpty()) items.add(item)
                }
                if (items.isNotEmpty()) {
                    rvPopular.adapter = HomeMenuAdapter(items) { addToCart(it) }
                } else {
                    // Fallback: load any available items
                    loadAnyItems(rvPopular)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("HOME", "Popular load failed: ${e.message}")
                loadAnyItems(rvPopular)
            }
    }

    private fun loadAnyItems(rv: RecyclerView) {
        db.collection("menu")
            .whereEqualTo("available", true)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    MenuItem(
                        id = doc.id, name = name,
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        prepTime = doc.getString("prepTime") ?: "20 mins",
                        rating = doc.getDouble("rating") ?: 4.0
                    )
                }
                rv.adapter = HomeMenuAdapter(items) { addToCart(it) }
            }
    }

    private fun loadRecommendedDishes() {
        db.collection("menu")
            .whereEqualTo("available", true)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    MenuItem(
                        id = doc.id, name = name,
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        prepTime = doc.getString("prepTime") ?: "20 mins",
                        rating = doc.getDouble("rating") ?: 4.0
                    )
                }
                rvRecommended.adapter = RecommendedAdapter(items) { addToCart(it) }
            }
    }

    private fun addToCart(item: MenuItem) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }
        val itemRef = db.collection("carts").document(uid)
            .collection("items").document(item.id)

        itemRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val qty = (doc.getLong("quantity") ?: 1).toInt()
                itemRef.update("quantity", qty + 1)
                    .addOnSuccessListener {
                        Toast.makeText(this, "${item.name} quantity updated ✓", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val cartItem = hashMapOf(
                    "itemId" to item.id, "name" to item.name,
                    "price" to item.price, "imageUrl" to item.imageUrl, "quantity" to 1
                )
                itemRef.set(cartItem, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "${item.name} added to cart ✓", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Adapter for popular dishes (vertical list)
class HomeMenuAdapter(
    private val items: List<MenuItem>,
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<HomeMenuAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMenuItemName)
        val tvDesc: TextView = view.findViewById(R.id.tvMenuItemDesc)
        val tvPrice: TextView = view.findViewById(R.id.tvMenuItemPrice)
        val tvPrepTime: TextView = view.findViewById(R.id.tvMenuItemPrepTime)
        val tvRating: TextView = view.findViewById(R.id.tvMenuItemRating)
        val btnAdd: Button = view.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDesc.text = item.description
        holder.tvPrice.text = "₹%.2f".format(item.price)
        holder.tvPrepTime.text = "⏱ ${item.prepTime}"
        holder.tvRating.text = "★ ${"%.1f".format(item.rating)}"
        holder.btnAdd.setOnClickListener { onAddToCart(item) }
    }

    override fun getItemCount() = items.size
}

// Adapter for recommended dishes (horizontal scroll)
class RecommendedAdapter(
    private val items: List<MenuItem>,
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<RecommendedAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvRecName)
        val tvPrice: TextView = view.findViewById(R.id.tvRecPrice)
        val tvRating: TextView = view.findViewById(R.id.tvRecRating)
        val btnAdd: Button = view.findViewById(R.id.btnRecAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recommended, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "₹%.2f".format(item.price)
        holder.tvRating.text = "★ ${"%.1f".format(item.rating)}"
        holder.btnAdd.setOnClickListener { onAddToCart(item) }
    }

    override fun getItemCount() = items.size
}