package net.pantasystem.milktea.api.misskey.notes.favorite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFavorite(
    @SerialName("i")
    val i: String,

    @SerialName("noteId")
    val noteId: String,
)