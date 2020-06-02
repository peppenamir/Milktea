package jp.panta.misskeyandroidclient.model.notes.draft.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import jp.panta.misskeyandroidclient.model.notes.draft.DraftNote

@Entity(tableName = "user_id", primaryKeys = ["userId", "draft_note_id"],
        foreignKeys = [
        ForeignKey(
            childColumns = ["draft_note_id"],
            parentColumns = ["draft_note_id"],
            entity = DraftNoteDTO::class,
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )]
)
data class UserIdDTO(
    val userId: String,
    @ColumnInfo(name = "draft_note_id") val draftNoteId: Long
){

    companion object{
        fun make(userId: String, draftNoteId: Long): UserIdDTO{
            return UserIdDTO(userId, draftNoteId)
        }
    }
    fun toUserId(): String{
        return userId
    }
}