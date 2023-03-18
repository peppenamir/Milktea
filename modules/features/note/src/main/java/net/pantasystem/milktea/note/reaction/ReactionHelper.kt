package net.pantasystem.milktea.note.reaction

import android.annotation.SuppressLint
import android.widget.LinearLayout
import net.pantasystem.milktea.model.nodeinfo.NodeInfo
import net.pantasystem.milktea.model.notes.reaction.Reaction
import net.pantasystem.milktea.note.R

object ReactionHelper {

    @SuppressLint("UseCompatLoadingForDrawables")
    fun LinearLayout.applyBackgroundColor(reaction: ReactionViewData, nodeInfo: NodeInfo?){

        // NOTE: Misskeyはローカルに存在するカスタム絵文字しかリアクションすることができない
        if (nodeInfo?.type is NodeInfo.SoftwareType.Misskey) {
            if(!Reaction(reaction.reaction).isLocal()) {
                this.background = context.resources.getDrawable(R.drawable.shape_normal_reaction_backgruond, context.theme).apply {
                    alpha = 75
                }
                return
            }
        }

        if(reaction.isMyReaction){
            this.background = context.resources.getDrawable(R.drawable.shape_selected_reaction_background, context.theme)
        }else{
            this.background = context.resources.getDrawable(R.drawable.shape_normal_reaction_backgruond, context.theme)
        }
    }
}