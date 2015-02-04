package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 */
//TODO:B need to handle what to do when deal runs out (shuffle discard pile)
public class DrawPile extends CardList {
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
