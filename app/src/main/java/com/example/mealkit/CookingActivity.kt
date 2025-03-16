package com.example.mealkit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.RecognitionListener
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.mealkit.databinding.ActivityCookingBinding
import java.util.*
import java.util.regex.Pattern
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Vibrator

class CookingActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityCookingBinding
    private var currentStep = 0
    private var instructions: List<Instruction> = listOf()
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val PERMISSION_REQUEST_CODE = 123
    private var countDownTimer: CountDownTimer? = null
    private var timerIsRunning = false
    private var remainingTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permission for speech recognition if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            initializeSpeechRecognizer()
        }

        // Get instructions passed from the previous activity
        instructions = intent.getParcelableArrayListExtra("INSTRUCTIONS") ?: listOf()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Show ingredient check dialog
        showIngredientCheckDialog { hasAllIngredients ->
            if (hasAllIngredients) {
                displayCurrentStep()
            } else {
                Toast.makeText(this, "Please gather all ingredients before proceeding.", Toast.LENGTH_SHORT).show()
                finish() // Close the activity if the user doesn't have all ingredients
            }
        }

        // Start listening to speech
        startListening()

        // Button listeners
        binding.nextStepButton.setOnClickListener { goToNextStep() }
        binding.backButton.setOnClickListener { goToPreviousStep() }
        binding.ttsButton.setOnClickListener {
            val instructionText = instructions[currentStep].text ?: "No instruction available"
            speakInstruction(instructionText)
        }
        binding.timerButton.setOnClickListener {
            if (timerIsRunning) {
                showTimerOptionsDialog()
            } else {
                startCountdown()
            }
        }
    }
    private fun startCountdown() {
        val instructionText = instructions[currentStep].text ?: "No instruction available"

        // Log the instruction text to check the format
        Log.d("CookingActivity", "Instruction Text: $instructionText")

        // Extract minutes or hours from the instruction text using regex
        val timePattern = Pattern.compile("(\\d+)(?:\\s*(minutes|hour)s?)")
        val matcher = timePattern.matcher(instructionText)

        if (matcher.find()) {
            val timeValue = matcher.group(1).toInt()
            val timeUnit = matcher.group(2)

            // Log the matched values
            Log.d("CookingActivity", "Matched Time: $timeValue $timeUnit")

            var timeInMillis = 0L

            if (timeUnit.equals("minutes", ignoreCase = true)) {
                timeInMillis = timeValue * 60 * 1000L
            } else if (timeUnit.equals("hour", ignoreCase = true)) {
                timeInMillis = timeValue * 60 * 60 * 1000L
            }

            // Make the timer button visible
            binding.timerButton.visibility = View.VISIBLE

            // Start the countdown timer
            startTimer(timeInMillis)
        } else {
            // Hide the timer button if no time instruction is found
            binding.timerButton.visibility = View.GONE
            Toast.makeText(this, "No valid time found in instruction", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimerOptionsDialog() {
        val options = arrayOf("Restart Timer", "Stop Timer", "Edit Timer")
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Timer Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restartTimer() // Restart the timer
                    1 -> stopTimer() // Stop the timer
                    2 -> editTimer() // Allow the user to edit the timer
                }
            }
            .create()
            .show()
    }

    private fun restartTimer() {
        countDownTimer?.cancel() // Cancel the existing timer
        startTimer(remainingTime) // Start a new timer with remaining time
    }

    private fun stopTimer() {
        countDownTimer?.cancel() // Cancel the existing timer
        timerIsRunning = false // Update the flag to reflect that the timer is no longer running
        binding.timerButton.text = "Start Timer" // Update the button text to "Start Timer"
    }

    private fun editTimer() {
        // Prompt the user to edit the time (e.g., enter new time in minutes)
        val timeInput = EditText(this).apply {
            hint = "Enter new time (in minutes)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Timer")
            .setView(timeInput)
            .setPositiveButton("OK") { _, _ ->
                val newTime = timeInput.text.toString().toIntOrNull()
                if (newTime != null && newTime > 0) {
                    val newTimeInMillis = newTime * 60 * 1000L
                    remainingTime = newTimeInMillis // Update remaining time
                    startTimer(newTimeInMillis) // Restart timer with new time
                } else {
                    Toast.makeText(this, "Invalid time entered", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }


    private fun startTimer(timeInMillis: Long) {
        // Cancel the existing timer (if any) before starting a new one
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished // Update remaining time
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.timerButton.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.timerButton.text = "Done!"
                timerIsRunning = false
                Toast.makeText(this@CookingActivity, "Time's up!", Toast.LENGTH_SHORT).show()

                // Vibration
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    // Vibrate for 500 milliseconds
                    vibrator.vibrate(500)
                }

                // Play notification sound
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
                ringtone.play()
            }
        }.start()

        timerIsRunning = true
        binding.timerButton.text = "Pause Timer" // Change button text when timer is running
    }

    private fun refreshTimerButtonVisibility() {
        val instructionText = instructions[currentStep].text ?: "No instruction available"
        val timePattern = Pattern.compile("(\\d+)(?:\\s*(minutes|hour)s?)")
        val matcher = timePattern.matcher(instructionText)

        if (matcher.find()) {
            binding.timerButton.visibility = View.VISIBLE
        } else {
            binding.timerButton.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer()
        } else {
            Toast.makeText(this, "Permission denied. Can't use speech recognition.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error code: $error")
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    else -> "Unknown error"
                }
                //Toast.makeText(this@CookingActivity, "Speech recognition error: $errorMessage", Toast.LENGTH_SHORT).show()
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    restartListening()  // Explicitly restart listening for no match or busy error
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].toLowerCase(Locale.ROOT)
                    when {
                        spokenText.contains("next") -> {
                            goToNextStep()
                            restartListening()  // Restart listening after processing
                        }
                        spokenText.contains("back") -> {
                            goToPreviousStep()
                            restartListening()  // Restart listening after processing
                        }
                        spokenText.contains("read") -> {
                            val instructionText = instructions[currentStep].text ?: "No instruction available"
                            speakInstruction(instructionText)
                            restartListening()  // Restart listening after processing
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Start listening to speech after initializing recognizer
        startListening()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'next' or 'back'") // Optional prompt
        }
        speechRecognizer.startListening(intent)
    }

    private fun restartListening() {
        // Restart speech recognition to listen for the next command
        speechRecognizer.stopListening()
        startListening()
    }

    private fun goToNextStep() {
        if (currentStep < instructions.size - 1) {
            currentStep++
            displayCurrentStep()
            refreshTimerButtonVisibility()
        } else {
            Toast.makeText(this, "You've completed all steps!", Toast.LENGTH_SHORT).show()
            finish() // Optionally, finish the activity when all steps are done
        }
    }

    private fun goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--
            displayCurrentStep()
            refreshTimerButtonVisibility()
        } else {
            Toast.makeText(this, "You're already at the first step.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayCurrentStep() {
        // Get the current instruction
        val currentInstruction = instructions[currentStep]
        val instructionText = currentInstruction.text
        val imageUrl = currentInstruction.image

        // Set the instruction text in the TextView
        binding.instructionTextView.text = "Step ${currentStep + 1}: $instructionText"

        // Speak out the instruction text only (NOT the entire Instruction object)
        textToSpeech.speak("Step ${currentStep + 1}: $instructionText", TextToSpeech.QUEUE_FLUSH, null, null)

        // Check if the image URL is null or empty
        if (imageUrl.isNullOrEmpty()) {
            // Set the placeholder if no image is available
            binding.instructionImageView.setImageResource(R.drawable.no_image_available)
            binding.noImageTextView.text = "No Image Available"  // Display no image text
        } else {
            // Load the image using Glide if image URL is present
            Glide.with(this)
                .load(imageUrl)
                .into(binding.instructionImageView)
            binding.noImageTextView.text = ""  // Clear the "No Image Available" text
        }

        // Calculate and update the progress based on the current step
        val progressPercentage = ((currentStep + 1).toFloat() / instructions.size.toFloat()) * 100
        binding.progressBar.progress = progressPercentage.toInt()

        binding.backmenuButton.setOnClickListener {
            onBackPressed()
        }


        // Display current step number
        binding.stepLabel.text = "Step ${currentStep + 1} of ${instructions.size}"
    }



    private fun speakInstruction(instructionText: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak("Step ${currentStep + 1}: $instructionText", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Text-to-Speech initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            // Set the speech rate (default is 1.0, lower values will slow it down)
            textToSpeech.setSpeechRate(0.7f)  // Slow down the speech rate (range 0.1 to 2.0)
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Shutdown Text-to-Speech and Speech Recognizer to free up resources when the activity is destroyed
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
        super.onDestroy()
    }

    private fun showIngredientCheckDialog(onConfirmed: (Boolean) -> Unit) {
        val ingredients = intent.getParcelableArrayListExtra<Ingredient>("INGREDIENTS") ?: listOf()
        val ingredientList = ingredients.joinToString("\n") { "${it.quantity} ${it.unit} of ${it.name}" }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Check Ingredients")
            .setMessage("Do you have the following ingredients?\n\n$ingredientList")
            .setPositiveButton("Yes") { _, _ -> onConfirmed(true) }
            .setNegativeButton("No") { _, _ -> onConfirmed(false) }
            .create()
            .show()
    }
}
