package com.example.gharchef

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar

    private val allOrders = mutableListOf<AdminOrder>()
    private var filterStatus = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders_activty)

        db          = FirebaseFirestore.getInstance()
        rvOrders    = findViewById(R.id.rvAdminOrders)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)

        rvOrders.layoutManager = LinearLayoutManager(this)

        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }
        setupBottomNavigation()

        // Status filter tabs
        listOf(
            R.id.tabAllOrders to "all",
            R.id.tabPending   to "Confirmed",
            R.id.tabPreparing to "Preparing",
            R.id.tabDelivery  to "Out for Delivery",
            R.id.tabDelivered to "Delivered"
        ).forEach { (id, status) ->
            try { findViewById<TextView>(id).setOnClickListener { setFilter(status) } } catch (e: Exception) {}
        }

        loadOrders()
    }

    override fun onResume() { super.onResume(); loadOrders() }

    private fun setupBottomNavigation() {
        try {
            findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener {
                startActivity(Intent(this, AdminDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            findViewById<LinearLayout>(R.id.navAdminOrders).setOnClickListener { /* already here */ }
            findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener {
                startActivity(Intent(this, AdminItemsActivity::class.java))
            }
            findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener {
                startActivity(Intent(this, AdminUsersActivity::class.java))
            }
            findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
                startActivity(Intent(this, AdminProfileActivity::class.java))
            }
        } catch (e: Exception) { /* optional nav */ }
    }

    private fun setFilter(status: String) {
        filterStatus = status
        updateRecycler()
    }

    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE
        db.collection("orders").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allOrders.clear()
                for (doc in result.documents) {
                    val items = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList()
                    allOrders.add(AdminOrder(
                        id          = doc.id,
                        userId      = doc.getString("userId")     ?: "",
                        items       = items,
                        total       = doc.getDouble("totalAmount") ?: 0.0,
                        status      = doc.getString("status")     ?: "Confirmed",
                        timestamp   = doc.getLong("timestamp")    ?: 0L
                    ))
                }
                updateRecycler()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecycler() {
        val filtered = if (filterStatus == "all") allOrders else allOrders.filter { it.status == filterStatus }
        if (filtered.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            rvOrders.visibility = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            rvOrders.visibility = View.VISIBLE
            rvOrders.adapter = AdminOrdersAdapter(filtered) { order -> showStatusDialog(order) }
        }
    }

    private fun showStatusDialog(order: AdminOrder) {
        val statuses = arrayOf("Confirmed", "Preparing", "Out for Delivery", "Delivered", "Cancelled")
        var selectedIndex = statuses.indexOf(order.status).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Update Order Status")
            .setSingleChoiceItems(statuses, selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton("Update") { _, _ ->
                val newStatus = statuses[selectedIndex]
                db.collection("orders").document(order.id)
                    .update("status", newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Status updated to $newStatus ✓", Toast.LENGTH_SHORT).show()
                        loadOrders()
                    }
                    .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

data class AdminOrder(
    val id: String,
    val userId: String,
    val items: List<Map<String, Any>>,
    val total: Double,
    val status: String,
    val timestamp: Long
)

class AdminOrdersAdapter(
    private val orders: List<AdminOrder>,
    private val onStatusClick: (AdminOrder) -> Unit
) : RecyclerView.Adapter<AdminOrdersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvId:      TextView = view.findViewById(R.id.tvAdminOrderId)
        val tvItems:   TextView = view.findViewById(R.id.tvAdminOrderItems)
        val tvTotal:   TextView = view.findViewById(R.id.tvAdminOrderTotal)
        val tvStatus:  TextView = view.findViewById(R.id.tvAdminOrderStatus)
        val tvTime:    TextView = view.findViewById(R.id.tvAdminOrderTime)
        val btnUpdate: Button   = view.findViewById(R.id.btnUpdateStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = orders[position]
        holder.tvId.text    = "Order #GC-${order.id.takeLast(4).uppercase()}"
        holder.tvTotal.text = "₹%.0f".format(order.total)

        val summary = order.items.joinToString(", ") { item ->
            "${(item["quantity"] as? Long)?.toInt() ?: 1}x ${item["name"] as? String ?: ""}"
        }
        holder.tvItems.text = summary.ifEmpty { "No items" }

        holder.tvStatus.text = order.status
        val statusColor = when (order.status) {
            "Delivered"        -> "#4CAF50"
            "Cancelled"        -> "#F44336"
            "Out for Delivery" -> "#2196F3"
            "Preparing"        -> "#FF9800"
            else               -> "#E8871A"
        }
        holder.tvStatus.setTextColor(Color.parseColor(statusColor))

        val timeAgo = getTimeAgo(order.timestamp)
        holder.tvTime.text = timeAgo

        holder.btnUpdate.setOnClickListener { onStatusClick(order) }
    }

    override fun getItemCount() = orders.size

    private fun getTimeAgo(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        val mins = diff / 60000
        return when {
            mins < 1  -> "just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins/60}h ago"
            else -> "${mins/1440}d ago"
        }
    }
}