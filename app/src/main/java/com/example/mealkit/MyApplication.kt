package com.example.mealkit

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable offline persistence
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)

        // Keep the "users" node synced for accurate login/signup operations
        database.reference.child("users").keepSynced(true)
    }
}
