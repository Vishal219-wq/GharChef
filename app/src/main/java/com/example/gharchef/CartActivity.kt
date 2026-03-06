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

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    var quantity: Int = 1
)

class CartActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvCart: RecyclerView
    private lateinit var tvEmptyCart: LinearLayout
    private lateinit var layoutBottom: LinearLayout
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDelivery: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnPlaceOrder: Button
    private lateinit var progressBar: ProgressBar

    private val cartItems = mutableListOf<CartItem>()
    private val DELIVERY_CHARGE = 40.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvCart = findViewById(R.id.rvCart)
        tvEmptyCart = findViewById(R.id.tvEmptyCart)
        layoutBottom = findViewById(R.id.layoutBottom)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvDelivery = findViewById(R.id.tvDelivery)
        tvTotal = findViewById(R.id.tvTotal)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)
        progressBar = findViewById(R.id.progressBar)

        rvCart.layoutManager = LinearLayoutManager(this)

        // Back button
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Bottom navigation
        setupBottomNavigation()

        loadCart()

        btnPlaceOrder.setOnClickListener { placeOrder() }
    }

    private fun setupBottomNavigation() {
        // Home — go back to ActivityHome (clear top so we don't stack)
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, ActivityHome::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // Orders
        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        // Cart — already here, do nothing (or just show a gentle toast)
        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            // Already on Cart screen
        }

        // Profile
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload cart every time screen comes back into focus
        loadCart()
    }

    private fun loadCart() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmptyState()
            return
        }

        progressBar.visibility = View.VISIBLE

        db.collection("carts")
            .document(uid)
            .collection("items")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                cartItems.clear()

                Log.d("CART", "Loaded ${result.size()} items from cart")

                for (doc in result.documents) {
                    val item = CartItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        quantity = (doc.getLong("quantity") ?: 1).toInt()
                    )
                    if (item.name.isNotEmpty()) {
                        cartItems.add(item)
                    }
                }
                updateUI()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("CART", "Failed to load cart: ${e.message}")
                Toast.makeText(this, "Failed to load cart: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun updateUI() {
        if (cartItems.isEmpty()) {
            showEmptyState()
        } else {
            rvCart.visibility = View.VISIBLE
            tvEmptyCart.visibility = View.GONE
            layoutBottom.visibility = View.VISIBLE

            rvCart.adapter = CartAdapter(
                cartItems,
                onQuantityChanged = { updateTotals() },
                onIncrement = { item -> updateQuantityInDb(item, item.quantity) },
                onDecrement = { item -> updateQuantityInDb(item, item.quantity) },
                onItemRemoved = { item -> removeFromCart(item) }
            )
            updateTotals()
        }
    }

    private fun showEmptyState() {
        rvCart.visibility = View.GONE
        tvEmptyCart.visibility = View.VISIBLE
        layoutBottom.visibility = View.GONE
    }

    private fun updateTotals() {
        val subtotal = cartItems.sumOf { it.price * it.quantity }
        tvSubtotal.text = "₹%.2f".format(subtotal)
        tvDelivery.text = "₹%.2f".format(DELIVERY_CHARGE)
        tvTotal.text = "₹%.2f".format(subtotal + DELIVERY_CHARGE)
    }

    private fun updateQuantityInDb(item: CartItem, newQty: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("carts").document(uid)
            .collection("items").document(item.id)
            .update("quantity", newQty)
            .addOnFailureListener { e ->
                Log.e("CART", "Quantity update failed: ${e.message}")
            }
    }

    private fun removeFromCart(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("carts").document(uid)
            .collection("items").document(item.id)
            .delete()
            .addOnSuccessListener {
                cartItems.remove(item)
                updateUI()
                Toast.makeText(this, "${item.name} removed", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("CART", "Remove failed: ${e.message}")
            }
    }

    private fun placeOrder() {
        val uid = auth.currentUser?.uid ?: return
        if (cartItems.isEmpty()) return

        btnPlaceOrder.isEnabled = false
        btnPlaceOrder.text = "Placing Order..."

        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val total = subtotal + DELIVERY_CHARGE

        val orderData = hashMapOf(
            "userId" to uid,
            "items" to cartItems.map {
                mapOf("name" to it.name, "price" to it.price, "quantity" to it.quantity)
            },
            "subtotal" to subtotal,
            "deliveryCharge" to DELIVERY_CHARGE,
            "totalAmount" to total,
            "status" to "Confirmed",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener {
                clearCart(uid)
            }
            .addOnFailureListener { e ->
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order"
                Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCart(uid: String) {
        val batch = db.batch()
        val cartRef = db.collection("carts").document(uid).collection("items")
        cartRef.get().addOnSuccessListener { docs ->
            for (doc in docs) batch.delete(doc.reference)
            batch.commit().addOnSuccessListener {
                Toast.makeText(this, "Order placed successfully! 🎉", Toast.LENGTH_LONG).show()
                cartItems.clear()
                updateUI()
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order"
            }
        }
    }
}

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onQuantityChanged: () -> Unit,
    private val onIncrement: (CartItem) -> Unit,
    private val onDecrement: (CartItem) -> Unit,
    private val onItemRemoved: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnMinus: ImageView = view.findViewById(R.id.btnMinus)
        val btnPlus: ImageView = view.findViewById(R.id.btnPlus)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "₹%.2f".format(item.price * item.quantity)
        holder.tvQuantity.text = item.quantity.toString()

        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                holder.tvQuantity.text = item.quantity.toString()
                holder.tvPrice.text = "₹%.2f".format(item.price * item.quantity)
                onDecrement(item)
                onQuantityChanged()
            }
        }

        holder.btnPlus.setOnClickListener {
            item.quantity++
            holder.tvQuantity.text = item.quantity.toString()
            holder.tvPrice.text = "₹%.2f".format(item.price * item.quantity)
            onIncrement(item)
            onQuantityChanged()
        }

        holder.btnRemove.setOnClickListener {
            onItemRemoved(item)
        }
    }

    override fun getItemCount() = items.size
}