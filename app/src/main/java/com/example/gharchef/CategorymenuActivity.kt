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

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "",
    val prepTime: String = "20 mins",
    val rating: Double = 4.0,
    val available: Boolean = true
)

class CategoryMenuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvMenu: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var progressBar: ProgressBar

    private val menuItems = mutableListOf<MenuItemData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorymenu)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvMenu = findViewById(R.id.rvMenu)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        progressBar = findViewById(R.id.progressBar)

        rvMenu.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Setup bottom navigation
        setupBottomNavigation()

        // Get category from intent
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Menu"
        val categoryKey = intent.getStringExtra("CATEGORY_KEY") ?: "all"

        tvCategoryTitle.text = categoryName

        loadMenuItems(categoryKey)
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
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadMenuItems(categoryKey: String) {
        progressBar.visibility = View.VISIBLE
        rvMenu.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        val query = if (categoryKey == "all") {
            db.collection("menu")
                .whereEqualTo("available", true)
        } else {
            db.collection("menu")
                .whereEqualTo("category", categoryKey)
                .whereEqualTo("available", true)
        }

        query.get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                menuItems.clear()

                for (doc in result.documents) {
                    val item = MenuItemData(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        prepTime = doc.getString("prepTime") ?: "20 mins",
                        rating = doc.getDouble("rating") ?: 4.0,
                        available = doc.getBoolean("available") ?: true
                    )
                    if (item.name.isNotEmpty()) menuItems.add(item)
                }

                if (menuItems.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvMenu.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvMenu.visibility = View.VISIBLE
                    rvMenu.adapter = MenuItemAdapter(menuItems) { item ->
                        addToCart(item)
                    }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("MENU", "Failed: ${e.message}")
                Toast.makeText(this, "Failed to load menu: ${e.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun addToCart(item: MenuItemData) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val itemRef = db.collection("carts").document(uid)
            .collection("items").document(item.id)

        itemRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val currentQty = (doc.getLong("quantity") ?: 1).toInt()
                    itemRef.update("quantity", currentQty + 1)
                        .addOnSuccessListener {
                            Toast.makeText(this, "${item.name} quantity updated ✓", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val cartItem = hashMapOf(
                        "itemId" to item.id,
                        "name" to item.name,
                        "price" to item.price,
                        "imageUrl" to item.imageUrl,
                        "quantity" to 1
                    )
                    itemRef.set(cartItem, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "${item.name} added to cart ✓", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

class MenuItemAdapter(
    private val items: List<MenuItemData>,
    private val onAddToCart: (MenuItemData) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMenuItemName)
        val tvDesc: TextView = view.findViewById(R.id.tvMenuItemDesc)
        val tvPrice: TextView = view.findViewById(R.id.tvMenuItemPrice)
        val tvPrepTime: TextView = view.findViewById(R.id.tvMenuItemPrepTime)
        val tvRating: TextView = view.findViewById(R.id.tvMenuItemRating)
        val btnAdd: Button = view.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
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