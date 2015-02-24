package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 */
@Deprecated
class DrawPile extends CardList {
    DrawPile() {
        super();
        clear();
    }
    //this constructor is for copying the Deck to the DrawPile
    DrawPile(CardList initCards){
        this();
        this.addAll(initCards.getCards());
    }
}
