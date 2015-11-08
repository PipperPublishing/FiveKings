package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jeffrey on 3/13/2015.
 * 3/13/2015    Subclasses Card for Joker-specific stuff
 * 11/9/2015    Added parcelable code for Joker
 */
class Joker extends Card {
    static private final int JOKER_VALUE =50;
    static private final String JOKER_STRING="Joker";

    //no arguments constructor is for Jokers
    Joker() {
        super(null, null); //TODO:B : Should be a base class without suit and rank
        this.cardString = JOKER_STRING;
        this.cardValue = JOKER_VALUE;
    }

    @Override
    final public boolean isWildCard(final Rank wildCardRank) {
        return true;
    }

    @Override
    final int getCardValue(final Rank wildCardRank) {
        return JOKER_VALUE;
    }
    @Override
    final public boolean isJoker() { return true; }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags); //will write nulls for suit/rank
    }

    public static final Parcelable.Creator<Joker> CREATOR = new Parcelable.Creator<Joker>() {
        @Override
        public Joker createFromParcel(Parcel parcel) {
            return new Joker(parcel);
        }

        @Override
        public Joker[] newArray(int size) {
            return new Joker[size];
        }
    };

    //consume the null suit, rank
    private Joker(Parcel in) {
        this();
        in.readValue(Suit.class.getClassLoader());
        in.readValue(Rank.class.getClassLoader());
    }

}
