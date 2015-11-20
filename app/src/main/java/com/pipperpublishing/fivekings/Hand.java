/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.util.Log;

import com.pipperpublishing.fivekings.view.FiveKings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by Jeffrey on 11/8/2015.
11/8/2015   Moved from Player because it was becoming unwieldy
            Have to pass player to get correct HandComparator
 11/9/2015  Use super class parcelable behavior appropriately

            Remove constructor and dealer
            Write discard to parcel as reference into Hand
 */


class Hand extends MeldedCardList {
    private Card discard; //discard associated with this hand

    Hand(final Rank roundOf) {
        super(roundOf);
        discard = null;
    }

    //minimal constructor - copies and sets the cards (used in trying out different discards)
    //note that it doesn't copy other values which are not relevant
    protected Hand(final Rank roundOf, final CardList cards, final Card discard) {
        this(roundOf);
        this.clear();
        this.addAll(cards);
        this.discard = discard;
    }

    //deal and return hand
    void deal() {
        singles = new Meld(Deck.getInstance().drawFromDrawPile(roundOf.getRankValue())); //only for human really
        Collections.sort(singles, Card.cardComparatorRankFirstDesc);
        this.clear();
        this.addAll(singles);
    }

    boolean checkSize() {
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
        Meld newMeld = new Meld(Meld.MeldComplete.SINGLES);
        addToMeld(card, newMeld);
        melds.add(newMeld);
        return true;
    }

    //Use iterators so we can remove current value
    // When we add/delete a card, it could be showing in multiple partial melds so those need to be adjusted
    private boolean syncCardsAndMelds() {
        //check that all hand.cards are in melds or singles (add to singles if not)
        for (Card card : this) {
            boolean foundCard = singles.contains(card) || Meld.contains(melds, card) || Meld.contains(partialMelds, card);
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
    // TODO:A should move this into ComputerPlayer since it is only called there - but would it extend Hand there?
    protected int meldAndEvaluate(final Meld.MeldMethod method, HandComparator handComparator, final boolean isFinalTurn) throws InterruptedException {
        if (method == Meld.MeldMethod.PERMUTATIONS) meldBestUsingPermutations(handComparator, isFinalTurn);
        else meldUsingHeuristics(handComparator, isFinalTurn);
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
    private boolean meldUsingHeuristics(HandComparator handComparator, final boolean isFinalTurn) {
        //Log.d(FiveKings.APP_TAG,"Entering meldUsingHeuristics");
        final int numCards = this.size();
        final Rank wildCardRank = this.roundOf;
        //list of potential melds (pairs and broken sequences) - could be many of these
        //because a card can be in the list multiple times
        ArrayList<Meld> fullRankMelds = new ArrayList<>(numCards);
        ArrayList<Meld> fullSequences = new ArrayList<>(numCards);
        ArrayList<Meld> partialRankMelds = new ArrayList<>(numCards);
        ArrayList<Meld> partialSequences = new ArrayList<>(numCards);

        //separate into wildcards and non-wildcards
        //Log.d(FiveKings.APP_TAG,"---Separate out wildcards");
        CardList wildCards = new CardList(numCards);
        CardList nonWildCards = new CardList(numCards);
        Meld.sortAndSeparateWildCards(wildCardRank, this, nonWildCards, wildCards);

        //Rank Matches - loop from highest to lowest rank
        //Log.d(FiveKings.APP_TAG,"---find matches in each rank");
        CardList cardsSortedByRankDesc = new CardList(nonWildCards);
        Collections.sort(cardsSortedByRankDesc, Card.cardComparatorRankFirstDesc); //e.g. (K*,KH,QD,JC...)
        Meld rankMatch = new Meld(Meld.MeldComplete.SINGLES);
        rankMatch.clear();
        //will meld larger cards first because of the comparator above
        for (Card card : cardsSortedByRankDesc) {
            //if the card is already in a full meld, then skip it
            // don't' have to decomposeAndCheck wildcards because we've removed them
            if (!Meld.contains(fullRankMelds, card)) {
                if (rankMatch.isEmpty()) rankMatch.add(card);
                else if (card.isSameRank(rankMatch.get(rankMatch.size() - 1))) {
                    rankMatch.setMeldType(Meld.MeldType.RANK);
                    rankMatch.add(card);
                } else {
                    //not same rank; record and restart the sequence
                    addWildcardsAndRecord(handComparator, fullRankMelds, partialRankMelds, rankMatch, wildCards, isFinalTurn);
                    rankMatch.clear();
                    rankMatch.add(card);
                }//end-if same rank
            }
        }
        addWildcardsAndRecord(handComparator, fullRankMelds, partialRankMelds, rankMatch, wildCards, isFinalTurn);

        //Sequences - for now full sequences (3*-4*-5*) or broken pairs (e.g. 3*-5*) in partialSequences list
        //Log.d(FiveKings.APP_TAG,"---find matches in each sequence");
        CardList cardsSortedBySuit = new CardList(nonWildCards);
        Collections.sort(cardsSortedBySuit, Card.cardComparatorSuitFirst); //e.g. 3S,5S,JS,6H,8H...
        Meld sequenceMatch = new Meld(Meld.MeldComplete.SINGLES);
        for (Suit suit : Suit.values()) {
            sequenceMatch.clear();
            for (Card card : cardsSortedBySuit) {
                //if the card is already in a full meld or sequence, then skip it
                if (card.isSameSuit(suit) && !Meld.contains(fullRankMelds, card) && !Meld.contains(fullSequences, card)) {
                    if (sequenceMatch.isEmpty()) sequenceMatch.add(card);
                    else if (1 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size() - 1))) {
                        sequenceMatch.setMeldType(Meld.MeldType.SEQUENCE);
                        sequenceMatch.add(card);
                    }
                    //broken sequence; record but put into partial sequences AND into next sequence (unless we used aa wildcard to make it full)
                    else if ((1 == sequenceMatch.size()) && (2 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size() - 1)))) {
                        sequenceMatch.setMeldType(Meld.MeldType.SEQUENCE);
                        sequenceMatch.add(card);
                        boolean madePartialIntoFull = addWildcardsAndRecord(handComparator, fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
                        sequenceMatch.clear();
                        if (!madePartialIntoFull) sequenceMatch.add(card);
                    } else {
                        //not adjacent; record and restart the sequence
                        addWildcardsAndRecord(handComparator, fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
                        sequenceMatch.clear();
                        sequenceMatch.add(card);
                    }
                }//end-if same suit
            }
            addWildcardsAndRecord(handComparator, fullSequences, partialSequences, sequenceMatch, wildCards, isFinalTurn);
        }//end for Suits

        //Go back and check if partial rank melds overlap with full sequences; if so, drop the partial rank meld
        for (Iterator<Meld> iterator = partialRankMelds.iterator(); iterator.hasNext(); ) {
            CardList rankMeld = iterator.next();
            if (Meld.contains(fullSequences, rankMeld)) iterator.remove();
        }

        //If we still have 2+ wildcards, meld singles into a full meld - but don't create partials with a wildcard
        Meld meldOfSingles = new Meld(Meld.MeldComplete.SINGLES);
        for (Card card : cardsSortedByRankDesc) {
            if (wildCards.size() <= 1) break;
            if (!Meld.contains(fullRankMelds, card) && !Meld.contains(fullSequences, card)) {
                meldOfSingles.clear();
                meldOfSingles.add(card);
                meldOfSingles.add(wildCards.get(0));
                wildCards.remove(0);
                meldOfSingles.setMeldType(Meld.MeldType.EITHER); //because it's X-Wild-Wild
                addWildcardsAndRecord(handComparator, fullRankMelds, partialRankMelds, meldOfSingles, wildCards, isFinalTurn);
            }
        }


        //if there are remaining wildcards, keep adding them to existing melds until we run out
        while (!wildCards.isEmpty() && (!fullRankMelds.isEmpty() || !fullSequences.isEmpty())) {
            //Log.d(FiveKings.APP_TAG, "---extend existing melds/sequence");
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
        Meld.setCheckedAndValid(melds);

        // For final scoring (last licks) we don't show partial melds (they all go into singles)
        // For intermediate scoring they count reduced and we don't show in singles
        if (isFinalTurn) {
            //partialMelds should always already be clear (because checked earlier)
            if (!partialRankMelds.isEmpty() || !partialSequences.isEmpty())
                Log.e(FiveKings.APP_TAG, "meldUsingHeuristics: partialMelds not empty in final scoring");
            partialMelds.clear();
        } else {
            partialMelds = new ArrayList<>(partialRankMelds);
            partialMelds.addAll(partialSequences);
        }
        //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
        // since the wildcards are no longer being melded into partials need to add them back here
        singles = new Meld(nonWildCards);
        for (Card card : nonWildCards) {
            if (Meld.contains(melds, card)) singles.remove(card);
            if (!isFinalTurn && Meld.contains(partialMelds, card)) singles.remove(card);
        }
        singles.addAll(wildCards);

        //don't need to find discard (we are looping over possible discards if this is a Computer turn)
        //and scoring is now done in the caller
        //Log.d(FiveKings.APP_TAG,"---exiting meldUsingHeuristics");
        return true;
    }//end int meldUsingHeuristics

    //save test if full; see if you can pad it to a full with wildcards and keep/discard if not
    private boolean addWildcardsAndRecord(HandComparator handComparator, final ArrayList<Meld> fulls, final ArrayList<Meld> partials, final Meld test, final CardList wildCards, final boolean isFinalTurn) {
        boolean madePartialIntoFull = false;
        if (test.size() >= 3) {
            test.setMeldComplete(Meld.MeldComplete.FULL);
            fulls.add((Meld) test.clone());
        } else if (2 == test.size()) {
            if (!wildCards.isEmpty()) {
                test.add(wildCards.get(0));
                wildCards.remove(0);
                //These should all be short melds - call decompose to order them correctly
                //also set meldComplete and isCheckedAndValid
                try {
                    if (test.decomposeAndCheck(handComparator,this.roundOf, isFinalTurn, new MeldedCardList(this.roundOf)) != 0) {
                        Log.e(FiveKings.APP_TAG, String.format("addWildCardsAndRecord: full meld %s not evaluating to 0", test.getString()));
                        //throw new RuntimeException(String.format("addWildCardsAndRecord: full meld %s not evaluating to 0", test.getString()));
                    }
                } catch (InterruptedException e) {
                    //continue
                }
                fulls.add((Meld) test.clone());
                madePartialIntoFull = true;
            } else if (!isFinalTurn) { //only create partial melds on intermediate turns
                test.setMeldComplete(Meld.MeldComplete.PARTIAL);
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
    private void meldBestUsingPermutations(HandComparator handComparator, final boolean isFinalTurn) throws InterruptedException {
        Meld cardsCopy = new Meld(this);
        MeldedCardList bestMeldedCardList = new MeldedCardList(this.roundOf);
        //have to do this two step process (passing best.. and cardsCopy separately)
        //because decomposeAndCheck is used in multiple ways
        cardsCopy.decomposeAndCheck(handComparator,this.roundOf, isFinalTurn, bestMeldedCardList);
        this.copyJustMelds(bestMeldedCardList);
    }//end meldBestUsingPermutations

    //Helper for HumanPlayer.checkMeldsAndEvaluate (throws away decomposition, but calculates valuation)
    //TODO:B May be able to use a version of this in meldUsing Heuristics (as part of ComputerPlayer)
    final int checkMeldsAndEvaluate(HandComparator handComparator, final boolean isFinalTurn) {
        final MeldedCardList decomposedMelds = new MeldedCardList(this.roundOf);
        for (Meld meld : this.melds) {
            try {
                meld.decomposeAndCheck(handComparator,this.roundOf , isFinalTurn, decomposedMelds);
            } catch (InterruptedException e) {
                //Ignore for humans
            }
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

    /* PARCELABLE interface for Hand */
    protected Hand(Parcel parcel) {
        super(parcel);
        if (parcel.readByte() == 0x00) discard=null;
        else {
            discard = (Card) parcel.readValue(Card.class.getClassLoader());
            //if discard was in the Hand, then reference it back there
            final int index = parcel.readInt();
            if (index >= 0) discard = this.get(index);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    //write both the card and the index to the parcel
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        if (discard == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeValue(discard);
            parcel.writeInt(this.indexOf(discard));
        }
    }

    public static final Creator<Hand> CREATOR = new Creator<Hand>() {
        @Override
        public Hand createFromParcel(Parcel in) {
            return new Hand(in);
        }

        @Override
        public Hand[] newArray(int size) {
            return new Hand[size];
        }
    };

}//end Class Hand
