package com.example.mealkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecipeData(
    val id: String? = null,
    val recipeName: String? = null,
    val description: String? = null,
    val ingredients: List<Ingredient>? = null,
    val instructions: List<Instruction>? = null,
    val image: String? = null,
    val servingSize: String? = null,
    val mealType: String? = null,
    var isSelected: Boolean = false // Track if the recipe is selected
) : Parcelable


@Parcelize
data class Ingredient(
    val name: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    var completed: Boolean = false // Use this property
) : Parcelable {
    fun toMap(): Map<String, String?> {
        return mapOf(
            "name" to name,
            "quantity" to quantity,
            "unit" to unit
        )
    }
}

@Parcelize
data class Instruction(
    val text: String? = null,
    val image: String? = null // Assuming you store the image URL for each instruction
) : Parcelable
