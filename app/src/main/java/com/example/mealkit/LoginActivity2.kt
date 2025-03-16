package com.example.mealkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mealkit.databinding.ActivityLogin2Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.security.MessageDigest

class LoginActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityLogin2Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogin2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val openFromNotification = intent.getBooleanExtra("open_from_notification", false)
        val mealType = intent.getStringExtra("MEAL_TYPE")
        // Check login status using isLoggedIn flag in SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        binding.loginButton.setOnClickListener {
            val username = binding.loginUsername.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (isNetworkAvailable()) {
                    loginUserOnline(username, password, openFromNotification, mealType)
                } else {
                    loginUserOffline(username, password, openFromNotification, mealType)
                }
            } else {
                showToast("All fields are required")
            }
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.signUpRedirect.setOnClickListener {
            startActivity(Intent(this, SignupActivity2::class.java))
            finish()
        }
    }

    // Online login method with data saving to SharedPreferences
    private fun loginUserOnline(username: String, password: String, openFromNotification: Boolean, mealType: String?) {
        databaseReference.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val userData = userSnapshot.getValue(UserData::class.java)

                            if (userData != null) {
                                // Authenticate with Firebase Authentication
                                userData.email?.let {
                                    FirebaseAuth.getInstance()
                                        .signInWithEmailAndPassword(it, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                if (userData.userType == "Admin") {
                                                    showToast("Admin can't log in")
                                                    return@addOnCompleteListener
                                                }

                                                // Save user data to SharedPreferences for offline use
                                                val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                                val editor = sharedPreferences.edit()
                                                editor.putString("loggedInUserId", userData.id) // Save the user ID
                                                editor.putString("username", username)
                                                editor.putString("email", userData.email)
                                                editor.putBoolean("isLoggedIn", true)
                                                editor.apply()

                                                // Proceed to MainActivity with notification data if logged in
                                                val mainIntent = Intent(this@LoginActivity2, MainActivity::class.java)
                                                if (openFromNotification) {
                                                    mainIntent.putExtra("open_meal_plan", true)
                                                    mainIntent.putExtra("MEAL_TYPE", mealType)
                                                }
                                                showToast("Login Successful")
                                                startActivity(mainIntent)
                                                finish()
                                            } else {
                                                showToast("Incorrect password")
                                            }
                                        }
                                }
                            }
                        }
                    } else {
                        showToast("User does not exist")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showToast("Database Error: ${databaseError.message}")
                }
            })
    }


    // Offline login using cached data from SharedPreferences only
    private fun loginUserOffline(username: String, password: String, openFromNotification: Boolean, mealType: String?) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString("username", null)
        val savedPasswordHash = sharedPreferences.getString("passwordHash", null)

        if (username == savedUsername && savedPasswordHash != null && savedPasswordHash == hashPassword(password)) {
            showToast("Offline Login Successful")
            sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

            // Proceed to MainActivity with notification data if logged in
            val mainIntent = Intent(this, MainActivity::class.java)
            if (openFromNotification) {
                mainIntent.putExtra("open_meal_plan", true)
                mainIntent.putExtra("MEAL_TYPE", mealType)
            }
            startActivity(mainIntent)
            finish()
        } else {
            showToast("Invalid credentials. Please try again with an internet connection.")
        }
    }


    @SuppressLint("ServiceCast")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun hashPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(password.toByteArray())
        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
