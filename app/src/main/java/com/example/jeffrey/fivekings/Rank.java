package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/2/2015 Made member variables final (initialized once only)
 * 2/3/2015: Pulled out Wild Card values into Card
 * 2/4/2015: Moved JOKER into Card and cleaned up accordingly
 */
enum Rank {
    THREE(3,"3"),FOUR(4,"4"),FIVE(5,"5"),SIX(6,"6"),SEVEN(7,"7"),EIGHT(8,"8"),NINE(9,"9"),
    TEN(10,"10"),JACK(11,"J"),QUEEN(12,"Q"),KING(13,"K");

    private final int value;
    private final String string;


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
    int getRankValue() {
        return value;
    }
    String getRankString() {
        return string;
    }
}
