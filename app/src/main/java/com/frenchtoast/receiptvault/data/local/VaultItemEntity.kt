package com.frenchtoast.receiptvault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultItem
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val entryType: String = EntryType.RECEIPT.name,
    val title: String,
    val storeName: String,
    val category: String,
    val purchaseDateEpochDay: Long,
    val priceCents: Long,
    val currencyCode: String,
    val serialNumber: String,
    val returnDeadlineEpochDay: Long?,
    val warrantyExpirationEpochDay: Long?,
    val notes: String,
    val rawOcrText: String,
    val receiptImagePath: String?,
    val serialImagePath: String?,
    val documentPath: String? = null,
    val documentDisplayName: String? = null,
    val documentMimeType: String? = null,
    val isDemo: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun toDomain(): VaultItem = VaultItem(
        id = id,
        entryType = runCatching { EntryType.valueOf(entryType) }.getOrDefault(EntryType.RECEIPT),
        title = title,
        storeName = storeName,
        category = runCatching { ItemCategory.valueOf(category) }.getOrDefault(ItemCategory.OTHER),
        purchaseDateEpochDay = purchaseDateEpochDay,
        priceCents = priceCents,
        currencyCode = currencyCode,
        serialNumber = serialNumber,
        returnDeadlineEpochDay = returnDeadlineEpochDay,
        warrantyExpirationEpochDay = warrantyExpirationEpochDay,
        notes = notes,
        rawOcrText = rawOcrText,
        receiptImagePath = receiptImagePath,
        serialImagePath = serialImagePath,
        documentPath = documentPath,
        documentDisplayName = documentDisplayName,
        documentMimeType = documentMimeType,
        isDemo = isDemo,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

    companion object {
        fun fromDomain(item: VaultItem): VaultItemEntity = VaultItemEntity(
            id = item.id,
            entryType = item.entryType.name,
            title = item.title,
            storeName = item.storeName,
            category = item.category.name,
            purchaseDateEpochDay = item.purchaseDateEpochDay,
            priceCents = item.priceCents,
            currencyCode = item.currencyCode,
            serialNumber = item.serialNumber,
            returnDeadlineEpochDay = item.returnDeadlineEpochDay,
            warrantyExpirationEpochDay = item.warrantyExpirationEpochDay,
            notes = item.notes,
            rawOcrText = item.rawOcrText,
            receiptImagePath = item.receiptImagePath,
            serialImagePath = item.serialImagePath,
            documentPath = item.documentPath,
            documentDisplayName = item.documentDisplayName,
            documentMimeType = item.documentMimeType,
            isDemo = item.isDemo,
            createdAtMillis = item.createdAtMillis,
            updatedAtMillis = item.updatedAtMillis,
        )
    }
}
