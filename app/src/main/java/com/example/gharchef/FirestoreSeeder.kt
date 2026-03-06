package com.example.gharchef


import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FirestoreSeeder
 *
 * Run this ONCE to populate your Firestore database with sample menu items.
 * Call FirestoreSeeder.seedAll(db) from any Activity (e.g. MainActivity or AdminDashboard).
 * After seeding, remove the call so it doesn't overwrite data on every launch.
 */
object FirestoreSeeder {

    private const val TAG = "SEEDER"

    fun seedAll(db: FirebaseFirestore) {
        seedMenuItems(db)
    }

    private fun seedMenuItems(db: FirebaseFirestore) {
        val menuItems = listOf(

            // ── NORTH INDIAN ──────────────────────────────────────
            hashMapOf(
                "name" to "Paneer Butter Masala",
                "description" to "Creamy tomato-based curry with soft paneer cubes. Best served with naan.",
                "price" to 280.0,
                "category" to "north_indian",
                "prepTime" to "25 mins",
                "rating" to 4.8,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Dal Makhani",
                "description" to "Slow-cooked black lentils in rich buttery gravy, a Punjab classic.",
                "price" to 220.0,
                "category" to "north_indian",
                "prepTime" to "35 mins",
                "rating" to 4.7,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Butter Chicken",
                "description" to "Tender chicken in smooth, mildly spiced tomato-butter sauce.",
                "price" to 320.0,
                "category" to "north_indian",
                "prepTime" to "30 mins",
                "rating" to 4.9,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Chole Bhature",
                "description" to "Spicy chickpea curry served with fluffy deep-fried bread.",
                "price" to 180.0,
                "category" to "north_indian",
                "prepTime" to "20 mins",
                "rating" to 4.6,
                "available" to true,
                "popular" to false,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Aloo Paratha",
                "description" to "Whole wheat flatbread stuffed with spiced mashed potatoes.",
                "price" to 120.0,
                "category" to "north_indian",
                "prepTime" to "15 mins",
                "rating" to 4.5,
                "available" to true,
                "popular" to false,
                "imageUrl" to ""
            ),

            // ── EAST INDIAN (REGIONAL) ────────────────────────────
            hashMapOf(
                "name" to "Machher Jhol",
                "description" to "Bengali light fish curry with potatoes in turmeric-mustard gravy.",
                "price" to 260.0,
                "category" to "east_indian",
                "prepTime" to "30 mins",
                "rating" to 4.6,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Litti Chokha",
                "description" to "Baked wheat balls with spiced sattu filling, served with roasted eggplant.",
                "price" to 150.0,
                "category" to "east_indian",
                "prepTime" to "40 mins",
                "rating" to 4.4,
                "available" to true,
                "popular" to false,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Kosha Mangsho",
                "description" to "Slow-cooked spicy Bengali mutton curry with caramelized onions.",
                "price" to 380.0,
                "category" to "east_indian",
                "prepTime" to "60 mins",
                "rating" to 4.8,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Mediterranean Bowl",
                "description" to "Hummus, falafel, roasted veggies, and pita — a wholesome meal kit.",
                "price" to 320.0,
                "category" to "east_indian",
                "prepTime" to "15 mins",
                "rating" to 4.5,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),

            // ── STREET FOOD (FAST COOKING) ────────────────────────
            hashMapOf(
                "name" to "Pav Bhaji",
                "description" to "Spiced mashed vegetable curry served with buttered pav buns.",
                "price" to 130.0,
                "category" to "street_food",
                "prepTime" to "15 mins",
                "rating" to 4.7,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Vada Pav",
                "description" to "Mumbai's favourite — spiced potato fritter in a soft bun with chutneys.",
                "price" to 60.0,
                "category" to "street_food",
                "prepTime" to "10 mins",
                "rating" to 4.5,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Pani Puri",
                "description" to "Crispy hollow puris filled with spiced water, tamarind, and chaat masala.",
                "price" to 80.0,
                "category" to "street_food",
                "prepTime" to "5 mins",
                "rating" to 4.8,
                "available" to true,
                "popular" to true,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Quick Garden Salad",
                "description" to "Fresh seasonal vegetables with lemon dressing. Light and healthy.",
                "price" to 110.0,
                "category" to "street_food",
                "prepTime" to "5 mins",
                "rating" to 4.3,
                "available" to true,
                "popular" to false,
                "imageUrl" to ""
            ),
            hashMapOf(
                "name" to "Spicy Dal Tadka",
                "description" to "Hearty yellow lentil comfort meal with a smoky tempering.",
                "price" to 160.0,
                "category" to "street_food",
                "prepTime" to "20 mins",
                "rating" to 4.6,
                "available" to true,
                "popular" to false,
                "imageUrl" to ""
            )
        )

        var successCount = 0
        var failCount = 0
        val total = menuItems.size

        for (item in menuItems) {
            db.collection("menu")
                .add(item)
                .addOnSuccessListener {
                    successCount++
                    Log.d(TAG, "✅ Seeded: ${item["name"]} ($successCount/$total)")
                    if (successCount + failCount == total) {
                        Log.d(TAG, "🎉 Seeding complete! $successCount succeeded, $failCount failed.")
                    }
                }
                .addOnFailureListener { e ->
                    failCount++
                    Log.e(TAG, "❌ Failed to seed ${item["name"]}: ${e.message}")
                }
        }
    }
}