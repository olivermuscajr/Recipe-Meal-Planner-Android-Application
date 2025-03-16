package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mealkit.databinding.ItemGeneratedRecipeBinding

class SavedGeneratedRecipeItemAdapter(
    private val generatedRecipeList: MutableList<RecipeData>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SavedGeneratedRecipeItemAdapter.GeneratedRecipeViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(recipe: RecipeData)
        fun onDeleteClick(recipe: RecipeData) // Add delete click handler
    }

    inner class GeneratedRecipeViewHolder(
        private val binding: ItemGeneratedRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(recipe: RecipeData) {
            binding.generatedRecipeNameTextView.text = recipe.recipeName
            binding.recipeDetailsTextView.text = recipe.description

            // Set delete button functionality
            binding.deleteButton.setOnClickListener {
                listener.onDeleteClick(recipe) // Notify activity to delete the recipe
            }
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(generatedRecipeList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeneratedRecipeViewHolder {
        val binding = ItemGeneratedRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GeneratedRecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GeneratedRecipeViewHolder, position: Int) {
        val recipe = generatedRecipeList[position]
        holder.bind(recipe)
    }

    override fun getItemCount(): Int = generatedRecipeList.size
}
