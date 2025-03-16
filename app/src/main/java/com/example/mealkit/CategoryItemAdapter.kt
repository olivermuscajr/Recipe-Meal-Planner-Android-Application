package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivityCategoryItemAdapterBinding

class CategoryItemAdapter(
    private val recipeList: List<RecipeData>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(recipe: RecipeData)
        fun onSeeAllClick() // Click listener for the "See All" button
    }

    private val VIEW_TYPE_RECIPE = 0
    private val VIEW_TYPE_BUTTON = 1

    inner class RecipeViewHolder(private val binding: ActivityCategoryItemAdapterBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(recipe: RecipeData) {
            binding.categoryRecipeName.text = recipe.recipeName
            Glide.with(binding.categoryRecipeImageView.context)
                .load(recipe.image)
                .into(binding.categoryRecipeImageView)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(recipeList[position])
            }
        }
    }
    inner class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val button: Button = view.findViewById(R.id.seeAllButton)

        init {
            button.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener.onSeeAllClick()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RECIPE) {
            val binding = ActivityCategoryItemAdapterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            RecipeViewHolder(binding)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_see_all_button, parent, false)
            ButtonViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RecipeViewHolder) {
            val recipe = recipeList[position]
            holder.bind(recipe)
        }
    }

    override fun getItemCount(): Int = recipeList.size + 1 // Add 1 for the button

    override fun getItemViewType(position: Int): Int {
        return if (position < recipeList.size) VIEW_TYPE_RECIPE else VIEW_TYPE_BUTTON
    }

}