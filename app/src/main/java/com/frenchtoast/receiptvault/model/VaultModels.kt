package com.frenchtoast.receiptvault.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class EntryType(val label: String) {
    RECEIPT("Receipt"),
    WARRANTY("Warranty"),
    DOCUMENT("Document"),
}

enum class ItemCategory(val label: String) {
    ELECTRONICS("Electronics"),
    APPLIANCES("Appliances"),
    TOOLS("Tools"),
    GAMING("Gaming"),
    OTHER("Other"),
}

enum class WindowStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    NONE,
}

enum class HomeFilterChip(val label: String) {
    ALL("All"),
    RECEIPTS("Receipts"),
    WARRANTIES("Warranties"),
    DOCUMENTS("Documents"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
}

data class VaultItem(
    val id: String,
    val entryType: EntryType = EntryType.RECEIPT,
    val title: String,
    val storeName: String,
    val category: ItemCategory,
    val purchaseDateEpochDay: Long,
    val priceCents: Long,
    val currencyCode: String = "USD",
    val serialNumber: String = "",
    val returnDeadlineEpochDay: Long? = null,
    val warrantyExpirationEpochDay: Long? = null,
    val notes: String = "",
    val rawOcrText: String = "",
    val receiptImagePath: String? = null,
    val serialImagePath: String? = null,
    val documentPath: String? = null,
    val documentDisplayName: String? = null,
    val documentMimeType: String? = null,
    val isDemo: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    fun returnDaysRemaining(todayEpochDay: Long = VaultDate.todayEpochDay()): Long? =
        returnDeadlineEpochDay?.let { it - todayEpochDay }

    fun warrantyDaysRemaining(todayEpochDay: Long = VaultDate.todayEpochDay()): Long? =
        warrantyExpirationEpochDay?.let { it - todayEpochDay }

    fun returnStatus(
        todayEpochDay: Long = VaultDate.todayEpochDay(),
        returnLeadDays: Int = 5,
    ): WindowStatus {
        val remaining = returnDaysRemaining(todayEpochDay) ?: return WindowStatus.NONE
        return when {
            remaining < 0 -> WindowStatus.EXPIRED
            remaining <= returnLeadDays -> WindowStatus.EXPIRING_SOON
            else -> WindowStatus.ACTIVE
        }
    }

    fun warrantyStatus(
        todayEpochDay: Long = VaultDate.todayEpochDay(),
        warrantyLeadDays: Int = 30,
    ): WindowStatus {
        val remaining = warrantyDaysRemaining(todayEpochDay) ?: return WindowStatus.NONE
        return when {
            remaining < 0 -> WindowStatus.EXPIRED
            remaining <= warrantyLeadDays -> WindowStatus.EXPIRING_SOON
            else -> WindowStatus.ACTIVE
        }
    }

    fun matchesHomeFilter(
        filter: HomeFilterChip,
        todayEpochDay: Long = VaultDate.todayEpochDay(),
        returnLeadDays: Int = 5,
        warrantyLeadDays: Int = 30,
    ): Boolean {
        val rStatus = returnStatus(todayEpochDay, returnLeadDays)
        val wStatus = warrantyStatus(todayEpochDay, warrantyLeadDays)
        return when (filter) {
            HomeFilterChip.ALL -> true
            HomeFilterChip.RECEIPTS -> entryType == EntryType.RECEIPT
            HomeFilterChip.WARRANTIES -> entryType == EntryType.WARRANTY
            HomeFilterChip.DOCUMENTS -> entryType == EntryType.DOCUMENT
            HomeFilterChip.EXPIRING_SOON ->
                rStatus == WindowStatus.EXPIRING_SOON || wStatus == WindowStatus.EXPIRING_SOON
            HomeFilterChip.EXPIRED -> {
                val hasDeadline = rStatus != WindowStatus.NONE || wStatus != WindowStatus.NONE
                hasDeadline &&
                    (rStatus == WindowStatus.EXPIRED || rStatus == WindowStatus.NONE) &&
                    (wStatus == WindowStatus.EXPIRED || wStatus == WindowStatus.NONE)
            }
        }
    }

    fun hasOriginalFiles(): Boolean =
        !receiptImagePath.isNullOrBlank() ||
            !serialImagePath.isNullOrBlank() ||
            !documentPath.isNullOrBlank()

    fun attachedFileCount(): Int =
        listOfNotNull(receiptImagePath, serialImagePath, documentPath)
            .count { it.isNotBlank() }

    fun formattedPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        return format.format(priceCents / 100.0)
    }

    fun primaryCountdownSummary(
        todayEpochDay: Long = VaultDate.todayEpochDay(),
        returnLeadDays: Int = 5,
        warrantyLeadDays: Int = 30,
    ): Pair<String, WindowStatus> {
        val rRemaining = returnDaysRemaining(todayEpochDay)
        val wRemaining = warrantyDaysRemaining(todayEpochDay)
        val rStatus = returnStatus(todayEpochDay, returnLeadDays)
        val wStatus = warrantyStatus(todayEpochDay, warrantyLeadDays)

        if (rRemaining != null && rRemaining >= 0) {
            val label = when (rRemaining) {
                0L -> "Return due today"
                1L -> "Return: 1 day left"
                else -> "Return: $rRemaining days left"
            }
            return label to rStatus
        }

        if (wRemaining != null) {
            val prefix = if (entryType == EntryType.DOCUMENT) "Expires" else "Warranty"
            val label = when {
                wRemaining < 0 -> "$prefix expired"
                wRemaining == 0L -> "$prefix ends today"
                wRemaining == 1L -> "$prefix: 1 day left"
                wRemaining >= 365L -> {
                    val years = wRemaining / 365L
                    val months = (wRemaining % 365L) / 30L
                    if (months > 0) "$prefix: ${years}y ${months}m left" else "$prefix: ${years}y left"
                }
                else -> "$prefix: $wRemaining days left"
            }
            return label to wStatus
        }

        if (rRemaining != null && rRemaining < 0) {
            return "Return window closed" to WindowStatus.EXPIRED
        }

        return if (entryType == EntryType.DOCUMENT) {
            "Stored document" to WindowStatus.NONE
        } else {
            "No deadlines set" to WindowStatus.NONE
        }
    }
}
