package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealkit.databinding.FragmentSavedBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SavedFragment : Fragment(), SavedRecipeItemAdapter.OnItemClickListener {

    private lateinit var adapter: SavedRecipeItemAdapter
    private lateinit var databaseReference: DatabaseReference
    private val recipeList: MutableList<RecipeData> = mutableListOf()
    private val filteredRecipeList: MutableList<RecipeData> = mutableListOf()
    private lateinit var binding: FragmentSavedBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SavedRecipeItemAdapter(filteredRecipeList, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter

        fetchRecipes()

        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.d("SavedFragment", "Chip clicked with ID: $checkedId")
            val category = when (checkedId) {
                R.id.chip_all -> null
                R.id.chip_breakfast -> "Breakfast"
                R.id.chip_lunch -> "Lunch"
                R.id.chip_dinner -> "Dinner"
                else -> null
            }
            filterRecipes(category)
        }

        binding.generatedRecipes.setOnClickListener{
            val intent = Intent(requireContext(), SavedGeneratedRecipeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun filterRecipes(category: String?) {
        Log.d("SavedFragment", "Filtering recipes by category: $category")
        filteredRecipeList.clear()
        if (category == null) {
            filteredRecipeList.addAll(recipeList)
        } else {
            filteredRecipeList.addAll(recipeList.filter { it.mealType.equals(category, ignoreCase = true) })
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchRecipes() {
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)
        binding.progressBar.visibility = View.VISIBLE

        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().reference
            val userFavoritesReference = databaseReference.child("users").child(userId).child("favorites")

            userFavoritesReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    recipeList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val recipe = dataSnapshot.getValue(RecipeData::class.java)
                        recipe?.let { recipeList.add(it) }
                    }

                    // Get the currently selected chip category for filtering
                    val selectedChipId = binding.chipGroup.checkedChipId
                    val category = when (selectedChipId) {
                        R.id.chip_all -> null
                        R.id.chip_breakfast -> "Breakfast"
                        R.id.chip_lunch -> "Lunch"
                        R.id.chip_dinner -> "Dinner"
                        else -> null
                    }
                    // Apply filter
                    filterRecipes(category)
                    binding.progressBar.visibility = View.GONE

                    if (recipeList.isEmpty()) {
                        binding.emptySavedListMessage.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptySavedListMessage.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SavedFragment", "Failed to retrieve data", error.toException())
                    binding.progressBar.visibility = View.GONE
                }
            })
        } else {
            Log.e("SavedFragment", "User ID is null")
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onItemClick(recipe: RecipeData) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_DATA", recipe) // Pass the recipe data
        }
        startActivity(intent)
    }

    override fun onDeleteClick(recipe: RecipeData) {
        AlertDialog.Builder(requireContext())
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
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            // Remove recipe from Firebase
            val userFavoritesReference = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("favorites").child(recipe.id ?: "")
            userFavoritesReference.removeValue().addOnSuccessListener {
                // Recipe deleted successfully
                recipeList.remove(recipe) // Remove from local list
                adapter.notifyDataSetChanged() // Notify adapter of changes

                // Optionally, show a message or perform any other UI updates
            }.addOnFailureListener { exception ->
                Log.e("SavedFragment", "Failed to delete recipe", exception)
            }
        }
    }
}