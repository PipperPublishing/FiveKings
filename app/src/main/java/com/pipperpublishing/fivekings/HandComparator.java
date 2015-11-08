package com.pipperpublishing.fivekings;

/**
 * Created by Jeffrey on 3/17/2015.
 * 3/17/2015    Implement Strategy design pattern through this interface; passed to Meld
 */
interface HandComparator {
    boolean isFirstBetterThanSecond(final MeldedCardList testHand, final MeldedCardList bestHand, final boolean isFinalTurn);
    //TODO:A: Probably should add method as part of the strategy
}
