package com.example.mealkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mealkit.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RecipeAIBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = BottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set click listeners for each option
        binding.generateFromIngredients.setOnClickListener {
            // Handle "Generate Recipe from Ingredients" click
            startActivity(Intent(requireContext(), GenerateFromIngredients::class.java))
            dismiss() // Close the bottom sheet after selection
        }

        binding.generateFromImage.setOnClickListener {
            // Handle "Generate Recipe from Image" click
            startActivity(Intent(requireContext(), GenerateFromImage::class.java))
            dismiss() // Close the bottom sheet after selection
        }
    }
}
