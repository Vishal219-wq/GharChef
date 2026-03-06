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

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val city: String = "",
    val status: String = "active",
    val role: String = "user"
)

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText

    private val allUsers = mutableListOf<AppUser>()
    private val filteredUsers = mutableListOf<AppUser>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        db          = FirebaseFirestore.getInstance()
        rvUsers     = findViewById(R.id.rvUsers)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        etSearch    = findViewById(R.id.etSearchUsers)

        rvUsers.layoutManager = LinearLayoutManager(this)

        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }
        setupBottomNavigation()
        loadUsers()

        // Filter tabs
        listOf(
            R.id.tabAllUsers to "all",
            R.id.tabActive   to "active",
            R.id.tabSuspended to "suspended"
        ).forEach { (id, key) ->
            findViewById<TextView>(id).setOnClickListener { setFilter(key) }
        }

        // Search
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { filterUsers(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // More options
        findViewById<android.widget.ImageView>(R.id.ivMore).setOnClickListener { showMoreOptions() }
    }

    override fun onResume() { super.onResume(); loadUsers() }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navAdminOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        db.collection("users").whereEqualTo("role", "user").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allUsers.clear()
                for (doc in result.documents) {
                    allUsers.add(AppUser(
                        uid     = doc.id,
                        name    = doc.getString("name")   ?: doc.getString("username") ?: "Unknown",
                        email   = doc.getString("email")  ?: "",
                        mobile  = doc.getString("mobile") ?: "",
                        city    = listOfNotNull(doc.getString("city"), doc.getString("state")).joinToString(", ").ifEmpty { "India" },
                        status  = doc.getString("status") ?: "active",
                        role    = doc.getString("role")   ?: "user"
                    ))
                }
                filterUsers("")
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        val tabIds = mapOf("all" to R.id.tabAllUsers, "active" to R.id.tabActive, "suspended" to R.id.tabSuspended)
        tabIds.forEach { (key, id) ->
            val tv = findViewById<TextView>(id)
            if (key == filter) {
                tv.setTextColor(Color.parseColor("#E8871A"))
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tv.setTextColor(Color.parseColor("#AAAAAA"))
                tv.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
        filterUsers(etSearch.text.toString())
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()
        var base = allUsers
        if (currentFilter != "all") {
            base = allUsers.filter { it.status == currentFilter }.toMutableList()
        }
        filteredUsers.addAll(
            if (query.isEmpty()) base
            else base.filter { it.name.contains(query, true) || it.email.contains(query, true) || it.mobile.contains(query, true) }
        )
        updateRecycler()
    }

    private fun updateRecycler() {
        if (filteredUsers.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            rvUsers.visibility  = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            rvUsers.visibility  = View.VISIBLE
            rvUsers.adapter = AdminUsersAdapter(
                filteredUsers,
                onViewOrders   = { user -> viewUserOrders(user) },
                onStatusToggle = { user -> toggleUserStatus(user) }
            )
        }
    }

    private fun viewUserOrders(user: AppUser) {
        val intent = Intent(this, AdminUserOrdersActivity::class.java)
        intent.putExtra("userId", user.uid)
        intent.putExtra("userName", user.name)
        startActivity(intent)
    }

    private fun toggleUserStatus(user: AppUser) {
        val newStatus = if (user.status == "active") "suspended" else "active"
        val label     = if (newStatus == "suspended") "Suspend" else "Reactivate"

        AlertDialog.Builder(this)
            .setTitle("$label User")
            .setMessage("Are you sure you want to $label ${user.name}?")
            .setPositiveButton(label) { _, _ ->
                db.collection("users").document(user.uid).update("status", newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, "${user.name} ${newStatus}d ✓", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener { Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoreOptions() {
        val options = arrayOf("Export Users List", "Refresh")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
                    1 -> loadUsers()
                }
            }.show()
    }
}

class AdminUsersAdapter(
    private val users: List<AppUser>,
    private val onViewOrders:   (AppUser) -> Unit,
    private val onStatusToggle: (AppUser) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:       TextView = view.findViewById(R.id.tvUserName)
        val tvCity:       TextView = view.findViewById(R.id.tvUserCity)
        val tvEmail:      TextView = view.findViewById(R.id.tvUserEmail)
        val tvMobile:     TextView = view.findViewById(R.id.tvUserMobile)
        val tvStatus:     TextView = view.findViewById(R.id.tvUserStatus)
        val btnViewOrders:Button   = view.findViewById(R.id.btnViewOrders)
        val btnEdit:      Button   = view.findViewById(R.id.btnEditUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.tvName.text   = user.name
        holder.tvCity.text   = user.city.ifEmpty { "India" }
        holder.tvEmail.text  = user.email
        holder.tvMobile.text = if (user.mobile.isNotEmpty()) "+91 ${user.mobile}" else "No mobile"

        val isActive = user.status != "suspended"
        holder.tvStatus.text = if (isActive) "ACTIVE" else "SUSPENDED"
        holder.tvStatus.setBackgroundColor(
            Color.parseColor(if (isActive) "#4CAF50" else "#F44336")
        )

        holder.btnViewOrders.setOnClickListener { onViewOrders(user) }
        holder.btnEdit.setOnClickListener { onStatusToggle(user) }
    }

    override fun getItemCount() = users.size
}