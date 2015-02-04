package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 Removed suit NONE so simplified
 * 2/4/2015 Pulled Jokers into Card; also add two copies of each card
 */
//TODO:C Make this a singleton pattern (http://jroller.com/hoskinator/entry/static_classes_in_java)
class Deck extends CardList {
    Deck(boolean doShuffle) {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.add(new Card(suit, rank));
                this.add(new Card(suit, rank));
            }
        }//end for suits
        //add six Jokers
        for (int iJoker=1; iJoker<=6; iJoker++) this.add(new Card());
        if (doShuffle) this.shuffle();
    }//end Deck() constructor
}
