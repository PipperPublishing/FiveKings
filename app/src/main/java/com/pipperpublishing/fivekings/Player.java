package com.pipperpublishing.fivekings;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

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
 * 9/30/2015    Added showCards (depends on Human or Computer)
 * TODO:A Move roundScore to Hand (Down from Player)
 */
abstract class Player implements HandComparator, Parcelable {
    private String name;
    private int roundScore;
    private int cumulativeScore;

    protected Hand hand;
    protected Card drawnCard; //possibly should also be in hand (like the Discard)
    static protected enum TurnState {
        NOT_MY_TURN, PREPARE_TURN, PLAY_TURN
    }
    protected TurnState turnState;
    protected PlayerMiniHandLayout miniHandLayout; //representation on-screen including score etc

    @Deprecated
    static final Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(final Player lhs, final Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };

    Player(final String name) {
        this.name = name;
        initGame();
    }

    //Copy constructor
    Player(final Player player) {
        this.name = player.name;
        this.roundScore = player.roundScore;
        this.cumulativeScore = player.cumulativeScore;
        this.hand = player.hand;
        this.miniHandLayout = player.miniHandLayout;
    }


    boolean initGame() {
        this.cumulativeScore = 0;
        this.roundScore = 0; //only zero this here because we display all scores at the start of a new game
        this.hand = null;
        this.miniHandLayout = null;
        this.drawnCard = null;
        this.turnState = TurnState.NOT_MY_TURN;
        return true;
    }

    boolean initAndDealNewHand(final Deck.DrawPile drawPile, final Rank roundOf) {
        this.roundScore = 0;
        this.turnState = TurnState.NOT_MY_TURN;
        this.hand = new Hand(drawPile, roundOf);
        return true;
    }

    //default implementation does nothing
    int checkMeldsAndEvaluate(final boolean isFinalTurn) {
        return -1;
    }

    abstract Card discardFromHand(final Card cardToDiscard);

    protected void checkHandSize() throws RuntimeException {
        if (!hand.checkSize())
            throw new RuntimeException(Game.APP_TAG + "checkHandSize: Hand length is too short/long");
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

    final void updatePlayerMiniHand(final boolean isCurrent, boolean updateCumScore) {
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

    final int addToCumulativeScore() {
        cumulativeScore += roundScore;
        return cumulativeScore;
    }

    final String getName() {
        return name;
    }

    final Card getHandDiscard() {
        return hand.getDiscard();
    }

    void setHandDiscard(Card discard) {
        hand.setDiscard(discard);
    }

    final String getMeldedString(final boolean withBraces) {
        StringBuilder mMelds = new StringBuilder("Melds ");
        if (withBraces) mMelds.append("{");
        mMelds.append(MeldedCardList.getString(hand.melds));
        if (withBraces) mMelds.append("} ");
        return mMelds.toString();
    }

    final String getPartialAndSingles(final boolean withBraces) {
        String unMelded = MeldedCardList.getString(hand.partialMelds);
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

    final int getHandValueOrScore(final boolean isFinalScore) {
        return hand.calculateValueAndScore(isFinalScore);
    }

    final int getRoundScore() {
        return roundScore;
    }

    final PlayerMiniHandLayout getMiniHandLayout() {
        return miniHandLayout;
    }

    final ArrayList<CardList> getHandMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getMelded());
        return combined;
    }

    //TODO:A: Not unrolling these right now (Human doesn't see this)
    //because otherwise we don't know what to add back to
    //have to eliminate "combined"
    final ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
    }

    final int getCumulativeScore() {
        return cumulativeScore;
    }

    Card getDrawnCard() {
        return drawnCard;
    }

    abstract boolean isHuman();

    final boolean isOut() {
        return ((hand != null) && (hand.calculateValueAndScore(true) == 0));
    }

    /*-----------------------------------------------------*/
    /* GAME PLAYING TURNS - depends on what type of player */
    /*-----------------------------------------------------*/
    abstract void prepareTurn(final FiveKings fKActivity, final boolean hideHandInitially);

    abstract void takeTurn(final FiveKings fKActivity, Game.PileDecision drawOrDiscardPile, final Deck deck, final boolean isFinalTurn);

    abstract void logTurn(final boolean isFinalTurn);

    abstract void updateHandsAndCards(final FiveKings fkActivity, final boolean afterFinalTurn);

    void findBestHandStart(final boolean isFinalTurn, final Card addedCard) {
        //default does nothing
    }

    final Player endTurn(final Player passedPlayerWentOut, final Deck deck) {
        Player playerWentOut = passedPlayerWentOut;
        //TODO:A Nasty that we have to pass deck down all this way to do discard
        //remove discard from player's hand and add to discardPile
        deck.discardPile.add(this.discardFromHand(this.getHandDiscard()));
        //only HumanPlayer does anything
        this.checkMeldsAndEvaluate(playerWentOut != null);

        String sValuationOrScore = (null == playerWentOut) ? "Valuation=" : "Score=";
        Log.d(Game.APP_TAG, "after...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
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

    final TurnState getTurnState() {
        return turnState;
    }

    final void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }

    /*-------------------------------------------------------------
            /* INNER CLASS: Hand - use of MeldedCardList specific to Players */
    /*-----------------------------------------------------------*/
    protected class Hand extends MeldedCardList {
        private Card discard; //discard associated with this hand

        Hand(final Rank roundOf) {
            super(roundOf, Player.this);
            discard = null;
        }

        //deal and return hand
        private Hand(final Deck.DrawPile drawPile, final Rank roundOf) {
            this(roundOf);
            if (drawPile != null) {
                singles = new Meld(this.playerHandComparator, drawPile.deal(roundOf.getRankValue())); //only for human really
                Collections.sort(singles,Card.cardComparatorRankFirstDesc);
                this.clear();
                this.addAll(singles);
            }
        }

        //minimal constructor - copies and sets the cards (used in trying out different discards)
        //note that it doesn't copy other values which are not relevant
        protected Hand(final Rank roundOf, final CardList cards, final Card discard) {
            this(roundOf);
            this.clear();
            this.addAll(cards);
            this.discard = discard;
        }

        private boolean checkSize() {
            return roundOf.getRankValue() == this.size();
        }

        Card discardFrom(final Card discardedCard) {
            this.remove(discardedCard);
            syncCardsAndMelds();
            return discardedCard;
        }

        void addAndSync(final Card addedCard) {
            this.add(addedCard);
            syncCardsAndMelds();
        }

        boolean makeNewMeld(final Card card) {
            Meld newMeld = new Meld(this.playerHandComparator, MeldComplete.SINGLES);
            newMeld.addTo(card);
            melds.add(newMeld);
            return true;
        }

        //TODO:A: And update hand valuation (would need to pass isFinalScore or call twice)
        //Use iterators so we can remove current value
        private boolean syncCardsAndMelds() {
            //check that all hand.cards are in melds or singles (add to singles if not)
            for (Card card : this) {
                boolean foundCard = singles.contains(card) || contains(melds, card) || contains(partialMelds, card);
                if (!foundCard) singles.add(card);
            }
            //check that all cards in melds, partialMelds, and singles are in hand.cards (remove if not)
            for (Iterator<Meld> mIterator = melds.iterator(); mIterator.hasNext(); ) {
                CardList cl = mIterator.next();
                for (Iterator<Card> cIter = cl.iterator(); cIter.hasNext(); ) {
                    if (!this.contains(cIter.next())) cIter.remove();
                }
                if (cl.isEmpty()) mIterator.remove();
            }
            for (Iterator<Meld> clIterator = partialMelds.iterator(); clIterator.hasNext(); ) {
                CardList cl = clIterator.next();
                for (Iterator<Card> cIter = cl.iterator(); cIter.hasNext(); ) {
                    if (!this.contains(cIter.next())) cIter.remove();
                }
                if (cl.isEmpty()) clIterator.remove();
            }
            for (Iterator<Card> cardIterator = singles.iterator(); cardIterator.hasNext(); ) {
                if (!this.contains(cardIterator.next())) cardIterator.remove();
            }

            //and sort the singles
            Collections.sort(singles, Card.cardComparatorRankFirstDesc);
            return true;
        }


        //used by Computer (or eventually maybe Human with [Meld] button)
        // the Hand this is being called on is the test hand, so we set here the member variables
        protected int meldAndEvaluate(final MeldMethod method, final boolean isFinalTurn) {
            if (method == MeldMethod.PERMUTATIONS) meldBestUsingPermutations(isFinalTurn);
            else meldUsingHeuristics(isFinalTurn);
            return this.calculateValueAndScore(isFinalTurn);
        }

        /* HEURISTICS
        v2 values keeping pairs and potential sequences, because otherwise we'll throw away a King from a pair of Kings
        Strategy is to:
        1. Maximize melds: 3 or more rank or sequence melds - maximize this
        2. Maximize partialMelds: pairs and broken sequences (for now just +/-1) - want to maximize number of cards in this
        3. Minimize unMelded: remaining singles - minimize the score of this by throwing away the
        In the final scoring, calculate partialMelds and unMelded value
        Evaluation accounts for hand potential, but Scoring is just what's left after melding
        */
        private boolean meldUsingHeuristics(final boolean isFinalTurn) {
            //Log.d(Game.APP_TAG,"Entering meldUsingHeuristics");
            final int numCards = this.size();
            final Rank wildCardRank = this.roundOf;
            //list of potential melds (pairs and broken sequences) - could be many of these
            //because a card can be in the list multiple times
            ArrayList<Meld> fullRankMelds = new ArrayList<>(numCards);
            ArrayList<Meld> fullSequences = new ArrayList<>(numCards);
            ArrayList<Meld> partialRankMelds = new ArrayList<>(numCards);
            ArrayList<Meld> partialSequences = new ArrayList<>(numCards);

            //separate into wildcards and non-wildcards
            //Log.d(Game.APP_TAG,"---Separate out wildcards");
            CardList wildCards = new CardList(numCards);
            CardList nonWildCards = new CardList(numCards);
            sortAndSeparateWildCards(wildCardRank, this, nonWildCards, wildCards);

            //Rank Matches - loop from highest to lowest rank
            //Log.d(Game.APP_TAG,"---find matches in each rank");
            CardList cardsSortedByRankDesc = new CardList(nonWildCards);
            Collections.sort(cardsSortedByRankDesc, Card.cardComparatorRankFirstDesc); //e.g. (K*,KH,QD,JC...)
            Meld rankMatch = new Meld(this.playerHandComparator, MeldComplete.SINGLES);
            rankMatch.clear();
            //will meld larger cards first because of the comparator above
            for (Card card : cardsSortedByRankDesc) {
                //if the card is already in a full meld, then skip it
                // don't' have to decomposeAndCheck wildcards because we've removed them
                if (!contains(fullRankMelds, card)) {
                    if (rankMatch.isEmpty()) rankMatch.add(card);
                    else if (card.isSameRank(rankMatch.get(rankMatch.size() - 1))) {
                        rankMatch.setMeldType(MeldType.RANK);
                        rankMatch.add(card);
                    } else {
                        //not same rank; record and restart the sequence
                        addWildcardsAndRecord(fullRankMelds, partialRankMelds, rankMatch, wildCards, isFinalTurn);
                        rankMatch.clear();
                        rankMatch.add(card);
                    }//end-if same rank
                }
            }
            addWildcardsAndRecord(fullRankMelds, partialRankMelds, rankMatch, wildCards, isFinalTurn);

            //Sequences - for now full sequences (3*-4*-5*) or broken pairs (e.g. 3*-5*) in partialSequences list
            //Log.d(Game.APP_TAG,"---find matches in each sequence");
            CardList cardsSortedBySuit = new CardList(nonWildCards);
            Collections.sort(cardsSortedBySuit, Card.cardComparatorSuitFirst); //e.g. 3S,5S,JS,6H,8H...
            Meld sequenceMatch = new Meld(this.playerHandComparator, MeldComplete.SINGLES);
            for (Suit suit : Suit.values()) {
                sequenceMatch.clear();
                for (Card card : cardsSortedBySuit) {
                    //if the card is already in a full meld or sequence, then skip it
                    if (card.isSameSuit(suit) && !contains(fullRankMelds, card) && !contains(fullSequences, card)) {
                        if (sequenceMatch.isEmpty()) sequenceMatch.add(card);
                        else if (1 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size() - 1))) {
                            sequenceMatch.setMeldType(MeldType.SEQUENCE);
                            sequenceMatch.add(card);
                        }
                        //broken sequence; record but put into partial sequences AND into next sequence (unless we used aa wildcard to make it full)
                        else if ((1 == sequenceMatch.size()) && (2 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size() - 1)))) {
                            sequenceMatch.setMeldType(MeldType.SEQUENCE);
                            sequenceMatch.add(card);
                            boolean madePartialIntoFull = addWildcardsAndRecord(fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
                            sequenceMatch.clear();
                            if (!madePartialIntoFull) sequenceMatch.add(card);
                        } else {
                            //not adjacent; record and restart the sequence
                            addWildcardsAndRecord(fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
                            sequenceMatch.clear();
                            sequenceMatch.add(card);
                        }
                    }//end-if same suit
                }
                addWildcardsAndRecord(fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
            }//end for Suits

            //Go back and check if partial rank melds overlap with full sequences; if so, drop the partial rank meld
            for (Iterator<Meld> iterator = partialRankMelds.iterator(); iterator.hasNext(); ) {
                CardList rankMeld = iterator.next();
                if (contains(fullSequences, rankMeld)) iterator.remove();
            }

            //If we still have 2+ wildcards, meld singles into a full meld - but don't create partials with a wildcard
            Meld meldOfSingles = new Meld(this.playerHandComparator, MeldComplete.SINGLES);
            for (Card card : cardsSortedByRankDesc) {
                if (wildCards.size() <= 1) break;
                if (!contains(fullRankMelds, card) && !contains(fullSequences, card)) {
                    meldOfSingles.clear();
                    meldOfSingles.add(card);
                    meldOfSingles.add(wildCards.get(0));
                    wildCards.remove(0);
                    meldOfSingles.setMeldType(MeldType.EITHER); //because it's X-Wild-Wild
                    addWildcardsAndRecord(fullRankMelds, partialRankMelds, meldOfSingles, wildCards, isFinalTurn);
                }
            }


            //if there are remaining wildcards, keep adding them to existing melds until we run out
            while (!wildCards.isEmpty() && (!fullRankMelds.isEmpty() || !fullSequences.isEmpty())) {
                //Log.d(Game.APP_TAG, "---extend existing melds/sequence");
                for (Meld rankMeld : fullRankMelds) {
                    rankMeld.add(wildCards.get(0));
                    wildCards.remove(0);
                    if (wildCards.isEmpty()) break;
                }
                if (!wildCards.isEmpty()) {
                    for (Meld sequenceMeld : fullSequences) {
                        sequenceMeld.add(wildCards.get(0));
                        wildCards.remove(0);
                        if (wildCards.isEmpty()) break;
                    }
                }
            }

            //ArrayList of Melds (so we can separate melds from each other)
            melds = new ArrayList<>(fullRankMelds);
            melds.addAll(fullSequences);
            setCheckedAndValid(melds);

            // For final scoring (last licks) we don't show partial melds (they all go into singles)
            // For intermediate scoring they count reduced and we don't show in singles
            if (isFinalTurn) {
                //partialMelds should always already be clear (because checked earlier)
                if (!partialRankMelds.isEmpty() || !partialSequences.isEmpty())
                    Log.e(Game.APP_TAG, "meldUsingHeuristics: partialMelds not empty in final scoring");
                partialMelds.clear();
            } else {
                partialMelds = new ArrayList<>(partialRankMelds);
                partialMelds.addAll(partialSequences);
            }
            //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
            // since the wildcards are no longer being melded into partials need to add them back here
            singles = new Meld(this.playerHandComparator, nonWildCards);
            for (Card card : nonWildCards) {
                if (contains(melds, card)) singles.remove(card);
                if (!isFinalTurn && contains(partialMelds, card)) singles.remove(card);
            }
            singles.addAll(wildCards);

            //don't need to find discard (we are looping over possible discards if this is a Computer turn)
            //and scoring is now done in the caller
            //Log.d(Game.APP_TAG,"---exiting meldUsingHeuristics");
            return true;
        }//end int meldUsingHeuristics

        //save test if full; see if you can pad it to a full with wildcards and keep/discard if not
        private boolean addWildcardsAndRecord(final ArrayList<Meld> fulls, final ArrayList<Meld> partials, final Meld test, final CardList wildCards, final boolean isFinalTurn) {
            boolean madePartialIntoFull = false;
            if (test.size() >= 3) {
                test.setMeldComplete(MeldComplete.FULL);
                fulls.add((Meld) test.clone());
            } else if (2 == test.size()) {
                if (!wildCards.isEmpty()) {
                    test.add(wildCards.get(0));
                    wildCards.remove(0);
                    //These should all be short melds - call decompose to order them correctly
                    //also set meldComplete and isCheckedAndValid
                    if (test.decomposeAndCheck(isFinalTurn, new MeldedCardList(this.roundOf,playerHandComparator)) != 0) {
                        throw new RuntimeException("addWildCardsAndRecord: full meld not evaluating to 0");
                    }
                    fulls.add((Meld) test.clone());
                    madePartialIntoFull = true;
                } else if (!isFinalTurn) { //only create partial melds on intermediate turns
                    test.setMeldComplete(MeldComplete.PARTIAL);
                    partials.add((Meld) test.clone());
                }
            }
            return madePartialIntoFull;
        }


        /* PERMUTATIONS - v1
            Consider all permutations (shouldn't be too expensive) and return value of unmelded
            This is the sledgehammer approach - we use this until it gets too slow and then switch to heuristics
            Everything melded gives the maximum evaluation of 0
             */
        private void meldBestUsingPermutations(final boolean isFinalTurn) {
            final int numCards = this.size();
            final Rank wildCardRank = this.roundOf;

            Meld cardsCopy = new Meld(this.playerHandComparator, this);
            MeldedCardList bestMeldedCardList = new MeldedCardList(this.roundOf, this.playerHandComparator);
            //have to do this two step process (passing best.. and cardsCopy separately)
            //because decomposeAndCheck is used in multiple ways
            cardsCopy.decomposeAndCheck(isFinalTurn, bestMeldedCardList);
            this.copyJustMelds(bestMeldedCardList);

        }//end meldBestUsingPermutations

        //Helper for HumanPlayer.checkMeldsAndEvaluate (throws away decomposition, but calculates valuation)
        //TODO:A May be able to use a version of this in meldUsing Heuristics (as part of ComputerPlayer)
        final int checkMeldsAndEvaluate(final boolean isFinalTurn) {
            final MeldedCardList decomposedMelds = new MeldedCardList(this.roundOf, this.playerHandComparator);
            for (MeldedCardList.Meld meld : this.melds) {
                meld.decomposeAndCheck(isFinalTurn, decomposedMelds);
            }
            return this.calculateValueAndScore(isFinalTurn);
        }

        /* SETTERS and GETTERS */
        final Card getDiscard() {
            return this.discard;
        }

        final void setDiscard(final Card discard) {
            if (null == discard) throw new RuntimeException("setDiscard: discard == null");
            this.discard = discard;
        }


    }//end Inner Class Hand

    /* PARCELABLE INTERFACE */
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
        name = in.readString();
        roundScore = in.readInt();
        cumulativeScore = in.readInt();
        hand = (Hand) in.readValue(Hand.class.getClassLoader());
        drawnCard = (Card) in.readValue(Card.class.getClassLoader());
        turnState = TurnState.valueOf(in.readString());
        //not saving miniHandLayout reference

    }

}
