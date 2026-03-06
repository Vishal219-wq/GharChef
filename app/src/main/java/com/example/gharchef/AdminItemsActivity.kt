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

class AdminItemsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvItems: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAddItem: Button

    private val items = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_items)

        db          = FirebaseFirestore.getInstance()
        rvItems     = findViewById(R.id.rvAdminItems)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        btnAddItem  = findViewById(R.id.btnAddItem)

        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.setHasFixedSize(true)   // perf optimisation

        setupBottomNavigation()
        loadItems()

        btnAddItem.setOnClickListener { showItemDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener {
            startActivity(
                Intent(this, AdminDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadItems() {
        progressBar.visibility = View.VISIBLE
        db.collection("menu").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                items.clear()
                for (doc in result.documents) {
                    items.add(
                        MenuItem(
                            id          = doc.id,
                            name        = doc.getString("name")        ?: "",
                            description = doc.getString("description") ?: "",
                            price       = doc.getDouble("price")       ?: 0.0,
                            imageUrl    = doc.getString("imageUrl")    ?: "",
                            category    = doc.getString("category")    ?: "",
                            prepTime    = doc.getString("prepTime")    ?: "20 mins",
                            rating      = doc.getDouble("rating")      ?: 4.0,
                            available   = doc.getBoolean("available")  ?: true
                        )
                    )
                }
                if (items.isEmpty()) {
                    tvEmpty.visibility  = View.VISIBLE
                    rvItems.visibility  = View.GONE
                } else {
                    tvEmpty.visibility  = View.GONE
                    rvItems.visibility  = View.VISIBLE
                    rvItems.adapter     = AdminItemsAdapter(
                        items,
                        onEdit            = { showItemDialog(it) },
                        onDelete          = { deleteItem(it) },
                        onToggleAvailable = { toggleAvailable(it) }
                    )
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showItemDialog(existing: MenuItem?) {
        val isEdit     = existing != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_item, null)

        val etName        = dialogView.findViewById<EditText>(R.id.etItemName)
        val etDesc        = dialogView.findViewById<EditText>(R.id.etItemDesc)
        val etPrice       = dialogView.findViewById<EditText>(R.id.etItemPrice)
        val etCategory    = dialogView.findViewById<EditText>(R.id.etItemCategory)
        val etPrepTime    = dialogView.findViewById<EditText>(R.id.etItemPrepTime)
        val etRating      = dialogView.findViewById<EditText>(R.id.etItemRating)
        val swAvailable   = dialogView.findViewById<Switch>(R.id.switchAvailable)
        val swPopular     = dialogView.findViewById<Switch>(R.id.switchPopular)

        existing?.let {
            etName.setText(it.name)
            etDesc.setText(it.description)
            etPrice.setText(it.price.toString())
            etCategory.setText(it.category)
            etPrepTime.setText(it.prepTime)
            etRating.setText(it.rating.toString())
            swAvailable.isChecked = it.available
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Item" else "Add New Item")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Update" else "Add") { _, _ ->
                val name     = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val data = hashMapOf(
                    "name"        to name,
                    "description" to etDesc.text.toString().trim(),
                    "price"       to (etPrice.text.toString().toDoubleOrNull() ?: 0.0),
                    "category"    to etCategory.text.toString().trim(),
                    "prepTime"    to etPrepTime.text.toString().trim().ifEmpty { "20 mins" },
                    "rating"      to (etRating.text.toString().toDoubleOrNull() ?: 4.0),
                    "available"   to swAvailable.isChecked,
                    "popular"     to swPopular.isChecked,
                    "imageUrl"    to (existing?.imageUrl ?: "")
                )
                val col = db.collection("menu")
                if (isEdit && existing != null) {
                    col.document(existing.id).set(data)
                        .addOnSuccessListener { Toast.makeText(this, "Item updated ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                        .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                } else {
                    col.add(data)
                        .addOnSuccessListener { Toast.makeText(this, "Item added ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                        .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Delete \"${item.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("menu").document(item.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Deleted ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                    .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleAvailable(item: MenuItem) {
        db.collection("menu").document(item.id)
            .update("available", !item.available)
            .addOnSuccessListener { loadItems() }
            .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
    }
}

class AdminItemsAdapter(
    private val items: List<MenuItem>,
    private val onEdit:            (MenuItem) -> Unit,
    private val onDelete:          (MenuItem) -> Unit,
    private val onToggleAvailable: (MenuItem) -> Unit
) : RecyclerView.Adapter<AdminItemsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:     TextView = view.findViewById(R.id.tvAdminItemName)
        val tvCategory: TextView = view.findViewById(R.id.tvAdminItemCategory)
        val tvPrice:    TextView = view.findViewById(R.id.tvAdminItemPrice)
        val tvStatus:   TextView = view.findViewById(R.id.tvAdminItemStatus)
        val btnEdit:    Button   = view.findViewById(R.id.btnEditItem)
        val btnDelete:  Button   = view.findViewById(R.id.btnDeleteItem)
        val btnToggle:  Button   = view.findViewById(R.id.btnToggleItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_menu, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text     = item.name
        holder.tvCategory.text = item.category.replace("_", " ").replaceFirstChar { it.uppercase() }
        holder.tvPrice.text    = "₹%.2f".format(item.price)

        if (item.available) {
            holder.tvStatus.text      = "Available"
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_green)
            holder.btnToggle.text     = "Disable"
        } else {
            holder.tvStatus.text      = "Unavailable"
            holder.tvStatus.setTextColor(Color.parseColor("#C62828"))
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_red)
            holder.btnToggle.text     = "Enable"
        }

        holder.btnEdit.setOnClickListener   { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.btnToggle.setOnClickListener { onToggleAvailable(item) }
    }

    override fun getItemCount() = items.size
}