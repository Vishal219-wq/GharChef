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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalRevenue: TextView
    private lateinit var rvRecentOrders: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvTotalOrders  = findViewById(R.id.tvTotalOrders)
        tvTotalUsers   = findViewById(R.id.tvTotalUsers)
        tvTotalItems   = findViewById(R.id.tvTotalItems)
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        rvRecentOrders = findViewById(R.id.rvRecentOrders)
        progressBar    = findViewById(R.id.progressBar)

        rvRecentOrders.layoutManager = LinearLayoutManager(this)
        rvRecentOrders.isNestedScrollingEnabled = false

        setupBottomNavigation()
        loadStats()

        findViewById<TextView>(R.id.tvViewAllOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }

        findViewById<Button>(R.id.btnSeedData).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Seed Sample Data")
                .setMessage("Add 14 sample menu items to Firestore? Do this only on a fresh database.")
                .setPositiveButton("Seed Now") { _, _ ->
                    FirestoreSeeder.seedAll(db)
                    Toast.makeText(this, "Seeding started! Check Logcat.", Toast.LENGTH_LONG).show()
                    progressBar.postDelayed({ loadStats() }, 4000)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() { super.onResume(); loadStats() }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navAdminOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadStats() {
        progressBar.visibility = View.VISIBLE

        db.collection("orders").get()
            .addOnSuccessListener { orders ->
                tvTotalOrders.text = orders.size().toString()
                var revenue = 0.0
                for (doc in orders.documents) revenue += doc.getDouble("totalAmount") ?: 0.0

                // Format revenue nicely
                tvTotalRevenue.text = when {
                    revenue >= 100000 -> "₹%.1fL".format(revenue / 100000)
                    revenue >= 1000   -> "₹%.1fK".format(revenue / 1000)
                    else              -> "₹%.0f".format(revenue)
                }

                // Recent orders sorted by timestamp
                val recent = orders.documents
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .take(6)

                val recentList = recent.map { doc ->
                    val items = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList()
                    val firstName = (items.firstOrNull()?.get("name") as? String) ?: "Order"
                    RecentOrderItem(
                        id = doc.id,
                        label = "Order #${doc.id.takeLast(4).uppercase()} – $firstName",
                        status = doc.getString("status") ?: "Confirmed",
                        amount = doc.getDouble("totalAmount") ?: 0.0,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
                rvRecentOrders.adapter = RecentOrdersAdapter(recentList) { orderId ->
                    // open order detail if needed
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { progressBar.visibility = View.GONE }

        db.collection("users").get().addOnSuccessListener { tvTotalUsers.text = it.size().toString() }
        db.collection("menu").get().addOnSuccessListener { tvTotalItems.text = it.size().toString() }
    }
}

data class RecentOrderItem(
    val id: String,
    val label: String,
    val status: String,
    val amount: Double,
    val timestamp: Long
)

class RecentOrdersAdapter(
    private val items: List<RecentOrderItem>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RecentOrdersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val flIcon:    FrameLayout = view.findViewById(R.id.flStatusIcon)
        val ivIcon:    ImageView   = view.findViewById(R.id.ivStatusIcon)
        val tvLabel:   TextView    = view.findViewById(R.id.tvRecentOrderId)
        val tvStatus:  TextView    = view.findViewById(R.id.tvRecentOrderStatus)
        val tvAmount:  TextView    = view.findViewById(R.id.tvRecentOrderAmount)
        val dotStatus: View        = view.findViewById(R.id.dotStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recent_order, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvLabel.text  = item.label
        holder.tvAmount.text = "₹%.0f".format(item.amount)

        val timeAgo = getTimeAgo(item.timestamp)
        holder.tvStatus.text = "${item.status} • $timeAgo"

        val (dotColor, bgColor) = when (item.status) {
            "Delivered"        -> Pair("#4CAF50", "#E8F5E9")
            "Cancelled"        -> Pair("#F44336", "#FFEBEE")
            "Out for Delivery" -> Pair("#2196F3", "#E3F2FD")
            "Preparing"        -> Pair("#FF9800", "#FFF3E0")
            else               -> Pair("#E8871A", "#FFF3E0")
        }
        holder.dotStatus.setBackgroundColor(Color.parseColor(dotColor))
        holder.flIcon.setBackgroundColor(Color.parseColor(bgColor))

        holder.itemView.setOnClickListener { onClick(item.id) }
    }

    override fun getItemCount() = items.size

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val mins = diff / 60000
        return when {
            mins < 1  -> "just now"
            mins < 60 -> "${mins} mins ago"
            mins < 1440 -> "${mins / 60} hrs ago"
            else -> "${mins / 1440} days ago"
        }
    }
}