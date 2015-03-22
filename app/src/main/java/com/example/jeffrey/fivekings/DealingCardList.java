package com.example.jeffrey.fivekings;

import java.util.Collections;

/**
 * Created by Jeffrey on 3/21/2015.
 * 3/21/2015    Not sure this is the right way to do things, but deal() etc does not belong in CardList
 */
abstract class DealingCardList extends CardList{
    boolean shuffle() {
        Collections.shuffle(this);
        return true;
    }

    Card deal() {
        return remove(0);
    }

    Card peekNext() {
        return (0== this.size() ? null : this.get(0));
    }

}
