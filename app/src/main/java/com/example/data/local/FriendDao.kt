package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Friend
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY score DESC")
    fun getAllFriendsFlow(): Flow<List<Friend>>

    @Query("SELECT * FROM friends ORDER BY score DESC")
    suspend fun getAllFriendsSync(): List<Friend>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)

    @Query("DELETE FROM friends WHERE friendUserId = :friendId")
    suspend fun deleteFriendById(friendId: String)

    @Query("SELECT COUNT(*) FROM friends")
    suspend fun getFriendsCount(): Int
}
