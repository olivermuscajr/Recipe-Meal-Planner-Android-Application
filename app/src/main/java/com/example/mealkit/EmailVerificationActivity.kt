package com.example.mealkit

import DeleteUnverifiedUserWorker
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mealkit.databinding.ActivityEmailVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private var email: String = ""
    private var username: String = ""
    private var password: String = ""
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        email = intent.getStringExtra("email").toString()
        username = intent.getStringExtra("username").toString()
        password = intent.getStringExtra("password").toString()

        binding.email.text = email

        sendVerificationEmail()
        startVerificationTimer()

        binding.doneButton.setOnClickListener {
            verifyEmailAndSignup()
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun sendVerificationEmail() {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                    if (verificationTask.isSuccessful) {
                        Toast.makeText(this, "Verification email sent to $email. Please verify your email.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        scheduleDeletionWorker() // Schedule the worker
                    } else {
                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVerificationTimer() {
        timer = object : CountDownTimer(300000, 1000) { // 5 minutes countdown
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.timerTextView.text = "Verify your email within ${minutes}m ${seconds}s."
            }

            override fun onFinish() {
                checkAndDeleteUnverifiedUser()
                finish()
            }
        }
        timer?.start()
    }


    private fun checkAndDeleteUnverifiedUser() {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == false) {
                    // Delete the user if not verified
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(this, "Unverified email account deleted.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete unverified account.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to sign in for deletion check.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyEmailAndSignup() {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    // Stop the timer and cancel deletion worker if verified
                    timer?.cancel()
                    WorkManager.getInstance(this).cancelAllWorkByTag("delete_unverified_user")

                    val userId = user.uid
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateCreated = dateFormat.format(Date())
                    val userData = UserData(
                        id = userId,
                        username = username,
                        email = email,
                        dateCreated = dateCreated
                    )

                    databaseReference.child(userId).setValue(userData).addOnCompleteListener { databaseTask ->
                        if (databaseTask.isSuccessful) {
                            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity2::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to sign in for verification check", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun scheduleDeletionWorker() {
        val inputData = Data.Builder()
            .putString("email", email)
            .putString("password", password)
            .build()

        val deleteUserRequest = OneTimeWorkRequestBuilder<DeleteUnverifiedUserWorker>()
            .setInitialDelay(5, TimeUnit.MINUTES) // Adjust delay as needed
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(deleteUserRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Cancel timer if the activity is destroyed
    }
}
