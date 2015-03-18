package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    For testing new algorithms on computer logic
 * We could do a mixture of approaches for the computer players - from Smart to Stupid
 * 3/13/2015    Provide a scoring comparator to look at partial melds and singles
 * 3/15/2015    Override isTestBetterThanBest and introduce keepDiscardDecision
 * 3/17/2015    Implement interface that is passed to Meld.check
 */
class StrategyComputerPlayer extends EvaluationComputerPlayer{

    StrategyComputerPlayer(final String name) {
        super(name);
    }


    @Override
    Game.PileDecision tryDiscardOrDrawPile(final Hand.MeldMethod method, final boolean isFinalRound, final Card discardPileCard, final Card drawPileCard) {
        Game.PileDecision decision;

        //also sets method parameter this.discard - ugly way of returning that
        PlayerHand bestHand = findBestHand(method, isFinalRound, discardPileCard);
        //use Discard Pile if it was better than the existing hand
        //OR if it's the best we can probably do (we probably won't reduce valuation with DrawPile)
        if ((bestHand.getDiscard() != discardPileCard) && keepDiscardDecision(bestHand, this.hand, discardPileCard, isFinalRound)) {
            decision = Game.PileDecision.DISCARD_PILE;
            this.hand = bestHand;
        } else {
            decision = Game.PileDecision.DRAW_PILE;
            this.hand = findBestHand(method, isFinalRound, drawPileCard);
        }

        return decision; //just for logging and animation
    }


    //most of these are the same test, but we don't use calculateHandValue if it was just because a single card improved things
    // and there's a good chance of drawing to add to a meld / complete a partial meld
    private boolean keepDiscardDecision(final PlayerHand bestDiscardPileHand, final PlayerHand currentHand, final Card discardPileCard, final boolean isFinalRound) {
        final boolean keepDiscardDecision;

        //1. If we're out with bestDiscardPileHand then obviously pick that
        if (bestDiscardPileHand.calculateValueAndScore(isFinalRound) == 0) keepDiscardDecision = true;

        //2. Whichever option has more full melds is better (not always true depending on what is left)
        else if (bestDiscardPileHand.melds.size() > currentHand.melds.size()) keepDiscardDecision = true;
        else if (bestDiscardPileHand.melds.size() < currentHand.melds.size()) keepDiscardDecision = false;

        //4. Same number of full melds, does new reduce the number of unmelded cards even if the valuation goes up?
        else if (numMeldedCards(bestDiscardPileHand) < numMeldedCards(currentHand))  keepDiscardDecision = true;
        //5. or more partialmelds on an intermediate round
        else if (!isFinalRound && bestDiscardPileHand.partialMelds.size() > currentHand.partialMelds.size()) keepDiscardDecision = true;
        //6. same number of melds, same number of unmelded cards; are there partial or full melds we could meld to?
        else if (!currentHand.partialMelds.isEmpty() || !currentHand.melds.isEmpty()) keepDiscardDecision = false;
        //7. if melds are empty and the improvement was just a card replacement, use DrawPile if we might be able to do better
        else if ((bestDiscardPileHand.calculateValueAndScore(isFinalRound) < currentHand.calculateValueAndScore(isFinalRound))
                && (discardPileCard.getRankDifference(Rank.EIGHT) > 0)) keepDiscardDecision = false;
        else keepDiscardDecision = true;

        return keepDiscardDecision;
    }

    @Override
    public boolean isFirstBetterThanSecond(final Hand testHand, final Hand bestHand, final boolean isFinalRound) {
        final boolean testBetterThanBest;
        //1. If we're out with testHand then obviously pick that
        if (testHand.calculateValueAndScore(isFinalRound) == 0) testBetterThanBest = true;

            //2. Whichever option has more full melds is better (not always true depending on what is left)
        else if (testHand.melds.size() > bestHand.melds.size()) testBetterThanBest = true;
        else if (testHand.melds.size() < bestHand.melds.size()) testBetterThanBest = false;

            //4. Same number of full melds, does new increase the number of melded cards?
        else if (numMeldedCards(testHand) > numMeldedCards(bestHand)) testBetterThanBest = true;
            //5. or more partialmelds
        else if (testHand.partialMelds.size() > bestHand.partialMelds.size()) testBetterThanBest = true;
            //6. This check is only used to decide on a discard, not on DrawPile vs. DiscardPile
        else testBetterThanBest =  (testHand.calculateValueAndScore(isFinalRound) < bestHand.calculateValueAndScore(isFinalRound));
        return testBetterThanBest;
    }

    static private int numMeldedCards(final Hand hand) {
        int numMelded=0;
        for (Hand.Meld meld : hand.getMelded()) numMelded += meld.size();
        return numMelded;
    }

}
