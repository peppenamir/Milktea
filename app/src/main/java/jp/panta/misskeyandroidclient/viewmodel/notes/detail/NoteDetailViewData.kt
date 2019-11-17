package jp.panta.misskeyandroidclient.viewmodel.notes.detail

import androidx.lifecycle.MutableLiveData
import jp.panta.misskeyandroidclient.model.notes.Note
import jp.panta.misskeyandroidclient.viewmodel.notes.NoteViewData
import jp.panta.misskeyandroidclient.viewmodel.notes.PlaneNoteViewData

/**
 * ノートの詳細表示となる一つのオブジェクト
 * 基本的にはPlaneNoteViewDataとは変わらないが
 * ListAdapterで詳細ビューの対象か判定する
 */
class NoteDetailViewData(note: Note) : PlaneNoteViewData(note)