package com.example.pricecart.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreUserProfileStore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserProfileStore {
    override fun createUserProfile(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                mapOf(
                    "uid" to userId,
                    "email" to emailAddress,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSignedInAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    override fun recordUserSignIn(
        userId: String,
        emailAddress: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                mapOf(
                    "uid" to userId,
                    "email" to emailAddress,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSignedInAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}
