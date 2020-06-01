package jp.panta.misskeyandroidclient.model.notes.draft

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import jp.panta.misskeyandroidclient.model.notes.draft.DraftNote


data class DraftFile(
    val remoteFileId: String?,
    val filePath: String?,
    val draftNoteId: Long
){

    var fileId: Int? = null
}