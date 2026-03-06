package com.example.gharchef.adapter                  // ← adapter package

import android.view.LayoutInflater                    // ← inflate layout
import android.view.View                              // ← View
import android.view.ViewGroup                         // ← ViewGroup
import android.widget.Button                          // ← Button
import android.widget.ImageView                       // ← ImageView
import android.widget.TextView                        // ← TextView
import androidx.recyclerview.widget.RecyclerView      // ← RecyclerView
import com.bumptech.glide.Glide                       // ← Glide image loader
import com.example.gharchef.R                         // ← your R file
import com.example.gharchef.model.Product             // ← Product model

class ProductAdapter(
    private val products: MutableList<Product>,
    private val onAddToCart: (Product) -> Unit,
    private val onFavouriteClick: (Product, Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvCategory: TextView      = itemView.findViewById(R.id.tvProductCategory)
        val tvTimeBadge: TextView     = itemView.findViewById(R.id.tvTimeBadge)
        val ivVegIndicator: ImageView = itemView.findViewById(R.id.ivVegIndicator)
        val ivHeart: ImageView        = itemView.findViewById(R.id.ivHeart)
        val tvTitle: TextView         = itemView.findViewById(R.id.tvProductTitle)
        val tvPrice: TextView         = itemView.findViewById(R.id.tvProductPrice)
        val tvRatingStars: TextView   = itemView.findViewById(R.id.tvRatingStars)
        val tvRatingCount: TextView   = itemView.findViewById(R.id.tvRatingCount)
        val tvDescription: TextView   = itemView.findViewById(R.id.tvProductDescription)
        val btnAddToCart: Button      = itemView.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvTitle.text       = product.name
        holder.tvPrice.text       = product.price
        holder.tvCategory.text    = product.category
        holder.tvTimeBadge.text   = product.cookTime
        holder.tvDescription.text = product.description
        holder.tvRatingStars.text = "★ ${product.rating}"
        holder.tvRatingCount.text = "(${product.ratingCount}+ reviews)"

        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(holder.ivProductImage)

        // Veg / Non-veg indicator
        holder.ivVegIndicator.setImageResource(
            if (product.isVeg) R.drawable.ic_veg_indicator
            else R.drawable.ic_nonveg_indicator
        )

        // Heart toggle
        holder.ivHeart.setImageResource(
            if (product.isFavourite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
        holder.ivHeart.setOnClickListener {
            onFavouriteClick(product, position)
        }

        // Add to cart
        holder.btnAddToCart.setOnClickListener {
            onAddToCart(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun addProduct(product: Product) {
        products.add(0, product)
        notifyItemInserted(0)
    }

    fun toggleFavourite(position: Int) {
        val updated = products[position].copy(
            isFavourite = !products[position].isFavourite
        )
        products[position] = updated
        notifyItemChanged(position)
    }
}