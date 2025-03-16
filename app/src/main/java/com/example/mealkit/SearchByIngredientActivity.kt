package com.example.mealkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealkit.databinding.ActivitySearchByIngredientBinding
import com.google.android.material.chip.Chip
import com.google.firebase.database.*

class SearchByIngredientActivity : AppCompatActivity(), SearchItemAdapter.OnItemClickListener {

    private lateinit var adapter: SearchItemAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var ingredientReference: DatabaseReference
    private val recipeList: MutableList<RecipeData> = mutableListOf()
    private val filteredRecipeList: MutableList<RecipeData> = mutableListOf()
    private val ingredientsList: MutableList<String> = mutableListOf()
    private val ingredientSuggestions: MutableList<String> = mutableListOf()
    private lateinit var binding: ActivitySearchByIngredientBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchByIngredientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().getReference("defaultRecipes")
        ingredientReference = FirebaseDatabase.getInstance().getReference("defaultIngredients")

        adapter = SearchItemAdapter(filteredRecipeList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
//change 1
        fetchRecipes()
        fetchIngredientSuggestions()
        setUpAddButton()

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.buttonClearIngredients.setOnClickListener{
            clearIngredients()
        }
    }

    private fun fetchRecipes() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipeList.clear()
                for (dataSnapshot in snapshot.children) {
                    val recipe = dataSnapshot.getValue(RecipeData::class.java)
                    recipe?.let { recipeList.add(it) }
                }
                filteredRecipeList.addAll(recipeList)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchByIngredientActivity", "Failed to retrieve data", error.toException())
            }
        })
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
        binding.editTextIngredient.setAdapter(adapter)
        binding.editTextIngredient.threshold = 1 // Start suggesting after 1 character
    }

    private fun setUpAddButton() {
        binding.buttonAdd.setOnClickListener {
            val ingredient = binding.editTextIngredient.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                ingredientsList.add(ingredient)
                binding.editTextIngredient.text.clear()
                addChip(ingredient)
                filterRecipesByIngredients()
            } else {
                Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addChip(ingredient: String) {
        val chip = Chip(this).apply {
            text = ingredient
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                ingredientsList.remove(ingredient)
                binding.chipGroupIngredients.removeView(this)
                filterRecipesByIngredients()
                if (ingredientsList.isEmpty()) {
                    binding.buttonClearIngredients.visibility = View.GONE
                }
            }
        }
        binding.chipGroupIngredients.addView(chip)
        binding.buttonClearIngredients.visibility = View.VISIBLE
        binding.chipgroup.visibility = View.VISIBLE
    }


    private fun clearIngredients() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Clear Ingredients")
        builder.setMessage("Are you sure you want to clear all ingredients? This action cannot be undone.")
        builder.setPositiveButton("Yes") { dialog, which ->
            ingredientsList.clear()
            binding.chipGroupIngredients.removeAllViews()
            binding.buttonClearIngredients.visibility = View.GONE
            binding.chipgroup.visibility = View.GONE
            fetchRecipes()
            Toast.makeText(this, "Ingredients cleared", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No") { dialog, which ->
            // Dismiss the dialog if user cancels
            dialog.dismiss()
        }
        builder.setCancelable(false) // Prevent dialog from being dismissed on outside touch
        builder.show()
    }

    private fun filterRecipesByIngredients() {
        filteredRecipeList.clear()

        if (ingredientsList.isEmpty()) {
            filteredRecipeList.addAll(recipeList) // Show all recipes if no ingredients are selected
        } else {
            recipeList.forEach { recipe ->
                val recipeIngredients = recipe.ingredients?.map { it.name?.lowercase() ?: "" } ?: emptyList()
                if (ingredientsList.all { ingredient ->
                        recipeIngredients.any { recipeIngredient ->
                            recipeIngredient.contains(ingredient.lowercase())
                        }
                    }) {
                    filteredRecipeList.add(recipe)
                }
            }
        }

        // Show the "No results" message if the filtered list is empty
        if (filteredRecipeList.isEmpty()) {
            binding.textViewNoResults.visibility = View.VISIBLE
        } else {
            binding.textViewNoResults.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    override fun onItemClick(recipe: RecipeData) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_DATA", recipe)
        }
        startActivity(intent)
    }
}
