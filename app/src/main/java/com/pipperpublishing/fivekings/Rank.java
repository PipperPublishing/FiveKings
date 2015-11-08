package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Made member variables final (initialized once only)
 * 2/3/2015: Pulled out Wild Card values into Card
 * 2/4/2015: Moved JOKER into Card and cleaned up accordingly
 * * 9/3/2015 Made Rank parcelable using http://stackoverflow.com/questions/2836256/passing-enum-or-object-through-an-intent-the-best-solution
 */
public enum Rank implements Parcelable{
    THREE(3,"3"),FOUR(4,"4"),FIVE(5,"5"),SIX(6,"6"),SEVEN(7,"7"),EIGHT(8,"8"),NINE(9,"9"),
    TEN(10,"10"),JACK(11,"J"),QUEEN(12,"Q"),KING(13,"K");

    private final int value;
    private final String string;

    static final Rank getLowestRank() { return Rank.THREE;}

    private Rank(int value, String string){
        this.value = value;
        this.string = string;
    }
    Rank getNext() {
        return this.ordinal() < values().length-1 ? values()[this.ordinal()+1] : null;
    }
    boolean isHighestRank() {
        return null == getNext();
    }
    boolean isLowestRank() {
        return 0 == this.ordinal();
    }

    //GETTERS
    public int getRankValue() {
        return value;
    }

    public int getOrdinal() {
        return this.ordinal();
    }

    public String getString() {
        return string;
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

    public static final Parcelable.Creator<Rank> CREATOR = new Parcelable.Creator<Rank>() {
        @Override
        public Rank createFromParcel(final Parcel in) {
            return Rank.valueOf(in.readString());
        }

        @Override
        public Rank[] newArray(final int size) {
            return new Rank[size];
        }
    };

}
