package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivityMyRecipeDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.floor
import kotlin.math.roundToInt

class MyRecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyRecipeDetailBinding
    private var isSaved: Boolean = false
    private var currentServingSize: Int = 0
    private var defaultServingSize: Int = 0
    private lateinit var recipe: MyRecipe

    companion object {
        const val EDIT_RECIPE_REQUEST_CODE = 1001
        const val UPDATED_RECIPE_EXTRA = "UPDATED_RECIPE_EXTRA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the recipe object from intent
        recipe = intent.getParcelableExtra("RECIPE_EXTRA") ?: MyRecipe()

        binding.backRecipeButton.setOnClickListener {
            onBackPressed()
        }
        binding.addToListChip.setOnClickListener {
            recipe.ingredients?.let { ingredients ->
                val intent = Intent(this, IngredientsActivity::class.java)
                intent.putParcelableArrayListExtra("INGREDIENTS", ArrayList(ingredients))
                startActivity(intent)
            }
        }

        // Initialize button state based on whether the recipe is saved
        recipe.let {
            updateSaveButtonState(it)
            defaultServingSize = it.servingSize?.toIntOrNull() ?: 1
            currentServingSize = defaultServingSize
            binding.servingSizeTextView.text = currentServingSize.toString()
            updateIngredientsDisplay(it.ingredients, currentServingSize)
        }

        // Save/Remove recipe from favorites
        binding.saveRecipeButton.setOnClickListener {
            if (isSaved) {
                removeFromFavorites(recipe)
            } else {
                checkAndSaveRecipe(recipe)
            }
        }

        binding.decrementButton.setOnClickListener {
            if (currentServingSize > 1) {
                currentServingSize--
                binding.servingSizeTextView.text = currentServingSize.toString()
                updateIngredientsDisplay(recipe.ingredients, currentServingSize)

            }
        }
        binding.incrementButton.setOnClickListener {
            currentServingSize++
            binding.servingSizeTextView.text = currentServingSize.toString()
            updateIngredientsDisplay(recipe.ingredients, currentServingSize)
        }

        // Set up the recipe details
        displayRecipeDetails()

        // Edit recipe button functionality
        binding.editRecipeButton.setOnClickListener {
            val intent = Intent(this, EditRecipeActivity::class.java)
            intent.putExtra("recipeId", recipe.id)
            startActivityForResult(intent, EDIT_RECIPE_REQUEST_CODE)
        }
    }

    private fun updateSaveButtonState(recipe: MyRecipe) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userFavoritesReference = databaseReference.child("users").child(userId).child("favorites")

            userFavoritesReference.child(recipe.id ?: "").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isSaved = snapshot.exists()
                    val buttonDrawable = if (isSaved) R.drawable.savedfilledbutton else R.drawable.saverecipebutton
                    binding.saveRecipeButton.setImageResource(buttonDrawable)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RecipeDetailActivity", "Failed to check recipe existence", error.toException())
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_RECIPE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val updatedRecipe = data.getParcelableExtra<MyRecipe>(UPDATED_RECIPE_EXTRA)
            if (updatedRecipe != null) {
                // Ensure the ID is retained and update other fields
                recipe.id = updatedRecipe.id ?: recipe.id
                recipe.recipeName = updatedRecipe.recipeName
                recipe.description = updatedRecipe.description
                recipe.ingredients = updatedRecipe.ingredients ?: recipe.ingredients
                recipe.instructions = updatedRecipe.instructions ?: recipe.instructions
                recipe.image = updatedRecipe.image ?: recipe.image // Update the image URL

                // Update the UI with the updated recipe details
                displayRecipeDetails()

                // Load the updated image
                loadRecipeImage(recipe.image)

                Log.d("MyRecipeDetailActivity", "Recipe updated: ${recipe.recipeName}")
            }
        }
    }

    private fun checkAndSaveRecipe(recipe: MyRecipe?) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null && recipe != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userFavoritesReference = databaseReference.child("users").child(userId).child("favorites")

            userFavoritesReference.child(recipe.id ?: "").setValue(recipe)
                .addOnSuccessListener {
                    isSaved = true
                    binding.saveRecipeButton.setImageResource(R.drawable.savedfilledbutton)
                    Toast.makeText(this, "Recipe saved to favorites successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in or recipe data is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromFavorites(recipe: MyRecipe?) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null && recipe != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userFavoritesReference = databaseReference.child("users").child(userId).child("favorites")

            userFavoritesReference.child(recipe.id ?: "").removeValue()
                .addOnSuccessListener {
                    isSaved = false
                    binding.saveRecipeButton.setImageResource(R.drawable.saverecipebutton)
                    Toast.makeText(this, "Recipe removed from favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in or recipe data is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayRecipeDetails() {
        binding.recipeNameTextView.text = recipe.recipeName
        binding.descriptionTextView.text = recipe.description
        binding.mealTypeTextView.text = recipe.mealType
        binding.servingSizeTextView.text = recipe.servingSize.toString()

        // Display ingredients
        recipe.ingredients?.let { ingredients ->
            binding.ingredientsTextView.text = ingredients.joinToString("\n") {
                "${it.quantity} ${it.unit} ${it.name}"
            }
        } ?: run {
            binding.ingredientsTextView.text = "No ingredients available."
        }

        recipe.instructions?.let { instructions ->
            binding.instructionsTextView.text = instructions.mapIndexed { index, instruction ->
                // Only extract the text part, ignore the 'image' part
                "<b>Step ${index + 1}:</b><br>${instruction.text}"
            }.joinToString("<br><br>").let {
                Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY)
            }
        } ?: run {
            binding.instructionsTextView.text = "No instructions available."
        }

        // Load recipe image
        loadRecipeImage(recipe.image)
    }

    private fun updateIngredientsDisplay(ingredients: List<Ingredient>?, servings: Int) {
        val updatedIngredients = ingredients?.joinToString("\n") {
            // Adjust the quantity of each ingredient
            val adjustedQuantity = (it.quantity?.toDoubleOrNull() ?: 0.0) * servings / defaultServingSize
            val formattedQuantity = formatQuantity(adjustedQuantity)
            "$formattedQuantity ${it.unit} of ${it.name}"
        }
        binding.ingredientsTextView.text = updatedIngredients
    }
    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            // If the quantity is a whole number, convert it to an integer
            quantity.roundToInt().toString()
        } else {
            // Convert to fraction with improved precision handling
            convertToFraction(quantity)
        }
    }
    private fun convertToFraction(value: Double): String {
        val tolerance = 1.0E-3 // Tolerance threshold to round small fractions
        val wholeNumber = floor(value).toInt()
        val fractionalPart = value - wholeNumber

        if (fractionalPart < tolerance) {
            return wholeNumber.toString()
        }

        // Convert to fractional part with improved precision handling
        val fraction = toFraction(fractionalPart)

        return if (wholeNumber > 0) {
            if (fraction != "0") {
                "$wholeNumber $fraction"
            } else {
                wholeNumber.toString()
            }
        } else {
            fraction
        }
    }

    private fun toFraction(decimal: Double): String {
        val tolerance = 1.0E-3
        val denominators = listOf(8, 4, 3, 2) // Added more fractions (8, 4, 3, 2) to increase precision
        val closestFraction = denominators.map { denominator ->
            val numerator = (decimal * denominator).roundToInt()
            val gcdValue = gcd(numerator, denominator)
            val simplifiedNumerator = numerator / gcdValue
            val simplifiedDenominator = denominator / gcdValue

            simplifiedNumerator to simplifiedDenominator
        }.minByOrNull { Math.abs(decimal - (it.first.toDouble() / it.second)) }

        return if (closestFraction != null) {
            val (numerator, denominator) = closestFraction
            if (numerator == denominator) {
                "1"
            } else {
                "$numerator/$denominator"
            }
        } else {
            "0"  // For cases where fraction could not be determined, though this should not happen often
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        if (b == 0) return a
        return gcd(b, a % b)
    }

    private fun loadRecipeImage(imageUrl: String?) {
        imageUrl?.let {
            Glide.with(this).load(it).into(binding.uploadedImageView)
        }
    }
}
