package com.maltaisn.nines.core.widget

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.widget.card.CardActor
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.table.TableView
import com.maltaisn.msdfgdx.FontStyle
import com.maltaisn.msdfgdx.widget.MsdfLabel
import ktx.style.get


class HandsTable(skin: Skin, private val cardStyle: CardActor.CardStyle) :
        TableView(skin, listOf(1f, 4f)) {

    private val style: HandsTableStyle = skin.get()

    /**
     * The players with a hand shown in this table.
     */
    var players = emptyList<PlayerRow>()
        set(value) {
            field = value
            cellAdapter?.notifyChanged()
        }


    init {
        alternateColors = true

        cellAdapter = object : CellAdapter() {
            override val rowCount: Int
                get() = players.size

            override fun createViewHolder(column: Int) = if (column == 0) {
                NameViewHolder()
            } else {
                HandViewHolder()
            }

            override fun bindViewHolder(viewHolder: ViewHolder, row: Int, column: Int) {
                if (viewHolder is NameViewHolder) {
                    viewHolder.bind(players[row].name)
                } else if (viewHolder is HandViewHolder) {
                    viewHolder.bind(players[row].cards)
                }
            }
        }

        val strings: I18NBundle = skin.get()
        val headers = listOf(strings["game_summary_hands_player"], strings["game_summary_hands_hand"])
        headerAdapter = object : HeaderAdapter() {
            override fun createViewHolder(column: Int) = HeaderViewHolder()

            override fun bindViewHolder(viewHolder: ViewHolder, column: Int) {
                (viewHolder as HeaderViewHolder).bind(headers[column])
            }
        }
    }


    private inner class HeaderViewHolder : ViewHolder() {

        private val titleLabel = MsdfLabel(null, skin, style.headerFontStyle)

        init {
            table.add(titleLabel).growX().pad(20f)
            titleLabel.setWrap(true)
            titleLabel.setAlignment(Align.center)
        }

        fun bind(header: String) {
            titleLabel.setText(header)
        }
    }

    private inner class NameViewHolder : ViewHolder() {

        private val nameLabel = MsdfLabel(null, skin, style.playerNameFontStyle)

        init {
            table.add(nameLabel).grow().pad(20f)
            nameLabel.setWrap(true)
            nameLabel.setAlignment(Align.center)
        }

        fun bind(name: String) {
            nameLabel.setText(name)
        }
    }

    private inner class HandViewHolder : ViewHolder() {

        private val hand = CardHand(cardStyle).apply {
            cardSize = CardActor.SIZE_SMALL
            align = Align.bottom
            clipPercent = 0.3f
            minCardSpacing = 0f
            enabled = false
            sorter = PCard.DEFAULT_SORTER
        }

        init {
            table.add(hand).grow().pad(30f, 15f, 0f, 15f)
            table.clip = true
        }

        fun bind(cards: List<PCard>) {
            hand.cards = cards
        }
    }


    class PlayerRow(val name: String, val cards: List<PCard>)


    class HandsTableStyle(
            val headerFontStyle: FontStyle,
            val playerNameFontStyle: FontStyle)

}
