package com.example.mealkit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mealkit.databinding.ItemIngredientsBinding

class IngredientAdapter(
    private val ingredients: List<Ingredient>,
    private val onItemSelected: (Ingredient, Boolean) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(private val binding: ItemIngredientsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Bind method for individual ingredient
        fun bind(ingredient: Ingredient) {
            binding.quantityTextView.text = ingredient.quantity
            binding.unitTextView.text = ingredient.unit
            binding.ingredientNameTextView.text = ingredient.name

            // Update CheckBox state
            binding.circularCheckBox.isChecked = ingredient.completed // Set the checked state based on completion

            // Set listener for CheckBox
            binding.circularCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onItemSelected(ingredient, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.bind(ingredient)
    }

    override fun getItemCount(): Int = ingredients.size
}
