package com.example.mealkit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealkit.databinding.FragmentGroceryListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroceryListFragment : Fragment() {

    private lateinit var binding: FragmentGroceryListBinding
    private lateinit var adapter: IngredientAdapter
    private lateinit var completedAdapter: IngredientAdapter
    private var ingredientsList: MutableList<Ingredient> = mutableListOf()
    private var completedList: MutableList<Ingredient> = mutableListOf()

    companion object {
        const val REQUEST_CODE_ADD_INGREDIENT = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroceryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the main grocery list adapter
        adapter = IngredientAdapter(ingredientsList) { ingredient, isSelected ->
            handleItemSelection(ingredient, isSelected)
        }

        // Initialize the completed list adapter
        completedAdapter = IngredientAdapter(completedList) { ingredient, isSelected ->
            if (!isSelected) {
                moveToGroceryList(ingredient)
            }
        }

        // Set up the RecyclerViews
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.completedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.completedRecyclerView.adapter = completedAdapter

        // Fetch ingredients from Firebase
        fetchIngredients()

        // Handle the Add Ingredient button click
        binding.addItemToListButton.setOnClickListener {
            val intent = Intent(requireContext(), AddItemIngredient::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_INGREDIENT)
        }

        // Handle the Clear List button
        binding.clearListBtn.setOnClickListener {
            showClearConfirmationDialog()
        }

        binding.clearCompletedButton.setOnClickListener {
            clearCompletedList()
        }
    }

    private fun handleItemSelection(ingredient: Ingredient, isSelected: Boolean) {
        if (isSelected) {
            moveToCompletedList(ingredient)
        }
    }

    private fun moveToCompletedList(ingredient: Ingredient) {
        // Remove from the grocery list and add to the completed list
        ingredientsList.remove(ingredient)
        ingredient.completed = true // Set the completed state
        completedList.add(ingredient)

        // Update Firebase to reflect the change
        updateIngredientInFirebase(ingredient)

        // Notify adapters of data change
        adapter.notifyDataSetChanged()
        completedAdapter.notifyDataSetChanged()

        // Update visibility of completed items
        updateCompletedListVisibility()
    }


    private fun moveToGroceryList(ingredient: Ingredient) {
        // Move item back to grocery list
        completedList.remove(ingredient)
        ingredient.completed = false // Update the completed state
        ingredientsList.add(ingredient)

        // Update Firebase to reflect the change
        updateIngredientInFirebase(ingredient)

        // Notify adapters of data change
        adapter.notifyDataSetChanged()
        completedAdapter.notifyDataSetChanged()
        updateGroceryListVisibility()
    }

    private fun fetchIngredients() {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val userGroceryListReference = databaseReference.child("users").child(userId).child("groceryList")

            binding.progressBar.visibility = View.VISIBLE

            userGroceryListReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ingredients = mutableListOf<Ingredient>()
                    val completed = mutableListOf<Ingredient>()
                    snapshot.children.forEach { dataSnapshot ->
                        val ingredient = dataSnapshot.getValue(Ingredient::class.java)
                        if (ingredient != null) {
                            if (ingredient.completed) {
                                completed.add(ingredient)
                            } else {
                                ingredients.add(ingredient)
                            }
                        }
                    }

                    // Clear and update both lists
                    ingredientsList.clear()
                    ingredientsList.addAll(ingredients)
                    completedList.clear()
                    completedList.addAll(completed)
                    adapter.notifyDataSetChanged()
                    completedAdapter.notifyDataSetChanged()

                    binding.progressBar.visibility = View.GONE

                    // Show or hide the empty message based on the lists' content
                    binding.emptyGroceryListMessage.visibility = if (ingredientsList.isEmpty() && completedList.isEmpty()) View.VISIBLE else View.GONE

                    // Update visibility of completed items
                    updateGroceryListVisibility()
                    updateCompletedListVisibility()

                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load grocery list", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    private fun updateGroceryListVisibility() {
        if (ingredientsList.isNotEmpty()) {
            binding.toBuyListLabel.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.toBuyListLabel.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
        }
    }

    private fun updateCompletedListVisibility() {
        // Show or hide completed items based on their presence
        if (completedList.isNotEmpty()) {
            binding.completedListLabel.visibility = View.VISIBLE
            binding.completedRecyclerView.visibility = View.VISIBLE
            binding.clearCompletedButton.visibility = View.VISIBLE
        } else {
            binding.completedListLabel.visibility = View.GONE
            binding.completedRecyclerView.visibility = View.GONE
            binding.clearCompletedButton.visibility = View.GONE
        }
    }


    private fun clearCompletedList() {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val userGroceryListReference = databaseReference.child("users").child(userId).child("groceryList")

            userGroceryListReference.removeValue().addOnSuccessListener {
                completedList.clear()
                Toast.makeText(requireContext(), "Completed list cleared.", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to clear completed list.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Clear Grocery List")
            setMessage("Are you sure you want to clear all the items from the grocery list?")
            setPositiveButton("Yes") { dialog, _ ->
                clearGroceryList()
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            create()
            show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearGroceryList() {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val userGroceryListReference = databaseReference.child("users").child(userId).child("groceryList")

            userGroceryListReference.removeValue().addOnSuccessListener {
                ingredientsList.clear() // Also clear the completed list
                adapter.notifyDataSetChanged() // Notify the completed adapter
                Toast.makeText(requireContext(), "Grocery list cleared.", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to clear grocery list.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADD_INGREDIENT && resultCode == Activity.RESULT_OK) {
            // Get the ingredient details from the result
            val ingredientName = data?.getStringExtra("ingredientName")
            val quantity = data?.getStringExtra("quantity")
            val unit = data?.getStringExtra("unit")

            // Create a new ingredient object and add it to the list
            if (ingredientName != null && quantity != null && unit != null) {
                val newIngredient = Ingredient(ingredientName, quantity, unit)
                ingredientsList.add(newIngredient)
                adapter.notifyDataSetChanged()  // Refresh RecyclerView
            }
        }
    }

    private fun updateIngredientInFirebase(ingredient: Ingredient) {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val userGroceryListReference = databaseReference.child("users").child(userId).child("groceryList")

            // Update the specific ingredient in Firebase
            userGroceryListReference.orderByChild("name").equalTo(ingredient.name).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { dataSnapshot ->
                        dataSnapshot.ref.child("completed").setValue(ingredient.completed) // Ensure you're updating only the 'completed' field
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to update ingredient", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
