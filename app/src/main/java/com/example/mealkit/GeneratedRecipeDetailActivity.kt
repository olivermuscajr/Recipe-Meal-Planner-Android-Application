package com.example.mealkit

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.mealkit.databinding.ActivityGeneratedRecipeDetailBinding

class GeneratedRecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGeneratedRecipeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val recipe = intent.getParcelableExtra<RecipeData>("RECIPE_DATA")
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        recipe?.let {
            binding.recipeNameTextView.text = it.recipeName ?: "No Name Available"
            binding.descriptionTextView.text = it.description ?: "No Description Available"
            binding.servingSizeTextView.text = "Serving Size: ${it.servingSize ?: "N/A"}"

            val ingredientsText = it.ingredients?.joinToString(separator = "\n") { ingredient ->
                "${ingredient.quantity ?: ""} ${ingredient.unit ?: ""} ${ingredient.name ?: ""}"
            } ?: "No Ingredients Available"
            binding.ingredientsTextView.text = ingredientsText

            val instructionsText = it.instructions?.joinToString(separator = "\n") { instruction ->
                instruction.text.toString()  // Extract only the text field of the Instruction object
            } ?: "No Instructions Available"

            binding.instructionsTextView.text = instructionsText
        }
    }
}
