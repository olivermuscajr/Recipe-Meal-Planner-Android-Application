package com.example.mealkit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(
    private var recipes: List<MyRecipe>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(recipe: MyRecipe)
        fun onDeleteClick(recipe: MyRecipe)
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val recipeNameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
        val mealTypeTextView: TextView = itemView.findViewById(R.id.mealTypeTextView)
        val recipeImageView: ImageView = itemView.findViewById(R.id.recipeImageView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        init {
            itemView.setOnClickListener(this)
            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(recipes[position])
                }
            }
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val recipe = recipes[position]
                listener.onItemClick(recipe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val currentRecipe = recipes[position]
        holder.recipeNameTextView.text = currentRecipe.recipeName
        holder.mealTypeTextView.text = currentRecipe.mealType

        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(currentRecipe.image)
            .placeholder(R.mipmap.ic_launcher) // Replace with actual placeholder resource
            .error(R.mipmap.ic_launcher) // Replace with actual error resource
            .into(holder.recipeImageView)
    }

    override fun getItemCount(): Int {
        return recipes.size
    }

    // Function to update recipes data
    fun updateRecipes(newRecipes: List<MyRecipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}
