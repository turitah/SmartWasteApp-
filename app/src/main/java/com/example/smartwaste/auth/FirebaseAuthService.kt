package com.example.smartwaste.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim()
            val cleanPassword = password.trim()
            
            if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
            result.user?.let {
                Log.d("FirebaseAuth", "Login successful: ${it.email}")
                Result.success(it)
            } ?: Result.failure(Exception("Login failed: user is null"))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Login error: ${e.message}", e)
            val customMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Incorrect Password. Please try again."
                is FirebaseAuthInvalidUserException -> "User not found. Please register first."
                else -> {
                    val msg = e.message ?: ""
                    if (msg.contains("network", ignoreCase = true) || 
                        msg.contains("timeout", ignoreCase = true) || 
                        msg.contains("unreachable", ignoreCase = true) ||
                        msg.contains("interrupted", ignoreCase = true)) {
                        "A network error occurred. Please check your internet connection and try again."
                    } else {
                        e.localizedMessage ?: "Login failed"
                    }
                }
            }
            Result.failure(Exception(customMessage))
        }
    }

    /**
     * Register new user with email and password
     */
    suspend fun register(email: String, password: String, fullName: String, role: String? = null): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim()
            val cleanPassword = password.trim()
            val cleanName = fullName.trim()

            if (cleanEmail.isEmpty() || cleanPassword.isEmpty() || cleanName.isEmpty()) {
                return Result.failure(Exception("All fields are required"))
            }
            
            if (cleanPassword.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters"))
            }
            
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
            result.user?.let { user ->
                val finalRole = role ?: if (cleanEmail.contains("admin", ignoreCase = true)) "admin" 
                                      else if (cleanEmail.contains("driver", ignoreCase = true)) "driver" 
                                      else "user"

                // Save user profile to Realtime Database with initial zero values
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to cleanEmail,
                    "fullName" to cleanName,
                    "createdAt" to System.currentTimeMillis(),
                    "role" to finalRole,
                    "wasteCollected" to 0.0,
                    "impact" to 0,
                    "ecoPoints" to 0
                )
                
                db.child("users").child(user.uid).setValue(userData).await()
                Log.d("FirebaseAuth", "Registration successful: $cleanEmail")
                Result.success(user)
            } ?: Result.failure(Exception("Registration failed: user is null"))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Registration error: ${e.message}", e)
            val msg = e.message ?: ""
            val customMessage = if (msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true)) {
                "A network error occurred. Please check your connection."
            } else {
                e.localizedMessage ?: "Registration failed"
            }
            Result.failure(Exception(customMessage))
        }
    }

    fun logout() {
        auth.signOut()
        Log.d("FirebaseAuth", "User logged out")
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getUserProfile(userId: String): Map<String, Any>? {
        return try {
            val snapshot = db.child("users").child(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            snapshot.value as? Map<String, Any>
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error getting user profile", e)
            null
        }
    }
}
