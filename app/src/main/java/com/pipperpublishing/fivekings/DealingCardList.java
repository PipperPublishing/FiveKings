package com.pipperpublishing.fivekings;

import java.util.Collections;

/**
 * Created by Jeffrey on 3/21/2015.
 * 3/21/2015    Not sure this is the right way to do things, but deal() etc does not belong in CardList
 * 4/8/2015     Made deal return null if empty (might have been the default anyway)
 */
abstract class DealingCardList extends CardList {
    boolean shuffle() {
        Collections.shuffle(this);
        return true;
    }

    public Card deal() {
        return (this.isEmpty() ? null : this.remove(0));
    }

    public Card peekNext() {
        return (this.isEmpty() ? null : this.get(0));
    }

}
