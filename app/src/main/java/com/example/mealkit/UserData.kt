    package com.example.mealkit
    
    data class UserData(
        val id: String? = null,
        val username: String? = null,
        val email: String?=null,
        val passwordHash: String? = null,
        val dateCreated: String? = null,
        val userType: String? = "User"
    )