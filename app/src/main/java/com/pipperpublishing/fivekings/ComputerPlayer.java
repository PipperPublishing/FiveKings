package com.pipperpublishing.fivekings;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    Instrument findBestHand to see how soon we could break out of loop once we find a hand where we go out
    3/13/2015   Breaks out if you find a 0 scoring hand
 * (should just have Scoring in human hands and Valuation in Computer ones)
 * 3/15/2015    Add Computer implementation of discardFromHand (does nothing)
 *              Moved actually drawing from DrawPile here as part of tryDiscardOrDraw
 * 4/4/2015     Add support for multi-threading (inherited by StrategyComputerPlayer)
 * 4/5/2015     startFindBestDiscard kicks off threads for Discard pile
 */

public class ComputerPlayer extends Player {
    protected static final int PERMUTATION_THRESHOLD=750; //if longer than 0.75s for each player we switch to heuristic approach
    private Thread[] t;
    private ThreadedHand[] testHand;
    private int numThreads;

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
        numThreads = 0;
        t = null;
        testHand = null;
        return true;
    }

    @Override
    final boolean initAndDealNewHand(final Deck.DrawPile drawPile,final Rank roundOf) {
        super.initAndDealNewHand(drawPile, roundOf);
        //TODO:B use the current method instead of always using heuristics to do the initial meld
        hand.meldAndEvaluate(method, false);
        this.numThreads = roundOf.getRankValue()+1;
        t = new Thread[numThreads];
        testHand = new ThreadedHand[numThreads];
        return true;
    }

    @Override
    //In Computer, there is no actual "discard"; it's handled by findBestHand
    final Card discardFromHand(final Card cardToDiscard) {
        return cardToDiscard;
    }

    @Override
    final boolean isHuman() {return false;}


    /*----------------------------------------*/
    /* GAME PLAYING METHODS (moved from Game) */
    /*----------------------------------------*/
    @Override
    //TODO:A move to a prepareComputerTurn
    //Must be some way to simplify showComputerCards and hideHands... into one player call
    void prepareTurn(final FiveKings fKActivity, final boolean hideHandInitially) {
        turnState = TurnState.PLAY_TURN;
        //FIXME: should use an overridden method updateHandsAndCards so that it can be called correctly elsewhere
        fKActivity.updateHandsAndCards(fKActivity.getmGame().isShowComputerCards(), false);
        //if showCards is false, no reason to force another click - just go ahead and play
        if (!fKActivity.getmGame().isShowComputerCards()) {
            fKActivity.cancelAnimatePlayerMiniHand(this.getMiniHandLayout());
            takeTurn(fKActivity, null, fKActivity.getmGame().getDeck(),fKActivity.getmGame().isFinalTurn());
        }
    }

    @Override
    void takeTurn(final FiveKings fKActivity, final Game.PileDecision drawOrDiscardPile, final Deck deck, final boolean isFinalTurn) {

        final StringBuilder turnInfo = new StringBuilder(100);
        turnInfo.setLength(0);
        final String turnInfoFormat = fKActivity.getText(R.string.computerTurnInfo).toString();

        logTurn(isFinalTurn);

        //improve hand by picking up from Discard pile or from Draw pile - use pickFromDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        //if Computer, we decide here whether to draw or discard
        final long playerStartTime = System.currentTimeMillis();

        //put this on a separate thread so we display a spinner while it works
        //(note that the decision among discards is also multi-threaded)
        class DiscardOrDrawTask extends AsyncTask<Void, Void, Game.PileDecision> {

            @Override
            protected Game.PileDecision doInBackground(Void... params) {
                final Game.PileDecision pickFrom = tryDiscardOrDrawPile(isFinalTurn, deck.discardPile.peekNext(), deck.drawPile.peekNext());
                //now actually deal the card
                if (pickFrom == Game.PileDecision.DISCARD_PILE) {
                    drawnCard = deck.discardPile.deal();
                    turnInfo.append(String.format(turnInfoFormat, ComputerPlayer.this.getName(), drawnCard.getCardString(),
                            "Discard", getHandDiscard().getCardString()));
                } else { //DRAW_PILE
                    //if we decided to not use the Discard pile, then tryDiscardOrDrawPile has already found the right discard for the Draw pile
                    drawnCard = deck.drawPile.deal();
                    turnInfo.append(String.format(turnInfoFormat, ComputerPlayer.this.getName(), drawnCard.getCardString(),
                            "Draw", getHandDiscard().getCardString()));
                }
                final long playerStopTime = System.currentTimeMillis();
                if (ComputerPlayer.this.method == MeldedCardList.MeldMethod.PERMUTATIONS)
                    ComputerPlayer.this.method = ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD) ? MeldedCardList.MeldMethod.PERMUTATIONS : MeldedCardList.MeldMethod.HEURISTICS;

                Log.d(Game.APP_TAG, turnInfo.toString());

                return pickFrom;
            }

            @Override
            protected void onPreExecute() {
                fKActivity.showSpinner(true);
            }

            @Override
            protected void onPostExecute(final Game.PileDecision pickFrom) {
                fKActivity.showSpinner(false);
                //Moved actual discard into endCurrentPlayerTurn so we can do animation at same time
                fKActivity.showHint(turnInfo.toString(), false);

                //at this point the DiscardPile still shows the old card
                //animate... also calls syncDisplay and checkEndRound() in the OnAnimationEnd listener
                fKActivity.animateComputerPickUpAndDiscard(ComputerPlayer.this.getMiniHandLayout(), pickFrom);
                turnState = TurnState.NOT_MY_TURN;
            }
        }
        DiscardOrDrawTask discardOrDrawTask= new DiscardOrDrawTask();
        discardOrDrawTask.execute();
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

    protected Game.PileDecision tryDiscardOrDrawPile(final boolean isFinalTurn, final Card discardPileCard, final Card drawPileCard) {
        Game.PileDecision decision;

        //findBestHandStart for Discard called on previous players completion (or on round start)
        Hand bestHand = findBestHandFinish(discardPileCard);
        //if the discard is not the drawn card then use Discard Pile
        if ((bestHand.getDiscard() != discardPileCard) && keepDiscardDecision(bestHand, this.hand, discardPileCard, isFinalTurn)) {
            decision = Game.PileDecision.DISCARD_PILE;
            this.hand = bestHand;
        } else {
            decision = Game.PileDecision.DRAW_PILE;
            findBestHandStart(isFinalTurn, drawPileCard);
            this.hand = findBestHandFinish(drawPileCard);
        }
        return decision; //just for logging and animation
    }

    //has no effect for Computer Hand
    protected boolean keepDiscardDecision(final Hand bestDiscardPileHand, final Hand currentHand, final Card discardPileCard, final boolean isFinalTurn) {
        return true;
    }

    //Computer has to use this version which loops through possible discards to find the best one
    @Override
    void findBestHandStart(final boolean isFinalTurn, final Card addedCard) {
        Log.d(Game.APP_TAG,"findBestHandStart: addedCard = " + addedCard.getCardString());
        //Loop over possible discards, so that now addAndEvaluate just looks at your hand without added
        //in fact, each loop the actual hand is different (including hand.cards) and will be saved if best
        CardList cardsWithAdded = new CardList(hand);
        cardsWithAdded.add(addedCard);
        int iThread = 0;
        for (Card disCard : cardsWithAdded) {
            CardList cards = new CardList(cardsWithAdded);
            cards.remove(disCard);

            testHand[iThread] = new ThreadedHand(this.hand.roundOf, cards, disCard, this.method, isFinalTurn); //creates new hand with replaced cards
            t[iThread] = new Thread(testHand[iThread]);
            t[iThread].start(); //calls meldAndEvaluate
            iThread++;
        }
    }

    private Hand findBestHandFinish(final Card addedCard) {
        Hand bestHand = this.hand;
        bestHand.setDiscard(addedCard); //default if we don't improve the score
        //wait for all threads to complete
        for (int iThread=0; iThread < this.numThreads; iThread++) {
            try {
                t[iThread].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int iThread=0; iThread < this.numThreads; iThread++) {
            if (isFirstBetterThanSecond(testHand[iThread], bestHand, testHand[iThread].isFinalTurn)) {
                bestHand = testHand[iThread];
                if (bestHand.calculateValueAndScore(testHand[iThread].isFinalTurn) == 0) {
                    Log.d(Game.APP_TAG, String.format("findBestHandFinish: Went out after %d/%d possible discards",
                            iThread, this.numThreads));
                    break;
                }
            }
        }//end for loop over possible discards

        return bestHand;
    }


    /* INNER CLASS ThreadedHand */
    //Threaded version of Hand for multi-threading and for spinner display
    //(computation has to be in the non-UI thread because the spinner has to run in the UI thread)
    private class ThreadedHand extends Hand implements Runnable {
        final private MeldMethod method;
        final private boolean isFinalTurn;


        ThreadedHand (final Rank roundOf, final CardList cards, final Card discard,final MeldMethod method, final boolean isFinalTurn) {
            super(roundOf, cards, discard);
            this.method = method;
            this.isFinalTurn = isFinalTurn;
        }

        @Override
        public void run() {
            this.meldAndEvaluate(this.method, this.isFinalTurn);
        }

    }



}//end ComputerPlayer
