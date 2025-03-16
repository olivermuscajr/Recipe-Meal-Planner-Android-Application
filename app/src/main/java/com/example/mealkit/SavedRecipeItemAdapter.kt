package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivitySavedRecipeItemAdapterBinding

class SavedRecipeItemAdapter(
    private val recipeList: MutableList<RecipeData>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SavedRecipeItemAdapter.SavedViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(recipe: RecipeData)
        fun onDeleteClick(recipe: RecipeData)
    }

    inner class SavedViewHolder(private val binding: ActivitySavedRecipeItemAdapterBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)

        }

        fun bind(recipe: RecipeData) {
            binding.savedrecipeNameTextView.text = recipe.recipeName
            Glide.with(binding.savedrecipeImageView.context)
                .load(recipe.image)
                .into(binding.savedrecipeImageView)

            binding.saveddeleteButton.setOnClickListener { // Set the delete button click listener
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(recipeList[position])
                }
            }
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(recipeList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val binding = ActivitySavedRecipeItemAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {

        val recipe = recipeList[position]
        holder.bind(recipe)
    }
    override fun getItemCount(): Int = recipeList.size
}