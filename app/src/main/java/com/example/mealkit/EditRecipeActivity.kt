package com.example.mealkit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivityEditRecipeBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditRecipeActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private lateinit var binding: ActivityEditRecipeBinding
    private lateinit var recipeId: String
    private lateinit var userId: String
    private var isImageRemoved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve recipeId and userId with checks
        recipeId = intent.getStringExtra("recipeId") ?: ""
        userId = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("loggedInUserId", null) ?: ""

        // Show an error if recipeId or userId is missing
        if (recipeId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Recipe ID or User ID missing. Unable to load recipe.", Toast.LENGTH_SHORT).show()
            finish()  // Close activity if data is invalid
            return
        }

        setupListeners()
        setupSpinners()
    }

    override fun onStart() {
        super.onStart()
        loadRecipeData()  // Load data when the activity starts
    }

    private fun setupListeners() {
        binding.addInstructionButton.setOnClickListener { addInstructionEditText() }
        binding.saveRecipeButton.setOnClickListener { saveRecipe() }
        binding.removeImageButton.setOnClickListener { confirmRemoveImage() }
        binding.addIngredientButton.setOnClickListener { addEmptyIngredientRow() }
        binding.imageView.setOnClickListener {
            openFileChooser()
        }

        binding.backButton.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Cancel Edit Recipe")
            alertDialogBuilder.setMessage("Are you sure you want to cancel editing the recipe?")
            alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
                onBackPressed()
            }
            alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
                // User cancelled deletion
                dialog.dismiss()
            }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()


        }
    }

    private fun addEmptyIngredientRow() {
        val ingredient = Ingredient("", "", "") // Create an empty ingredient
        addIngredientRow(ingredient) // Use the existing method to add the row
    }


    private fun saveRecipe() {
        binding.uploadProgressBar.visibility = View.VISIBLE

        if (isImageRemoved) {
            // Delete the old image from Firebase Storage and update data if image was removed
            deleteOldImageAndSave()
        } else if (imageUri != null) {
            // Upload new image and update recipe if a new image is selected
            handleImageUpload()
        } else {
            // Just update recipe data without new image
            updateRecipeDataWithoutNewImage()
        }
    }
    private fun deleteOldImageAndSave() {
        // Retrieve and delete the old image from Firebase Storage, then update the recipe
        val recipeRef = FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")
        recipeRef.child("image").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val oldImageUrl = snapshot.getValue(String::class.java)
                if (!oldImageUrl.isNullOrEmpty()) {
                    deleteOldImage(oldImageUrl)
                }
                updateRecipeData(null) // Remove image URL from Firebase Database
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EditRecipeActivity", "Failed to retrieve old image URL: ${error.message}")
                showToast("Failed to retrieve image for deletion")
            }
        })
    }

    private fun openFileChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun confirmRemoveImage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Remove Image")
        builder.setMessage("Are you sure you want to remove the image?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            removeImageFromUI() // Call to remove image only from UI
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }
    private fun removeImageFromUI() {
        // Only update the UI to reflect that there's no image, don't delete from Firebase
        binding.imageView.setImageResource(R.drawable.upload_image)
        imageUri = null
        binding.removeImageButton.visibility = View.GONE
        isImageRemoved = true // Set flag to indicate image is removed in UI
    }

    private fun deleteOldImageAndRemoveFromUI() {
        val recipeRef = FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")
        recipeRef.child("image").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val oldImageUrl = snapshot.getValue(String::class.java)
                oldImageUrl?.let { deleteOldImage(it) } // Delete the old image in Firebase Storage

                // Update the UI to reflect that there's no image
                binding.imageView.setImageResource(R.drawable.upload_image)
                imageUri = null
                binding.removeImageButton.visibility = View.GONE

                // Update the recipe data in Firebase Database to remove the image URL
                recipeRef.child("image").removeValue()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EditRecipeActivity", "Failed to retrieve old image URL: ${error.message}")
                showToast("Failed to retrieve image for deletion")
            }
        })
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            imageUri = data.data
            binding.imageView.setImageURI(imageUri)
            binding.removeImageButton.visibility = View.VISIBLE
        }
    }

    private fun loadRecipeData() {
        val recipeRef = FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")

        recipeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val recipe = dataSnapshot.getValue(MyRecipe::class.java)
                if (recipe != null) {
                    populateFieldsWithRecipeData(recipe)

                    // Add at least one instruction field if there are no instructions
                    if (recipe.instructions.isNullOrEmpty()) {
                        addInstructionEditText() // Add a default instruction field if none exist
                    }
                } else {
                    showToast("Recipe not found")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Failed to load recipe data")
            }
        })
    }

    private fun populateFieldsWithRecipeData(recipe: MyRecipe) {
        with(binding) {
            recipeNameEditText.setText(recipe.recipeName)
            descriptionEditText.setText(recipe.description)
            mealTypeSpinner.setSelection(getSpinnerIndex(R.array.meal_types, recipe.mealType))
            servingSizeSpinner.setSelection(getSpinnerIndex(R.array.serving_sizes, recipe.servingSize))

            if (!recipe.image.isNullOrEmpty()) {
                Glide.with(this@EditRecipeActivity).load(recipe.image).into(imageView)
                removeImageButton.visibility = View.VISIBLE
            } else {
                imageView.setImageResource(R.drawable.upload_image)
                removeImageButton.visibility = View.GONE
            }

            // Clear existing instruction fields and populate with existing instructions
            instructionsLayout.removeAllViews()
            recipe.instructions?.forEach { addInstructionEditText(it.toString()) }

            // Clear existing ingredient rows and populate with existing ingredients
            ingredientsLayout.removeAllViews()
            recipe.ingredients?.forEach { addIngredientRow(it) }
        }
    }


    private fun getSpinnerIndex(arrayId: Int, value: String?): Int {
        val array = resources.getStringArray(arrayId)
        return array.indexOf(value).takeIf { it >= 0 } ?: 0
    }

    private fun setupSpinners() {
        setupSpinner(binding.mealTypeSpinner, R.array.meal_types)
        setupSpinner(binding.servingSizeSpinner, R.array.serving_sizes)
    }

    private fun setupSpinner(spinner: Spinner, arrayId: Int) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resources.getStringArray(arrayId))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun uploadImageAndUpdateRecipe() {
        if (imageUri != null) {
            handleImageUpload()
        } else {
            updateRecipeDataWithoutNewImage()
        }
    }

    private fun handleImageUpload() {
        val recipeRef = FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")
        recipeRef.child("image").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val oldImageUrl = snapshot.getValue(String::class.java)
                oldImageUrl?.let { deleteOldImage(it) }

                val storageRef = FirebaseStorage.getInstance().reference
                val newImageRef = storageRef.child("userRecipeImages/${recipeId}_${System.currentTimeMillis()}.jpg")

                newImageRef.putFile(imageUri!!)
                    .addOnSuccessListener {
                        newImageRef.downloadUrl.addOnSuccessListener { uri ->
                            updateRecipeData(uri.toString())
                        }
                    }
                    .addOnFailureListener {
                        binding.uploadProgressBar.visibility = View.GONE // Hide the progress bar if upload fails
                        Log.e("EditRecipeActivity", "Image upload failed: ${it.message}")
                        showToast("Image upload failed: ${it.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EditRecipeActivity", "Failed to retrieve old image URL: ${error.message}")
            }
        })
    }


    private fun deleteOldImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete()
                .addOnSuccessListener {
                    Log.d("EditRecipeActivity", "Old image deleted successfully")
                    showToast("Image removed successfully")
                }
                .addOnFailureListener {
                    Log.e("EditRecipeActivity", "Failed to delete old image: ${it.message}")
                    showToast("Failed to remove image: ${it.message}")
                }
        } else {
            Log.d("EditRecipeActivity", "No old image to delete")
        }
    }

    private fun updateRecipeData(imageUrl: String?) {
        val updatedRecipe = MyRecipe(
            recipeName = binding.recipeNameEditText.text.toString(),
            mealType = binding.mealTypeSpinner.selectedItem.toString(),
            description = binding.descriptionEditText.text.toString(),
            servingSize = binding.servingSizeSpinner.selectedItem.toString(),
            instructions = getInstructionsFromLayout(),
            ingredients = getIngredientsFromLayout(),
            image = imageUrl
        )

        // Save updated recipe in the database
        FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")
            .setValue(updatedRecipe)
            .addOnCompleteListener { task ->
                binding.uploadProgressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    showToast("Recipe updated successfully!")

                    // Load the new image into the ImageView immediately
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@EditRecipeActivity)
                            .load(imageUrl)
                            .into(binding.imageView)
                    }

                    // Return to MyRecipeDetailActivity with recipeId and userId
                    val resultIntent = Intent().apply {
                        putExtra("recipeId", recipeId)
                        putExtra("userId", userId)
                        putExtra("UPDATED_RECIPE_EXTRA", updatedRecipe)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    showToast("Failed to update recipe: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }



    private fun updateRecipeDataWithoutNewImage() {
        val recipeRef = FirebaseDatabase.getInstance().getReference("users/$userId/userRecipes/$recipeId")

        recipeRef.child("image").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingImageUrl = snapshot.getValue(String::class.java)
                val imageUrlToSave = existingImageUrl ?: "" // Preserve old URL if no new image
                updateRecipeData(imageUrlToSave)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EditRecipeActivity", "Failed to retrieve existing image URL: ${error.message}")
                updateRecipeData("") // Handle error, set empty if the image URL retrieval fails
            }
        })
    }

    private fun getIngredientsFromLayout(): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()
        for (i in 0 until binding.ingredientsLayout.childCount) {
            val rowView = binding.ingredientsLayout.getChildAt(i) as ViewGroup
            val quantity = rowView.findViewById<EditText>(R.id.quantityEditText).text.toString().trim()
            val unit = rowView.findViewById<Spinner>(R.id.unitSpinner).selectedItem.toString()
            val name = rowView.findViewById<EditText>(R.id.ingredientEditText).text.toString().trim()

            if (name.isNotEmpty() && quantity.isNotEmpty()) {
                ingredients.add(Ingredient(name, quantity, unit))
            }
        }
        return ingredients
    }


    private fun hasEmptyIngredientFields(): Boolean {
        val ingredientLayout = findViewById<LinearLayout>(R.id.ingredientsLayout)
        for (i in 0 until ingredientLayout.childCount) {
            val ingredientRow = ingredientLayout.getChildAt(i) as LinearLayout
            val ingredientEditText = ingredientRow.findViewById<EditText>(R.id.ingredientEditText)
            val quantityEditText = ingredientRow.findViewById<EditText>(R.id.quantityEditText)
            if (ingredientEditText.text.toString().isEmpty() || quantityEditText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }

    @SuppressLint("MissingInflatedId")
    private fun addIngredientRow(ingredient: Ingredient) {
        if (hasEmptyIngredientFields()) {
            Toast.makeText(this, "Please fill out all fields in ingredient.", Toast.LENGTH_SHORT).show()
            return
        }
        val inflater = LayoutInflater.from(this)
        val ingredientRow = inflater.inflate(R.layout.ingredient_row, binding.ingredientsLayout, false)
        val quantityEditText = ingredientRow.findViewById<EditText>(R.id.quantityEditText)
        val ingredientEditText = ingredientRow.findViewById<EditText>(R.id.ingredientEditText)
        val unitSpinner = ingredientRow.findViewById<Spinner>(R.id.unitSpinner)
        val removeButton = ingredientRow.findViewById<ImageButton>(R.id.removeIngredientButton)

        quantityEditText.setText(ingredient.quantity)
        ingredientEditText.setText(ingredient.name)

        // Set up the unit spinner with the units array
        val unitsArray = resources.getStringArray(R.array.unit_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitsArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter

        // Set the spinner selection to match the ingredient's unit
        val unitIndex = unitsArray.indexOf(ingredient.unit)
        if (unitIndex >= 0) {
            unitSpinner.setSelection(unitIndex)
        }

        // Hide the remove button for the first ingredient row
        val rowIndex = binding.ingredientsLayout.childCount
        if (rowIndex == 0) {
            removeButton.visibility = View.GONE
        } else {
            removeButton.setOnClickListener {
                // Remove ingredient row when clicked
                binding.ingredientsLayout.removeView(ingredientRow)
            }
        }
        binding.ingredientsLayout.addView(ingredientRow)
    }


    private fun hasEmptyInstructionFields(): Boolean {
        val instructionLayout = findViewById<LinearLayout>(R.id.instructionsLayout)
        for (i in 0 until instructionLayout.childCount) {
            val instructionRow = instructionLayout.getChildAt(i) as LinearLayout
            val instructionEditText = instructionRow.findViewById<EditText>(R.id.instructionEditText)
            if (instructionEditText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }

    private fun addInstructionEditText(instruction: String? = null) {
        if (hasEmptyInstructionFields()) {
            Toast.makeText(this, "Please fill out Instruction.", Toast.LENGTH_SHORT).show()
            return
        }
        val inflater = LayoutInflater.from(this)
        val instructionRow = inflater.inflate(R.layout.instruction_row, binding.instructionsLayout, false)
        val instructionEditText = instructionRow.findViewById<EditText>(R.id.instructionEditText)
        val removeButton = instructionRow.findViewById<ImageButton>(R.id.removeInstructionButton)

        instructionEditText.setText(instruction)

        // Check if it's the first row and hide the remove button accordingly
        val rowIndex = binding.instructionsLayout.childCount
        if (rowIndex == 0) {
            removeButton.visibility = View.GONE
        } else {
            removeButton.setOnClickListener {
                // Remove instruction row when clicked
                binding.instructionsLayout.removeView(instructionRow)
            }
        }

        // Add the instruction row to the layout
        binding.instructionsLayout.addView(instructionRow)
    }

    private fun getInstructionsFromLayout(): List<Instruction> {
        val instructions = mutableListOf<Instruction>()  // List of Instruction objects
        for (i in 0 until binding.instructionsLayout.childCount) {
            val rowView = binding.instructionsLayout.getChildAt(i) as ViewGroup
            val instructionText = rowView.findViewById<EditText>(R.id.instructionEditText).text.toString().trim()

            // Only add non-empty instructions
            if (instructionText.isNotEmpty()) {
                // Assuming Instruction class has a constructor that takes a String
                instructions.add(Instruction(instructionText))  // Convert String to Instruction
            }
        }
        return instructions
    }




    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}