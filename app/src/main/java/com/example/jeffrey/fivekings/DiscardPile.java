package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 1/22/2015.
 */
public class DiscardPile extends CardList {
    public DiscardPile() {
        super();
        this.clear();
    }
    @Override
    //Override deal because the DiscardPile is actually LIFO
    protected Card deal() {
        return this.remove(this.size() - 1);
    }

    @Override
    protected Card peekNext() {
        //DiscardPile should never run out
        if (0 == this.size()) throw new RuntimeException("DiscardPile.peekNext: this.size()==0");
        return this.get(this.size() - 1);
    }
}
