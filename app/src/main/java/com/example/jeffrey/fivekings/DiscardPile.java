package com.example.jeffrey.fivekings;

import android.util.Log;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/18/2015       - Don't throw exception when peekNext() is empty, because it can be like that in between drawing and discarding when we try to display image
 */
//TODO:B Convert DiscardPile and DrawPile to singletons
    @Deprecated
class DiscardPile extends CardList {
    public DiscardPile() {
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
        //DiscardPile should never run out
        if (this.isEmpty()) {
            Log.i(Game.APP_TAG, "DiscardPile.peekNext: is empty");
            return null;
        }
        return this.get(this.size() - 1);
    }
}
