package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jeffrey on 1/28/2015.
 * 1/28/15 Split into separate class to add string representation
 * 2/3/2015 Removed suit NONE (was here for Jokers)
 * 9/3/2015 Made Suit parcelable using http://stackoverflow.com/questions/2836256/passing-enum-or-object-through-an-intent-the-best-solution
 */
public enum Suit implements Parcelable{
    SPADES("S"),HEARTS("H"),DIAMONDS("D"),CLUBS("C"),STARS("*");

    private final String suitString;

    Suit(String suitString) {
        this.suitString = suitString;
    }

    String getString() {
        return suitString;
    }
    public int getOrdinal() {
        return this.ordinal();
    }


    /* PARCELABLE Implementation */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.toString());
    }

    public static final Creator<Suit> CREATOR = new Creator<Suit>() {
        @Override
        public Suit createFromParcel(final Parcel in) {
            return Suit.valueOf(in.readString());
        }

        @Override
        public Suit[] newArray(final int size) {
            return new Suit[size];
        }
    };
}
