package com.example.mealkit

import android.widget.AutoCompleteTextView
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddItemIngredient : AppCompatActivity() {


    private lateinit var ingredientReference: DatabaseReference
    private lateinit var doneButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var quantityEditText: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var ingredientEditText: AutoCompleteTextView
    private val ingredientSuggestions: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item_ingredient)


        ingredientReference = FirebaseDatabase.getInstance().getReference("defaultIngredients")
        doneButton = findViewById(R.id.doneButton)
        backButton = findViewById(R.id.backButton)
        ingredientEditText = findViewById(R.id.ingredientEditText)
        quantityEditText = findViewById(R.id.quantityEditText)
        unitSpinner = findViewById(R.id.unitSpinner)

        backButton.setOnClickListener{
            finish()
        }

        doneButton.setOnClickListener {
            saveIngredientToDatabase()
        }

        fetchIngredientSuggestions()
    }

    private fun fetchIngredientSuggestions() {
        ingredientReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ingredientSuggestions.clear()
                for (dataSnapshot in snapshot.children) {
                    val ingredientName = dataSnapshot.child("name").getValue(String::class.java)
                    ingredientName?.let { ingredientSuggestions.add(it) }
                }
                setUpAutoCompleteTextView()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchByIngredientActivity", "Failed to retrieve ingredients", error.toException())
            }
        })
    }

    private fun setUpAutoCompleteTextView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ingredientSuggestions)
        ingredientEditText.setAdapter(adapter)
        ingredientEditText.threshold = 1
    }

    private fun saveIngredientToDatabase() {
        val ingredientName = ingredientEditText.text.toString().trim()
        val quantity = quantityEditText.text.toString().trim()
        val unit = unitSpinner.selectedItem.toString()

        if (ingredientName.isNotEmpty() && quantity.isNotEmpty()) {
            // Retrieve logged-in user ID from SharedPreferences
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId != null) {
                val databaseReference = FirebaseDatabase.getInstance().reference
                val userGroceryListRef = databaseReference.child("users").child(userId).child("groceryList")
                val ingredient = Ingredient(ingredientName, quantity, unit)
                // Save the ingredient to the user's grocery list
                userGroceryListRef.child(ingredientName).setValue(ingredient)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ingredient saved successfully", Toast.LENGTH_SHORT).show()

                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save ingredient: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Added to list successfully", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter valid ingredient details", Toast.LENGTH_SHORT).show()
        }
    }
}
