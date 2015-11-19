/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Jeffrey on 11/9/2015.
* 3/17/2015    Removed numCards argument; just initialize capacity as MAX_CARDS
 3/19/2015   Replace isSequenceMeld and isRankMeld with meldType (SEQUENCE, RANK, or null)
 3/19/2015    decomposeAndCheck: Simplify isRankMeld logic. Categorize melds by isSequenceMeld etc flag
 * 11/9/2015    Moved out of MeldedCardList because need CREATOR for parceling
 */

public class Meld extends CardList {
    enum MeldMethod {PERMUTATIONS, HEURISTICS}
    enum MeldComplete {FULL, PARTIAL, SINGLES}
    //a meld of Q-*-* is EITHER type
    enum MeldType {RANK, SEQUENCE, EITHER}

    private int valuation; //0 if this is a valid meld
    private boolean needToCheck; //a card has been added or removed so we need to recheck
    private boolean isValidFullMeld = false;
    private MeldComplete meldComplete;
    MeldType meldType = null;


    protected Meld(final CardList cl) {
        this(MeldComplete.SINGLES);
        this.addAll(cl);
    }

    Meld(MeldComplete meldComplete) {
        super(Game.MAX_CARDS);
        this.valuation = 0;
        this.isValidFullMeld = false;
        this.needToCheck = true;
        this.meldComplete = meldComplete;
    }

    /*      Find the best arrangement of this meld and break if we get zero
            also sorts the meld rank first (which should help if it's a rank meld)
            If it's not a single meld, will return the breakdown
            it's the caller's responsibility to not call this with too long a "meld" (since permutation time goes up geometrically)
            returns best arrangement and returns partial meld and singles components if it can't be fully melded*/
    protected int decomposeAndCheck(final HandComparator playerHandComparator, final Rank wildCardRank, final boolean isFinalTurn, final MeldedCardList bestMeldedCardList) throws InterruptedException {
        int numCards = this.size();
        //TODO:A This probably could throw an out of memory exception
        Permuter indexes = new Permuter(numCards);
        CardList wildCards = new CardList(numCards);
        CardList nonWildCards = new CardList(numCards);
        //although not explicitly used, this sorts into descending rank followed by wild cards which could speed up permutations?
        sortAndSeparateWildCards(wildCardRank, this, nonWildCards, wildCards);

        int bestValuation = -1;
        int[] cardListIdx;
        //Loop over all valid permutations - break as soon as we confirm a meld
        for (cardListIdx = indexes.getNext(); cardListIdx != null; cardListIdx = indexes.getNext()) {
            //if we've exceeded the time available for permutations, interrupt the calculations (this will get checked very often)
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            //if lastMelded was a non-wild card, then these are just that card's Rank and Suit
            //if it was a wild card, then they are the "substitute" value(s) the wild card plays
            Rank rankMeldRank = null;
            Rank sequenceMeldLastRank = null;
            Suit sequenceMeldSuit = null;
            boolean isSequenceMeld = true;
            boolean isRankMeld = true;
            boolean isBrokenSequence = true;
            Meld testMeld = new Meld(MeldComplete.SINGLES);
            Card testCard = null;
            MeldedCardList permHand = new MeldedCardList(wildCardRank, this);


/*           note *this* loop is over the cards in the permutation to see whether they can be melded
            (and there is no longer a discard here)
            We only look for melds in order (including ascending sequences) because at least one permutation will have that if it exists
*/
            Card lastMeldedCard = this.get(cardListIdx[0]);
            testMeld.clear();
            testMeld.add(lastMeldedCard);
            for (int iCard = 1; iCard < numCards; iCard++) {
                /*
                * if this is a wildcard or lastMeldedCard is a wildcard
                * or cardsCopy[cardListIdx[iCard] melds in rank (any suit) or seq (same suit)
                * But have to deal with several tricky problems:
                * 1. After we get the first real match (not a wildcard) need to record that this is a sequence or rank-meld
                * 2. We have to give wild cards a "substitute" value that they are playing in the meld (and that might be sequence and/or rank meld or unknown)
                */
                lastMeldedCard = testMeld.get(testMeld.size() - 1);
                if (lastMeldedCard.isWildCard(wildCardRank)) {
                    //leave rankMeldRank unchanged (either null or a known Rank)
                    //leave sequenceMeldSuit unchanged and increment to the next rank (if possible)
                    if (sequenceMeldLastRank != null)
                        sequenceMeldLastRank = sequenceMeldLastRank.getNext();
                } else {
                    rankMeldRank = lastMeldedCard.getRank();
                    sequenceMeldSuit = lastMeldedCard.getSuit();
                    sequenceMeldLastRank = lastMeldedCard.getRank();
                }

                testCard = this.get(cardListIdx[iCard]);
                //this convoluted logic is because BOTH isRankMeld and isSequenceMeld could be true in a permutation like Q-Wild-Wild
                boolean testIsMelding = false;
                if (isRankMeld) {
                    //this card melds if (a) it's wild, (b) the previous card was wild, (c) it's the same rank
                    if (testCard.isWildCard(wildCardRank) || (rankMeldRank == null))
                        testIsMelding = true;
                    else if (testCard.isSameRank(rankMeldRank)) {
                        testIsMelding = true;
                        testMeld.meldType = MeldType.RANK;
                        isSequenceMeld = false; //now we know it's a Rank meld and not a Sequence meld
                    } else isRankMeld = false;
                }
                //order of tests is important here (to make sure the card can't be wild or null)
                if (isSequenceMeld) {
                    if (sequenceMeldLastRank == null) testIsMelding = true;
                        //can't be a sequenceMeld if the lastRank is a King (nothing greater)
                    else if (sequenceMeldLastRank.isHighestRank()) isSequenceMeld = false;
                    else if (testCard.isWildCard(wildCardRank)) testIsMelding = true;
                        // can't be a sequenceMeld if this is a 3 and the previous card was wild
                    else if (testCard.getRank().isLowestRank() && lastMeldedCard.isWildCard(wildCardRank))
                        isSequenceMeld = false;
                        //same Suit and next in sequence
                    else if (testCard.isSameSuit(sequenceMeldSuit) && (1 == testCard.getRankDifference(sequenceMeldLastRank))) {
                        testIsMelding = true;
                        testMeld.meldType = MeldType.SEQUENCE;
                        isRankMeld = false; //now we know it's a sequence meld
                    } else isSequenceMeld = false;
                }
                if (isRankMeld && isSequenceMeld) testMeld.meldType = MeldType.EITHER;

                //Broken Sequence
                //don't use else-if, because above block sets isSequenceMeld false
                //testCard broke the sequence, but may still be a brokenSequence (e.g. 10C-QC)
                if ((1 == testMeld.size()) && !isSequenceMeld && !isRankMeld && isBrokenSequence && (sequenceMeldLastRank != null)) {
                    if (testCard.isSameSuit(sequenceMeldSuit) && (2 == testCard.getRankDifference(sequenceMeldLastRank))) {
                        testIsMelding = true;
                        testMeld.meldType = MeldType.SEQUENCE;
                    }
                    isBrokenSequence = false; //don't want to add more cards
                }

                if (testIsMelding) testMeld.add(testCard);
                else {// testCard doesn't fit the testMeld - now check if testMeld has fewer than 3 cards then we can move it to unMelded
                    permHand.addDecomposition(isFinalTurn, testMeld);
                    testMeld.clear();
                    testMeld.add(testCard);
                    isRankMeld = true;
                    isSequenceMeld = true;
                    //Don't do pruning at this point any more to allow scoring algorithm to be more complicated
                }
            }//for iCard=1..numCards (testing permutation)
            //anything left over in testMeld needs to be added to unMelded or Melded appropriately
            permHand.addDecomposition(isFinalTurn, testMeld);

            //reset the bestPermutation if this is a lower score
            //use the player's specific criteria (EasyComputerPlayer, HardComputerPlayer, or ExpertComputerPlayer)
            if ((bestValuation == -1) || playerHandComparator.isFirstBetterThanSecond(permHand, bestMeldedCardList, isFinalTurn)) {
                bestValuation = permHand.calculateValueAndScore(isFinalTurn);
                //use Hand.copyFrom because bestMeldedCardList is a (final) parameter
                bestMeldedCardList.copyFrom(permHand);
            }
            if (bestValuation == 0) break;
        }//end for cardListIdx in all permutations of cards

        //Reorder the meld according to the best permutation - if we got a meld
        if (cardListIdx != null) {
            CardList permedCards = new CardList(numCards);
            for (int iCard = 0; iCard < numCards; iCard++) {
                permedCards.add(this.get(cardListIdx[iCard]));
            }
            //and now copy back into this
            this.clear();
            this.addAll(permedCards);
        }

        //this looks recursive - we set for both the this meld and for the ones we have broken it down into (if any)
        //all the bestMeldedCardList melds are known to be valid
        this.setCheckedAndValid(bestMeldedCardList, bestValuation);

        return bestValuation;
    }//end int decomposeAndCheck

    private void setCheckedAndValid() {
        this.isValidFullMeld = true;
        this.needToCheck = false;
        this.valuation = 0;
    }

    private void setCheckedAndValid(final MeldedCardList decomposed, final int valuation) {
        for (Meld meld : decomposed.melds) {
            meld.setCheckedAndValid();
            meld.meldComplete = MeldComplete.FULL;
        }
        this.isValidFullMeld = decomposed.partialMelds.isEmpty() && decomposed.singles.isEmpty();
        this.needToCheck = false;
        this.valuation = valuation;
    }


    Meld.MeldType getMeldType() {
        return meldType;
    }

    int getValuation() {
        return valuation;
    }

    void setMeldComplete(final Meld.MeldComplete meldComplete) {
        this.meldComplete = meldComplete;
    }

    void setMeldType(final Meld.MeldType meldType) {
        this.meldType = meldType;
    }

    public void setNeedToCheck(boolean needToCheck) {
        this.needToCheck = needToCheck;
    }

    /* Parcelable implementation for Meld */
    protected Meld(Parcel parcel) {
        super(parcel); //get list from CardList
        valuation = parcel.readInt();
        needToCheck = parcel.readByte() != 0x00;
        isValidFullMeld = parcel.readByte() != 0x00;
        meldComplete = Meld.MeldComplete.valueOf(parcel.readString());
        if (parcel.readByte() != 0x00) {
            meldType = Meld.MeldType.valueOf(parcel.readString());
        } else {
            meldType = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeInt(valuation);
        parcel.writeByte((byte) (needToCheck ? 0x01 : 0x00));
        parcel.writeByte((byte) (isValidFullMeld ? 0x01 : 0x00));
        parcel.writeString(meldComplete.toString());
        if (meldType == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeString(meldType.toString());
        }
    }

    public static final Parcelable.Creator<Meld> CREATOR = new Parcelable.Creator<Meld>() {
        @Override
        public Meld createFromParcel(Parcel parcel) {
            return new Meld(parcel);
        }

        @Override
        public Meld[] newArray(int size) {
            return new Meld[size];
        }
    };

    /* STATIC METHODS associated with Meld  */

    //Helper for meldUsingHeuristics
    //TODO:B Would like to merge with other setCheckedAndValid - when we move these to right context
    static void setCheckedAndValid (final ArrayList<Meld> melds) {
        for (Meld meld : melds) {
            meld.setCheckedAndValid();
            meld.meldComplete = MeldComplete.FULL;
        }
    }

    //Helper for FiveKings
    static public boolean isValidMeld(final CardList cl) {
        return (cl instanceof Meld) && ((Meld)cl).isValidFullMeld;
    }

    static public String getString(final ArrayList<Meld> meldsOrUnMelds) {
        StringBuilder meldedString = new StringBuilder();
        if (null != meldsOrUnMelds) {
            for (CardList melds : meldsOrUnMelds) {
                meldedString.append(melds.getString());
            }
        }
        return meldedString.toString();
    }


    static final int INTERMEDIATE_WILD_CARD_VALUE=1;
    //by default, wildcards have value INTERMEDIATE_WILDCARD_VALUE so that there is incentive to meld them
    //but not incentive to discard them
    //In final scoring they have full value (20 for rank wildcards, and 50 for Jokers)
    static int getCardScore(Card card, Rank wildCardRank, boolean isFinalTurn) {
        final int cardScore;
        //if final round, then rank wildCard=20, Joker=50, otherwise Rank value
        if (isFinalTurn) cardScore = card.getCardValue(wildCardRank);
            //if intermediate, then any wildcard=1 (including Jokers), otherwise Rank value
        else cardScore = (card.isWildCard(wildCardRank) ? INTERMEDIATE_WILD_CARD_VALUE : card.getCardValue(wildCardRank));
        return cardScore;
    }

    static void sortAndSeparateWildCards(final Rank wildCardRank,final CardList cards, final CardList nonWildCards, final CardList wildCards) {
        //cards contains the list to be sorted
        if ((nonWildCards == null) || (wildCards == null) || (cards.isEmpty())) return;
        nonWildCards.clear();
        nonWildCards.addAll(cards);
        wildCards.clear();
        for (Card card : cards) {
            if (card.isWildCard(wildCardRank)) {
                wildCards.add(card);
                nonWildCards.remove(card);
            }
        }
        Collections.sort(nonWildCards, Card.cardComparatorRankFirstDesc);
        cards.clear();
        cards.addAll(nonWildCards);
        cards.addAll(wildCards);
    }

    //delegate method to look for card in ArrayList<CardList> (otherwise we'd have to override ArrayList)
    static boolean contains (final ArrayList<Meld> melds, Card card) {
        if (null == melds) return false;
        for (Meld meld : melds)
            if (meld.contains(card)) return true;
        return false;
    }
    static boolean contains (final ArrayList<Meld> containingMelds, CardList cardList) {
        if (null == containingMelds) return false;
        for (Card card:cardList) if (contains(containingMelds, card)) return true;
        return false;
    }


}//end  class Meld
