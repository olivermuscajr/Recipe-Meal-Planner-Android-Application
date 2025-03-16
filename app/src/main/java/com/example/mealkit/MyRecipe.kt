package com.example.mealkit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyRecipe(
    var id: String? = null,
    var recipeName: String = "",
    var mealType: String = "",
    var description: String = "",
    var ingredients: List<Ingredient>? = null, // Change to List<Ingredient>
    var instructions: List<Instruction>? = null,
    var servingSize: String? = null,
    var image: String? = null,
    var userId: String? = null
) : Parcelable

