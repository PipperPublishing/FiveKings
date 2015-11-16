/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.pipperpublishing.fivekings.view.FiveKings;
import com.pipperpublishing.fivekings.view.PlayerMiniHandLayout;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 If DiscardPile reduces score, then use it, otherwise use drawPile
 * 2/4/2015 Remove drawPileCard from useDiscardPile decision (may later add back % decisions) and changes to separate meld/score with drawCard
 * 2/4/2015 Split discard/draw, meld&score, and discard into separate steps to make it easier to include human players
 * 2/15/2015 Sort unmelded cards for easier viewing
 * 2/15/2015 Initialize isFirstTurn and then check in useDiscardPile to see if we need to do initial melding
 * 2/17/2015 Don't use iPosition for now
 * 2/17/2015    Added isHuman to control when you can click on piles
 * Added checkHandSize() to make sure we have the right number of cards
 * 2/24/2015    Move Hand to be inner class
 * Eliminated meldAndEvaluateAsIs, evaluateIfFirst by melding when we deal the hand
 * 2/26/2015    getUnMelded returns melds and singles unrolled into one CardList for display
 * 2/27/2015    Added addToMeld and makeNewMeld; include checking for dropping back onto itself
 * 2/28/2015    Added Meld definition to record valuation (tells us whether it's a valid meld)
 * 3/3/2015     Added static method to check if a CardList is a valid meld
 * 3/4/2015     Removed meldAndEvaluate (replaced with checkMeldsAndEvaluate)
 * 3/7/2015     Zero round score in initGame so that display shows correctly
 * and split updateRoundScore out so it can be called in endCurrentPlayerTurn
 * 3/11/2015    Moved updatePlayerLayout to PlayerLayout as "update"
 * 3/12/2015    v0.4.01:
 * Change method parameters to final
 * 3/13/2015    Moved calculateValue to be a Player method so it can be overridden and brought cardScore in here from Card
 * 3/14/2015    meldUsingHeuristics: When melding singles need 2 wildcards to make a meld from a single (don't keep partials with a wildcard)
 * Removed iPosition; maintaining this during deletions or adds would be painful
 * 3/15/2015    meldUsingHeuristics needs to add wildcards into singles if they aren't melded away
 * 3/16/2015    Convert to Meld using Hand; also create a PlayerHand which has roundOf and discard added
 * and relevant methods
 * 3/22/2015    Move logTurn etc from Game to here or subclasses
 * 3/26/2015    Introduced TURN_STATE to tell what to do when clicked
 * 4/8/2015     Not a bug to have no wildcards and call addWildCards...
 *              meldUsingHeuristics calls decomposeAndCheck to order wildcard melds
 * 6/18/2015    Sort Hand immediately after dealing
 * 8/30/2015    Implement Parcelable so we can save Game state
 * 9/3/2015     Save TurnState using string/valueOf
 * 9/30/2015    Added showHandsAndCards (depends on Human or Computer)
 * 10/10/2015   turnState was not being copied in Copy Constructor which messes up player updates
 * 10/18/2015   Change per player showHandsAndCards to returning a showCards flag
 *  10/20/2015  Hide drawPile and discardPile - access through deck
 */
abstract public class Player implements HandComparator, Parcelable {
    private String name;
    private int roundScore;
    private int cumulativeScore;

    protected Hand hand;
    protected Card drawnCard; //possibly should also be in hand (like the Discard)
    public enum TurnState {NOT_MY_TURN, PREPARE_TURN, PLAY_TURN }
    protected TurnState turnState;
    protected PlayerMiniHandLayout miniHandLayout; //representation on-screen including score etc

    @Deprecated
    static final public Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(final Player lhs, final Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };

    Player(final String name) {
        this.name = name;
        //don't null miniHandLayout between games; this is not removed unless you add/delete players
        this.miniHandLayout = null;
        initGame();
    }

    //Copy constructor
    Player(final Player player) {
        this.name = player.name;
        this.roundScore = player.roundScore;
        this.cumulativeScore = player.cumulativeScore;
        this.hand = player.hand;
        this.drawnCard = player.drawnCard;
        this.turnState = player.turnState;
        this.miniHandLayout = player.miniHandLayout;

        //Update the miniHandLayout to point to the new player
        this.miniHandLayout.updatePlayer(this);
    }



    boolean initGame() {
        this.cumulativeScore = 0;
        this.roundScore = 0; //only zero this here because we display all scores at the start of a new game
        this.hand = null;
        this.drawnCard = null;
        this.turnState = TurnState.NOT_MY_TURN;
        return true;
    }

    boolean initAndDealNewHand(final Rank roundOf) {
        this.roundScore = 0;
        this.turnState = TurnState.NOT_MY_TURN;
        this.hand = new Hand(roundOf);
        this.hand.deal();
        return true;
    }

    //default implementation does nothing
    int checkMeldsAndEvaluate(final boolean isFinalTurn) {
        return -1;
    }

    abstract public Card discardFromHand(final Card cardToDiscard);

    protected void checkHandSize() throws RuntimeException {
        if (!hand.checkSize())
            throw new RuntimeException(FiveKings.APP_TAG + "checkHandSize: Hand length is too short/long");
    }

    final PlayerMiniHandLayout addPlayerMiniHandLayout(final Context c, final int iPlayer, final int numPlayers) {
        this.miniHandLayout = new PlayerMiniHandLayout(c, this, iPlayer, numPlayers);
        return this.miniHandLayout;
    }

    final void removePlayerMiniHand() {
        this.miniHandLayout = null;
    }

    final void resetPlayerMiniHand() {
        if (miniHandLayout != null) miniHandLayout.reset();
    }

    final public void updatePlayerMiniHand(final boolean isCurrent, boolean updateCumScore) {
        if (miniHandLayout != null) {
            miniHandLayout.update(isCurrent, this.isOut(), this.getRoundScore(),
                    updateCumScore ? this.getCumulativeScore() : -1);
        }
    }

    //Player GETTERS and SETTERS
    final int updateRoundScore() {
        this.roundScore = hand.calculateValueAndScore(true);
        return roundScore;
    }

    final void updateName(final String updatedName) {
        this.name = updatedName;
        this.miniHandLayout.updateName(updatedName);
    }

    final public int addToCumulativeScore() {
        cumulativeScore += roundScore;
        return cumulativeScore;
    }

    final public String getName() {
        return name;
    }

    final public Card getHandDiscard() {
        return hand.getDiscard();
    }

    void setHandDiscard(Card discard) {
        hand.setDiscard(discard);
    }

    final String getMeldedString(final boolean withBraces) {
        StringBuilder mMelds = new StringBuilder("Melds ");
        if (withBraces) mMelds.append("{");
        mMelds.append(Meld.getString(hand.melds));
        if (withBraces) mMelds.append("} ");
        return mMelds.toString();
    }

    final protected String getPartialAndSingles(final boolean withBraces) {
        String unMelded = Meld.getString(hand.partialMelds);
        String singles = hand.singles.getString();
        StringBuilder partialAndSingles = new StringBuilder();
        if (!unMelded.isEmpty()) {
            partialAndSingles.append("Potential melds ");
            if (withBraces) partialAndSingles.append("{");
            partialAndSingles.append(unMelded);
            if (withBraces) partialAndSingles.append("} ");
        }
        if (!singles.isEmpty()) {
            partialAndSingles.append("Unmelded");
            if (withBraces) partialAndSingles.append("{");
            partialAndSingles.append(singles);
            if (withBraces) partialAndSingles.append("} ");
        }
        return partialAndSingles.toString();
    }

    final protected int getHandValueOrScore(final boolean isFinalScore) {
        return hand.calculateValueAndScore(isFinalScore);
    }

    final public int getRoundScore() {
        return roundScore;
    }

    final public PlayerMiniHandLayout getMiniHandLayout() {
        return miniHandLayout;
    }

    final public ArrayList<CardList> getHandMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getMelded());
        return combined;
    }

    //TODO:B: Not unrolling these right now (Human doesn't see this)
    //because otherwise we don't know what to add back to
    //have to eliminate "combined"
    final public ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
    }

    final public int getCumulativeScore() {
        return cumulativeScore;
    }

    public Card getDrawnCard() {
        return drawnCard;
    }

    abstract public boolean isHuman();

    final boolean isOut() {
        return ((hand != null) && (hand.calculateValueAndScore(true) == 0));
    }

    /*-----------------------------------------------------*/
    /* GAME PLAYING TURNS - depends on what type of player */
    /*-----------------------------------------------------*/
    public void prepareTurn(final FiveKings fKActivity) {
        turnState = TurnState.PLAY_TURN;
        fKActivity.showHandsAndCards(showCards(fKActivity.isShowComputerCards()), fKActivity.getmGame().getCurrentPlayer().isHuman());
    }

    abstract public void takeTurn(final FiveKings fKActivity, Game.PileDecision drawOrDiscardPile, final boolean isFinalTurn);

    abstract void logTurn(final boolean isFinalTurn);

    final void logRoundScore() {
        Log.i(FiveKings.APP_TAG, "Player " + getName() + ": " + getMeldedString(true) + getPartialAndSingles(true) + ". Cumulative score=" + getCumulativeScore());
    }

    abstract public boolean showCards(final boolean isShowComputerCards);

    void findBestHandStart(final boolean isFinalTurn, final Card addedCard) {
        //default does nothing
    }

    final Player endTurn(final Player passedPlayerWentOut) {
        Player playerWentOut = passedPlayerWentOut;

        //only HumanPlayer does anything
        this.checkMeldsAndEvaluate(playerWentOut != null);

        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        Log.d(FiveKings.APP_TAG, "after...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
                + sValuationOrScore + this.getHandValueOrScore(null != playerWentOut));

        if ((playerWentOut == null) && this.isOut()) playerWentOut = this;
        if (playerWentOut != null) {
            this.updateRoundScore();
            miniHandLayout.setPlayedInFinalTurn(true);
        }
        this.turnState = TurnState.NOT_MY_TURN;
        return playerWentOut;
    }

    @Override
    public boolean isFirstBetterThanSecond(final MeldedCardList testHand, final MeldedCardList bestHand, final boolean isFinalTurn) {
        return testHand.calculateValueAndScore(isFinalTurn) <= bestHand.calculateValueAndScore(isFinalTurn);
    }

    final public TurnState getTurnState() {
        return turnState;
    }

    final void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }

    /* PARCELABLE INTERFACE for Player */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeInt(roundScore);
        out.writeInt(cumulativeScore);

        out.writeValue(hand);
        out.writeValue(drawnCard);
        out.writeString(turnState.toString());
        //Not saving miniHandLayout reference - we recreate this

    }

    protected Player(Parcel in) {
        Log.d(FiveKings.APP_TAG,"Unparceling Player...");
        name = in.readString();
        roundScore = in.readInt();
        cumulativeScore = in.readInt();
        hand = (Hand) in.readValue(Hand.class.getClassLoader());
        drawnCard = (Card) in.readValue(Card.class.getClassLoader());
        turnState = TurnState.valueOf(in.readString());
        //not saving miniHandLayout reference

    }

}
