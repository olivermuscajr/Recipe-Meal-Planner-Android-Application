import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class DeleteUnverifiedUserWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val email = inputData.getString("email") ?: return Result.failure()
        val password = inputData.getString("password") ?: return Result.failure()

        val auth = Firebase.auth

        // Sign in to check if the email is verified
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == false) {
                    // Delete the user if not verified
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            // Account deletion succeeded
                        } else {
                            // Handle deletion failure (retry logic, etc.)
                        }
                    }
                }
            }
        }

        return Result.success()
    }
}
