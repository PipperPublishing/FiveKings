package com.example.jeffrey.fivekings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jeffrey on 1/22/2015.
 * Base Class for other collections of cards (e.g. Deck, DiscardPile)
 * Provides basic functions like shuffling, removing, adding
 * 2/1/2015 replaced "cards" member variable with this and commented out unnecessary Overrides
 * 2/3/2015 pushed card scoring down into Card
 * 2/5/2015 Call Collections.shuffle()
 * 2/25/2015    Deprecated myShuffle
 * 3/4/2015 Removed deprecated methods myShuffle and getHighestScoreCard
 */
class CardList extends ArrayList<Card> {

    protected CardList() {
        super();
    }

    protected CardList(int capacity) {
        super(capacity);
    }

    protected CardList(CardList cards) {
        super(cards);
    }

    CardList(Card card) {
        this();
        this.add(card);
    }

    protected boolean shuffle() {
        Collections.shuffle(this);
        return true;
    }

    protected Card deal() {
        return remove(0);
    }

    //TODO:B: Is this an interface? is not meaningful in the base class (but is called by DrawPile)
    protected Card peekNext() {
        return (0== this.size() ? null : this.get(0));
    }


    List<Card> getCards() {
        return this;
    }



    String getString() {
        if (this.isEmpty()) return "";
        StringBuilder sCards = new StringBuilder(2 * this.size());
        sCards.append("(");
        for (Card card : this) {
            sCards.append(card.getCardString());
            sCards.append(" ");
        }
        sCards.append(")");
        return sCards.toString();
    }


}
