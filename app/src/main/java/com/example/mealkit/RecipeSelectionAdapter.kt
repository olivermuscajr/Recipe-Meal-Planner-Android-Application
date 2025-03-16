package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeSelectionAdapter(
    private var recipes: MutableList<RecipeData> = mutableListOf(), // Ensure it's initialized
    private val listener: OnRecipeSelectListener,
    private val isViewMode: Boolean = false
) : RecyclerView.Adapter<RecipeSelectionAdapter.RecipeViewHolder>() {

    private val selectedItems = mutableMapOf<String, Boolean>()

    fun clearSelections() {
        selectedItems.clear() // Clear selection state
        recipes.forEach { it.isSelected = false } // Reset isSelected on all recipes
        notifyDataSetChanged() // Refresh UI
    }

    interface OnRecipeSelectListener {
        fun onItemClick(recipe: RecipeData)
        fun onRecipeSelected(recipe: RecipeData)
        fun onRecipeDeselected(recipe: RecipeData)
        fun onDeleteRecipe(recipe: RecipeData) // Delete functionality
        fun onToggleRecipeInMealPlan(recipe: RecipeData, isSelected: Boolean) // Added this method
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        val recipeName: TextView = itemView.findViewById(R.id.recipeName)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(recipe: RecipeData) {
            recipeName.text = recipe.recipeName
            Glide.with(recipeImage.context)
                .load(recipe.image)
                .into(recipeImage)

            // Adjust CardView width based on view mode
            val cardView = itemView.findViewById<CardView>(R.id.cardView)
            val layoutParams = cardView.layoutParams

            if (!isViewMode) {
                // Match parent width when selecting recipe
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                // Set width to 250dp in view mode
                val context = cardView.context
                layoutParams.width = (250 * context.resources.displayMetrics.density).toInt() // Convert 250dp to pixels
            }
            cardView.layoutParams = layoutParams

            // Preserve and restore checkbox state
            checkbox.setOnCheckedChangeListener(null) // Prevent triggering during binding
            checkbox.isChecked = selectedItems[recipe.id] ?: recipe.isSelected // Use selection map
            recipe.isSelected = checkbox.isChecked // Sync with recipe model
            checkbox.visibility = if (isViewMode) View.GONE else View.VISIBLE
            checkbox.isEnabled = !isViewMode // Disable checkbox in view mode

            // Set checkbox listener
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                selectedItems[recipe.id!!] = isChecked // Update selection map
                recipe.isSelected = isChecked // Sync with recipe model

                if (isChecked) {
                    listener.onRecipeSelected(recipe)
                } else {
                    listener.onRecipeDeselected(recipe)
                }
                listener.onToggleRecipeInMealPlan(recipe, isChecked)
            }

            deleteButton.visibility = if (isViewMode) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener {
                listener.onDeleteRecipe(recipe)
            }
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(recipes[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_selection, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }


    override fun getItemCount(): Int = recipes.size

    fun updateRecipes(newRecipes: List<RecipeData>) {
        recipes.clear()
        recipes.addAll(newRecipes)

        recipes.forEach { recipe ->
            recipe.id?.let {
                selectedItems[it] = recipe.isSelected
            }
        }
        notifyDataSetChanged()
    }
}
