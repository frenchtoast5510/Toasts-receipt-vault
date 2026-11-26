package com.frenchtoast.receiptvault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY purchaseDateEpochDay DESC, updatedAtMillis DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY purchaseDateEpochDay DESC, updatedAtMillis DESC")
    suspend fun getAllSnapshot(): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<VaultItemEntity?>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultItemEntity?

    @Query("SELECT COUNT(*) FROM vault_items WHERE isDemo = 1")
    suspend fun countDemoItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_items WHERE isDemo = 1")
    suspend fun deleteAllDemoItems()

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()
}
