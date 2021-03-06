/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.pipperpublishing.fivekings.view.FiveKings;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/15/2015    Moved discardFromHand here - actually does discard
 * 3/31/2015    Pushed takeTurn and logTurn here so that we don't have Human vs. Computer checks in Game
 * 10/12/2015   Pass drawnCard to animateHumanPickup to further separate logic from View
 *  * 10/18/2015   Change per player showHandsAndCards to returning a showCards flag
 10/18/2015     Removed second click logic that would expose the hand if it was hidden
 *  10/20/2015  Hide drawPile and discardPile - access through deck
 * * 11/19/2015   Constructors must be public to support reflection (used in add/update player)
 *
 */
public class HumanPlayer extends Player {
    public HumanPlayer(final String name) {
        super(name);
    }
    public HumanPlayer(final Player player) {
        super(player);
    }

    @Override
    final boolean initAndDealNewHand(final Rank roundOf) {
        super.initAndDealNewHand(roundOf);
        this.hand.calculateValueAndScore(false);
        return true;
    }

    //Human player uses this version which adds the addedCard (the card a human picked) and checks the melds
    final void addAndEvaluate(final boolean isFinalTurn, final Card addedCard) {
        addCardToHand(addedCard);
        checkMeldsAndEvaluate(isFinalTurn);
    }

    @Override
    final int checkMeldsAndEvaluate(final boolean isFinalTurn) {
        //sets the valuation of each meld (0 for a valid meld)
        //throws away the decomposition, because correct melding is the HumanPlayer's responsibility
        //This allows for later implementation of automatic melding
        return hand.checkMeldsAndEvaluate(this, isFinalTurn);
    }

    private boolean addCardToHand(final Card card) {
        if (card != null) {
            checkHandSize();
            hand.addAndSync(card); //calls syncCardsAndMelds
        }
        return (card != null);
    }

    @Override
    final public Card discardFromHand(final Card cardToDiscard) {
        if (cardToDiscard != null) {
            hand.discardFrom(cardToDiscard); //calls syncCardsAndMelds
            checkHandSize();
        }
        return cardToDiscard;
    }


    final public boolean makeNewMeld(final Card card) {
        hand.makeNewMeld(card);
        return true;
    }
    final public void addToMeld(final CardList meld, Card card) {
        hand.addToMeld(card, (Meld) meld);
    }

    /*----------------------------------------*/
    /* Game playing methods (moved from Game) */
    /*----------------------------------------*/
    @Override
    public void prepareTurn(final FiveKings fKActivity) {
        //base method sets PLAY_TURN and updates hands and cards
        super.prepareTurn(fKActivity);
        fKActivity.enableDrawDiscardClick(); //also animates piles and sets the hint
        this.getMiniHandLayout().clearAnimatedMiniHand();
    }

    @Override
    //Human cards are always shown, unless the next player is also human and this is the end of the turn
    final public boolean showCards(final boolean isShowComputerCards) {
        return true;
    }

    /* getHandUnMelded has to return the actual melds (not just copies) that are in the hand, so that dragging cards into another meld works
However, for Humans we don't currently use Partial melds (cards are either Singles or "Full" melds) and for Droid hands we don't allow dragging
So for Droid hands we override this to unroll it into just a single set of cards, and for Humans we rely on the fact that nothing is in unmelded
(just in case in future we want to add dragging between partial melds)
 */
    @Override
    final public ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
    }


    @Override
    //TODO:A Bad smell from all those fKActivity calls
    public void takeTurn(final FiveKings fKActivity, final Game.PileDecision drawOrDiscardPile, final boolean isFinalTurn) {
        /* If you click on the Human hand a second time then takeTurn is called
        If a card was picked, then show the animation; if not, then show a hint
        */
        if (fKActivity.getmGame().getGameState() == GameState.HUMAN_PICKED_CARD) {
            final StringBuilder turnInfo = new StringBuilder(100);
            turnInfo.setLength(0);
            final String turnInfoFormat = fKActivity.getString(R.string.humanTurnInfo);

            logTurn(isFinalTurn);

            this.drawnCard = (drawOrDiscardPile == Game.PileDecision.DISCARD_PILE) ? Deck.getInstance().drawFromDiscardPile() : Deck.getInstance().drawFromDrawPile();
            this.addAndEvaluate(isFinalTurn, this.drawnCard);
            turnInfo.append(String.format(turnInfoFormat, this.getName(), this.drawnCard.getCardString(),
                    (drawOrDiscardPile == Game.PileDecision.DISCARD_PILE) ? "Discard" : "Draw"));

            Log.d(FiveKings.APP_TAG, turnInfo.toString());
            //don't meld, because it was done as part of evaluation

            fKActivity.getmGame().setGameState(GameState.HUMAN_READY_TO_DISCARD);
            //at this point the DiscardPile still shows the old card if you picked it
            //handles animating the card off the appropriate pile and making it appear in the hand
            fKActivity.animateHumanPickUp(drawOrDiscardPile,drawnCard);
        } else fKActivity.setShowHint(null, FiveKings.HandleHint.SHOW_HINT , true);
    }

    @Override
    void logTurn(final boolean isFinalTurn) {
        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        Log.d(FiveKings.APP_TAG, "Player " + this.getName());
        String sValuationOrScore = (isFinalTurn) ? "Score=" : "Valuation=";
        Log.d(FiveKings.APP_TAG, "before...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
                + sValuationOrScore + this.getHandValueOrScore(isFinalTurn));
    }

    /* PARCELABLE read/write (using superclass implementation) */
    protected HumanPlayer(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<HumanPlayer> CREATOR = new Parcelable.Creator<HumanPlayer>() {
        @Override
        public HumanPlayer createFromParcel(Parcel in) {
            return new HumanPlayer(in);
        }

        @Override
        public HumanPlayer[] newArray(int size) {
            return new HumanPlayer[size];
        }
    };

}
