package jp.panta.misskeyandroidclient.impl.notes

import jp.panta.misskeyandroidclient.model.notes.Note
import jp.panta.misskeyandroidclient.model.notes.NoteRepository

class InMemoryNoteRepository : NoteRepository {

    private val notes = HashMap<String, Note>()

    override suspend fun get(noteId: String): Note? {
        synchronized(notes){
            return notes[noteId]
        }
    }

    /**
     * @param note 追加するノート
     * @return ノートが新たに追加されるとtrue、上書きされた場合はfalseが返されます。
     */
    fun add(note: Note): Boolean {
        synchronized(notes){
            val n = this.notes[note.id]
            this.notes[note.id] = note
            return n == null
        }
    }

    /**
     * @param noteId 削除するNoteのid
     * @return 実際に削除されるとtrue、そもそも存在していなかった場合にはfalseが返されます
     */
    fun remove(noteId: String): Boolean {
        synchronized(notes){
            val n = this.notes[noteId]
            this.notes.remove(noteId)
            return n != null
        }
    }
}