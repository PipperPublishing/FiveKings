package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/28/2015.
 * 1/28/15 Split into separate class to add string representation
 * 2/3/2015 Removed suit NONE (was here for Jokers)
 */
enum Suit {
    SPADES("S"),HEARTS("H"),DIAMONDS("D"),CLUBS("C"),STARS("*");

    private final String suitString;

    Suit(String suitString) {
        this.suitString = suitString;
    }

    String getSuitString() {
        return suitString;
    }
    int getOrdinal() {
        return this.ordinal();
    }


}
