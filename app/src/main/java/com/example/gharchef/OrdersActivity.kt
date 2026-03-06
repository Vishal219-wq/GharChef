package com.example.gharchef

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Order(
    val orderId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "Confirmed",
    val timestamp: Long = 0L
)

class OrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvNoOrders: TextView
    private lateinit var tabCurrent: TextView
    private lateinit var tabPast: TextView
    private lateinit var progressBar: ProgressBar

    private val allOrders = mutableListOf<Order>()
    private var showingCurrent = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvOrders    = findViewById(R.id.rvOrders)
        tvNoOrders  = findViewById(R.id.tvNoOrders)
        tabCurrent  = findViewById(R.id.tabCurrent)
        tabPast     = findViewById(R.id.tabPast)
        progressBar = findViewById(R.id.progressBar)

        rvOrders.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        setupBottomNavigation()

        tabCurrent.setOnClickListener { setTab(true) }
        tabPast.setOnClickListener { setTab(false) }

        loadOrders()
    }

    private fun setTab(isCurrent: Boolean) {
        showingCurrent = isCurrent
        tabCurrent.setTextColor(if (isCurrent) Color.parseColor("#E8871A") else Color.parseColor("#AAAAAA"))
        tabCurrent.setTypeface(null, if (isCurrent) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tabPast.setTextColor(if (!isCurrent) Color.parseColor("#E8871A") else Color.parseColor("#AAAAAA"))
        tabPast.setTypeface(null, if (!isCurrent) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        filterAndShow()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, ActivityHome::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        findViewById<LinearLayout>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navOrders).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navFavs).setOnClickListener { /* optional */ }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadOrders() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("orders").whereEqualTo("userId", uid).get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allOrders.clear()
                for (doc in result.documents) {
                    allOrders.add(Order(
                        orderId     = doc.id,
                        items       = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList(),
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        status      = doc.getString("status") ?: "Confirmed",
                        timestamp   = doc.getLong("timestamp") ?: 0L
                    ))
                }
                allOrders.sortByDescending { it.timestamp }
                filterAndShow()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterAndShow() {
        val currentStatuses = listOf("Confirmed", "Preparing", "Out for Delivery", "Pending")
        val pastStatuses    = listOf("Delivered", "Cancelled")
        val filtered = allOrders.filter {
            if (showingCurrent) it.status in currentStatuses else it.status in pastStatuses
        }
        if (filtered.isEmpty()) {
            rvOrders.visibility  = View.GONE
            tvNoOrders.visibility = View.VISIBLE
            tvNoOrders.text = if (showingCurrent) "No active orders" else "No past orders"
        } else {
            rvOrders.visibility  = View.VISIBLE
            tvNoOrders.visibility = View.GONE
            rvOrders.adapter = OrdersAdapter(filtered)
        }
    }
}

class OrdersAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner:     ImageView = view.findViewById(R.id.ivOrderBanner)
        val tvStatusBadge:TextView  = view.findViewById(R.id.tvStatusBadge)
        val tvOrderId:    TextView  = view.findViewById(R.id.tvOrderId)
        val tvOrderTotal: TextView  = view.findViewById(R.id.tvOrderTotal)
        val tvOrderItems: TextView  = view.findViewById(R.id.tvOrderItems)
        val tvOrderDate:  TextView  = view.findViewById(R.id.tvOrderDate)
        val btnAction:    Button    = view.findViewById(R.id.btnOrderAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = orders[position]
        holder.tvOrderId.text    = "Order #GC-${order.orderId.takeLast(4).uppercase()}"
        holder.tvOrderTotal.text = "₹%.2f".format(order.totalAmount)
        holder.tvStatusBadge.text = "⏱ ${order.status.uppercase()}"

        // Build items summary
        val itemsSummary = order.items.joinToString(", ") { item ->
            val qty  = (item["quantity"] as? Long)?.toInt() ?: 1
            val name = item["name"] as? String ?: ""
            "${qty}x $name"
        }
        holder.tvOrderItems.text = itemsSummary.ifEmpty { "${order.items.size} item(s)" }

        val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(order.timestamp))
        holder.tvOrderDate.text = date

        // Status badge color
        val badgeColor = when (order.status) {
            "Delivered"        -> "#4CAF50"
            "Cancelled"        -> "#F44336"
            "Out for Delivery" -> "#2196F3"
            "Preparing"        -> "#FF9800"
            else               -> "#555555"
        }
        holder.tvStatusBadge.setBackgroundColor(Color.parseColor(badgeColor))
        holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_orange)

        // Load first item image if available
        val firstImageUrl = order.items.firstOrNull()?.get("imageUrl") as? String ?: ""
        if (firstImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(firstImageUrl)
                .placeholder(R.drawable.bg_order_banner).centerCrop().into(holder.ivBanner)
        }

        holder.btnAction.text = if (order.status in listOf("Delivered", "Cancelled")) "Reorder" else "Track Order"
    }

    override fun getItemCount() = orders.size
}