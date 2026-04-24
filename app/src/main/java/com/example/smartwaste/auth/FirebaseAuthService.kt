package com.example.smartwaste.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            if (email.isEmpty() || password.isEmpty()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.d("FirebaseAuth", "Login successful: ${it.email}")
                Result.success(it)
            } ?: Result.failure(Exception("Login failed: user is null"))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Login error", e)
            Result.failure(e)
        }
    }

    /**
     * Register new user with email and password
     */
    suspend fun register(email: String, password: String, fullName: String): Result<FirebaseUser> {
        return try {
            if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                return Result.failure(Exception("All fields are required"))
            }
            
            if (password.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters"))
            }
            
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Save user profile to Firestore
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "fullName" to fullName,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "role" to if (email.contains("admin", ignoreCase = true)) "admin" else if (email.contains("driver", ignoreCase = true)) "driver" else "user"
                )
                
                db.collection("users").document(user.uid).set(userData).await()
                Log.d("FirebaseAuth", "Registration successful: $email")
                Result.success(user)
            } ?: Result.failure(Exception("Registration failed: user is null"))
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Registration error", e)
            Result.failure(e)
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        auth.signOut()
        Log.d("FirebaseAuth", "User logged out")
    }

    /**
     * Get current logged in user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get user role from Firestore
     */
    suspend fun getUserRole(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("role") ?: "user"
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error getting user role", e)
            "user"
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d("FirebaseAuth", "Password reset email sent to $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuth", "Error sending password reset email", e)
            Result.failure(e)
        }
    }
}
