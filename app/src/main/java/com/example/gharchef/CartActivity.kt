package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private lateinit var scrollContent: ScrollView
    private lateinit var layoutPlaceOrder: LinearLayout
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDelivery: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvBottomTotal: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var btnPlaceOrder: Button
    private lateinit var progressBar: ProgressBar

    private val cartItems = mutableListOf<CartItem>()
    private val DELIVERY_CHARGE = 40.0
    private val TAX_RATE = 0.05  // 5%

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvCart           = findViewById(R.id.rvCart)
        tvEmptyCart      = findViewById(R.id.tvEmptyCart)
        scrollContent    = findViewById(R.id.scrollContent)
        layoutPlaceOrder = findViewById(R.id.layoutPlaceOrder)
        tvSubtotal       = findViewById(R.id.tvSubtotal)
        tvDelivery       = findViewById(R.id.tvDelivery)
        tvTax            = findViewById(R.id.tvTax)
        tvTotal          = findViewById(R.id.tvTotal)
        tvBottomTotal    = findViewById(R.id.tvBottomTotal)
        tvItemCount      = findViewById(R.id.tvItemCount)
        btnPlaceOrder    = findViewById(R.id.btnPlaceOrder)
        progressBar      = findViewById(R.id.progressBar)

        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.isNestedScrollingEnabled = false

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        setupBottomNavigation()
        loadCart()

        btnPlaceOrder.setOnClickListener { placeOrder() }
    }

    override fun onResume() { super.onResume(); loadCart() }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, ActivityHome::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        findViewById<LinearLayout>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navCart).setOnClickListener { /* already here */ }
    }

    private fun loadCart() {
        val uid = auth.currentUser?.uid ?: run { showEmpty(); return }
        progressBar.visibility = View.VISIBLE

        db.collection("carts").document(uid).collection("items").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                cartItems.clear()
                for (doc in result.documents) {
                    val item = CartItem(
                        id       = doc.id,
                        name     = doc.getString("name") ?: "",
                        price    = doc.getDouble("price") ?: 0.0,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        quantity = (doc.getLong("quantity") ?: 1).toInt()
                    )
                    if (item.name.isNotEmpty()) cartItems.add(item)
                }
                updateUI()
            }
            .addOnFailureListener { progressBar.visibility = View.GONE; showEmpty() }
    }

    private fun updateUI() {
        if (cartItems.isEmpty()) {
            showEmpty()
        } else {
            tvEmptyCart.visibility   = View.GONE
            scrollContent.visibility = View.VISIBLE
            layoutPlaceOrder.visibility = View.VISIBLE

            tvItemCount.text = "${cartItems.size} ITEM${if (cartItems.size > 1) "S" else ""} ADDED"

            rvCart.adapter = CartAdapter(cartItems,
                onQuantityChanged = { updateTotals() },
                onIncrement = { item -> updateQtyInDb(item) },
                onDecrement = { item ->
                    if (item.quantity == 0) removeFromCart(item) else updateQtyInDb(item)
                },
                onItemRemoved = { item -> removeFromCart(item) }
            )
            updateTotals()
        }
    }

    private fun showEmpty() {
        scrollContent.visibility    = View.GONE
        layoutPlaceOrder.visibility = View.GONE
        tvEmptyCart.visibility      = View.VISIBLE
    }

    private fun updateTotals() {
        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val tax      = subtotal * TAX_RATE
        val total    = subtotal + DELIVERY_CHARGE + tax

        tvSubtotal.text    = "₹%.0f".format(subtotal)
        tvDelivery.text    = "₹%.0f".format(DELIVERY_CHARGE)
        tvTax.text         = "₹%.2f".format(tax)
        tvTotal.text       = "₹%.2f".format(total)
        tvBottomTotal.text = "₹%.2f".format(total)
    }

    private fun updateQtyInDb(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("carts").document(uid).collection("items").document(item.id)
            .update("quantity", item.quantity)
    }

    private fun removeFromCart(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("carts").document(uid).collection("items").document(item.id).delete()
            .addOnSuccessListener {
                cartItems.remove(item)
                updateUI()
                Toast.makeText(this, "${item.name} removed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun placeOrder() {
        val uid = auth.currentUser?.uid ?: return
        if (cartItems.isEmpty()) return

        btnPlaceOrder.isEnabled = false
        btnPlaceOrder.text = "Placing..."

        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val tax      = subtotal * TAX_RATE
        val total    = subtotal + DELIVERY_CHARGE + tax

        val orderData = hashMapOf(
            "userId"         to uid,
            "items"          to cartItems.map { mapOf("name" to it.name, "price" to it.price, "quantity" to it.quantity, "imageUrl" to it.imageUrl) },
            "subtotal"       to subtotal,
            "deliveryCharge" to DELIVERY_CHARGE,
            "tax"            to tax,
            "totalAmount"    to total,
            "status"         to "Confirmed",
            "timestamp"      to System.currentTimeMillis()
        )

        db.collection("orders").add(orderData)
            .addOnSuccessListener { clearCart(uid) }
            .addOnFailureListener { e ->
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order  ›"
                Toast.makeText(this, "Order failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCart(uid: String) {
        val cartRef = db.collection("carts").document(uid).collection("items")
        cartRef.get().addOnSuccessListener { docs ->
            val batch = db.batch()
            for (doc in docs) batch.delete(doc.reference)
            batch.commit().addOnSuccessListener {
                Toast.makeText(this, "Order placed! 🎉", Toast.LENGTH_LONG).show()
                cartItems.clear()
                updateUI()
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order  ›"
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
) : RecyclerView.Adapter<CartAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage:    ImageView = view.findViewById(R.id.ivCartItemImage)
        val tvName:     TextView  = view.findViewById(R.id.tvItemName)
        val tvPrice:    TextView  = view.findViewById(R.id.tvItemPrice)
        val tvQty:      TextView  = view.findViewById(R.id.tvQuantity)
        val btnMinus:   ImageView = view.findViewById(R.id.btnMinus)
        val btnPlus:    ImageView = view.findViewById(R.id.btnPlus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text  = item.name
        holder.tvPrice.text = "₹%.0f".format(item.price * item.quantity)
        holder.tvQty.text   = item.quantity.toString()

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder).centerCrop().into(holder.ivImage)
        }

        holder.btnPlus.setOnClickListener {
            item.quantity++
            holder.tvQty.text   = item.quantity.toString()
            holder.tvPrice.text = "₹%.0f".format(item.price * item.quantity)
            onIncrement(item)
            onQuantityChanged()
        }

        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                holder.tvQty.text   = item.quantity.toString()
                holder.tvPrice.text = "₹%.0f".format(item.price * item.quantity)
                onDecrement(item)
                onQuantityChanged()
            } else {
                onItemRemoved(item)
            }
        }
    }

    override fun getItemCount() = items.size
}