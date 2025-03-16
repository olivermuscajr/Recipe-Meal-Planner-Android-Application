package com.example.mealkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class CreateRecipeActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var recipeId: String
    private lateinit var removeButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_recipe)

        // Initialize views for image upload
        imageView = findViewById(R.id.imageView)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        removeButton = findViewById(R.id.removeImageButton)

        imageView.setOnClickListener {
            openFileChooser()
        }

        val mealTypeSpinner: Spinner = findViewById(R.id.mealTypeSpinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.meal_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mealTypeSpinner.adapter = adapter
        }

        val servingSizeSpinner: Spinner = findViewById(R.id.servingSizeSpinner)
        val servingSizes = (1..16).toList()
        val servingSizeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            servingSizes
        )
        servingSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        servingSizeSpinner.adapter = servingSizeAdapter

        val addIngredientButton: Button = findViewById(R.id.addIngredientButton)
        val addInstructionButton: Button = findViewById(R.id.addInstructionButton)
        val saveRecipeButton: Button = findViewById(R.id.saveRecipeButton)
        val backButton: ImageButton = findViewById(R.id.backButton)

        addIngredientButton.setOnClickListener {
            addIngredientRow()
        }

        addInstructionButton.setOnClickListener {
            addInstructionEditText()
        }

        saveRecipeButton.setOnClickListener {
            uploadImageAndSaveRecipe()
        }
        backButton.setOnClickListener {
            onBackPressed()
        }

        removeButton.setOnClickListener {
            // Show confirmation dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Remove Image")
            builder.setMessage("Are you sure you want to remove the image?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                removeImage() // Call the method to remove the image
                dialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog if the user chooses not to remove the image
            }
            builder.setCancelable(false) // Prevent dialog from being dismissed on outside touch
            builder.show()
        }
        recipeId = System.currentTimeMillis().toString()

        addIngredientRow()
        addInstructionEditText()
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun removeImage() {
        imageView.setImageResource(R.drawable.upload_image)
        imageUri = null
        removeButton.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            imageView.setImageURI(imageUri)
            removeButton.visibility = View.VISIBLE
        }
    }

    private fun uploadImageAndSaveRecipe() {
        if (imageUri != null) {
            // Get the actual file name from the URI
            val imageFile = File(imageUri!!.path ?: "")
            val imageName = imageFile.name // Original file name

            // Create a storage reference using the recipeId and the original image name
            val storageReference = FirebaseStorage.getInstance().getReference("userRecipeImages/${recipeId}_${imageName}")
            uploadProgressBar.visibility = View.VISIBLE

            storageReference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        saveRecipe(uri.toString())
                    }
                    uploadProgressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    uploadProgressBar.visibility = View.GONE
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    uploadProgressBar.progress = progress.toInt()
                }
        } else {
            saveRecipe(null)
        }
    }

    private fun saveRecipe(image: String?) {
        val recipeNameEditText: EditText = findViewById(R.id.recipeNameEditText)
        val recipeName = recipeNameEditText.text.toString()
        val descriptionEditText: EditText = findViewById(R.id.descriptionEditText)
        val description = descriptionEditText.text.toString()
        val mealTypeSpinner: Spinner = findViewById(R.id.mealTypeSpinner)
        val mealType = mealTypeSpinner.selectedItem.toString()
        val servingSizeSpinner: Spinner = findViewById(R.id.servingSizeSpinner)
        val servingSize = servingSizeSpinner.selectedItem.toString()
        val ingredients = mutableListOf<Ingredient>()
        val ingredientLayout = findViewById<LinearLayout>(R.id.ingredientsLayout)

        if (recipeName.isEmpty() && ingredients.isEmpty()) {
            Toast.makeText(this, "Recipe name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        var hasValidIngredient = false
        for (i in 0 until ingredientLayout.childCount) {
            val ingredientRow = ingredientLayout.getChildAt(i) as LinearLayout
            val ingredientEditText = ingredientRow.findViewById<EditText>(R.id.ingredientEditText)
            val quantityEditText = ingredientRow.findViewById<EditText>(R.id.quantityEditText)
            val unitSpinner = ingredientRow.findViewById<Spinner>(R.id.unitSpinner)
            val ingredient = ingredientEditText.text.toString().trim()
            val quantity = quantityEditText.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()

            if (ingredient.isNotEmpty() && quantity.isNotEmpty()) {
                ingredients.add(Ingredient(ingredient, quantity, unit))
                hasValidIngredient = true
            }
        }

        if (!hasValidIngredient) {
            Toast.makeText(this, "Please add at least one valid ingredient", Toast.LENGTH_SHORT).show()
            return
        }

        val instructions = mutableListOf<Instruction>()
        val instructionLayout = findViewById<LinearLayout>(R.id.instructionsLayout)
        for (i in 0 until instructionLayout.childCount) {
            val instructionEditText = instructionLayout.getChildAt(i).findViewById<EditText>(R.id.instructionEditText)
            val instruction = instructionEditText.text.toString()
            if (instruction.isNotEmpty()) {
                instructions.add(Instruction(instruction))  // Create Instruction object
            }
        }

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            val databaseReference = FirebaseDatabase.getInstance().reference
            val userRecipesReference = databaseReference.child("users").child(userId).child("userRecipes")
            val recipe = MyRecipe(recipeId, recipeName, mealType, description, ingredients, instructions, servingSize, image)
            userRecipesReference.child(recipeId).setValue(recipe)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
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


    private fun addIngredientRow() {
        if (hasEmptyIngredientFields()) {
            Toast.makeText(this, "Please fill out all fields in ingredient.", Toast.LENGTH_SHORT).show()
            return
        }
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.ingredient_row, null)
        val unitSpinner = rowView.findViewById<Spinner>(R.id.unitSpinner)

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 8, 0, 8)  // Set top and bottom margins as needed
        rowView.layoutParams = layoutParams

        ArrayAdapter.createFromResource(
            this,
            R.array.unit_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            unitSpinner.adapter = adapter
        }

        val ingredientLayout = findViewById<LinearLayout>(R.id.ingredientsLayout)
        val removeIngredientButton = rowView.findViewById<ImageButton>(R.id.removeIngredientButton)
        if (ingredientLayout.childCount == 0) {
            removeIngredientButton.visibility = View.GONE
        } else {
            removeIngredientButton.visibility = View.VISIBLE
            removeIngredientButton.setOnClickListener {
                val parentLayout = rowView.parent as LinearLayout
                parentLayout.removeView(rowView)
            }
        }
        ingredientLayout.addView(rowView)
    }

    private var instructionCounter = 1

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

    private fun addInstructionEditText() {
        if (hasEmptyInstructionFields()) {
            Toast.makeText(this, "Please fill out Instruction.", Toast.LENGTH_SHORT).show()
            return
        }
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.instruction_row, null)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 8, 0, 8)
        rowView.layoutParams = layoutParams

        val instructionEditText = rowView.findViewById<EditText>(R.id.instructionEditText)
        val removeInstructionButton = rowView.findViewById<ImageButton>(R.id.removeInstructionButton)

        instructionEditText.hint = "Step $instructionCounter"
        instructionCounter++

        removeInstructionButton.setOnClickListener {
            val parentLayout = rowView.parent as LinearLayout
            parentLayout.removeView(rowView)
            instructionCounter -= 1
        }

        val instructionLayout = findViewById<LinearLayout>(R.id.instructionsLayout)

        if (instructionLayout.childCount == 0) {
            removeInstructionButton.visibility = View.GONE
        } else {
            removeInstructionButton.visibility = View.VISIBLE
            removeInstructionButton.setOnClickListener {
                val parentLayout = rowView.parent as LinearLayout
                parentLayout.removeView(rowView)
            }
        }
        instructionLayout.addView(rowView)
    }
}