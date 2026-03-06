package com.example.gharchef

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

data class AdminUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val city: String = ""
)

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvUserCount: TextView

    private val users = mutableListOf<AdminUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        db = FirebaseFirestore.getInstance()

        rvUsers = findViewById(R.id.rvAdminUsers)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        tvUserCount = findViewById(R.id.tvUserCount)

        rvUsers.layoutManager = LinearLayoutManager(this)

        setupBottomNavigation()
        loadUsers()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener {
            startActivity(Intent(this, AdminItemsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        db.collection("users").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                users.clear()
                for (doc in result.documents) {
                    users.add(AdminUser(
                        uid = doc.id,
                        name = doc.getString("name") ?: "No Name",
                        email = doc.getString("email") ?: "",
                        mobile = doc.getString("mobile") ?: "",
                        city = doc.getString("city") ?: ""
                    ))
                }
                tvUserCount.text = "${users.size} users registered"
                if (users.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvUsers.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvUsers.visibility = View.VISIBLE
                    rvUsers.adapter = AdminUsersAdapter(users) { viewUserOrders(it) }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewUserOrders(user: AdminUser) {
        Toast.makeText(this, "Showing orders for ${user.name}", Toast.LENGTH_SHORT).show()
        // Could launch a filtered orders view
    }
}

class AdminUsersAdapter(
    private val users: List<AdminUser>,
    private val onView: (AdminUser) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvMobile: TextView = view.findViewById(R.id.tvUserMobile)
        val tvCity: TextView = view.findViewById(R.id.tvUserCity)
        val btnView: Button = view.findViewById(R.id.btnViewOrders)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name.ifEmpty { "No Name" }
        holder.tvEmail.text = user.email.ifEmpty { "No email" }
        holder.tvMobile.text = if (user.mobile.isNotEmpty()) "📱 ${user.mobile}" else "No phone"
        holder.tvCity.text = if (user.city.isNotEmpty()) "📍 ${user.city}" else "No city"
        holder.btnView.setOnClickListener { onView(user) }
    }

    override fun getItemCount() = users.size
}