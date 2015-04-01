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

public class ComputerPlayer extends Player {
    protected static final int PERMUTATION_THRESHOLD=750; //if longer than 0.75s for each player we switch to heuristic approach

    private MeldedCardList.MeldMethod method = MeldedCardList.MeldMethod.PERMUTATIONS;


    ComputerPlayer(final String name) {
        super(name);
    }

    ComputerPlayer(final Player player) {
        super(player);
    }

    @Override
    boolean initGame() {
        super.initGame();
        this.method = MeldedCardList.MeldMethod.PERMUTATIONS;
        return true;
    }

    @Override
    final boolean initAndDealNewHand(final Deck.DrawPile drawPile,final Rank roundOf) {
        super.initAndDealNewHand(drawPile, roundOf);
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


    Game.PileDecision tryDiscardOrDrawPile(final MeldedCardList.MeldMethod method, final boolean isFinalTurn, final Card discardPileCard, final Card drawPileCard) {
        Game.PileDecision decision;

        //also sets method parameter this.discard - ugly way of returning that
        Hand bestHand = findBestHand(method, isFinalTurn, discardPileCard);
        //if the discard is not the drawn card then use Discard Pile
        if (bestHand.getDiscard() != discardPileCard) {
            decision = Game.PileDecision.DISCARD_PILE;
            this.hand = bestHand;
        } else {
            decision = Game.PileDecision.DRAW_PILE;
            this.hand = findBestHand(method, isFinalTurn, drawPileCard);
        }
        return decision; //just for logging and animation
    }
    //Computer has to use this version which loops through possible discards to find the best one
    protected Hand findBestHand(final MeldedCardList.MeldMethod method, final boolean isFinalTurn, final Card addedCard) {
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
            testHand.meldAndEvaluate(method, isFinalTurn);
            if (isFirstBetterThanSecond(testHand, bestHand, isFinalTurn)) {
                bestHand = testHand;
                if (bestHand.calculateValueAndScore(isFinalTurn) == 0) {
                    Log.d(Game.APP_TAG, String.format("findBestHand: Went out after %d/%d possible discards",
                            cardsWithAdded.indexOf(disCard), cardsWithAdded.size()));
                    break;
                }
            }
        }//end for loop over possible discards

        return bestHand;
    }

    /*----------------------------------------*/
    /* GAME PLAYING METHODS (moved from Game) */
    /*----------------------------------------*/
    @Override
    //if this is final turn, or we are showing computer hands then do that
    void prepareTurn(final FiveKings fKActivity) {
        fKActivity.setWidgetsByGameState();
        //If SHOW_ALL_CARDS is false we still show after each player's final turn
        fKActivity.updateHandsAndCards(Game.SHOW_ALL_CARDS, false);
        turnState = TurnState.PLAY_TURN;
    }

    @Override
    Game.PileDecision takeTurn(final FiveKings fKActivity, final PlayerMiniHandLayout playerMiniHandLayout, final Game.PileDecision drawOrDiscardPile, final Deck deck, final boolean isFinalTurn) {

        fKActivity.setWidgetsByGameState();
        final StringBuilder turnInfo = new StringBuilder(100);
        turnInfo.setLength(0);
        final String turnInfoFormat = fKActivity.getText(R.string.computerTurnInfo).toString();

        logTurn(isFinalTurn);

        //improve hand by picking up from Discard pile or from Draw pile - use pickFromDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        //if Computer, we decide here whether to draw or discard
        final long playerStartTime = System.currentTimeMillis();
        final Game.PileDecision pickFrom = this.tryDiscardOrDrawPile(method, isFinalTurn, deck.discardPile.peekNext(), deck.drawPile.peekNext());
        //now actually deal the card
        if (pickFrom == Game.PileDecision.DISCARD_PILE) {
            this.drawnCard = deck.discardPile.deal();
            turnInfo.append(String.format(turnInfoFormat, this.getName(), this.drawnCard.getCardString(),
                    "Discard", this.getHandDiscard().getCardString()));
        } else { //DRAW_PILE
            //if we decided to not use the Discard pile, then we need to find discard and best hand with drawPile card
            this.drawnCard = deck.drawPile.deal();
            turnInfo.append(String.format(turnInfoFormat, this.getName(), this.drawnCard.getCardString(),
                    "Draw", this.getHandDiscard().getCardString()));
        }
        final long playerStopTime = System.currentTimeMillis();
        if (this.method == MeldedCardList.MeldMethod.PERMUTATIONS)
            this.method = ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD) ? MeldedCardList.MeldMethod.PERMUTATIONS : MeldedCardList.MeldMethod.HEURISTICS;

        Log.d(Game.APP_TAG, turnInfo.toString());
        //Moved actual discard into endCurrentPlayerTurn so we can do animation at same time
        fKActivity.showHint(turnInfo.toString());

        //at this point the DiscardPile still shows the old card
        //animate... also calls syncDisplay and checkEndRound() in the OnAnimationEnd listener
        fKActivity.animateComputerPickUpAndDiscard(playerMiniHandLayout, pickFrom);
        turnState = TurnState.NOT_MY_TURN;
        return pickFrom;
    }

    @Override
    void logTurn(final boolean isFinalTurn) {
        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        if (this.method == MeldedCardList.MeldMethod.PERMUTATIONS)
            Log.d(Game.APP_TAG, "Player " + this.getName() + ": Using Permutations");
        else Log.d(Game.APP_TAG, "Player " + this.getName() + ": Using Heuristics");
        String sValuationOrScore = isFinalTurn ? "Score=" : "Valuation=";
        Log.d(Game.APP_TAG, "before...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
                + sValuationOrScore + this.getHandValueOrScore(isFinalTurn));
    }



}//end ComputerPlayer

