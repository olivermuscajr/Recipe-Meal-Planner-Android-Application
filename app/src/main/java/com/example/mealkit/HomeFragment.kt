package com.example.mealkit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mealkit.databinding.FragmentHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class HomeFragment : Fragment(), CategoryItemAdapter.OnItemClickListener {

    private lateinit var adapter: CategoryItemAdapter
    private lateinit var databaseReference: DatabaseReference
    private val recipeList: MutableList<RecipeData> = mutableListOf()
    private val filteredRecipeList: MutableList<RecipeData> = mutableListOf()
    private lateinit var binding: FragmentHomeBinding
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseReference = FirebaseDatabase.getInstance().getReference("defaultRecipes")
        adapter = CategoryItemAdapter(filteredRecipeList, object : CategoryItemAdapter.OnItemClickListener {
            override fun onItemClick(recipe: RecipeData) {
                val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
                    putExtra("RECIPE_DATA", recipe)
                }
                startActivity(intent)
            }

            override fun onSeeAllClick() {
                val intent = Intent(requireContext(), SearchRecipeActivity::class.java)
                startActivity(intent)
            }
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter
        fetchRecipes()

        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.d("HomeFragment", "Chip clicked with ID: $checkedId")
            val category = when (checkedId) {
                R.id.chip_all -> null
                R.id.chip_breakfast -> "Breakfast"
                R.id.chip_lunch -> "Lunch"
                R.id.chip_dinner -> "Dinner"
                else -> null
            }
            filterRecipes(category)
        }

        binding.searchBar.setOnClickListener {
            val intent = Intent(requireContext(), SearchRecipeActivity::class.java)
            startActivity(intent)
        }

        binding.mealplan.setOnClickListener {
            startActivity(Intent(requireContext(), MealPlanActivity::class.java))
        }
        binding.myrecipe.setOnClickListener {
            startActivity(Intent(requireContext(), MyRecipeActivity::class.java))
        }

        binding.recipeai.setOnClickListener {
            val bottomSheetFragment = RecipeAIBottomSheetFragment()
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        binding.sidebarMenu.setOnClickListener {
            (activity as? MainActivity)?.openSidebarMenu()
        }


        // Start the handler task
        handler.post(updateGreetingTask)
    }


    private val updateGreetingTask = object : Runnable {
        override fun run() {
            updateGreeting()
            handler.postDelayed(this, 5000)
        }
    }

    private fun updateGreeting() {
        val timeOfDay = getTimeOfDay()
        binding.greeting.text = getGreetingForTimeOfDay(timeOfDay)
    }

    private fun getTimeOfDay(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            else -> R.string.good_evening
        }
    }

    private fun getGreetingForTimeOfDay(timeOfDay: Int): String {
        return getString(timeOfDay)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateGreetingTask)
    }

    private fun filterRecipes(category: String?) {
        Log.d("HomeFragment", "Filtering recipes by category: $category")
        filteredRecipeList.clear()

        val filteredList = if (category == null) {
            recipeList
        } else {
            recipeList.filter { it.mealType.equals(category, ignoreCase = true) }
        }

        // Limit to first 5 recipes
        filteredRecipeList.addAll(filteredList.take(5))

        adapter.notifyDataSetChanged()
    }

    private fun fetchRecipes() {
        binding.progressBar.visibility = View.VISIBLE
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipeList.clear()
                for (dataSnapshot in snapshot.children) {
                    val recipe = dataSnapshot.getValue(RecipeData::class.java)
                    recipe?.let { recipeList.add(it) }
                }

                // Get the currently selected chip category for filtering
                val selectedChipId = binding.chipGroup.checkedChipId
                val category = when (selectedChipId) {
                    R.id.chip_all -> null
                    R.id.chip_breakfast -> "Breakfast"
                    R.id.chip_lunch -> "Lunch"
                    R.id.chip_dinner -> "Dinner"
                    else -> null
                }
                // Apply filter
                filterRecipes(category)
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Failed to retrieve data", error.toException())
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    override fun onItemClick(recipe: RecipeData) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra("RECIPE_DATA", recipe) // Pass the recipe data
        }
        startActivity(intent)
    }

    override fun onSeeAllClick() {
        TODO("Not yet implemented")
    }
}
