package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Question

@Dao
interface QuestionDao {
    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE level = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsByLevel(level: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE category = :category AND level = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsByCategoryAndLevel(category: String, level: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE category = :category ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsByCategory(category: String, limit: Int): List<Question>
}
