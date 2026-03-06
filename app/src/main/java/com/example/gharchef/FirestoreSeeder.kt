package com.example.gharchef

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreSeeder {

    fun seedAll(db: FirebaseFirestore) {
        val items = listOf(
            mapOf("name" to "Paneer Butter Masala", "description" to "Rich, creamy tomato gravy with soft paneer cubes, finished with Kasuri Methi and fresh cream.", "price" to 280.0, "category" to "north_indian", "prepTime" to "30 mins", "rating" to 4.8, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=400"),
            mapOf("name" to "Dal Makhani", "description" to "Slow-cooked black lentils in a rich buttery tomato sauce, simmered overnight for deep flavor.", "price" to 220.0, "category" to "north_indian", "prepTime" to "40 mins", "rating" to 4.7, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400"),
            mapOf("name" to "Butter Chicken", "description" to "Tender chicken in a velvety, mildly spiced tomato-butter sauce — the most loved Indian dish.", "price" to 320.0, "category" to "north_indian", "prepTime" to "35 mins", "rating" to 4.9, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=400"),
            mapOf("name" to "Jeera Rice", "description" to "Fluffy basmati rice tempered with cumin seeds, ghee, and whole spices.", "price" to 120.0, "category" to "north_indian", "prepTime" to "15 mins", "rating" to 4.5, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=400"),
            mapOf("name" to "Butter Naan", "description" to "Soft, fluffy leavened bread baked in tandoor, brushed with fresh butter.", "price" to 45.0, "category" to "street_food", "prepTime" to "10 mins", "rating" to 4.6, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1505253716362-afaea1d3d1af?w=400"),
            mapOf("name" to "Classic Samosa (2pcs)", "description" to "Golden, crispy triangles filled with spiced potatoes and peas. Served with green chutney.", "price" to 40.0, "category" to "street_food", "prepTime" to "15 mins", "rating" to 4.7, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=400"),
            mapOf("name" to "Pav Bhaji", "description" to "Mumbai's favorite street food — spiced mashed vegetables served with buttered pav.", "price" to 140.0, "category" to "street_food", "prepTime" to "20 mins", "rating" to 4.8, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=400"),
            mapOf("name" to "Chole Bhature", "description" to "Fluffy deep-fried bread with hearty spiced chickpea curry — a Punjabi classic.", "price" to 180.0, "category" to "north_indian", "prepTime" to "25 mins", "rating" to 4.7, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1626200926093-e2b738cfad7f?w=400"),
            mapOf("name" to "Matar Paneer", "description" to "Green peas and paneer in a mildly spiced tomato and onion gravy.", "price" to 240.0, "category" to "north_indian", "prepTime" to "25 mins", "rating" to 4.4, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400"),
            mapOf("name" to "Gulab Jamun (4pcs)", "description" to "Soft, melt-in-mouth milk solid dumplings soaked in rose-flavored sugar syrup.", "price" to 120.0, "category" to "east_indian", "prepTime" to "20 mins", "rating" to 4.9, "available" to false, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1666365434-b64ffb2fd45f?w=400"),
            mapOf("name" to "Kheer", "description" to "Creamy rice pudding with saffron, cardamom, almonds, and raisins. Bengali-style.", "price" to 100.0, "category" to "east_indian", "prepTime" to "30 mins", "rating" to 4.6, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1695158568573-3f40e0fb0ef8?w=400"),
            mapOf("name" to "Hyderabadi Biryani", "description" to "Aromatic long-grain rice layered with slow-cooked mutton, saffron, and fried onions.", "price" to 380.0, "category" to "east_indian", "prepTime" to "50 mins", "rating" to 4.9, "available" to true, "popular" to true, "imageUrl" to "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=400"),
            mapOf("name" to "Paneer Tikka", "description" to "Marinated paneer cubes grilled in tandoor with bell peppers and onions.", "price" to 240.0, "category" to "north_indian", "prepTime" to "25 mins", "rating" to 4.7, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=400"),
            mapOf("name" to "Dal Tadka", "description" to "Yellow lentils tempered with cumin, mustard seeds, and dry red chilies.", "price" to 180.0, "category" to "north_indian", "prepTime" to "20 mins", "rating" to 4.5, "available" to true, "popular" to false, "imageUrl" to "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400")
        )

        val col = db.collection("menu")
        for (item in items) {
            col.add(item)
                .addOnSuccessListener { Log.d("Seeder", "Added: ${item["name"]}") }
                .addOnFailureListener { e -> Log.e("Seeder", "Failed: ${e.message}") }
        }
    }
}