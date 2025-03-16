package com.example.mealkit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.database.FirebaseDatabase

class GenerateFromImage : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var imageView: ImageView
    private lateinit var generateButton: Button
    private lateinit var saveRecipeButton: Button
    private lateinit var removeButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageButton
    private lateinit var recipeNameInput: EditText

    private var selectedImageUri: Uri? = null
    private var bitmap: Bitmap? = null // Store the image bitmap after upload

    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST = 2

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aigenerate_recipe_from_image)

        textView = findViewById(R.id.textViewRecipe)
        imageView = findViewById(R.id.imageView)
        generateButton = findViewById(R.id.generateButton)
        saveRecipeButton = findViewById(R.id.saveRecipeButton)
        removeButton = findViewById(R.id.removeButton)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)
        recipeNameInput = findViewById(R.id.recipeNameEditText)

        // Disable the generate button initially
        generateButton.isEnabled = false

        imageView.setOnClickListener {
            chooseImageSource()
        }

        generateButton.setOnClickListener {
            bitmap?.let { bmp ->
                generateRecipeFromImage(bmp)
            }
        }

        saveRecipeButton.setOnClickListener {
            saveGeneratedRecipeToFirebase()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        removeButton.setOnClickListener {
            showRemoveImageConfirmationDialog()
        }
    }

    private fun chooseImageSource() {
        val options = arrayOf("Select from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery()
                1 -> openCamera()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        imageView.setImageURI(uri)
                        // Convert URI to Bitmap for consistency
                        bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        generateButton.isEnabled = true
                        removeButton.visibility = View.VISIBLE
                    }
                }
                CAMERA_REQUEST -> {
                    bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        imageView.setImageBitmap(it)
                        generateButton.isEnabled = true
                        removeButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateRecipeFromImage(bitmap: Bitmap) {
        val generativeModel = GenerativeModel("gemini-1.5-flash", "AIzaSyDCJhGBRpL2lF7Dr5OaETSXZ43BHyadokA")
        val model = GenerativeModelFutures.from(generativeModel)
        val userProvidedName = recipeNameInput.text.toString().trim()
        val recipeNameText = if (userProvidedName.isNotEmpty()) userProvidedName else "<Auto-detect Recipe Name>"

        val content: Content = Content.Builder()
            .text("Generate a recipe based on this image with the following format:\n" +
                    "Recipe Name: ${recipeNameText}\n" +
                    "Description: <description>\n" +
                    "Serving size: <servings_number>\n" +
                    "Ingredients:\n" +
                    "<quantity> <unit> - <ingredient_1>\n" +
                    "<quantity> <unit> - <ingredient_2>\n" +
                    "Instructions:\n" +
                    "1. <instruction_1>\n" +
                    "2. <instruction_2>\n" +
                    "Please ensure that each section (Recipe Name, Description, Serving size, Ingredients (US based units), Instructions) is clearly labeled and make spaces between each sections and include parenthesis after ingredients like(sliced, minced) " +
                    "If the image is not food related, then say it is not food related. Please Strictly follow the structure of Ingredients (Example: 2 cup - Rice (uncooked)")
            .image(bitmap)
            .build()

        progressBar.visibility = View.VISIBLE
        textView.text = "Generating recipe..."

        val response: ListenableFuture<GenerateContentResponse> = model.generateContent(content)
        Futures.addCallback(
            response,
            object : FutureCallback<GenerateContentResponse> {
                override fun onSuccess(result: GenerateContentResponse?) {
                    textView.text = result?.text ?: "No result returned."
                    progressBar.visibility = View.GONE
                    saveRecipeButton.visibility = View.VISIBLE
                    saveRecipeButton.isEnabled = true
                }

                override fun onFailure(t: Throwable) {
                    textView.text = "Error: ${t.message}"
                    progressBar.visibility = View.GONE
                }
            },
            this.mainExecutor
        )
    }

    private fun saveGeneratedRecipeToFirebase() {
        val recipeText = textView.text.toString()
        val recipeDetails = parseRecipeText(recipeText)
        val userId = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("loggedInUserId", null)

        if (recipeDetails != null && userId != null) {
            val userRecipesReference = FirebaseDatabase.getInstance().reference
                .child("users").child(userId).child("userGeneratedRecipes")

            val recipeId = userRecipesReference.push().key
            if (recipeId != null) {
                userRecipesReference.child(recipeId).setValue(recipeDetails)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Recipe saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "Invalid recipe data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeImage() {
        imageView.setImageResource(R.drawable.upload_image)
        bitmap = null
        selectedImageUri = null
        generateButton.isEnabled = false
        removeButton.visibility = View.GONE
    }

    private fun showRemoveImageConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm Remove Image")
            setMessage("Are you sure you want to remove the image?")
            setPositiveButton("Yes") { _, _ -> removeImage() }
            setNegativeButton("No", null)
            setCancelable(false)
            show()
        }
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

        val instructions = instructionsText?.split("\n")?.mapIndexed { index, instructionText ->
            Instruction(image = null, text = instructionText.trim())
        } ?: listOf()

        return if (name != null && description != null && servingSize != null) {
            MyRecipe(
                recipeName = name,
                description = description,
                servingSize = servingSize,
                ingredients = ingredients,
                instructions = instructions
            )
        } else {
            null
        }
    }

}


