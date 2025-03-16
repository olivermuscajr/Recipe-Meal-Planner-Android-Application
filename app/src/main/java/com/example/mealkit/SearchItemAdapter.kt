package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.SearchRecipeItemBinding

class SearchItemAdapter(
    private val recipeList: List<RecipeData>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SearchItemAdapter.SearchViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(recipe: RecipeData)
    }

    inner class SearchViewHolder(
        private val binding: SearchRecipeItemBinding) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(recipe: RecipeData) {
            binding.searchRecipeName.text = recipe.recipeName
            Glide.with(binding.searchRecipeImageView.context)
                .load(recipe.image)
                .into(binding.searchRecipeImageView)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(recipeList[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = SearchRecipeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.bind(recipe)
    }

    override fun getItemCount(): Int = recipeList.size
}