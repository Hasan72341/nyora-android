package com.nyora.hasan72341.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nyora.hasan72341.mihon.extensions.repo.ExternalExtensionType

@Entity(
    tableName = "external_extension_repos",
    indices = [
        Index(value = ["type", "baseUrl"], unique = true),
        Index(value = ["type", "signingKeyFingerprint"], unique = true),
    ]
)
data class ExternalExtensionRepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ExternalExtensionType,
    val baseUrl: String,
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSuccessAt: Long,
    val lastError: String?,
    val version: String? = null,
)
