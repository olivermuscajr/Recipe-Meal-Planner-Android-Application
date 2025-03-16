package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class MyRecipeActivity : AppCompatActivity(), RecipeAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var AddRecipebutton: Button
    private lateinit var backButton: ImageButton
    private lateinit var emptyTextView: TextView
    private var allRecipes: MutableList<MyRecipe> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recipe)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recipeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList(), this)
        recyclerView.adapter = adapter
        emptyTextView = findViewById(R.id.emptyTextView)

        AddRecipebutton = findViewById(R.id.addRecipe)
        AddRecipebutton.setOnClickListener {
            val intent = Intent(this, CreateRecipeActivity::class.java)
            startActivity(intent)
        }
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Initialize Meal Type Spinner
        val mealTypeSpinner: Spinner = findViewById(R.id.mealTypeSpinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.meal_types_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mealTypeSpinner.adapter = adapter
        }

        // Add a listener to the Meal Type Spinner
        mealTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMealType = parent?.getItemAtPosition(position).toString()
                val filteredRecipes = filterRecipes(selectedMealType)
                adapter.updateRecipes(filteredRecipes)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing if nothing is selected
            }
        }

        // Retrieve logged-in user ID from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            // Initialize Firebase Database reference
            databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("userRecipes")
            // Retrieve recipe data from Firebase
            retrieveRecipeData()
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveRecipeData() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allRecipes.clear()
                for (recipeSnapshot in dataSnapshot.children) {
                    val recipe = recipeSnapshot.getValue(MyRecipe::class.java)
                    recipe?.let {
                        it.id = recipeSnapshot.key
                        it.userId = databaseReference.parent!!.key // Assuming userId is the parent key
                        allRecipes.add(it)
                    }
                }

                // Update the RecyclerView with the data
                adapter.updateRecipes(allRecipes)

                // Show or hide the empty message based on list size
                if (allRecipes.isEmpty()) {
                    emptyTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MyRecipeActivity, "Failed to retrieve recipes: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterRecipes(selectedMealType: String): List<MyRecipe> {
        return if (selectedMealType == "All") {
            allRecipes
        } else {
            allRecipes.filter { it.mealType == selectedMealType }
        }
    }

    override fun onItemClick(recipe: MyRecipe) {
        val intent = Intent(this, MyRecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_EXTRA", recipe)
            putExtra("RECIPE_ID", recipe.id)
            putExtra("USER_ID", recipe.userId)
            putExtra("IMAGE_URL", recipe.image) // Pass the image URL to MyRecipeDetailActivity
        }
        startActivity(intent)
    }
    override fun onDeleteClick(recipe: MyRecipe) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirm Deletion")
        alertDialogBuilder.setMessage("Are you sure you want to delete this recipe?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            // User confirmed deletion
            deleteRecipe(recipe)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            // User cancelled deletion
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteRecipe(recipe: MyRecipe) {
        val recipeId = recipe.id
        if (recipeId!!.isNotEmpty()) {
            // 1. Delete recipe data from Firebase Realtime Database
            databaseReference.child(recipeId).removeValue()
                .addOnSuccessListener {
                    // Recipe data deleted successfully
                    Toast.makeText(this, "Recipe deleted successfully", Toast.LENGTH_SHORT).show()

                    // 2. Delete the image from Firestore Storage if it exists
                    val imageUrl = recipe.image
                    if (!imageUrl.isNullOrEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        storageRef.delete()
                            .addOnSuccessListener {
                                // Image deleted successfully
                                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                // Failed to delete image
                                Toast.makeText(this, "Failed to delete image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to delete recipe data
                    Toast.makeText(this, "Failed to delete recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
