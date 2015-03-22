package com.example.jeffrey.fivekings;

import android.util.Log;

/**
 * Created by Jeffrey on 2/20/2015.
 * Encapsulates the draw and discard pile so that we can redeal the Draw pile when it runs out
 * 2/24/2015    Moved hand deal to here to encapsulate it - just deals as many cards as requested
 * 3/20/2015    Moved Deck here as well
 * 3/21/2015    Dealing CardList implements shuffle, deal, peekNext
 * TODO:B: Also make them singletons?
 */
/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 Removed suit NONE so simplified
 * 2/4/2015 Pulled Jokers into Card; also add two copies of each card
 * 2/14/2015 Pass application context so we can read drawables
 * 2/17/2015 Converted to a singleton
 * 3/13/2015    Impact of sub-classing Jokers
 *              Removed context
 * 3/19/2015    Made Deck the outer class of DrawPile and DiscardPile
 */
class Deck extends DealingCardList  {
    DiscardPile discardPile;
    DrawPile drawPile;

    //private because we use singleton pattern to do static initialization
    private Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.add(new Card(suit, rank));
                this.add(new Card(suit, rank));
            }
        }//end for suits
        //add six Jokers
        for (int iJoker=1; iJoker<=6; iJoker++) this.add(new Joker());

        initDrawAndDiscardPile();
    }

    private static class DeckHolder {
        private static final Deck deck = new Deck();
    }

    static Deck getInstance(final boolean doShuffle) {
        if (doShuffle) DeckHolder.deck.shuffle();
        return DeckHolder.deck;
    }

    final void initDrawAndDiscardPile() {
        drawPile = new DrawPile();
        discardPile = new DiscardPile();
    }

    final void dealToDiscard() {
        discardPile.add(drawPile.deal());
    }

    /**
     * INNER CLASS DrawPile
     */
    class DrawPile extends DealingCardList {
        //this constructor is for copying the Deck to the DrawPile
        private DrawPile(){
            super();
            this.clear();
            this.addAll(Deck.this.getCards());
        }

        @Override
        protected Card deal() {
            reDealIfEmpty();
            return super.deal();
        }

        @Override
        protected Card peekNext() {
            reDealIfEmpty();
            return super.peekNext();
        }

        CardList deal(final int numToDeal) {
            CardList returnedCards = new CardList(numToDeal);
            for (int iCard=0; iCard < numToDeal; iCard++) returnedCards.add(deal());
            return returnedCards;
        }

        private void reDealIfEmpty () {
            if (this.isEmpty()) {
                Log.d(Game.APP_TAG, "DrawAndDiscardPiles.DrawPile.reDealIfEmpty: Redealing Discard Pile");
                this.clear();
                //I don't think we can do a new here, because this would be left pointing to the empty drawPile...or maybe not
                this.addAll(discardPile);
                this.shuffle();
                discardPile.clear();
                dealToDiscard();
            }
        }

    }//end DrawPile

    /* INNER CLASS DiscardPile */
    class DiscardPile extends DealingCardList {
        private DiscardPile() {
            super();
            this.clear();
        }

        @Override
        //Override deal because the DiscardPile is actually LIFO
        protected Card deal() {
            if (this.isEmpty()) throw new RuntimeException(Game.APP_TAG + "DiscardPile.deal: is empty");
            return this.remove(this.size() - 1);
        }

        @Override
        protected Card peekNext() {
            final Card nextCard;
            //DiscardPile can run out in the middle of a turn (when you draw the last card)
            //but it only affects display (a blank space where the pile was)
            if (this.isEmpty()) {
                Log.i(Game.APP_TAG, "DiscardPile.peekNext: is empty");
                nextCard = null;
            }
            else nextCard = this.get(this.size() - 1);

            return nextCard;
        }
    }



}
