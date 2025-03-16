package com.example.mealkit

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.mealkit.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userDeletionListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        drawerLayout = binding.drawerLayout

        createNotificationChannel()

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        // Check if activity was opened from a notification and should open MealPlanActivity directly
        val openFromNotification = intent.getBooleanExtra("open_meal_plan", false)
        if (openFromNotification) {
            val mealType = intent.getStringExtra("MEAL_TYPE")
            openMealPlanActivity(mealType)
            return // Exit the current function to avoid loading the HomeFragment
        }

        // Normal behavior, load the main layout and fragments
        val fragmentToLoad = intent.getStringExtra("fragmentToLoad")
        if (fragmentToLoad == "SavedFragment") {
            replaceFragment(SavedFragment())
            binding.bottomNavigation.selectedItemId = R.id.bottom_saved
        } else {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.bottom_home
        }

        setupNavigationListeners()
        setupUserDeletionListener()
        loadUserInfo()
    }

    private fun openMealPlanActivity(mealType: String?) {
        // Open MealPlanActivity directly
        val mealPlanIntent = Intent(this, MealPlanActivity::class.java).apply {
            putExtra("MEAL_TYPE", mealType)
            putExtra("open_from_notification", true)
        }
        startActivity(mealPlanIntent)
        finish() // Close MainActivity so MealPlanActivity appears directly
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private fun setupUserDeletionListener() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)

        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
            databaseReference.keepSynced(true)

            userDeletionListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        handleUserDeletion()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            }
            databaseReference.addValueEventListener(userDeletionListener)
        }
    }

    private fun handleUserDeletion() {
        Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()

        // Clear the session (logout user)
        auth.signOut()

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("loggedInUserId").apply() // Remove user data from SharedPreferences
        sharedPreferences.edit().remove("username").apply() // Remove username (important to reset)
        sharedPreferences.edit().remove("passwordHash").apply() // Remove password (important to reset)
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply() // Set isLoggedIn to false

        // Clear offline cache
        FirebaseDatabase.getInstance().purgeOutstandingWrites()

        // Restart login activity
        val loginIntent = Intent(this, LoginActivity2::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)

        finishAffinity() // Finish all activities in the current stack
    }



    private fun setupNavigationListeners() {
        navigationView = findViewById(R.id.navigationView)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set the navigation item selected listener for the drawer
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    bottomNavigationView.selectedItemId = R.id.bottom_home // Sync bottom navigation
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_grocery -> {
                    replaceFragment(GroceryListFragment())
                    bottomNavigationView.selectedItemId = R.id.bottom_grocery // Sync bottom navigation
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_saved -> {
                    replaceFragment(SavedFragment())
                    bottomNavigationView.selectedItemId = R.id.bottom_saved // Sync bottom navigation
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_about -> {
                    val intent = Intent(this@MainActivity, AboutActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment())
                    navigationView.setCheckedItem(R.id.nav_home) // Sync drawer with bottom navigation
                    true
                }
                R.id.bottom_grocery -> {
                    replaceFragment(GroceryListFragment())
                    navigationView.setCheckedItem(R.id.nav_grocery) // Sync drawer with bottom navigation
                    true
                }
                R.id.bottom_saved -> {
                    replaceFragment(SavedFragment())
                    navigationView.setCheckedItem(R.id.nav_saved) // Sync drawer with bottom navigation
                    true
                }
                else -> false
            }
        }
    }

    fun openSidebarMenu() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment) // Replace with the fragment container view
        transaction.addToBackStack(null) // Optional: Add to back stack if you want to handle back navigation
        transaction.commit()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Logout")
            setMessage("Are you sure you want to log out?")
            setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
                val intent = Intent(this@MainActivity, LoginActivity2::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    private fun loadUserInfo() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("loggedInUserId", null)
        val username = sharedPreferences.getString("username", null)
        val email = sharedPreferences.getString("email", null)

        if (userId != null && username != null && email != null) {
            val headerView = navigationView.getHeaderView(0)
            val usernameTextView = headerView.findViewById<TextView>(R.id.username)
            val emailTextView = headerView.findViewById<TextView>(R.id.userEmail)

            usernameTextView.text = username
            emailTextView.text = email
        } else {
            Toast.makeText(this, "User data unavailable", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meal Reminder"
            val descriptionText = "Channel for meal reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("MEAL_REMINDER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::userDeletionListener.isInitialized) {
            databaseReference.removeEventListener(userDeletionListener)
        }
    }
}
