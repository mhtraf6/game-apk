package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val friendUserId: String,
    val name: String,
    val status: String, // "نشط" (Active) or "غائب" (Offline)
    val score: Int = 0,
    val isHelpPending: Boolean = false
)
