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
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class AdminItemsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvItems: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText

    private val allItems = mutableListOf<MenuItemData>()
    private val filteredItems = mutableListOf<MenuItemData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_items)

        db          = FirebaseFirestore.getInstance()
        rvItems     = findViewById(R.id.rvAdminItems)
        tvEmpty     = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        etSearch    = findViewById(R.id.etSearchItems)

        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.setHasFixedSize(true)

        setupBottomNavigation()
        loadItems()

        // Category filter tabs
        listOf(
            R.id.tabAll         to "all",
            R.id.tabMainCourse  to "north_indian",
            R.id.tabSnacks      to "street_food",
            R.id.tabDesserts    to "east_indian"
        ).forEach { (id, key) ->
            findViewById<TextView>(id).setOnClickListener { filterByCategory(key) }
        }

        // Search
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { searchItems(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // FAB Add
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddItem)
            .setOnClickListener { showItemDialog(null) }
    }

    override fun onResume() { super.onResume(); loadItems() }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navAdminDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        findViewById<LinearLayout>(R.id.navAdminOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminItems).setOnClickListener { /* already here */ }
        findViewById<LinearLayout>(R.id.navAdminUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAdminProfile).setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
    }

    private fun loadItems() {
        progressBar.visibility = View.VISIBLE
        db.collection("menu").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                allItems.clear()
                for (doc in result.documents) {
                    allItems.add(MenuItemData(
                        id          = doc.id,
                        name        = doc.getString("name")        ?: "",
                        description = doc.getString("description") ?: "",
                        price       = doc.getDouble("price")       ?: 0.0,
                        imageUrl    = doc.getString("imageUrl")    ?: "",
                        category    = doc.getString("category")    ?: "",
                        prepTime    = doc.getString("prepTime")    ?: "20 mins",
                        rating      = doc.getDouble("rating")      ?: 4.0,
                        available   = doc.getBoolean("available")  ?: true
                    ))
                }
                filteredItems.clear()
                filteredItems.addAll(allItems)
                updateRecycler()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterByCategory(key: String) {
        filteredItems.clear()
        filteredItems.addAll(if (key == "all") allItems else allItems.filter { it.category == key })
        updateRecycler()
    }

    private fun searchItems(query: String) {
        filteredItems.clear()
        filteredItems.addAll(
            if (query.isEmpty()) allItems
            else allItems.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        )
        updateRecycler()
    }

    private fun updateRecycler() {
        if (filteredItems.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            rvItems.visibility  = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            rvItems.visibility  = View.VISIBLE
            rvItems.adapter = AdminItemsAdapter(
                filteredItems,
                onEdit            = { showItemDialog(it) },
                onDelete          = { deleteItem(it) },
                onToggleAvailable = { toggleAvailable(it) }
            )
        }
    }

    private fun showItemDialog(existing: MenuItemData?) {
        val isEdit     = existing != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_item, null)

        val etName      = dialogView.findViewById<EditText>(R.id.etItemName)
        val etDesc      = dialogView.findViewById<EditText>(R.id.etItemDesc)
        val etPrice     = dialogView.findViewById<EditText>(R.id.etItemPrice)
        val spinCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etPrepTime  = dialogView.findViewById<EditText>(R.id.etItemPrepTime)
        val etRating    = dialogView.findViewById<EditText>(R.id.etItemRating)
        val etImageUrl  = dialogView.findViewById<EditText>(R.id.etItemImageUrl)
        val ivPreview   = dialogView.findViewById<ImageView>(R.id.ivItemImagePreview)
        val swAvailable = dialogView.findViewById<Switch>(R.id.switchAvailable)
        val swPopular   = dialogView.findViewById<Switch>(R.id.switchPopular)

        // Category spinner
        val categories = arrayOf("north_indian", "east_indian", "street_food")
        val catLabels  = arrayOf("North Indian (Main Course)", "East Indian (Regional)", "Street Food (Snacks)")
        spinCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, catLabels)

        existing?.let {
            etName.setText(it.name)
            etDesc.setText(it.description)
            etPrice.setText(it.price.toString())
            etPrepTime.setText(it.prepTime)
            etRating.setText(it.rating.toString())
            etImageUrl.setText(it.imageUrl)
            swAvailable.isChecked = it.available
            spinCategory.setSelection(categories.indexOf(it.category).coerceAtLeast(0))
            if (it.imageUrl.isNotEmpty()) {
                Glide.with(this).load(it.imageUrl).centerCrop().into(ivPreview)
            }
        }

        // Live image preview
        etImageUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) Glide.with(this@AdminItemsActivity).load(url).centerCrop().into(ivPreview)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Dish" else "Add New Dish")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Save Dish" else "Add Dish") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

                val data = hashMapOf(
                    "name"        to name,
                    "description" to etDesc.text.toString().trim(),
                    "price"       to (etPrice.text.toString().toDoubleOrNull() ?: 0.0),
                    "category"    to categories[spinCategory.selectedItemPosition],
                    "prepTime"    to etPrepTime.text.toString().trim().ifEmpty { "20 mins" },
                    "rating"      to (etRating.text.toString().toDoubleOrNull() ?: 4.0),
                    "available"   to swAvailable.isChecked,
                    "popular"     to swPopular.isChecked,
                    "imageUrl"    to etImageUrl.text.toString().trim()
                )

                val col = db.collection("menu")
                if (isEdit && existing != null) {
                    col.document(existing.id).set(data)
                        .addOnSuccessListener { Toast.makeText(this, "Updated ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                        .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                } else {
                    col.add(data)
                        .addOnSuccessListener { Toast.makeText(this, "Added ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                        .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: MenuItemData) {
        AlertDialog.Builder(this)
            .setTitle("Delete Dish")
            .setMessage("Delete \"${item.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("menu").document(item.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Deleted ✓", Toast.LENGTH_SHORT).show(); loadItems() }
                    .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleAvailable(item: MenuItemData) {
        db.collection("menu").document(item.id)
            .update("available", !item.available)
            .addOnSuccessListener { loadItems() }
            .addOnFailureListener { e -> Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
    }
}

class AdminItemsAdapter(
    private val items: List<MenuItemData>,
    private val onEdit:            (MenuItemData) -> Unit,
    private val onDelete:          (MenuItemData) -> Unit,
    private val onToggleAvailable: (MenuItemData) -> Unit
) : RecyclerView.Adapter<AdminItemsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage:    ImageView = view.findViewById(R.id.ivAdminItemImage)
        val tvPrice:    TextView  = view.findViewById(R.id.tvAdminItemPrice)
        val tvVegBadge: TextView  = view.findViewById(R.id.tvVegBadge)
        val tvName:     TextView  = view.findViewById(R.id.tvAdminItemName)
        val tvCategory: TextView  = view.findViewById(R.id.tvAdminItemCategory)
        val tvAvail:    TextView  = view.findViewById(R.id.tvAvailLabel)
        val swAvail:    Switch    = view.findViewById(R.id.swAdminAvail)
        val btnEdit:    ImageView = view.findViewById(R.id.btnAdminEdit)
        val btnDelete:  ImageView = view.findViewById(R.id.btnAdminDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_menu, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text     = item.name
        holder.tvPrice.text    = "₹%.0f".format(item.price)
        holder.tvCategory.text = "Category: ${item.category.replace("_", " ").replaceFirstChar { it.uppercase() }}"

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder).centerCrop().into(holder.ivImage)
        }

        // Veg/non-veg badge based on category
        val isVeg = item.category != "non_veg"
        holder.tvVegBadge.text = if (isVeg) "VEG" else "NON-VEG"
        holder.tvVegBadge.setBackgroundColor(if (isVeg) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

        holder.tvAvail.text = if (item.available) "Available" else "Unavailable"
        holder.tvAvail.setTextColor(if (item.available) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        holder.swAvail.isChecked = item.available
        holder.swAvail.setOnCheckedChangeListener(null)
        holder.swAvail.setOnCheckedChangeListener { _, _ -> onToggleAvailable(item) }

        holder.btnEdit.setOnClickListener   { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}