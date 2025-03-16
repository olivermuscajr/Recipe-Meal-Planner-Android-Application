package com.example.mealkit

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class IngredientsActivity : AppCompatActivity() {

    private lateinit var ingredients: List<Ingredient>
    private val selectedIngredients = mutableSetOf<Ingredient>() // Change to mutableSetOf for better performance
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients)

        ingredients = intent.getParcelableArrayListExtra<Ingredient>("INGREDIENTS") ?: emptyList()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = IngredientAdapter(ingredients) { ingredient, isSelected ->
            if (isSelected) {
                selectedIngredients.add(ingredient)
            } else {
                selectedIngredients.remove(ingredient)
            }
        }
        recyclerView.adapter = adapter

        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        backButton.setOnClickListener {
            onBackPressed()
        }
        confirmButton.setOnClickListener {
            if (selectedIngredients.isNotEmpty()) {
                saveIngredientsToFirebase(selectedIngredients.toList())
                onBackPressed()
            } else {
                Toast.makeText(this, "No ingredients selected", Toast.LENGTH_SHORT).show()
            }
        }


        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveIngredientsToFirebase(ingredients: List<Ingredient>) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userGroceryListReference = databaseReference.child("users").child(userId).child("groceryList")

            ingredients.forEach { ingredient ->
                val ingredientKey = ingredient.name ?: "unknown" // Ensure unique key
                userGroceryListReference.child(ingredientKey).setValue(ingredient)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ingredients saved to grocery list", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save ingredients: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

}

