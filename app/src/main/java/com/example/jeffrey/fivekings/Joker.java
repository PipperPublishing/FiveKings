package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 3/13/2015.
 * 3/13/2015    Subclasses Card for Joker-specific stuff
 */
class Joker extends Card {
    private static final int JOKER_VALUE =50;
    static final String JOKER_STRING="Joker";

    //no arguments constructor is for Jokers
    Joker() {
        super(null, null); //TODO:B : Should be a base class without suit and rank
        this.cardString = JOKER_STRING;
        this.cardValue = JOKER_VALUE;
    }

    @Override
    boolean isWildCard(final Rank wildCardRank) {
        return true;
    }

    @Override
    final int getCardValue(final Rank wildCardRank) {
        return JOKER_VALUE;
    }
    @Override
    final boolean isJoker() { return true; }

}
