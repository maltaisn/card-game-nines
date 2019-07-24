package com.maltaisn.nines.core.widget

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.game.Card
import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.widget.FontStyle
import com.maltaisn.cardgame.widget.SdfLabel
import com.maltaisn.cardgame.widget.card.CardActor
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.menu.table.TableView
import ktx.style.get


class HandsTable(coreSkin: Skin, cardSkin: Skin) : TableView(coreSkin, listOf(1f, 4f)) {

    private val style: HandsTableStyle = coreSkin.get()

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
                NameViewHolder(coreSkin)
            } else {
                HandViewHolder(coreSkin, cardSkin)
            }

            override fun bindViewHolder(viewHolder: ViewHolder, row: Int, column: Int) {
                if (viewHolder is NameViewHolder) {
                    viewHolder.bind(players[row].name)
                } else if (viewHolder is HandViewHolder) {
                    viewHolder.bind(players[row].cards)
                }
            }
        }

        val strings: I18NBundle = coreSkin.get()
        val headers = listOf(strings["scoreboard_hands_player"], strings["scoreboard_hands_hand"])
        headerAdapter = object : HeaderAdapter() {
            override fun createViewHolder(column: Int) = HeaderViewHolder(coreSkin)

            override fun bindViewHolder(viewHolder: ViewHolder, column: Int) {
                (viewHolder as HeaderViewHolder).bind(headers[column])
            }
        }
    }


    private inner class HeaderViewHolder(skin: Skin) : ViewHolder() {

        private val titleLabel = SdfLabel(skin, style.headerFontStyle)

        init {
            table.add(titleLabel).growX().pad(20f)
            titleLabel.setWrap(true)
            titleLabel.setAlignment(Align.center)
        }

        fun bind(header: String) {
            titleLabel.setText(header)
        }
    }

    private inner class NameViewHolder(skin: Skin) : ViewHolder() {

        private val nameLabel = SdfLabel(skin, style.playerNameFontStyle)

        init {
            table.add(nameLabel).grow().pad(20f)
            nameLabel.setWrap(true)
            nameLabel.setAlignment(Align.center)
        }

        fun bind(name: String) {
            nameLabel.setText(name)
        }
    }

    private inner class HandViewHolder(coreSkin: Skin, cardSkin: Skin) : ViewHolder() {

        private val hand = CardHand(coreSkin, cardSkin).apply {
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

        fun bind(cards: List<Card>) {
            hand.cards = cards
        }
    }


    class PlayerRow(val name: String, val cards: List<PCard>)


    class HandsTableStyle {
        lateinit var headerFontStyle: FontStyle
        lateinit var playerNameFontStyle: FontStyle
    }

}
