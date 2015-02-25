package com.example.jeffrey.fivekings;

import android.util.Log;

/**
 * Created by Jeffrey on 2/20/2015.
 * Encapsulates the draw and discard pile so that we can redeal the Draw pile when it runs out
 * 2/24/2015    Moved hand deal to here to encapsulate it - just deals as many cards as requested
 * TODO:B: Also make them singletons?
 * TODO:B Make peekNext() and deal() part of an interface that they implement but CardList does not
 */
class DrawAndDiscardPiles {
    DiscardPile discardPile;
    DrawPile drawPile;

    DrawAndDiscardPiles(Deck deck) {
        drawPile = new DrawPile(deck);
        discardPile = new DiscardPile();
    }

    void dealToDiscard() {
        discardPile.add(drawPile.deal());
    }

    /**
     * Created by Jeffrey on 1/22/2015.
     */
    class DrawPile extends CardList {
        private DrawPile() {
            super();
            clear();
        }
        //this constructor is for copying the Deck to the DrawPile
        private DrawPile(CardList initCards){
            this();
            this.addAll(initCards.getCards());
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

        CardList deal(int numCards) {
            CardList returnedCards = new CardList(numCards);
            for (int iCard=0; iCard < numCards; iCard++) returnedCards.add(deal());
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


    class DiscardPile extends CardList {
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
            //DiscardPile can run out in the middle of a turn (when you draw the last card)
            if (this.isEmpty()) {
                Log.i(Game.APP_TAG, "DiscardPile.peekNext: is empty");
                return null;
            }
            return this.get(this.size() - 1);
        }
    }



}
