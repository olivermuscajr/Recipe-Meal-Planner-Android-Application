    package com.example.mealkit

    import android.annotation.SuppressLint
    import android.app.AlarmManager
    import android.app.PendingIntent
    import android.app.TimePickerDialog
    import android.content.Context
    import android.content.Intent
    import android.graphics.Paint
    import android.os.Bundle
    import android.view.View
    import android.widget.Button
    import android.widget.ImageButton
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.applandeo.materialcalendarview.CalendarDay
    import com.google.firebase.database.DatabaseReference
    import com.google.firebase.database.FirebaseDatabase
    import java.text.SimpleDateFormat
    import java.util.Calendar
    import com.applandeo.materialcalendarview.CalendarView
    import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
    import java.util.Locale
    import android.os.Handler
    import android.os.Looper
    import java.util.*

    class MealPlanActivity : AppCompatActivity() {

        private lateinit var breakfastRecyclerView: RecyclerView
        private lateinit var lunchRecyclerView: RecyclerView
        private lateinit var dinnerRecyclerView: RecyclerView
        private lateinit var calendarView: CalendarView
        private lateinit var dateTextView: TextView
        private lateinit var dayTextView: TextView
        private lateinit var breakfastAdapter: RecipeSelectionAdapter
        private lateinit var lunchAdapter: RecipeSelectionAdapter
        private lateinit var dinnerAdapter: RecipeSelectionAdapter
        private lateinit var databaseReference: DatabaseReference
        private lateinit var breakfastTimeTextView: TextView
        private lateinit var lunchTimeTextView: TextView
        private lateinit var dinnerTimeTextView: TextView
        private lateinit var timeTextView: TextView
        private var selectedDate: String? = null // To store the selected date
        private var currentlySelectedDate: CalendarDay? = null
        private val handler = Handler(Looper.getMainLooper()) // To update time periodically
        private val updateTimeRunnable = object : Runnable {
            override fun run() {
                updateCurrentTime() // Update the time
                handler.postDelayed(this, 1000) // Update every second
            }
        }

        companion object {
            private const val REQUEST_CODE_ADD_RECIPES = 1
        }

        @SuppressLint("SimpleDateFormat")
        private fun isToday(calendar: Calendar): Boolean {
            val today = Calendar.getInstance()
            return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }
        // Function to update the date text with "(Today)" if the date is today's date
        @SuppressLint("SetTextI18n")
        private fun updateDateText(calendar: Calendar) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            dateTextView.text = if (isToday(calendar)) {
                "${dateFormat.format(calendar.time)} (Today)"
            } else {
                dateFormat.format(calendar.time)
            }

            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayTextView.text = dayFormat.format(calendar.time)
        }

        private fun updateCurrentTime() {
            val currentTimeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val currentTime = currentTimeFormat.format(Date())
            timeTextView.text = currentTime
        }

        @SuppressLint("DefaultLocale")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_mealplan)

            val mealTypeFromNotification = intent.getStringExtra("MEAL_TYPE")
            if (mealTypeFromNotification != null) {
                Toast.makeText(this, "Opened from notification for $mealTypeFromNotification", Toast.LENGTH_SHORT).show()
            }

            // Initialize TextView elements
            dateTextView = findViewById(R.id.dateTextView)
            dayTextView = findViewById(R.id.dayTextView)
            timeTextView = findViewById(R.id.timeTextView)

            breakfastTimeTextView = findViewById(R.id.breakfastTimeTextView)
            breakfastTimeTextView.paintFlags = breakfastTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            lunchTimeTextView = findViewById(R.id.lunchTimeTextView)
            lunchTimeTextView.paintFlags = lunchTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            dinnerTimeTextView = findViewById(R.id.dinnerTimeTextView)
            dinnerTimeTextView.paintFlags = dinnerTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            databaseReference = FirebaseDatabase.getInstance().reference
            initializeRecyclerViews()

            // Initialize CalendarView and set a date change listener
            calendarView = findViewById(R.id.calendarView)

            // Set the selected date to today's date
            val calendar = Calendar.getInstance()
            val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1) // Month is 0-indexed
            val year = calendar.get(Calendar.YEAR)
            selectedDate = "$day-$month-$year"

            // Update UI with the selected date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

            dateTextView.text = dateFormat.format(calendar.time)
            dayTextView.text = dayFormat.format(calendar.time)

            currentlySelectedDate = CalendarDay(calendar)
            currentlySelectedDate?.backgroundResource = R.drawable.selected_date_background

            fetchMealPlanData(selectedDate!!)
            highlightMealPlanDates()

            findViewById<Button>(R.id.addBreakfastButton).setOnClickListener {
                openAddRecipesActivity("Breakfast")
            }
            findViewById<Button>(R.id.addLunchButton).setOnClickListener {
                openAddRecipesActivity("Lunch")
            }
            findViewById<Button>(R.id.addDinnerButton).setOnClickListener {
                openAddRecipesActivity("Dinner")
            }

            findViewById<ImageButton>(R.id.backButton).setOnClickListener {
                onBackPressed()
            }


            calendarView = findViewById(R.id.calendarView)
            updateDateText(calendar)

            handler.post(updateTimeRunnable)

            // Set up CalendarView day click listener
            calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
                override fun onClick(calendarDay: CalendarDay) {
                    // Remove background from the previously selected date
                    currentlySelectedDate?.let { previousDate ->
                        previousDate.backgroundResource = 0
                    }

                    // Update currently selected date
                    currentlySelectedDate = calendarDay
                    calendarDay.backgroundResource = R.drawable.selected_date_background

                    // Update selectedDate format
                    val selectedCalendar = calendarDay.calendar
                    val day = String.format("%02d", selectedCalendar.get(Calendar.DAY_OF_MONTH))
                    val month = String.format("%02d", selectedCalendar.get(Calendar.MONTH) + 1)
                    val year = selectedCalendar.get(Calendar.YEAR)
                    selectedDate = "$day-$month-$year"

                    updateDateText(selectedCalendar)

                    fetchMealPlanData(selectedDate!!)

                    highlightMealPlanDates()
                }
            })


        }
        override fun onPause() {
            super.onPause()
            handler.removeCallbacks(updateTimeRunnable) // Stop updating when activity is paused
        }
        override fun onResume() {
            super.onResume()
            handler.post(updateTimeRunnable) // Resume updating when activity is resumed
        }

        private fun openAddRecipesActivity(targetMealSection: String) {
            if (selectedDate != null) {
                val intent = Intent(this, AddRecipesActivity::class.java).apply {
                    putExtra("TARGET_MEAL_SECTION", targetMealSection) // Section to add recipes to
                    putExtra("SELECTED_DATE", selectedDate)
                }
                startActivityForResult(intent, REQUEST_CODE_ADD_RECIPES)
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            }
        }


        private fun highlightMealPlanDates() {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId != null) {
                val mealPlansReference = databaseReference.child("users").child(userId).child("mealPlans")
                mealPlansReference.get().addOnSuccessListener { snapshot ->
                    val calendars = ArrayList<CalendarDay>()

                    // Highlight dates with meal plans
                    for (dateSnapshot in snapshot.children) {
                        val date = dateSnapshot.key ?: continue
                        val dateParts = date.split("-")
                        if (dateParts.size == 3) {
                            val day = dateParts[0].toInt()
                            val month = dateParts[1].toInt() - 1 // Month is 0-indexed
                            val year = dateParts[2].toInt()
                            val calendar = Calendar.getInstance().apply {
                                set(year, month, day)
                            }
                            val calendarDay = CalendarDay(calendar)
                            calendarDay.backgroundResource = R.drawable.calendar_background_highlight
                            calendars.add(calendarDay)
                        }
                    }

                    // Add currently selected date if it’s not null and not in highlighted dates
                    currentlySelectedDate?.let {
                        it.backgroundResource = R.drawable.selected_date_background
                        if (!calendars.contains(it)) {
                            calendars.add(it)
                        }
                    }

                    // Set the highlighted days on the calendar view
                    calendarView.setCalendarDays(calendars)
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load meal plan dates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun initializeRecyclerViews() {
            breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
            lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
            dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)

            breakfastRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            lunchRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            dinnerRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

            breakfastAdapter = createRecipeAdapter("Breakfast")
            lunchAdapter = createRecipeAdapter("Lunch")
            dinnerAdapter = createRecipeAdapter("Dinner")

            breakfastRecyclerView.adapter = breakfastAdapter
            lunchRecyclerView.adapter = lunchAdapter
            dinnerRecyclerView.adapter = dinnerAdapter
        }

        private fun createRecipeAdapter(mealType: String): RecipeSelectionAdapter {
            return RecipeSelectionAdapter(mutableListOf(), object : RecipeSelectionAdapter.OnRecipeSelectListener {
                override fun onItemClick(recipe: RecipeData) {
                    val intent = Intent(this@MealPlanActivity, RecipeDetailActivity::class.java).apply {
                        putExtra("RECIPE_DATA", recipe)
                    }
                    startActivity(intent)
                }

                override fun onRecipeSelected(recipe: RecipeData) {
                    // Not used in view mode
                }

                override fun onRecipeDeselected(recipe: RecipeData) {
                    // Not used in view mode
                }

                override fun onDeleteRecipe(recipe: RecipeData) {
                    // Implement the logic to delete the recipe from the meal plan
                    deleteRecipeFromMealPlan(recipe, mealType)
                }

                override fun onToggleRecipeInMealPlan(recipe: RecipeData, isSelected: Boolean) {
                    TODO("Not yet implemented")
                }
            }, true) // Pass true to indicate view mode
        }

        private fun deleteRecipeFromMealPlan(recipe: RecipeData, mealType: String) {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId != null && selectedDate != null) {
                val mealPlanRef = databaseReference.child("users").child(userId).child("mealPlans").child(selectedDate!!)

                mealPlanRef.child(mealType)
                    .child(recipe.id!!)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Recipe removed from meal plan", Toast.LENGTH_SHORT).show()
                        cancelMealReminder(mealType) // Cancel the corresponding reminder
                        fetchMealPlanData(selectedDate!!) // Refresh meal plan after deletion
                        highlightMealPlanDates()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to remove recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun cancelMealReminder(mealType: String) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, MealReminderReceiver::class.java)

            val requestCode = when (mealType) {
                "Breakfast" -> 1001
                "Lunch" -> 1002
                "Dinner" -> 1003
                else -> 1000
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }


        private fun fetchMealPlanData(date: String) {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId != null) {
                val mealPlansReference = databaseReference.child("users").child(userId).child("mealPlans").child(date)

                mealPlansReference.get().addOnSuccessListener { snapshot ->

                    // Fetch recipes for each meal type excluding "reminderTime"
                    val breakfastRecipes = snapshot.child("Breakfast").children
                        .filter { it.key != "reminderTime" }
                        .mapNotNull { it.getValue(RecipeData::class.java) }

                    val lunchRecipes = snapshot.child("Lunch").children
                        .filter { it.key != "reminderTime" }
                        .mapNotNull { it.getValue(RecipeData::class.java) }

                    val dinnerRecipes = snapshot.child("Dinner").children
                        .filter { it.key != "reminderTime" }
                        .mapNotNull { it.getValue(RecipeData::class.java) }

                    // Update adapters with fetched data
                    breakfastAdapter.updateRecipes(breakfastRecipes)
                    lunchAdapter.updateRecipes(lunchRecipes)
                    dinnerAdapter.updateRecipes(dinnerRecipes)

                    // Show or hide "Empty Recipe" text based on the recipes available
                    findViewById<TextView>(R.id.emptyBreakfastTextView).visibility = if (breakfastRecipes.isEmpty()) View.VISIBLE else View.GONE
                    findViewById<TextView>(R.id.emptyLunchTextView).visibility = if (lunchRecipes.isEmpty()) View.VISIBLE else View.GONE
                    findViewById<TextView>(R.id.emptyDinnerTextView).visibility = if (dinnerRecipes.isEmpty()) View.VISIBLE else View.GONE

                    // Handle breakfast time TextView and remove reminder if empty
                    if (breakfastRecipes.isNotEmpty()) {
                        breakfastTimeTextView.isClickable = true
                        breakfastTimeTextView.paintFlags = breakfastTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        breakfastTimeTextView.setOnClickListener {
                            showTimePickerForMeal("Breakfast")
                        }
                        val breakfastReminderTime = snapshot.child("Breakfast").child("reminderTime").getValue(String::class.java)
                        breakfastTimeTextView.text = breakfastReminderTime ?: "Not Set"
                    } else {
                        mealPlansReference.child("Breakfast").child("reminderTime").removeValue()
                        breakfastTimeTextView.isClickable = false
                        breakfastTimeTextView.text = "Not Set"
                    }

                    // Handle lunch time TextView and remove reminder if empty
                    if (lunchRecipes.isNotEmpty()) {
                        lunchTimeTextView.isClickable = true
                        lunchTimeTextView.paintFlags = breakfastTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        lunchTimeTextView.setOnClickListener {
                            showTimePickerForMeal("Lunch")
                        }
                        val lunchReminderTime = snapshot.child("Lunch").child("reminderTime").getValue(String::class.java)
                        lunchTimeTextView.text = lunchReminderTime ?: "Not Set"
                    } else {
                        mealPlansReference.child("Lunch").child("reminderTime").removeValue()
                        lunchTimeTextView.isClickable = false
                        lunchTimeTextView.text = "Not Set"
                    }

                    // Handle dinner time TextView and remove reminder if empty
                    if (dinnerRecipes.isNotEmpty()) {
                        dinnerTimeTextView.isClickable = true
                        dinnerTimeTextView.paintFlags = breakfastTimeTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        dinnerTimeTextView.setOnClickListener {
                            showTimePickerForMeal("Dinner")
                        }
                        val dinnerReminderTime = snapshot.child("Dinner").child("reminderTime").getValue(String::class.java)
                        dinnerTimeTextView.text = dinnerReminderTime ?: "Not Set"
                    } else {
                        mealPlansReference.child("Dinner").child("reminderTime").removeValue()
                        dinnerTimeTextView.isClickable = false
                        dinnerTimeTextView.text = "Not Set"
                    }

                    // Update calendar highlights after fetching meal plan data
                    highlightMealPlanDates()

                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load meal plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please ensure you are logged in", Toast.LENGTH_SHORT).show()
            }
        }


        @Deprecated("This method has been deprecated...")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == REQUEST_CODE_ADD_RECIPES && resultCode == RESULT_OK) {
                selectedDate = data?.getStringExtra("SELECTED_DATE")
                val mealType = data?.getStringExtra("MEAL_TYPE") ?: "Meal"

                if (selectedDate != null) {
                    fetchMealPlanData(selectedDate!!)
                    highlightMealPlanDates()
                    showTimePickerForMeal(mealType)
                    Toast.makeText(this, "Recipes successfully added to $mealType", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun showTimePickerForMeal(mealType: String) {
            // Check if there are recipes for the selected meal type
            val adapter = when (mealType) {
                "Breakfast" -> breakfastAdapter
                "Lunch" -> lunchAdapter
                "Dinner" -> dinnerAdapter
                else -> null
            }

            /*if (adapter == null || adapter.itemCount == 0) {
                Toast.makeText(this, "No recipes selected for $mealType", Toast.LENGTH_SHORT).show()
                return
            }*/

            val calendar = Calendar.getInstance()
            val initialHour: Int
            val minHour: Int
            val minMinute: Int
            val maxHour: Int
            val maxMinute: Int

            // Define time ranges for each meal type in 24-hour format
            when (mealType) {
                "Breakfast" -> {
                    initialHour = 8
                    minHour = 4
                    minMinute = 0
                    maxHour = 11
                    maxMinute = 0
                }
                "Lunch" -> {
                    initialHour = 12
                    minHour = 11
                    minMinute = 0
                    maxHour = 15
                    maxMinute = 0
                }
                "Dinner" -> {
                    initialHour = 18
                    minHour = 18
                    minMinute = 0
                    maxHour = 23
                    maxMinute = 0
                }
                else -> {
                    initialHour = 8
                    minHour = 4
                    minMinute = 0
                    maxHour = 11
                    maxMinute = 0
                }
            }

            // Function to show the TimePickerDialog with custom validation
            fun showValidatedTimePicker() {
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        // Check if the selected time falls within the allowed range
                        val isValidTime = when (mealType) {
                            "Breakfast" -> hourOfDay in minHour..maxHour && (hourOfDay != maxHour || minute <= maxMinute)
                            "Lunch" -> hourOfDay in minHour..maxHour && (hourOfDay != minHour || minute >= minMinute)
                            "Dinner" -> hourOfDay in minHour..maxHour && (hourOfDay != minHour || minute >= minMinute)
                            else -> false
                        }

                        if (isValidTime) {
                            scheduleMealReminder(mealType, hourOfDay, minute)
                            updateMealTimeTextView(mealType, hourOfDay, minute)
                        } else {
                            // Convert min and max hours to 12-hour format with AM/PM
                            val formattedMinHour = if (minHour % 12 == 0) 12 else minHour % 12
                            val minAmPm = if (minHour < 12) "AM" else "PM"
                            val formattedMaxHour = if (maxHour % 12 == 0) 12 else maxHour % 12
                            val maxAmPm = if (maxHour < 12) "AM" else "PM"

                            // Format the min and max times
                            val formattedMinTime = String.format("%02d:%02d %s", formattedMinHour, minMinute, minAmPm)
                            val formattedMaxTime = String.format("%02d:%02d %s", formattedMaxHour, maxMinute, maxAmPm)

                            Toast.makeText(
                                this,
                                "$mealType reminder time must be between $formattedMinTime and $formattedMaxTime",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Reopen the dialog if the selected time is invalid
                            showValidatedTimePicker()
                        }
                    },
                    initialHour, // Use the initial hour in 24-hour format
                    0,
                    false // Set to false for 12-hour view with AM/PM
                )

                // Handle dialog cancellation
                timePicker.setOnCancelListener {
                    Toast.makeText(this, "Time selection canceled", Toast.LENGTH_SHORT).show()
                }

                timePicker.show()
            }

            // Show the time picker initially
            showValidatedTimePicker()
        }



        private fun updateMealTimeTextView(mealType: String, hourOfDay: Int, minute: Int) {
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val formattedHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val formattedTime = String.format("%02d:%02d %s", formattedHour, minute, amPm)

            when (mealType) {
                "Breakfast" -> breakfastTimeTextView.text = formattedTime
                "Lunch" -> lunchTimeTextView.text = formattedTime
                "Dinner" -> dinnerTimeTextView.text = formattedTime
            }
        }


        // Schedule the meal reminder based on the selected time
        @SuppressLint("ScheduleExactAlarm", "DefaultLocale")
        private fun scheduleMealReminder(mealType: String, hour: Int, minute: Int) {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("loggedInUserId", null)

            if (userId == null || selectedDate == null) {
                Toast.makeText(this, "User not logged in or date not selected", Toast.LENGTH_SHORT).show()
                return
            }

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, MealReminderReceiver::class.java).apply {
                putExtra("MEAL_TYPE", mealType)
                putExtra("USER_ID", userId)
            }

            val requestCode = when (mealType) {
                "Breakfast" -> 1001
                "Lunch" -> 1002
                "Dinner" -> 1003
                else -> 1000
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                time = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(selectedDate!!)!!
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            // Ensure the reminder is in the future
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Use setExactAndAllowWhileIdle for Doze mode compatibility
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            // Save the reminder time locally for offline persistence
            val formattedTime = String.format(
                "%02d:%02d %s",
                if (hour % 12 == 0) 12 else hour % 12,
                minute,
                if (hour < 12) "AM" else "PM"
            )
            sharedPreferences.edit().putString("${mealType}_ReminderTime", formattedTime).apply()

            // Save the reminder time to Firebase database for backup
            val mealPlansReference = databaseReference.child("users")
                .child(userId)
                .child("mealPlans")
                .child(selectedDate!!)
                .child(mealType)

            mealPlansReference.child("reminderTime").setValue(formattedTime)
                .addOnSuccessListener {
                    Toast.makeText(this, "$mealType reminder scheduled for $formattedTime", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
