package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.photozen.data.local.entity.FaceEntity
import com.example.photozen.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for face and person operations.
 */
@Dao
interface FaceDao {
    
    // ==================== FACE - INSERT / UPDATE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: FaceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>)
    
    @Update
    suspend fun updateFace(face: FaceEntity)
    
    // ==================== FACE - DELETE ====================
    
    @Query("DELETE FROM faces WHERE id = :faceId")
    suspend fun deleteFace(faceId: String)
    
    @Query("DELETE FROM faces WHERE photoId = :photoId")
    suspend fun deleteFacesByPhotoId(photoId: String)
    
    @Query("DELETE FROM faces")
    suspend fun deleteAllFaces()
    
    // ==================== FACE - QUERY ====================
    
    @Query("SELECT * FROM faces WHERE id = :faceId")
    suspend fun getFace(faceId: String): FaceEntity?
    
    @Query("SELECT * FROM faces WHERE photoId = :photoId")
    fun getFacesByPhotoId(photoId: String): Flow<List<FaceEntity>>
    
    @Query("SELECT * FROM faces WHERE photoId = :photoId")
    suspend fun getFacesByPhotoIdSync(photoId: String): List<FaceEntity>
    
    @Query("SELECT * FROM faces WHERE personId = :personId ORDER BY detectedAt DESC")
    fun getFacesByPersonId(personId: String): Flow<List<FaceEntity>>
    
    @Query("SELECT * FROM faces WHERE personId = :personId ORDER BY detectedAt DESC")
    suspend fun getFacesByPersonIdSync(personId: String): List<FaceEntity>
    
    @Query("SELECT * FROM faces WHERE personId IS NULL ORDER BY detectedAt DESC")
    fun getUnassignedFaces(): Flow<List<FaceEntity>>
    
    @Query("SELECT * FROM faces WHERE personId IS NULL")
    suspend fun getUnassignedFacesSync(): List<FaceEntity>
    
    @Query("SELECT COUNT(*) FROM faces")
    fun getTotalFaceCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM faces")
    suspend fun getTotalFaceCount(): Int
    
    @Query("SELECT COUNT(*) FROM faces WHERE personId IS NULL")
    suspend fun getUnassignedFaceCount(): Int
    
    // ==================== FACE - ASSIGNMENT ====================
    
    @Query("UPDATE faces SET personId = :personId WHERE id = :faceId")
    suspend fun assignFaceToPerson(faceId: String, personId: String)
    
    @Query("UPDATE faces SET personId = :personId WHERE id IN (:faceIds)")
    suspend fun assignFacesToPerson(faceIds: List<String>, personId: String)
    
    @Query("UPDATE faces SET personId = NULL WHERE id = :faceId")
    suspend fun unassignFace(faceId: String)
    
    @Query("UPDATE faces SET personId = NULL WHERE personId = :personId")
    suspend fun unassignAllFacesFromPerson(personId: String)
    
    // ==================== PERSON - INSERT / UPDATE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersons(persons: List<PersonEntity>)
    
    @Update
    suspend fun updatePerson(person: PersonEntity)
    
    // ==================== PERSON - DELETE ====================
    
    @Query("DELETE FROM persons WHERE id = :personId")
    suspend fun deletePerson(personId: String)
    
    @Query("DELETE FROM persons")
    suspend fun deleteAllPersons()
    
    // ==================== PERSON - QUERY ====================
    
    @Query("SELECT * FROM persons WHERE id = :personId")
    suspend fun getPerson(personId: String): PersonEntity?
    
    @Query("SELECT * FROM persons WHERE id = :personId")
    fun getPersonFlow(personId: String): Flow<PersonEntity?>
    
    @Query("SELECT * FROM persons WHERE isHidden = 0 ORDER BY faceCount DESC")
    fun getAllPersons(): Flow<List<PersonEntity>>
    
    @Query("SELECT * FROM persons WHERE isHidden = 0 ORDER BY faceCount DESC")
    suspend fun getAllPersonsSync(): List<PersonEntity>
    
    @Query("SELECT * FROM persons WHERE isFavorite = 1 AND isHidden = 0 ORDER BY faceCount DESC")
    fun getFavoritePersons(): Flow<List<PersonEntity>>
    
    @Query("SELECT * FROM persons WHERE name IS NOT NULL AND isHidden = 0 ORDER BY name ASC")
    fun getNamedPersons(): Flow<List<PersonEntity>>
    
    @Query("SELECT * FROM persons WHERE name IS NULL AND isHidden = 0 ORDER BY faceCount DESC")
    fun getUnnamedPersons(): Flow<List<PersonEntity>>
    
    @Query("SELECT COUNT(*) FROM persons WHERE isHidden = 0")
    fun getPersonCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM persons WHERE isHidden = 0")
    suspend fun getPersonCount(): Int
    
    // ==================== PERSON - UPDATE ACTIONS ====================
    
    @Query("UPDATE persons SET name = :name, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updatePersonName(personId: String, name: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE persons SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updatePersonFavorite(personId: String, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE persons SET isHidden = :isHidden, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updatePersonHidden(personId: String, isHidden: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE persons SET coverFaceId = :coverFaceId, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updatePersonCover(personId: String, coverFaceId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE persons SET faceCount = :faceCount, updatedAt = :timestamp WHERE id = :personId")
    suspend fun updatePersonFaceCount(personId: String, faceCount: Int, timestamp: Long = System.currentTimeMillis())
    
    // ==================== COMBINED QUERIES ====================
    
    /**
     * Get persons with their face counts recalculated.
     */
    @Transaction
    suspend fun recalculatePersonFaceCounts() {
        val persons = getAllPersonsSync()
        for (person in persons) {
            val faceCount = getFaceCountForPerson(person.id)
            if (faceCount != person.faceCount) {
                updatePersonFaceCount(person.id, faceCount)
            }
        }
    }
    
    @Query("SELECT COUNT(*) FROM faces WHERE personId = :personId")
    suspend fun getFaceCountForPerson(personId: String): Int
    
    /**
     * Get photo IDs that contain a specific person.
     */
    @Query("SELECT DISTINCT photoId FROM faces WHERE personId = :personId")
    fun getPhotoIdsForPerson(personId: String): Flow<List<String>>
    
    @Query("SELECT DISTINCT photoId FROM faces WHERE personId = :personId")
    suspend fun getPhotoIdsForPersonSync(personId: String): List<String>
    
    /**
     * Search persons by name.
     */
    @Query("SELECT * FROM persons WHERE name LIKE :query AND isHidden = 0 ORDER BY faceCount DESC")
    fun searchPersonsByNameFlow(query: String): Flow<List<PersonEntity>>
    
    /**
     * Search persons by name (sync version).
     */
    @Query("SELECT * FROM persons WHERE name LIKE :query AND isHidden = 0 ORDER BY faceCount DESC")
    suspend fun searchPersonsByName(query: String): List<PersonEntity>
}
