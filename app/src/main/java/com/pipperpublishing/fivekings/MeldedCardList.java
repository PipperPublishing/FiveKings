package com.pipperpublishing.fivekings;

/**
 * Re-created by Jeffrey on 3/16/2015.
 * 3/16/2015    Separated MeldedCardList out from Player (there is a Hand  sub-class which is an inner class)
 */

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/** MeldedCardList (was Hand) - general manipulation of melds, partial Melds etc.
 * Created by Jeffrey on 1/22/2015.
 * Initial version handling of permutations (each call): nPn-1=n!, where n=round+1
 * 1/28/2015 v2 Implement Heap's algorithm inline- see http://www.cs.princeton.edu/~rs/talks/perms.pdf
 * 1/29/2015 Implement Johnson-Trotter algorithm which can act as an iterator (so storage is less of a problem)
 * 1/30/2015 move to a different evaluation method which counts pairs and possible sequences
 * 2/1/2015 Make melds, unMelded ArrayList<CardList> so we can separate melds visually
 * 2/5/2015 Eliminate using wrapper, switch to unified hand evaluation
 * 2/15/2015 Return Card array of melds and unmelded
 * 2/15/2015 Return singles sorted for easier viewing
 * 2/15/2015 Identify partial sequences (JS-KS) in Permutations approach
 * 2/17/2015 Replace isSameRank with RankDifference
 * 2/17/2015 Finding discard when there are no singles/partial melds; remove from a full meld but leave full if >=3
 * 2/24/2015    Move checkSize from Player to Hand
 * 2/24/2015    Move Hand to be inner class of Player
 *              Remove addedCard from melding (is handled in outer loop)
 * 2/27/2015    calculateMeldsValue now calculates just valuation or finalScore and returns the appropriate one
 * 3/15/2015    Add discard as member variable
 * 3/16/2015    Add calculateValueAndScore is called on a Hand, and stores both types of valuation
 * 3/16/2015    Base class Hand - moved player specific stuff into PlayerHand
 * 3/17/2015    Introduce HandComparator interface in Meld constructor - use it when comparing hands
 * 3/19/2015    Don't store intermediateValue and finalScore but recalculate them when needed
 * 3/20/2015    Rename MeldedCardList to reflect it's not a Hand
 * 3/20/2015    Add RANK/SEQUENCE type and FULL/PARTIAL completeness to Heuristics
 * 3/21/2015    In addDecomposition, don't add partials if it's FinalRound (add to singles)
 * 11/8/2015    Implement parcelable
 * 11/9/2015    Removed HandComparator as a member variable and pass dynamically
 *              Add Parcelable support in Meld
 *              melds, partialMelds, and singles need to reference the cards in the CardList (otherwise scoring and melding won't work)

                Moved addToMeld here (used to be Meld.addTo)
 TODO:B Account for the overlap among partialMelds and partialSequences; is it ok to have ( 10C-10D and 10C-JC)

 */
public class MeldedCardList extends CardList{
    protected final Rank roundOf; //how many cards you should have and what is wild
    //all your cards are in the base class
    // and the cards organized into melds, partial melds and remaining singles
    protected ArrayList<Meld> melds;
    protected ArrayList<Meld> partialMelds;
    protected Meld singles;

    protected MeldedCardList(final Rank roundOf) {
        super(roundOf.getRankValue());
        this.roundOf = roundOf;
        this.melds = new ArrayList<>();
        this.partialMelds = new ArrayList<>();
        this.singles = new Meld(Meld.MeldComplete.SINGLES);
    }

    //replace cards only
    protected MeldedCardList(final Rank roundOf, CardList cards) {
        this(roundOf);
        if (cards != null) {
            this.clear();
            this.addAll(cards);
        }
    }

    void copyFrom(MeldedCardList fromMeldedCardList) {
        if (fromMeldedCardList != null) {
            this.clear();
            this.addAll(fromMeldedCardList);
            this.melds = fromMeldedCardList.melds;
            this.partialMelds = fromMeldedCardList.partialMelds;
            this.singles = fromMeldedCardList.singles;

        }
    }

    void copyJustMelds(MeldedCardList fromMeldedCardList) {
        if (fromMeldedCardList != null) {
            this.melds = fromMeldedCardList.melds;
            this.partialMelds = fromMeldedCardList.partialMelds;
            this.singles = fromMeldedCardList.singles;
        }
    }


    /* Scoring related methods - delegates for Players */
    int numPartialRankMelds() {
        int numPartialRankMelds = 0;
        for (Meld meld:this.partialMelds)
            if (meld.getMeldType() == Meld.MeldType.RANK) numPartialRankMelds++;
        return numPartialRankMelds;
    }

    int numFullRankMelds() {
        int numFullRankMelds = 0;
        for (Meld meld:this.melds)
            if (meld.getMeldType() == Meld.MeldType.RANK) numFullRankMelds++;
        return numFullRankMelds;

    }


    int numMelds() {return this.melds.size();}

    int numPartialMelds() {return this.partialMelds.size();}

    int numMeldedCards() {
        int numMelded=0;
        for (Meld meld : this.melds) numMelded += meld.size();
        return numMelded;
    }


    protected int calculateValueAndScore(boolean isFinalTurn) {
        float intermediateValue = 0.0f;
        int finalScore =0;
        for (Card card : this) {
            float cardValue = Meld.getCardScore(card, this.roundOf, false);
            float cardScore = Meld.getCardScore(card, this.roundOf, true);
            //if Final, then singles contains everything in partialMelds at full value
            //otherwise reduce it by 1/2 if partially melded
            if (Meld.contains(partialMelds,card) || singles.contains(card)) {
                finalScore += cardScore;
                if (Meld.contains(partialMelds, card)) intermediateValue += 0.5 * cardValue;
                else intermediateValue += cardValue;
            }
        }
        //"Melds" can actually be attempts by human so need to be added (have previously been evaluated by Meld.decomposeAndCheck)
        if (melds != null) {
            for (Meld meld : melds) {
                finalScore += meld.getValuation();
                intermediateValue += meld.getValuation();
            }
        }
        return isFinalTurn ? finalScore : (int)intermediateValue;
    }

    //add to a new meld just created, or to an existing meld and remove from other melds, partial melds, or singles
    // allows for dragging and dropping back onto itself
    protected void addToMeld (final Card card, final Meld meld) {
        //remove from unMelded, singles, or other melds
        for (CardList cl : this.melds) cl.remove(card);
        for (CardList cl : this.partialMelds) cl.remove(card);
        this.singles.remove(card);
        if (!meld.contains(card)) meld.add(card); //may remove and then re-add
        meld.setNeedToCheck(true);
    }


    //Helper for Meld.decomposeAndCheck - meldType is already set in decomposeAndCheck
    void addDecomposition(boolean isFinalTurn, final Meld testMeld) {
        if (testMeld.size() >= 3) {
            testMeld.setMeldComplete(Meld.MeldComplete.FULL);
            this.melds.add((Meld) testMeld.clone());
        }
        else if (!isFinalTurn && (2 == testMeld.size())) {
            testMeld.setMeldComplete(Meld.MeldComplete.PARTIAL);
            this.partialMelds.add((Meld) testMeld.clone());
        }
        else {
            //adding to singles if it's just one card left over, or partials in FinalRound
            for (Card card: testMeld) if (!this.singles.contains(card)) singles.add(card);
        }
    }



    //Hand GETTERS
    protected ArrayList<Meld> getMelded() {
        return melds;
    }

    protected ArrayList<Meld> getUnMelded() {
        //unroll the partialMelds, eliminating duplicates
        ArrayList<Meld> combined = new ArrayList<>();
        for (Meld cl : partialMelds) {
                /*for (Card c : cl) {
                    if (combined.isEmpty()) combined.add(new CardList());
                    if (!combined.get(0).contains(c)) combined.get(0).add(c);
                }*/
            combined.add(cl);
        }
        return combined;
    }

    protected CardList getSingles() {return singles;}

    protected Rank getRoundOf() {
        return roundOf;
    }


    /* PARCELABLE implementation for MeldedCardList read/write (override CardList implementation) */
    protected MeldedCardList(Parcel parcel) {
        super(parcel);
        roundOf = (Rank) parcel.readValue(Rank.class.getClassLoader());
        if (parcel.readByte() == 0x01) {
            melds = new ArrayList<Meld>();
            parcel.readList(melds, Meld.class.getClassLoader());
            readListAsRefs(parcel, melds);
        } else {
            melds = null;
        }
        if (parcel.readByte() == 0x01) {
            partialMelds = new ArrayList<Meld>();
            parcel.readList(partialMelds, Meld.class.getClassLoader());
            readListAsRefs(parcel, partialMelds);
        } else {
            partialMelds = null;
        }
        if (parcel.readByte() == 0x01) {
            //should probably implement this properly in Meld (seems to work for above ArrayList<Meld>
            singles = new Meld(Meld.MeldComplete.SINGLES);
            parcel.readList(singles, Card.class.getClassLoader());
            for (int cardIndex = 0; cardIndex < singles.size(); cardIndex++) {
                int index = parcel.readInt();
                singles.set(cardIndex, this.get(index)); //reference the card in the base list
            }
        } else {
            singles = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeValue(roundOf);
        if (melds == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeList(melds);
            writeListAsRefs(parcel, melds);
        }
        if (partialMelds == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeList(partialMelds);
            writeListAsRefs(parcel, partialMelds);
        }
        if (singles == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeList(singles);
            for (Card card : singles) {
                parcel.writeInt(this.indexOf(card));
            }
        }
    }
    //we use the standard writeList so that the Meld member variables are correctly recorded
    //then we also stored the list as references to the CardList
    private void writeListAsRefs(Parcel parcel, ArrayList<Meld> melds) {
        for (Meld meld : melds) {
            for (Card card : meld ) {
                parcel.writeInt(this.indexOf(card));
            }
        }
    }
    private void readListAsRefs(Parcel parcel, ArrayList<Meld> melds) {
        for (Meld meld : melds) {
            for (int cardIndex = 0; cardIndex < meld.size(); cardIndex++) {
                int index = parcel.readInt();
                meld.set(cardIndex, this.get(index)); //reference the card in the base list
            }
        }
    }


    public static final Parcelable.Creator<MeldedCardList> CREATOR = new Parcelable.Creator<MeldedCardList>() {
        @Override
        public MeldedCardList createFromParcel(Parcel parcel) {
            return new MeldedCardList(parcel);
        }

        @Override
        public MeldedCardList[] newArray(int size) {
            return new MeldedCardList[size];
        }
    };

}//end MeldedCardList