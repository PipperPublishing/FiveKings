package com.example.jeffrey.fivekings;

import android.util.Log;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    Instrument findBestHand to see how soon we could break out of loop once we find a hand where we go out
    3/13/2015   Breaks out if you find a 0 scoring hand
 * (should just have Scoring in human hands and Valuation in Computer ones)
 * 3/15/2015    Add Computer implementation of discardFromHand (does nothing)
 *              Moved actually drawing from DrawPile here as part of tryDiscardOrDraw
 */

public class EvaluationComputerPlayer extends Player {

    EvaluationComputerPlayer(final String name) {
        super(name);
    }

    EvaluationComputerPlayer(final Player player) {
        super(player);
    }

    @Override
    final boolean initAndDealNewHand(final Deck.DrawPile drawPile,final Rank roundOf, final MeldedCardList.MeldMethod method) {
        super.initAndDealNewHand(drawPile, roundOf, method);
        hand.meldAndEvaluate(method, false);
        return true;
    }

    @Override
    //In Computer, there is no actual "discard"; it's handled by findBestHand
    final Card discardFromHand(final Card cardToDiscard) {
        return cardToDiscard;
    }

    @Override
    final boolean isHuman() {return false;}


    //Computer has to use this version which loops through possible discards to find the best one
    protected Hand findBestHand(final MeldedCardList.MeldMethod method, final boolean isFinalRound, final Card addedCard) {
        Hand bestHand = this.hand;
        bestHand.setDiscard(addedCard); //default if we don't improve the score
        //Loop over possible discards, so that now addAndEvaluate just looks at your hand without added
        //in fact, each loop the actual hand is different (including hand.cards) and will be saved if best
        CardList cardsWithAdded = new CardList(hand);
        cardsWithAdded.add(addedCard);
        for (Card disCard : cardsWithAdded) {
            CardList cards = new CardList(cardsWithAdded);
            cards.remove(disCard);
            Hand testHand = new Hand(this.hand.roundOf, cards, disCard); //creates new hand with replaced cards
            testHand.meldAndEvaluate(method, isFinalRound);
            if (isFirstBetterThanSecond(testHand, bestHand, isFinalRound)) {
                bestHand = testHand;
                if (bestHand.calculateValueAndScore(isFinalRound) == 0) {
                    Log.d(Game.APP_TAG, String.format("findBestHand: Went out after %d/%d possible discards",
                            cardsWithAdded.indexOf(disCard), cardsWithAdded.size()));
                    break;
                }
            }
        }//end for loop over possible discards

        return bestHand;
    }

    Game.PileDecision tryDiscardOrDrawPile(final MeldedCardList.MeldMethod method, final boolean isFinalRound, final Card discardPileCard, final Card drawPileCard) {
        Game.PileDecision decision;

        //also sets method parameter this.discard - ugly way of returning that
        Hand bestHand = findBestHand(method, isFinalRound, discardPileCard);
        //if the discard is not the drawn card then use Discard Pile
        if (bestHand.getDiscard() != discardPileCard) {
            decision = Game.PileDecision.DISCARD_PILE;
            this.hand = bestHand;
        } else {
            decision = Game.PileDecision.DRAW_PILE;
            this.hand = findBestHand(method, isFinalRound, drawPileCard);
        }
        return decision; //just for logging and animation
    }

}//end ComputerPlayer

