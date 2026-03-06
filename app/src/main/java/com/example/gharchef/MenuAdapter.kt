package com.example.gharchef

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private val items: List<MenuItemData>,
    private val onAddToCartClick: (MenuItemData) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMenuItemName)
        val tvDesc: TextView = view.findViewById(R.id.tvMenuItemDesc)
        val tvPrice: TextView = view.findViewById(R.id.tvMenuItemPrice)
        val tvPrepTime: TextView = view.findViewById(R.id.tvMenuItemPrepTime)
        val tvRating: TextView = view.findViewById(R.id.tvMenuItemRating)
        val btnAdd: Button = view.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name
        holder.tvDesc.text = item.description
        holder.tvPrice.text = "₹${item.price}"
        holder.tvPrepTime.text = "⏱ ${item.cookingTime}"
        holder.tvRating.text = "★ 4.0" // Static for now (you can connect Firestore rating later)

        holder.btnAdd.setOnClickListener {
            onAddToCartClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}