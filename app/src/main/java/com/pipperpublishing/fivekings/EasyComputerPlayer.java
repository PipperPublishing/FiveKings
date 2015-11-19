/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.pipperpublishing.fivekings.view.FiveKings;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/12/2015    Instrument findBestHand to see how soon we could break out of loop once we find a hand where we go out
    3/13/2015   Breaks out if you find a 0 scoring hand
 * (should just have Scoring in human hands and Valuation in Computer ones)
 * 3/15/2015    Add Computer implementation of discardFromHand (does nothing)
 *              Moved actually drawing from DrawPile here as part of tryDiscardOrDraw
 * 4/4/2015     Add support for multi-threading (inherited by StrategyComputerPlayer)
 * 4/5/2015     startFindBestDiscard kicks off threads for Discard pile
 * 10/10/2015   Remove setting turnState in postExecute (needs to wait for animationEnd, and was
 *              already being set in Player.endTurn
 *              When converting from Human to Computer, you need to do the per-round initialization that happens
 *              in initAndDealNewHand
 * 10/16/2015   Moved endTurnCheckRound out of animation loop so that it happens immediately and next hand doesn't have to wait for animation finish
 *  * 10/18/2015   Change per player showHandsAndCards to returning a showCards flag
 *  10/20/2015  Hide drawPile and discardPile - access through deck
 *  11/5/2015   Log overall turn time and individual thread time to see breakdown for cutover Permutations -> Heuristics
 *              Add an extra sleep thread that sleeps for PERMUTATION_THRESHOLD milliseconds; if that runs to completion, then we need to terminate the other threads and switch to Heuristics
 *              If the other threads finish first, then terminate the sleep thread.
 *              (Can't use approach of waiting on join(time), because that is serial and we want to interrupt all threads
 * 11/8/2015    Support parcelable. Move Thread declaration to be dynamic
 * 11/10/2015   Interrupted thread was not cascading up and rerunning with Heuristics (add a wasInterrupted flag to find this)
 * 11/18/2015   Created from ComputerPlayer
 * 11/19/2015   Constructors must be public to support reflection (used in add/update player)
 */

public class EasyComputerPlayer extends Player {
    /* Picks "first" discard over "second" randomly */

    protected static final int PERMUTATION_THRESHOLD=500; //if longer than 0.5s for each player we switch to heuristic approach
    private static final String SLEEP_THREAD_NAME="Sleep Thread";
    final private ThreadGroup threadGroup = new ThreadGroup("HandThreads");
    private Thread[] t; //the hand calculation threads (for different discards)
    private ThreadedHand[] testHand;
    private int numThreads;

    private Meld.MeldMethod method = Meld.MeldMethod.PERMUTATIONS;


    public EasyComputerPlayer(final String name) {
        super(name);
    }

    //Copy constructor for converting from Human to Computer player
    public EasyComputerPlayer(final Player player) {
        super(player);

        //Set the class variables that are additional to ComputerPlayer
        //TODO:B use the current method instead of always using heuristics to do the initial meld
        this.method = Meld.MeldMethod.HEURISTICS;

        initAfterUpdateToComputer();
    }

    @Override
    boolean initGame() {
        super.initGame();
        this.method = Meld.MeldMethod.PERMUTATIONS;
        numThreads = 0;
        t = null;
        testHand = null;
        return true;
    }

    @Override
    final boolean initAndDealNewHand(final Rank roundOf) {
        super.initAndDealNewHand(roundOf);
        initAfterUpdateToComputer();
        return true;
    }

    final void initAfterUpdateToComputer() {
        //If hand is null then we haven't dealt yet and these will get initialized at that point
        if (hand != null) {
            try {
                hand.meldAndEvaluate(method,this , false);
            } catch (InterruptedException e) {
                //ignore
            }

        }
    }

    @Override
    //In Computer, there is no actual "discard"; it's handled by findBestHand
    final public Card discardFromHand(final Card cardToDiscard) {
        return cardToDiscard;
    }

    /*----------------------------------------*/
    /* GAME PLAYING METHODS (moved from Game) */
    /*----------------------------------------*/
    @Override
    //Must be some way to simplify showComputerCards and hideHands... into one player call
    final public void prepareTurn(final FiveKings fKActivity) {
        //base method sets PLAY_TURN and shows hands and cards
        super.prepareTurn(fKActivity);
        if (fKActivity.isShowComputerCards()) {
            //showing Computer cards, so you have to tap again to play
            fKActivity.setShowHint(fKActivity.resFormat(R.string.showingComputerCards, this.getName()), FiveKings.HandleHint.SHOW_HINT, true);
        }else {
            //if showCards is false, no reason to force another click - just go ahead and play
            this.getMiniHandLayout().clearAnimatedMiniHand();
            takeTurn(fKActivity, null, fKActivity.getmGame().isFinalTurn());
        }
    }

    @Override
    public boolean showCards(final boolean isShowComputerCards) {
        return isShowComputerCards;
    }

    @Override
    final public void takeTurn(final FiveKings fKActivity, final Game.PileDecision drawOrDiscardPile, final boolean isFinalTurn) {

        final StringBuilder turnInfo = new StringBuilder(100);
        turnInfo.setLength(0);
        final String turnInfoFormat = fKActivity.getString(R.string.computerTurnInfo);

        logTurn(isFinalTurn);

        //improve hand by picking up from Discard pile or from Draw pile - use pickFromDiscardPile to decide
        //Pick from drawPile unless you can meld/match from discardPile

        // We calculate playerStartTime here because it's the wall clock time that the player sees (as opposed to the thread time which starts in findBestStart)
        final long playerStartTime = System.currentTimeMillis();

        //put this on a separate thread so we display a spinner while it works
        //(note that the decision among discards is also multi-threaded)
        class DiscardOrDrawTask extends AsyncTask<Void, Void, Game.PileDecision> {

            @Override
            protected Game.PileDecision doInBackground(Void... params) {
                final Game.PileDecision pickFrom = tryDiscardOrDrawPile(isFinalTurn, Deck.getInstance().peekFromDiscardPile(), Deck.getInstance().peekFromDrawPile());
                //now actually deal the card
                if (pickFrom == Game.PileDecision.DISCARD_PILE) {
                    drawnCard = Deck.getInstance().drawFromDiscardPile();
                    turnInfo.append(String.format(turnInfoFormat, EasyComputerPlayer.this.getName(), drawnCard.getCardString(),
                            "Discard", getHandDiscard().getCardString()));
                } else { //DRAW_PILE
                    //if we decided to not use the Discard pile, then tryDiscardOrDrawPile has already found the right discard for the Draw pile
                    drawnCard = Deck.getInstance().drawFromDrawPile();
                    turnInfo.append(String.format(turnInfoFormat, EasyComputerPlayer.this.getName(), drawnCard.getCardString(),
                            "Draw", getHandDiscard().getCardString()));
                }
                final long playerStopTime = System.currentTimeMillis();
                Log.d(FiveKings.APP_TAG, String.format("Turn time was %.3f seconds", (playerStopTime - playerStartTime) / 1000.0));
                if (EasyComputerPlayer.this.method == Meld.MeldMethod.PERMUTATIONS)
                    EasyComputerPlayer.this.method = ((playerStopTime - playerStartTime) < PERMUTATION_THRESHOLD) ? Meld.MeldMethod.PERMUTATIONS : Meld.MeldMethod.HEURISTICS;

                Log.d(FiveKings.APP_TAG, turnInfo.toString() + String.format("(drew the %s)",drawnCard.getCardString()));

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
                fKActivity.setShowHint(turnInfo.toString(), FiveKings.HandleHint.SHOW_HINT, false);

                //at this point the DiscardPile still shows the old card
                //animate... also calls syncDisplay and checkEndRound() in the OnAnimationEnd listener
                fKActivity.animateComputerPickUpAndDiscard(EasyComputerPlayer.this.getMiniHandLayout(), pickFrom);
                fKActivity.endTurnCheckRound(fKActivity.isShowComputerCards()); //Also sets TurnState to NOT_MY_TURN

                //turnState is set to NOT_MY_TURN in endTurn (in onAnimationEnd)
            }
        }
        DiscardOrDrawTask discardOrDrawTask= new DiscardOrDrawTask();
        discardOrDrawTask.execute();
    }

    @Override
    void logTurn(final boolean isFinalTurn) {
        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        if (this.method == Meld.MeldMethod.PERMUTATIONS)
            Log.d(FiveKings.APP_TAG, "Player " + this.getName() + ": Using Permutations");
        else Log.d(FiveKings.APP_TAG, "Player " + this.getName() + ": Using Heuristics");
        String sValuationOrScore = isFinalTurn ? "Score=" : "Valuation=";
        Log.d(FiveKings.APP_TAG, "before...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
                + sValuationOrScore + this.getHandValueOrScore(isFinalTurn));
    }

    protected Game.PileDecision tryDiscardOrDrawPile(final boolean isFinalTurn, final Card discardPileCard, final Card drawPileCard) {
        Game.PileDecision decision = null;
        boolean foundBestHand;

        // If Permutations finishes successfully, then this loop will run once; if it times out it will run again using Heuristics
        do {
            foundBestHand = true;
            //try/catch for whether findBestHandFinish is interrupted because it takes too long - only for Permutations
            try {
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
            } catch (InterruptedException e) {
                if (method == Meld.MeldMethod.HEURISTICS) throw new RuntimeException(FiveKings.APP_TAG + "tryDiscardOrDrawPile: interrupted in Heuristics loop");
                //reset to using Heuristics and run findBestHandStart
                Log.d(FiveKings.APP_TAG,"Permutations timed out; restarting with Heuristics");
                foundBestHand = false;
                method = Meld.MeldMethod.HEURISTICS;
                findBestHandStart(isFinalTurn, discardPileCard);
            }
        }while (!foundBestHand);
        return decision; //just for logging and animation
    }

    //always true for Easy Computer Hand
    protected boolean keepDiscardDecision(final Hand bestDiscardPileHand, final Hand currentHand, final Card discardPileCard, final boolean isFinalTurn) {
        return true;
    }

    @Override
    public boolean isFirstBetterThanSecond(final MeldedCardList testHand, final MeldedCardList bestHand, final boolean isFinalTurn) {
        //randomly pick one or the other
        return Math.random() >0.5;
    }

    //Computer has to use this version which loops through possible discards to find the best one
    @Override
    void findBestHandStart(final boolean isFinalTurn, final Card addedCard) {
        Log.d(FiveKings.APP_TAG,"findBestHandStart: addedCard = " + addedCard.getCardString());
        //Loop over possible discards, so that now addAndEvaluate just looks at your hand without added
        //in fact, each loop the actual hand is different (including hand.cards) and will be saved if best
        CardList cardsWithAdded = new CardList(hand);
        cardsWithAdded.add(addedCard);

        this.numThreads = cardsWithAdded.size(); //number of possible discards to examine
        t = new Thread[numThreads];
        testHand = new ThreadedHand[numThreads];

        int iThread = 0;
        for (Card disCard : cardsWithAdded) {
            CardList cards = new CardList(cardsWithAdded);
            cards.remove(disCard);

            testHand[iThread] = new ThreadedHand(this.hand.getRoundOf(), cards, disCard, this.method, isFinalTurn); //creates new hand with replaced cards
            t[iThread] = new Thread(threadGroup,testHand[iThread]);
            t[iThread].start(); //calls meldAndEvaluate
            iThread++;
        }

    }

    private Hand findBestHandFinish(final Card addedCard) throws  InterruptedException {
        /* start sleeper timer once we start waiting; findBestStart is ok to run in the background between the previous player's discard
        and when you click on the next computer player
         */
        final Thread sleepThread = new Thread(new SleepThread(PERMUTATION_THRESHOLD),SLEEP_THREAD_NAME);
        sleepThread.start();

        Hand bestHand = this.hand;
        bestHand.setDiscard(addedCard); //default if we don't improve the score
        //wait for all threads to complete, but only for PERMUTATION_THRESHOLD time - the sleep timer will interrupt them all otherwise
        for (int iThread=0; iThread < this.numThreads; iThread++) {
            if (testHand[iThread].wasThreadInterrupted()) throw (new InterruptedException());
            try {
                t[iThread].join();

            } catch (InterruptedException e) {
                throw new InterruptedException(String.format("%s: findBestHandFinish interrupted in join()...",FiveKings.APP_TAG));
            }
            Log.d(FiveKings.APP_TAG, String.format("Thread \"%s\" (%d) took from %d to %d = %.3fs", t[iThread].getName(),iThread,
                    testHand[iThread].getThreadStart(),testHand[iThread].getThreadStop(),
                    (testHand[iThread].getThreadStop() - testHand[iThread].getThreadStart()) / 1000.0));
        }
        //Interrupt the sleep thread because we finished successfully
        Log.d(FiveKings.APP_TAG, "Interrupting sleep thread");
        sleepThread.interrupt();


        for (int iThread=0; iThread < this.numThreads; iThread++) {
            if (isFirstBetterThanSecond(testHand[iThread], bestHand, testHand[iThread].isFinalTurn)) {
                bestHand = testHand[iThread];
                if (bestHand.calculateValueAndScore(testHand[iThread].isFinalTurn) == 0) {
                    Log.d(FiveKings.APP_TAG, String.format("findBestHandFinish: Went out after %d/%d possible discards",
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
        final private Meld.MeldMethod method;
        final private boolean isFinalTurn;
        private long threadStart;
        private long threadStop;
        private boolean wasThreadInterrupted;


        ThreadedHand (final Rank roundOf, final CardList cards, final Card discard,final Meld.MeldMethod method, final boolean isFinalTurn) {
            super(roundOf, cards, discard);
            this.method = method;
            this.isFinalTurn = isFinalTurn;
            this.threadStart = 0;
            this.threadStop = 0;
            this.wasThreadInterrupted = false;
        }

        @Override
        public void run()  {
            threadStart = System.currentTimeMillis();
            wasThreadInterrupted = false;
            try {
                this.meldAndEvaluate(this.method, EasyComputerPlayer.this , this.isFinalTurn);
            } catch (InterruptedException e) {
                //just break out of this thread
                threadStop = System.currentTimeMillis();
                Log.d(FiveKings.APP_TAG, String.format("Thread \"%s\" interrupted after %.3fs",Thread.currentThread().getName(),
                        (threadStop - threadStart)/1000.0));
                wasThreadInterrupted = true;
            }
            threadStop = System.currentTimeMillis();
        }

        long getThreadStart() {
            return threadStart;
        }

        long getThreadStop() {
            return threadStop;
        }

        boolean wasThreadInterrupted() {
            return wasThreadInterrupted;
        }
    }

    /* INNER CLASS SleepThread */
    //Just sleeps for the PERMUTATION_THRESHOLD and then interrupts the other threads
    private class SleepThread implements Runnable {
        final private long sleepTime;

        SleepThread(final long sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            final long sleepStartTime = System.currentTimeMillis();
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // if interrupted, this means the other threads finished their work before the sleep ended
                Log.d(FiveKings.APP_TAG, String.format("Sleep interrupted after %.3fs", (System.currentTimeMillis() - sleepStartTime)/1000.0 ));
                return;
            }
            Log.d(FiveKings.APP_TAG, String.format("Sleep finished after %.3fs; interrupting other threads", (System.currentTimeMillis() - sleepStartTime)/1000.0 ));
            threadGroup.interrupt(); //interrupt the other calculation threads
        }
    }

    /* PARCELABLE read/write for ComputerPlayer (use superclass implementation) */
    protected EasyComputerPlayer(Parcel parcel) {
        super(parcel);
        //don't need thread member variables
        method = Meld.MeldMethod.valueOf(parcel.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        //don't need thread member variables (dynamically re-created)
        parcel.writeString(method.toString());
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<EasyComputerPlayer> CREATOR = new Parcelable.Creator<EasyComputerPlayer>() {
        @Override
        public EasyComputerPlayer createFromParcel(Parcel in) {
            return new EasyComputerPlayer(in);
        }

        @Override
        public EasyComputerPlayer[] newArray(int size) {
            return new EasyComputerPlayer[size];
        }
    };

}//end ComputerPlayer

