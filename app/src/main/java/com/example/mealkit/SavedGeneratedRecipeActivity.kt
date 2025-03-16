package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealkit.databinding.ActivityGeneratedRecipeBinding
import com.google.firebase.database.*

class SavedGeneratedRecipeActivity : AppCompatActivity(), SavedGeneratedRecipeItemAdapter.OnItemClickListener {

    private lateinit var binding: ActivityGeneratedRecipeBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var recipeList: MutableList<RecipeData>
    private lateinit var adapter: SavedGeneratedRecipeItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the recipe list and adapter
        recipeList = mutableListOf()
        adapter = SavedGeneratedRecipeItemAdapter(recipeList, this)

        // Set up RecyclerView with the adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        fetchGeneratedRecipes()
    }

    private fun fetchGeneratedRecipes() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        binding.progressBar.visibility = View.VISIBLE

        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().reference
            val userGeneratedRecipesRef = databaseReference.child("users").child(userId).child("userGeneratedRecipes")

            userGeneratedRecipesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    recipeList.clear()
                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(RecipeData::class.java)
                        recipe?.let { recipeList.add(it) }
                    }

                    adapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE

                    // Show empty message if the list is empty
                    binding.emptySavedListMessage.visibility = if (recipeList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SavedGeneratedRecipeActivity", "Failed to retrieve data", error.toException())
                    binding.progressBar.visibility = View.GONE
                }
            })
        } else {
            Log.e("SavedGeneratedRecipeActivity", "User ID is null")
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onItemClick(recipe: RecipeData) {
        val intent = Intent(this, GeneratedRecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_DATA", recipe)
        }
        startActivity(intent)
    }
    override fun onDeleteClick(recipe: RecipeData) {
        // Show confirmation dialog before deleting
        AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe?")
            .setPositiveButton("Yes") { dialog, _ ->
                // Perform the delete operation if confirmed
                deleteRecipe(recipe)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                // Dismiss the dialog if canceled
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun deleteRecipe(recipe: RecipeData) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val userGeneratedRecipesRef = databaseReference.child("users").child(userId).child("userGeneratedRecipes")
            userGeneratedRecipesRef.child(recipe.id!!).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Remove the recipe from the local list and notify the adapter
                    recipeList.remove(recipe)
                    adapter.notifyDataSetChanged()

                    // Show empty message if the list is empty
                    binding.emptySavedListMessage.visibility = if (recipeList.isEmpty()) View.VISIBLE else View.GONE
                    Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("SavedGeneratedRecipeActivity", "Failed to delete recipe")
                }
            }
        } else {
            Log.e("SavedGeneratedRecipeActivity", "User ID is null")
        }
    }
}
