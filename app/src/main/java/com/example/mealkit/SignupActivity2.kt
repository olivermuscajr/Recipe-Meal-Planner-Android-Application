package com.example.mealkit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.mealkit.databinding.ActivitySignup2Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignupActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySignup2Binding
    private lateinit var databaseReference: DatabaseReference
    private var currentUsername = ""
    private var currentPassword = ""
    private val handler = Handler(Looper.getMainLooper()) // Handler for debounce
    private var isUsernameValid = true
    private var isEmailValid = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignup2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().getReference("users")

        // Set up listeners for username and email fields
        binding.signupUsername.addTextChangedListener { text ->
            val username = text.toString().trim()
            if (username.isNotEmpty()) {
                currentUsername = username  // Update current username
                checkUsernameExists(username)
            } else {
                binding.usernameError.visibility = android.view.View.GONE
            }
        }

        // Debounce email validation
        binding.signupEmail.addTextChangedListener { text ->
            val email = text.toString().trim()

            // Remove any previous postDelayed callbacks
            handler.removeCallbacksAndMessages(null)

            // Delay email validation to avoid checking on every keystroke
            if (email.isNotEmpty()) {
                handler.postDelayed({
                    if (isValidEmail(email)) {
                        // Reset email error visibility
                        binding.emailError.visibility = android.view.View.GONE
                        // Check if the email exists in the database
                        checkEmailExists(email)
                    } else {
                        binding.emailError.visibility = android.view.View.VISIBLE
                        binding.emailError.text = "Please enter a valid email"
                    }
                }, 500) // Delay in milliseconds
            } else {
                binding.emailError.visibility = android.view.View.GONE
            }
        }

        // Handle signup button click
        binding.signupButton.setOnClickListener {
            val signupUsername = binding.signupUsername.text.toString().trim()
            val signupEmail = binding.signupEmail.text.toString().trim()
            val signUpPassword = binding.signupPassword.text.toString().trim()
            val confirmSignUpPassword = binding.signupConfirmPassword.text.toString().trim()

            if (signupUsername.isNotEmpty() && signupEmail.isNotEmpty() && signUpPassword.isNotEmpty() && confirmSignUpPassword.isNotEmpty()) {
                if (signUpPassword == confirmSignUpPassword) {
                    currentPassword = signUpPassword  // Store the password for later
                    // Validate the username and email, and proceed if valid
                    validateUsernameAndEmail(signupUsername, signupEmail, signUpPassword)
                } else {
                    Toast.makeText(this@SignupActivity2, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@SignupActivity2, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this@SignupActivity2, LoginActivity2::class.java))
            finish()
        }
    }

    // Function to check if username exists in the database
    private fun checkUsernameExists(username: String) {
        // Reset error messages before checking again
        binding.usernameError.visibility = android.view.View.GONE
        databaseReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Show error message for username
                    binding.usernameError.visibility = android.view.View.VISIBLE
                    binding.usernameError.text = "Username is already taken"
                    isUsernameValid = false
                } else {
                    isUsernameValid = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignupActivity2, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to check if email exists in the database
    private fun checkEmailExists(email: String) {
        // If username already has an error, do not proceed to email check
        if (binding.usernameError.visibility == android.view.View.VISIBLE) {
            return
        }

        // Reset email error visibility before checking email
        binding.emailError.visibility = android.view.View.GONE

        // Ensure email is not empty
        if (email.isEmpty()) {
            binding.emailError.visibility = android.view.View.VISIBLE
            binding.emailError.text = "Email cannot be empty"
            isEmailValid = false
            return
        }

        // Query the database to check if the email already exists
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Email already exists in the database, show the error message
                    binding.emailError.visibility = android.view.View.VISIBLE
                    binding.emailError.text = "Email is already registered"
                    isEmailValid = false
                } else {
                    isEmailValid = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignupActivity2, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Validate both username and email
    private fun validateUsernameAndEmail(username: String, email: String, password: String) {
        // Check if username exists first
        checkUsernameExists(username)

        // Wait for both username and email to be validated
        // Check email only if username is valid
        if (isUsernameValid) {
            checkEmailExists(email)
        }

        // If both validations pass, navigate to email verification
        if (isUsernameValid && isEmailValid) {
            navigateToEmailVerification(email, username, password)
        }
    }

    // Function to check if the email is valid
    private fun isValidEmail(email: String): Boolean {
        // Use regex to check if the email is valid
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to navigate to the email verification activity
    private fun navigateToEmailVerification(email: String, username: String, password: String) {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra("email", email)
        intent.putExtra("username", username)
        intent.putExtra("password", password)
        startActivity(intent)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
