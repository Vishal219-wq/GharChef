package com.example.gharchef                          // ← your package

import com.example.gharchef.model.Product             // ← Product model
import com.google.firebase.firestore.FirebaseFirestore // ← Firestore
import com.google.firebase.storage.FirebaseStorage    // ← Storage

class FirebaseHelper {

    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        const val COLLECTION_PRODUCTS = "products"
    }

    // ── 1. GET ALL PRODUCTS ──────────────────────────────────
    fun getAllProducts(
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                onSuccess(productList)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // ── 2. GET PRODUCTS BY CATEGORY ──────────────────────────
    fun getProductsByCategory(
        category: String,
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                onSuccess(productList)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // ── 3. ADD PRODUCT ───────────────────────────────────────
    fun addProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .add(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // ── 4. UPDATE PRODUCT ────────────────────────────────────
    fun updateProduct(
        productId: String,
        updatedProduct: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .document(productId)
            .set(updatedProduct)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // ── 5. DELETE PRODUCT ────────────────────────────────────
    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .document(productId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // ── 6. REAL-TIME LISTENER ────────────────────────────────
    fun listenToProducts(
        onUpdate: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                val productList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(productList)
            }
    }

    // ── 7. SEARCH PRODUCTS ───────────────────────────────────
    fun searchProducts(
        query: String,
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_PRODUCTS)
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                onSuccess(productList)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}