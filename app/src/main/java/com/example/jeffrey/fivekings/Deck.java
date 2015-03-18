package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 Removed suit NONE so simplified
 * 2/4/2015 Pulled Jokers into Card; also add two copies of each card
 * 2/14/2015 Pass application context so we can read drawables
 * 2/17/2015 Converted to a singleton
 * 3/13/2015    Impact of sub-classing Jokers
 *              Removed context
 */
class Deck extends CardList {

    private Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.add(new Card(suit, rank));
                this.add(new Card(suit, rank));
            }
        }//end for suits
        //add six Jokers
        for (int iJoker=1; iJoker<=6; iJoker++) this.add(new Joker());
    }

    private static class DeckHolder {
        private static final Deck deck = new Deck();
    }

    static Deck getInstance(boolean doShuffle) {
        if (doShuffle) DeckHolder.deck.shuffle();
        return DeckHolder.deck;
    }
}
