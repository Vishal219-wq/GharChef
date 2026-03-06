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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Order(
    val orderId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "Pending",
    val timestamp: Long = 0L
)

class OrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvNoOrders: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tabCurrent: TextView
    private lateinit var tabPast: TextView
    private lateinit var progressBar: ProgressBar

    private val allOrders = mutableListOf<Order>()
    private var showingCurrent = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvOrders = findViewById(R.id.rvOrders)
        tvNoOrders = findViewById(R.id.tvNoOrders)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        tabCurrent = findViewById(R.id.tabCurrent)
        tabPast = findViewById(R.id.tabPast)
        progressBar = findViewById(R.id.progressBar)

        rvOrders.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Bottom navigation
        setupBottomNavigation()

        tabCurrent.setOnClickListener {
            showingCurrent = true
            tabCurrent.setBackgroundResource(R.drawable.bg_button_green)
            tabCurrent.setTextColor(getColor(R.color.white))
            tabPast.setBackgroundResource(R.drawable.bg_chip_inactive)
            tabPast.setTextColor(getColor(R.color.text_secondary))
            filterAndShow()
        }

        tabPast.setOnClickListener {
            showingCurrent = false
            tabPast.setBackgroundResource(R.drawable.bg_button_green)
            tabPast.setTextColor(getColor(R.color.white))
            tabCurrent.setBackgroundResource(R.drawable.bg_chip_inactive)
            tabCurrent.setTextColor(getColor(R.color.text_secondary))
            filterAndShow()
        }

        loadOrders()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, ActivityHome::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener {
            // Already on Orders screen
        }

        findViewById<LinearLayout>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadOrders() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allOrders.clear()
                var totalSpent = 0.0

                for (doc in result.documents) {
                    val order = Order(
                        orderId = doc.id,
                        items = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList(),
                        totalAmount = (doc.getDouble("totalAmount") ?: 0.0),
                        status = doc.getString("status") ?: "Pending",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                    allOrders.add(order)
                    totalSpent += order.totalAmount
                }
                // Sort newest first in-memory (no composite index needed)
                allOrders.sortByDescending { it.timestamp }

                tvTotalSpent.text = "Total Spent: ₹%.2f".format(totalSpent)
                filterAndShow()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterAndShow() {
        val filtered = if (showingCurrent) {
            allOrders.filter { it.status in listOf("Pending", "Confirmed", "Preparing", "Out for Delivery") }
        } else {
            allOrders.filter { it.status in listOf("Delivered", "Cancelled") }
        }

        if (filtered.isEmpty()) {
            rvOrders.visibility = View.GONE
            tvNoOrders.visibility = View.VISIBLE
            tvNoOrders.text = if (showingCurrent) "No active orders" else "No past orders"
        } else {
            rvOrders.visibility = View.VISIBLE
            tvNoOrders.visibility = View.GONE
            rvOrders.adapter = OrdersAdapter(filtered)
        }
    }
}

class OrdersAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
        val tvOrderItems: TextView = view.findViewById(R.id.tvOrderItems)
        val tvOrderTotal: TextView = view.findViewById(R.id.tvOrderTotal)
        val tvOrderStatus: TextView = view.findViewById(R.id.tvOrderStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvOrderId.text = "Order #${order.orderId.takeLast(6).uppercase()}"
        holder.tvOrderTotal.text = "₹%.2f".format(order.totalAmount)
        holder.tvOrderItems.text = "${order.items.size} item(s)"

        val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(order.timestamp))
        holder.tvOrderDate.text = date

        holder.tvOrderStatus.text = order.status
        val statusColor = when (order.status) {
            "Delivered" -> android.graphics.Color.parseColor("#4CAF50")
            "Cancelled" -> android.graphics.Color.parseColor("#F44336")
            "Preparing" -> android.graphics.Color.parseColor("#FF9800")
            "Out for Delivery" -> android.graphics.Color.parseColor("#2196F3")
            else -> android.graphics.Color.parseColor("#9E9E9E")
        }
        holder.tvOrderStatus.setTextColor(statusColor)
    }

    override fun getItemCount() = orders.size
}