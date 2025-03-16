package com.example.mealkit

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivityRecipeDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.material.snackbar.Snackbar
import com.google.common.math.IntMath.gcd
import kotlin.math.floor
import kotlin.math.roundToInt

    class RecipeDetailActivity : AppCompatActivity() {

        private lateinit var binding: ActivityRecipeDetailBinding
        private var isSaved: Boolean = false
        private var currentServingSize: Int = 0
        private var defaultServingSize: Int = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val recipe = intent.getParcelableExtra<RecipeData>("RECIPE_DATA")

            binding.backRecipeButton.setOnClickListener {
                onBackPressed()
            }

            // Initialize button state based on whether recipe is saved
            recipe?.let {
                updateSaveButtonState(it)
                defaultServingSize = it.servingSize?.toIntOrNull() ?: 1 // Default to 1 if conversion fails
                currentServingSize = defaultServingSize
                binding.servingSizeTextView.text = currentServingSize.toString()
                updateIngredientsDisplay(it.ingredients, currentServingSize)

                // Pass the instructions list to CookingActivity
                val instructionsText = it.instructions?.map { instruction -> instruction.text } // Only pass the 'text' part
                val intent = Intent(this, CookingActivity::class.java)
                intent.putStringArrayListExtra("INSTRUCTIONS",
                    instructionsText?.let { it1 -> ArrayList(it1) })
            }

            binding.saveRecipeButton.setOnClickListener {
                if (isSaved) {
                    removeFromFavorites(recipe)
                } else {
                    checkAndSaveRecipe(recipe)
                }
            }

            binding.decrementButton.setOnClickListener {
                if (currentServingSize > 1) { // Prevent decrementing below 1
                    currentServingSize--
                    binding.servingSizeTextView.text = currentServingSize.toString()
                    updateIngredientsDisplay(recipe?.ingredients, currentServingSize)
                }
            }

            binding.incrementButton.setOnClickListener {
                currentServingSize++
                binding.servingSizeTextView.text = currentServingSize.toString()
                updateIngredientsDisplay(recipe?.ingredients, currentServingSize)
            }

            binding.cookNowButton.setOnClickListener {
                recipe?.let {
                    val intent = Intent(this, CookingActivity::class.java)
                    intent.putParcelableArrayListExtra("INGREDIENTS", ArrayList(it.ingredients ?: listOf()))
                    intent.putParcelableArrayListExtra("INSTRUCTIONS", ArrayList(it.instructions ?: listOf()))
                    startActivity(intent)
                }
            }

            binding.addToListChip.setOnClickListener {
                recipe?.ingredients?.let { ingredients ->
                    val intent = Intent(this, IngredientsActivity::class.java)
                    intent.putParcelableArrayListExtra("INGREDIENTS", ArrayList(ingredients))
                    startActivity(intent)
                }
            }

            recipe?.let {
                binding.recipeNameTextView.text = it.recipeName
                binding.categoryTextView.text = it.mealType
                binding.descriptionTextView.text = it.description
                Glide.with(this)
                    .load(it.image)
                    .into(binding.recipeImageView)

                // Map instructions, extract the text and add step formatting
                val instructionsText = it.instructions?.mapIndexed { index, instruction ->
                    // Create a SpannableString for each instruction
                    val stepText = "Step ${index + 1}.\n${instruction.text}"
                    val spannableString = SpannableString(stepText)

                    // Make "Step X" bold (apply this to the first step)
                    if (index == 0) {
                        spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, "Step ${index + 1}".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    spannableString
                }?.joinToString("\n\n")

                // Set the formatted instructions text to the TextView
                binding.instructionsTextView.text = instructionsText
            }
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


        private fun updateSaveButtonState(recipe: RecipeData) {
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

        private fun checkAndSaveRecipe(recipe: RecipeData?) {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId != null && recipe != null) {
                val databaseReference = FirebaseDatabase.getInstance().reference
            val userFavoritesReference = databaseReference.child("users").child(userId).child("favorites")

            userFavoritesReference.child(recipe.id ?: "").setValue(recipe)
                .addOnSuccessListener {
                    isSaved = true
                    binding.saveRecipeButton.setImageResource(R.drawable.savedfilledbutton)
                    Snackbar.make(binding.root, "Recipe saved to list", Snackbar.LENGTH_LONG)
                        .setAction("VIEW") {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("fragmentToLoad", "SavedFragment")
                            startActivity(intent)
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in or recipe data is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromFavorites(recipe: RecipeData?) {
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
}


/*
Formula used:
Adjusted Quantity = (Original Quantity * Current Serving Size) / Default Serving Size

Example:
Suppose the default serving size is 4.
The original quantity for an ingredient is 2 cups (for 4 servings).
The user adjusts the serving size to 2.
The formula to calculate the adjusted quantity would be:

Adjusted Quantity = (2 cups * 2 servings) / 4 servings
Adjusted Quantity = 4 cups / 4 = 1 cup

code:
val adjustedQuantity = (it.quantity?.toDoubleOrNull() ?: 0.0) * servings / defaultServingSize
*/
