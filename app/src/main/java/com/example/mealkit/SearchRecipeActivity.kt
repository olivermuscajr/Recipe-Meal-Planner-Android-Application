package com.example.mealkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.example.mealkit.databinding.ActivitySearchRecipeBinding

class SearchRecipeActivity : AppCompatActivity(), SearchItemAdapter.OnItemClickListener {

    private lateinit var adapter: SearchItemAdapter
    private lateinit var databaseReference: DatabaseReference
    private val recipeList: MutableList<RecipeData> = mutableListOf()
    private val filteredRecipeList: MutableList<RecipeData> = mutableListOf()
    private lateinit var binding: ActivitySearchRecipeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("defaultRecipes")

        // Initialize the adapter with the recipeList and listener
        adapter = SearchItemAdapter(filteredRecipeList, this)

        // Set up RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Fetch data from Firebase
        fetchRecipes()

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.searchByIngredient.setOnClickListener{
            val intent = Intent(this, SearchByIngredientActivity::class.java)
            startActivity(intent)
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes(s.toString()) // Filter the recipes as the user types
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchRecipes() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipeList.clear()
                for (dataSnapshot in snapshot.children) {
                    val recipe = dataSnapshot.getValue(RecipeData::class.java)
                    recipe?.let { recipeList.add(it) }
                }
                // Initially show all recipes
                filteredRecipeList.addAll(recipeList)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchRecipeActivity", "Failed to retrieve data", error.toException())
            }
        })
    }

    private fun filterRecipes(query: String) {
        val searchQuery = query.lowercase()
        filteredRecipeList.clear()

        if (searchQuery.isEmpty()) {
            filteredRecipeList.addAll(recipeList) // Show all recipes if search query is empty
        } else {
            recipeList.forEach { recipe ->
                if (recipe.recipeName?.lowercase()?.contains(searchQuery) == true) {
                    filteredRecipeList.add(recipe)
                }
            }
        }

        adapter.notifyDataSetChanged() // Notify adapter that data has changed

        // Show "No results found" if the filtered list is empty
        binding.noSearchResultText.visibility = if (filteredRecipeList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onItemClick(recipe: RecipeData) {
        // Create an intent to start RecipeDetailActivity
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_DATA", recipe) // Pass the recipe data
        }
        startActivity(intent)
    }
}