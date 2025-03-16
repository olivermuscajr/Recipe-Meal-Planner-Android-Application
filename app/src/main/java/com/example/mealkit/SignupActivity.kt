package com.example.mealkit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.mealkit.databinding.ActivitySignupBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        binding.nextButton.setOnClickListener {
            val signupUsername = binding.signupUsername.text.toString().trim()
            val signupEmail = binding.signupEmail.text.toString().trim()
            val signUpPassword = binding.signupPassword.text.toString().trim()
            val confirmSignUpPassword = binding.signupConfirmPassword.text.toString().trim()

            if(signupUsername.isNotEmpty() && signUpPassword.isNotEmpty() && confirmSignUpPassword.isNotEmpty()){
                if (signUpPassword == confirmSignUpPassword) {
                    signupUser(signupUsername, signupEmail, signUpPassword)
                } else {
                    Toast.makeText(this@SignupActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@SignupActivity, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirect.setOnClickListener{
            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun signupUser(username: String, email: String, password: String) {
        databaseReference.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        val id = databaseReference.push().key
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dateCreated = dateFormat.format(Date())
                        val userData = UserData(id, email, username, dateCreated)
                        databaseReference.child(id!!).setValue(userData)
                        Toast.makeText(this@SignupActivity, "Signup Successful", Toast.LENGTH_SHORT)
                            .show()
                        startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            "User already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@SignupActivity,
                        "Database Error: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}