package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.example.mealkit.databinding.ActivityAddRecipesBinding

class AddRecipesActivity : AppCompatActivity(), RecipeSelectionAdapter.OnRecipeSelectListener {

    private lateinit var binding: ActivityAddRecipesBinding
    private lateinit var adapter: RecipeSelectionAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mealType: String
    private val selectedRecipes = mutableListOf<RecipeData>()
    private val toBeDeletedRecipes = mutableListOf<RecipeData>() // Tracks recipes to delete
    private val existingMealPlanRecipes = mutableSetOf<String>()
    private lateinit var targetMealSection: String // Add this property
    private val selectedItems = mutableMapOf<String, RecipeData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecipesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().reference.child("defaultRecipes")
        targetMealSection = intent.getStringExtra("TARGET_MEAL_SECTION") ?: "Breakfast" // Default to Breakfast

        binding.recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeSelectionAdapter(mutableListOf(), this)
        binding.recipeRecyclerView.adapter = adapter
        mealType = intent.getStringExtra("MEAL_TYPE") ?: ""

        fetchExistingMealPlanRecipes()
        getAllRecipes() // Fetch all recipes
        setupChipGroup() // Initialize chip group listener

        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        binding.doneButton.setOnClickListener {
            saveSelectedRecipesToDatabase()
        }
    }
    private fun setupChipGroup() {
        // Pre-select the chip based on the targetMealSection
        when (targetMealSection) {
            "Breakfast" -> binding.chipGroupMealTypes.check(R.id.chipBreakfast)
            "Lunch" -> binding.chipGroupMealTypes.check(R.id.chipLunch)
            "Dinner" -> binding.chipGroupMealTypes.check(R.id.chipDinner)
            else -> binding.chipGroupMealTypes.check(R.id.chipAll) // Default to 'All' if no match
        }

        // Set up the chip group listener for user interaction
        binding.chipGroupMealTypes.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipAll -> filterRecipesByType("All")
                R.id.chipBreakfast -> filterRecipesByType("Breakfast")
                R.id.chipLunch -> filterRecipesByType("Lunch")
                R.id.chipDinner -> filterRecipesByType("Dinner")
            }
        }
    }

    private fun filterRecipesByType(type: String) {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipeList = mutableListOf<RecipeData>()
                for (dataSnapshot in snapshot.children) {
                    val recipe = dataSnapshot.getValue(RecipeData::class.java)
                    if (recipe != null) {
                        if (type == "All" || recipe.mealType == type) {
                            // Restore selection state from the global map
                            recipe.isSelected = selectedItems.containsKey(recipe.id)
                            recipeList.add(recipe)
                        }
                    }
                }
                adapter.updateRecipes(recipeList) // Update the adapter with filtered recipes
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddRecipesActivity, "Failed to filter recipes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun fetchExistingMealPlanRecipes() {
        val userId = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("loggedInUserId", null)
        val selectedDate = intent.getStringExtra("SELECTED_DATE") ?: ""

        if (userId != null && selectedDate.isNotEmpty()) {
            val mealPlanReference = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)
                .child("mealPlans")
                .child(selectedDate)
                .child(targetMealSection) // Fetch only for the target meal section

            mealPlanReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    existingMealPlanRecipes.clear()
                    for (dataSnapshot in snapshot.children) {
                        val recipeId = dataSnapshot.key
                        recipeId?.let { existingMealPlanRecipes.add(it) }
                    }
                    // Fetch recipes only after meal plan data is ready
                    getAllRecipes()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddRecipesActivity, "Failed to fetch meal plan: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // Fallback if user ID or date is unavailable
            getAllRecipes()
        }
    }



    private fun getAllRecipes() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipeList = mutableListOf<RecipeData>()
                for (dataSnapshot in snapshot.children) {
                    val recipe = dataSnapshot.getValue(RecipeData::class.java)
                    if (recipe != null) {
                        recipe.isSelected = existingMealPlanRecipes.contains(recipe.id) || selectedItems.containsKey(recipe.id)
                        if (recipe.isSelected) {
                            selectedItems[recipe.id!!] = recipe
                        }
                        recipeList.add(recipe)
                    }
                }
                if (::adapter.isInitialized) { // Ensure adapter is initialized
                    adapter.updateRecipes(recipeList)
                } else {
                    Log.e("AddRecipesActivity", "Adapter not initialized")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddRecipesActivity, "Failed to load recipes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    override fun onToggleRecipeInMealPlan(recipe: RecipeData, isSelected: Boolean) {
        if (isSelected) {
            selectedRecipes.add(recipe)
            selectedItems[recipe.id!!] = recipe
        } else {
            selectedRecipes.remove(recipe)
            selectedItems.remove(recipe.id)
        }
    }


    override fun onRecipeSelected(recipe: RecipeData) {
        selectedRecipes.add(recipe) // Add to the list of selected recipes
        selectedItems[recipe.id!!] = recipe // Add to the map of globally selected recipes
    }


    override fun onRecipeDeselected(recipe: RecipeData) {
        selectedRecipes.remove(recipe)
        toBeDeletedRecipes.add(recipe)
    }

    override fun onDeleteRecipe(recipe: RecipeData) {
        // Handle recipe deletion
    }

    // Implement the abstract method from OnRecipeSelectListener
    override fun onItemClick(recipe: RecipeData) {
        // Start RecipeDetailActivity with the selected recipe
        val intent = Intent(this, RecipeDetailActivity::class.java)
        intent.putExtra("RECIPE_DATA", recipe) // Pass the recipe data
        startActivity(intent)
    }

    private fun saveSelectedRecipesToDatabase() {
        val userId = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE).getString("loggedInUserId", null)
        val selectedDate = intent.getStringExtra("SELECTED_DATE") ?: ""

        if (userId != null && selectedDate.isNotEmpty()) {
            val mealPlanReference = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)
                .child("mealPlans")
                .child(selectedDate)
                .child(targetMealSection)

            // Save all selected recipes
            selectedRecipes.forEach { recipe ->
                val recipeDetails = hashMapOf(
                    "description" to recipe.description,
                    "id" to recipe.id,
                    "image" to recipe.image,
                    "ingredients" to recipe.ingredients,
                    "instructions" to recipe.instructions,
                    "mealType" to recipe.mealType,
                    "recipeName" to recipe.recipeName,
                    "servingSize" to recipe.servingSize,
                    "isSelected" to true
                )
                recipe.id?.let { id ->
                    mealPlanReference.child(id).setValue(recipeDetails)
                        .addOnSuccessListener {
                            Log.d("AddRecipesActivity", "Recipe added: ${recipe.recipeName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AddRecipesActivity", "Failed to add recipe: ${e.message}")
                        }
                }
            }

            setResult(RESULT_OK, Intent().apply {
                putExtra("SELECTED_DATE", selectedDate)
                putExtra("MEAL_TYPE", targetMealSection)
            })
            finish()
        } else {
            Toast.makeText(this, "Failed to save recipes. User not logged in or date not selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        // Clear selections when back is pressed
        selectedRecipes.clear()
        adapter.clearSelections()
        selectedItems.clear()

        // Indicate that no changes were saved
        setResult(RESULT_CANCELED)
        finish()
    }
}