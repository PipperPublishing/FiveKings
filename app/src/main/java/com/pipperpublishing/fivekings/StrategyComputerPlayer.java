package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    For testing new algorithms on computer logic
 * We could do a mixture of approaches for the computer players - from Smart to Stupid
 * 3/13/2015    Provide a scoring comparator to look at partial melds and singles
 * 3/15/2015    Override isTestBetterThanBest and introduce keepDiscardDecision
 * 3/17/2015    Implement interface that is passed to Meld.decomposeAndCheck
 * 3/18/2015    Wasn't checking for case of numUnmeldedCards increased
 * 3/19/2015    isFirstBetterThanSecond: If everything else is equal, prefer more rank melds to sequence melds
 * 3/20/2015    Extend rank meld preference to keepDiscardDecision
 * 3/21/2015    Prefer full rank melds over sequences
 * 4/5/2015     Only override strategy portions - execution is same as ComputerPlayer
 */
class StrategyComputerPlayer extends ComputerPlayer {

    StrategyComputerPlayer(final String name) {
        super(name);
    }


    //most of these are the same test, but we don't use calculateHandValue if it was just because a single card improved things
    // and there's a good chance of drawing to add to a meld / complete a partial meld
    @Override
    final protected boolean keepDiscardDecision(final Hand bestDiscardPileHand, final Hand currentHand, final Card discardPileCard, final boolean isFinalTurn) {
        final boolean keepDiscardDecision;

        //1. If we're out with bestDiscardPileHand then obviously pick that - MUST KEEP THIS TEST
        if (bestDiscardPileHand.calculateValueAndScore(isFinalTurn) == 0) keepDiscardDecision = true;

        //2. Whichever option has more full melds is better (not always true depending on what is left)
        else if (bestDiscardPileHand.numMelds() != currentHand.numMelds()) keepDiscardDecision = (bestDiscardPileHand.numMelds() > currentHand.numMelds());

        //4. Same number of full melds, does new increase the number of melded cards even if the valuation goes up?
        else if (bestDiscardPileHand.numMeldedCards() != currentHand.numMeldedCards())  keepDiscardDecision = (bestDiscardPileHand.numMeldedCards() > currentHand.numMeldedCards()) ;

        //5. or more partial melds on an intermediate round
        else if (!isFinalTurn && bestDiscardPileHand.numPartialMelds() > currentHand.numPartialMelds()) keepDiscardDecision = true;
        //6. on an Intermediate Round, prefer rank melds over sequence melds - this only adds partial meld test
        else if (!isFinalTurn && (bestDiscardPileHand.numPartialMelds() == currentHand.numPartialMelds())
                && (bestDiscardPileHand.numPartialMelds() > 0) && (bestDiscardPileHand.numPartialRankMelds() != currentHand.numPartialRankMelds())) {
            keepDiscardDecision = (bestDiscardPileHand.numPartialRankMelds() > currentHand.numPartialRankMelds());
        }
        //6. same number of melds, same number of unmelded cards; are there partial or full melds we could meld to?
        else if ((currentHand.numPartialMelds() > 0) || (currentHand.numMelds() > 0)) keepDiscardDecision = false;
        //7. if melds are empty and the improvement was just a card replacement, use DrawPile if we might be able to do better
        else if ((bestDiscardPileHand.calculateValueAndScore(isFinalTurn) < currentHand.calculateValueAndScore(isFinalTurn))
                && (discardPileCard.getRankDifference(Rank.EIGHT) > 0)) keepDiscardDecision = false;
        else keepDiscardDecision = true;

        return keepDiscardDecision;
    }

    @Override
    public boolean isFirstBetterThanSecond(final MeldedCardList testHand, final MeldedCardList bestHand, final boolean isFinalTurn) {
        final boolean testBetterThanBest;
        //1. If we're out with testHand then obviously pick that
        if (testHand.calculateValueAndScore(isFinalTurn) == 0) testBetterThanBest = true;

        //2. Whichever option has more full melds is better (not always true depending on what is left)
        else if (testHand.numMelds() != bestHand.numMelds()) testBetterThanBest = (testHand.numMelds() > bestHand.numMelds());

        //4. Same number of full melds, does new increase the number of melded cards?
        else if (testHand.numMeldedCards() != bestHand.numMeldedCards()) testBetterThanBest = (testHand.numMeldedCards() > bestHand.numMeldedCards());

        //5. More partialMelds?
        else if (testHand.numPartialMelds() > bestHand.numPartialMelds()) testBetterThanBest = true;

        //6. If equal number of full melds, prefer rank melds over sequence melds
        //#4 above tested for != so we already know they have the same number
        else if ((testHand.numMelds() > 0) && (testHand.numFullRankMelds() != bestHand.numFullRankMelds())) {
            testBetterThanBest = (testHand.numFullRankMelds() > bestHand.numFullRankMelds());
        }

        //7. If equal number of partial melds, prefer rank melds over sequence melds
        else if ((testHand.numPartialMelds() == bestHand.numPartialMelds()) && (testHand.numPartialMelds() > 0)
                && (testHand.numPartialRankMelds() != bestHand.numPartialRankMelds())) {
            testBetterThanBest = (testHand.numPartialRankMelds() > bestHand.numPartialRankMelds());
        }
        //8. This check is only used to decide on a discard, not on DrawPile vs. DiscardPile
        else testBetterThanBest =  (testHand.calculateValueAndScore(isFinalTurn) < bestHand.calculateValueAndScore(isFinalTurn));
        return testBetterThanBest;
    }

    /* PARCELABLE read/write for StrategyComputerPlayer (use superclass implementation) */
    protected StrategyComputerPlayer(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<StrategyComputerPlayer> CREATOR = new Parcelable.Creator<StrategyComputerPlayer>() {
        @Override
        public StrategyComputerPlayer createFromParcel(Parcel in) {
            return new StrategyComputerPlayer(in);
        }

        @Override
        public StrategyComputerPlayer[] newArray(int size) {
            return new StrategyComputerPlayer[size];
        }
    };

}
