package com.example.mealkit

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mealkit.databinding.ActivityAirecipeBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.runBlocking

class AIRecipeActivity : AppCompatActivity() {
    private val ingredientsList = mutableListOf<String>()
    private lateinit var generativeModel: GenerativeModel
    private lateinit var binding: ActivityAirecipeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAirecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpAddButton()
        setUpGenerateRecipeButton()
        setUpClearIngredientsButton()
    }

    private fun setUpClearIngredientsButton() {
        // Initially set the button invisible
        binding.buttonClearIngredients.visibility = if (ingredientsList.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        binding.buttonClearIngredients.setOnClickListener {
            clearIngredients()
        }
    }

    private fun clearIngredients() {
        ingredientsList.clear()
        binding.chipGroupIngredients.removeAllViews()
        // Hide the button when ingredients list is empty
        binding.buttonClearIngredients.visibility = View.INVISIBLE
    }

    private fun setUpAddButton() {
        binding.buttonAdd.setOnClickListener {
            val ingredient = binding.editTextIngredient.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                ingredientsList.add(ingredient)
                binding.editTextIngredient.text.clear()
                addChip(ingredient)
            } else {
                showErrorToast("Please enter an ingredient")
            }
        }
    }

    private fun setUpGenerateRecipeButton() {
        binding.buttonGenerateRecipe.setOnClickListener {
            if (ingredientsList.isNotEmpty()) {
                generateRecipe()
                // Change the text of the button to "Generate Another Recipe"
                Toast.makeText(this@AIRecipeActivity, "Generating another recipe", Toast.LENGTH_SHORT).show()
                binding.buttonGenerateRecipe.text = "Generate Another Recipe"
            } else {
                showErrorToast("Please add at least one ingredient")
            }
        }
    }

    private fun addChip(ingredient: String) {
        val chip = Chip(this).apply {
            text = ingredient
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                ingredientsList.remove(ingredient)
                binding.chipGroupIngredients.removeView(this)
                // Show the button when a chip is removed
                if (ingredientsList.isEmpty()) {
                    binding.buttonClearIngredients.visibility = View.INVISIBLE
                }
            }
        }
        binding.chipGroupIngredients.addView(chip)
        // Show the button when a chip is added
        binding.buttonClearIngredients.visibility = View.VISIBLE
    }

    private fun generateRecipe() {

        val prompt = "Generate a recipe based on the ingredients only: ${ingredientsList.joinToString(", ")}. Provide a unique recipe name based on the Ingredient list (do not include the Recipe Name label), Ingredients list, and Instructions steps."
        generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyDCJhGBRpL2lF7Dr5OaETSXZ43BHyadokA"
        )

        runBlocking {
            val response = generativeModel.generateContent(prompt)
            var recipe = response.text
            recipe = recipe?.replace("*", "")

            if (recipe != null) {
                val spannableRecipe = SpannableStringBuilder(recipe)
                Toast.makeText(this@AIRecipeActivity, "Generating Recipe", Toast.LENGTH_SHORT).show()
                // Apply bold and medium-sized font to the first two lines
                val lines = recipe.lines()
                if (lines.isNotEmpty()) {
                    val firstLineEndIndex = lines[0].length
                    spannableRecipe.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        firstLineEndIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableRecipe.setSpan(
                        RelativeSizeSpan(1.2f),
                        0,
                        firstLineEndIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (lines.size > 1) {
                    val secondLineStartIndex = lines[0].length + 1
                    val secondLineEndIndex = secondLineStartIndex + lines[1].length
                    spannableRecipe.setSpan(
                        StyleSpan(Typeface.BOLD),
                        secondLineStartIndex,
                        secondLineEndIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableRecipe.setSpan(
                        RelativeSizeSpan(1.2f),
                        secondLineStartIndex,
                        secondLineEndIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Find and style the word "Ingredients:"
                val ingredientIndex = recipe.indexOf("Ingredients:")
                if (ingredientIndex != -1) {
                    spannableRecipe.setSpan(
                        StyleSpan(Typeface.BOLD),
                        ingredientIndex,
                        ingredientIndex + "Ingredients:".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableRecipe.setSpan(
                        RelativeSizeSpan(1.1f),
                        ingredientIndex,
                        ingredientIndex + "Ingredients:".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                // Find and style the word "Instructions:"
                val instructionsIndex = recipe.indexOf("Instructions:")
                if (instructionsIndex != -1) {
                    spannableRecipe.setSpan(
                        StyleSpan(Typeface.BOLD),
                        instructionsIndex,
                        instructionsIndex + "Instructions:".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableRecipe.setSpan(
                        RelativeSizeSpan(1.1f),
                        instructionsIndex,
                        instructionsIndex + "Instructions:".length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                binding.textViewRecipe.text = spannableRecipe
            }
        }
    }

    private fun showErrorToast(message: String) {
        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}