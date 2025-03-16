package com.example.mealkit

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mealkit.databinding.ActivityAirecipeBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.chip.Chip
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GenerateFromIngredients : AppCompatActivity() {
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

        binding.buttonGenerateRecipe.isEnabled = false
    }

    private fun setUpClearIngredientsButton() {
        // Initially set the button invisible
        binding.buttonClearIngredients.visibility = if (ingredientsList.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        binding.buttonClearIngredients.setOnClickListener {
            clearIngredients()
        }

        binding.buttonSaveRecipe.setOnClickListener {
            saveRecipeToFirebase()
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun clearIngredients() {
        // Show confirmation dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Clear Ingredients")
        builder.setMessage("Are you sure you want to clear all ingredients? This action cannot be undone.")
        builder.setPositiveButton("Yes") { dialog, which ->
            // Clear ingredients if user confirms
            ingredientsList.clear()
            binding.chipGroupIngredients.removeAllViews()
            // Hide the button when the ingredients list is empty
            binding.buttonClearIngredients.visibility = View.INVISIBLE
            Toast.makeText(this, "Ingredients cleared", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No") { dialog, which ->
            // Dismiss the dialog if user cancels
            dialog.dismiss()
        }
        builder.setCancelable(false) // Prevent dialog from being dismissed on outside touch
        builder.show()
    }

    private fun setUpAddButton() {
        binding.buttonAdd.setOnClickListener {
            val ingredient = binding.editTextIngredient.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                ingredientsList.add(ingredient)
                binding.editTextIngredient.text.clear()
                addChip(ingredient)
                // Enable the Generate Recipe button when at least one ingredient is added
                binding.buttonGenerateRecipe.isEnabled = true
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
                Toast.makeText(this@GenerateFromIngredients, "Generating recipe", Toast.LENGTH_SHORT).show()
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
                if (ingredientsList.isEmpty()) {
                    binding.buttonGenerateRecipe.isEnabled = false
                }
                if (ingredientsList.isEmpty()) {
                    binding.buttonClearIngredients.visibility = View.INVISIBLE
                }
            }
        }
        binding.chipGroupIngredients.addView(chip)
        binding.buttonClearIngredients.visibility = View.VISIBLE
    }

    private fun generateRecipe() {
        val prompt = "Generate a budget-friendly Filipino recipe using only the following ingredients: ${ingredientsList.joinToString(", ")}. " +
                "The fewer ingredients, the better. The recipe should include the following sections:\n" +
                "Recipe Name: <recipe_name>\n" +
                "Description: <description>\n" +
                "Serving size: <servings_number>\n" +
                "Ingredients:\n" +
                "<quantity> <unit> - <ingredient_1>\n" +
                "<quantity> <unit> - <ingredient_2>\n" +
                "Instructions:\n" +
                "1. <instruction_1>\n" +
                "2. <instruction_2>\n" +
                "Please ensure that each section (Recipe Name, Description, Serving size, Ingredients (US based units), Instructions) is clearly labeled and make spaces between each sections and include parenthesis after ingredients like(sliced, minced)" +
                "and Please Strictly follow the structure of Ingredients (Example: 2 cup - Rice (uncooked)" +
                "Then detect if the ingredient name is valid or not, if the ingredient is not valid then show an invalid input ingredient message"


        binding.progressBar.visibility = View.VISIBLE
        binding.textViewRecipe.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                generativeModel = GenerativeModel(
                    modelName = "gemini-pro",
                    apiKey = "AIzaSyDCJhGBRpL2lF7Dr5OaETSXZ43BHyadokA"
                )
                val response = generativeModel.generateContent(prompt)
                var recipe = response.text?.replace("*", "")

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (recipe != null) {
                        displayRecipe(recipe)
                        binding.buttonGenerateRecipe.text = "Generate Another Recipe"
                        binding.buttonSaveRecipe.visibility = View.VISIBLE
                    } else {
                        showErrorToast("Recipe generation failed")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    showErrorToast("Error generating recipe: ${e.message}")
                }
            }
        }
    }

    private fun saveRecipeToFirebase() {
        val recipeText = binding.textViewRecipe.text.toString()
        val recipeDetails = parseRecipeText(recipeText)

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (recipeDetails != null && userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userRecipesReference = databaseReference.child("users").child(userId).child("userGeneratedRecipes")

            val recipeId = userRecipesReference.push().key
            val recipe = MyRecipe(
                id = recipeId,
                recipeName = recipeDetails.recipeName ?: "",
                description = recipeDetails.description ?: "",
                servingSize = recipeDetails.servingSize,
                ingredients = recipeDetails.ingredients,
                instructions = recipeDetails.instructions,
                userId = userId
            )

            if (recipeId != null) {
                userRecipesReference.child(recipeId).setValue(recipe)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Recipe saved successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error creating recipe ID", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Invalid recipe data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayRecipe(recipe: String) {

        binding.textViewRecipe.text = recipe
    }

    private fun parseRecipeText(recipeText: String): MyRecipe? {
        val nameRegex = Regex("Recipe Name: (.+)")
        val descriptionRegex = Regex("Description: (.+)")
        val servingSizeRegex = Regex("Serving size: (.+)")
        val ingredientsRegex = Regex("Ingredients:(.+?)Instructions:", RegexOption.DOT_MATCHES_ALL)
        val instructionsRegex = Regex("Instructions:(.+)", RegexOption.DOT_MATCHES_ALL)

        val name = nameRegex.find(recipeText)?.groups?.get(1)?.value?.trim()
        val description = descriptionRegex.find(recipeText)?.groups?.get(1)?.value?.trim()
        val servingSize = servingSizeRegex.find(recipeText)?.groups?.get(1)?.value?.trim()
        val ingredientsText = ingredientsRegex.find(recipeText)?.groups?.get(1)?.value?.trim()
        val instructionsText = instructionsRegex.find(recipeText)?.groups?.get(1)?.value?.trim()

        val ingredients = ingredientsText?.split("\n")?.map {
            val parts = it.split("-")
            if (parts.size == 2) {
                val quantityAndUnit = parts[0].trim().split(" ")
                val quantity = quantityAndUnit.getOrNull(0)
                val unit = quantityAndUnit.getOrNull(1)
                val name = parts[1].trim()
                Ingredient(name, quantity, unit)
            } else null
        }?.filterNotNull()

        // Now convert the instructions text into a list of Instruction objects
        val instructions = instructionsText?.split("\n")?.mapIndexed { index, instructionText ->
            Instruction(image = null, text = instructionText.trim())
        } ?: listOf()

        return if (name != null && description != null && servingSize != null) {
            MyRecipe(
                recipeName = name,
                description = description,
                servingSize = servingSize,
                ingredients = ingredients,
                instructions = instructions // Now passing List<Instruction>
            )
        } else {
            null
        }
    }


    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun onBackPressed() {
        // Check if there are any ingredients in the list
        if (ingredientsList.isNotEmpty()) {
            // Show confirmation dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Exit")
            builder.setMessage("Are you sure you want to exit?")
            builder.setPositiveButton("Yes") { dialog, which ->
                super.onBackPressed() // Proceed with the back action
            }
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Dismiss the dialog and stay on the current activity
            }
            builder.setCancelable(false) // Prevent dialog from being dismissed on outside touch
            builder.show()
        } else {
            super.onBackPressed() // Just go back if there are no ingredients
        }
    }

}