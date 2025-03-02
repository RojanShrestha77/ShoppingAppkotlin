package com.example.shoppingapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shoppingapp.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> get() = _products

    private val _cartItems = MutableLiveData<List<Product>>(emptyList())
    val cartItems: LiveData<List<Product>> get() = _cartItems

    init {
        fetchProducts()
        loadUserCart()
    }

    private fun fetchProducts() {
        db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w("ProductViewModel", "Error fetching products: $error")
                    _products.value = emptyList()
                    return@addSnapshotListener
                }
                val productList = snapshot.toObjects(Product::class.java)
                Log.d("ProductViewModel", "Fetched ${productList.size} products")
                _products.value = productList
            }
    }

    private fun loadUserCart() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).collection("cart")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("ProductViewModel", "Error loading cart: $error")
                        _cartItems.value = emptyList()
                        return@addSnapshotListener
                    }
                    val cartList = snapshot.toObjects(Product::class.java)
                    Log.d("ProductViewModel", "Loaded cart with ${cartList.size} items")
                    _cartItems.value = cartList
                }
        }
    }

    fun addToCart(product: Product) {
        val user = auth.currentUser
        if (user != null) {
            val currentCart = _cartItems.value?.toMutableList() ?: mutableListOf()
            val existingProduct = currentCart.find { it.id == product.id }
            if (existingProduct != null) {
                existingProduct.quantity += 1
                Log.d("Cart", "Updated ${existingProduct.name} to quantity ${existingProduct.quantity}")
            } else {
                currentCart.add(product.copy(quantity = 1))
                Log.d("Cart", "Added ${product.name} with quantity 1")
            }
            _cartItems.value = currentCart
            db.collection("users").document(user.uid).collection("cart")
                .document(product.id)
                .set(product.copy(quantity = currentCart.find { it.id == product.id }?.quantity ?: 1))
        } else {
            Log.w("Cart", "No user logged in, cannot add to cart")
        }
    }

    fun buyCart() {
        val user = auth.currentUser
        if (user != null) {
            Log.d("Cart", "Buying cart with ${cartItems.value?.size ?: 0} items")
            _cartItems.value = emptyList()
            db.collection("users").document(user.uid).collection("cart")
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                }
        }
    }

    fun getCartItemCount(): Int = _cartItems.value?.sumOf { it.quantity } ?: 0
}