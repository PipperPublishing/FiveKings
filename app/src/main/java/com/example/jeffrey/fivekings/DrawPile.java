package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 *  * 2/25/2015    Deprecated (now inner class of DrawAndDiscardPiles

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
