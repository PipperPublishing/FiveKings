package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 1/22/2015.
 * Base Class for other collections of cards (e.g. Deck, DiscardPile)
 * Provides basic functions like shuffling, removing, adding
 * 2/1/2015 replaced "cards" member variable with this and commented out unnecessary Overrides
 * 2/3/2015 pushed card scoring down into Card
 * 2/5/2015 Call Collections.shuffle()
 * 2/25/2015    Deprecated myShuffle
 * 3/4/2015 Removed deprecated methods myShuffle and getHighestScoreCard
 * 3/21/2015    Moved  shuffle(), deal(), peekNext should be moved to the Deck/DrawPile/DiscardPile class via CardDealing interface
 * 10/19/2015   Removed deprecated CardList(Card); cleaned up getString
 * 11/9/2015    Implement Parcelable by creating custom writeToParcel and createFromParcel
 *              Pass Card.classloader to read/write
 */
public class CardList extends ArrayList<Card> implements Parcelable {

    protected CardList() {
        super();
    }

    protected CardList(int capacity) {
        super(capacity);
    }

    protected CardList(CardList cards) {
        super(cards);
    }

    String getString() {
        StringBuilder sCards = new StringBuilder(2 * this.size() + 1);
        if (this.isEmpty()) sCards.append("");
        else {
            sCards.append("(");
            for (Card card : this) {
                sCards.append(card.getCardString());
                sCards.append(" ");
            }
            sCards.append(")");
        }
        return sCards.toString();
    }

    /* PARCELING write and creation methods */

    public CardList(Parcel parcel) {
        parcel.readList(this, Card.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeList(this);
    }

    // Method to recreate a CardList from a Parcel
    public static Creator<CardList> CREATOR = new Creator<CardList>() {

        @Override
        public CardList createFromParcel(Parcel parcel) {
            return new CardList(parcel);
        }

        @Override
        public CardList[] newArray(int size) {
            return new CardList[size];
        }

    };

}
