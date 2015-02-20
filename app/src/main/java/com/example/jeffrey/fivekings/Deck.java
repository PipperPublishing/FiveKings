package com.example.jeffrey.fivekings;

import android.content.Context;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 Removed suit NONE so simplified
 * 2/4/2015 Pulled Jokers into Card; also add two copies of each card
 * 2/14/2015 Pass application context so we can read drawables
 * 2/17/2015 Converted to a singleton
 */
class Deck extends CardList {
    private static Deck deck = null;

    private Deck(boolean doShuffle, Context context) {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.add(new Card(suit, rank, context));
                this.add(new Card(suit, rank, context));
            }
        }//end for suits
        //add six Jokers
        for (int iJoker=1; iJoker<=6; iJoker++) this.add(new Card(context));
    }

    static Deck getInstance(boolean doShuffle, Context context) {
        if (null == deck) deck = new Deck(doShuffle, context);
        if (doShuffle) deck.shuffle();
        return deck;
    }//end Deck() constructor
}
