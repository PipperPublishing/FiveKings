package com.example.jeffrey.fivekings;

/**
 * Re-created by Jeffrey on 3/16/2015.
 * 3/16/2015    Separated MeldedCardList out from Player (there is a Hand  sub-class which is an inner class)
 */

import java.util.ArrayList;
import java.util.Collections;

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

 TODO:B Account for the overlap among partialMelds and partialSequences; is it ok to have ( 10C-10D and 10C-JC)
 * TODO:A Can have the melds score themselves based on what they are

 */
class MeldedCardList extends CardList{
    protected final Rank roundOf; //how many cards you should have and what is wild
    //all your cards are in the base class
    // and the cards organized into melds, partial melds and remaining singles
    protected ArrayList<Meld> melds;
    protected ArrayList<Meld> partialMelds;
    protected Meld singles;

    protected HandComparator playerHandComparator;


    protected MeldedCardList(final Rank roundOf, final HandComparator playerHandComparator) {
        super(roundOf.getRankValue());
        this.roundOf = roundOf;
        this.melds = new ArrayList<>();
        this.partialMelds = new ArrayList<>();
        this.singles = new Meld(playerHandComparator, MeldComplete.SINGLES);

        this.playerHandComparator = playerHandComparator;
    }

    //replace cards only
    protected MeldedCardList(final Rank roundOf, final HandComparator playerHandComparator, CardList cards) {
        this(roundOf, playerHandComparator);
        if (cards != null) {
            this.clear();
            this.addAll(cards);
        }
    }

    private void copyFrom(MeldedCardList fromMeldedCardList) {
        if (fromMeldedCardList != null) {
            this.clear();
            this.addAll(fromMeldedCardList);
            this.melds = fromMeldedCardList.melds;
            this.partialMelds = fromMeldedCardList.partialMelds;
            this.singles = fromMeldedCardList.singles;

            this.playerHandComparator = fromMeldedCardList.playerHandComparator;
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
            if (meld.getMeldType() == MeldType.RANK) numPartialRankMelds++;
        return numPartialRankMelds;
    }

    int numFullRankMelds() {
        int numFullRankMelds = 0;
        for (Meld meld:this.melds)
            if (meld.getMeldType() == MeldType.RANK) numFullRankMelds++;
        return numFullRankMelds;

    }


    int numMelds() {return this.melds.size();}

    int numPartialMelds() {return this.partialMelds.size();}

    int numMeldedCards() {
        int numMelded=0;
        for (MeldedCardList.Meld meld : this.melds) numMelded += meld.size();
        return numMelded;
    }


    protected int calculateValueAndScore(boolean isFinalRound) {
        float intermediateValue = 0.0f;
        int finalScore =0;
        for (Card card : this) {
            float cardValue = getCardScore(card, this.roundOf, false);
            float cardScore = getCardScore(card, this.roundOf, true);
            //if Final, then singles contains everything in partialMelds at full value
            //otherwise reduce it by 1/2 if partially melded
            if (contains(partialMelds,card) || singles.contains(card)) {
                finalScore += cardScore;
                if (contains(partialMelds, card)) intermediateValue += 0.5 * cardValue;
                else intermediateValue += cardValue;
            }
        }
        //"Melds" can actually be attempts by human so need to be added (have previously been evaluated by Meld.decomposeAndCheck)
        if (melds != null) {
            for (Meld meld : melds) {
                finalScore += meld.valuation;
                intermediateValue += meld.valuation;
            }
        }
        return isFinalRound ? finalScore : (int)intermediateValue;
    }

    //Helper for Meld.decomposeAndCheck - meldType is already set in decomposeAndCheck
    void addDecomposition(boolean isFinalRound, final Meld testMeld) {
        if (testMeld.size() >= 3) {
            testMeld.setMeldComplete(MeldComplete.FULL);
            this.melds.add((Meld) testMeld.clone());
        }
        else if (!isFinalRound && (2 == testMeld.size())) {
            testMeld.setMeldComplete(MeldComplete.PARTIAL);
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
            combined.add(cl);//TODO:A: Temporary hack to make sure we're pointing back to real melds
        }
        return combined;
    }

    protected CardList getSingles() {return singles;}

    @Deprecated
        //now being done in syncCardsAndMelds
    CardList getSortedSingles() {
        CardList sortedCards = new CardList(singles);
        Collections.sort(sortedCards, Card.cardComparatorRankFirstDesc);
        return sortedCards;
    }



    /* 3/17/2015    Removed numCards argument; just initialize capacity as MAX_CARDS
        3/19/2015   Replace isSequenceMeld and isRankMeld with meldType (SEQUENCE, RANK, or null)
     3/19/2015    decomposeAndCheck: Simplify isRankMeld logic. Categorize melds by isSequenceMeld etc flag
     */
    static enum MeldMethod {PERMUTATIONS, HEURISTICS}
    static enum MeldComplete {FULL, PARTIAL, SINGLES}
    //a meld of Q-*-* is EITHER type
    static enum MeldType {RANK, SEQUENCE, EITHER}

    /*------------------*/
    /* INNER CLASS Meld */
    /*------------------*/
    protected class Meld extends CardList {

        private int valuation; //0 if this is a valid meld
        private boolean needToCheck; //a card has been added or removed so we need to recheck
        private boolean isValidFullMeld =false;
        private MeldComplete meldComplete;
        private MeldType meldType=null;

        private HandComparator playerHandComparator;


        protected Meld(final HandComparator playerHandComparator, final CardList cl) {
            this(playerHandComparator, MeldComplete.SINGLES);
            this.addAll(cl);
        }

        Meld(final HandComparator playerHandComparator, MeldComplete meldComplete) {
            super(Game.MAX_CARDS);
            this.valuation = 0;
            this.isValidFullMeld = false;
            this.needToCheck = true;
            this.meldComplete = meldComplete;
            this.playerHandComparator = playerHandComparator;
        }

/*      Find the best arrangement of this meld and break if we get zero
        also sorts the meld rank first (which should help if it's a rank meld)
        If it's not a single meld, will return the breakdown
        it's the caller's responsibility to not call this with too long a "meld" (since permutation time goes up geometrically)
        returns best arrangement and returns partial meld and singles components if it can't be fully melded*/
        protected int decomposeAndCheck(final boolean isFinalRound, final MeldedCardList bestMeldedCardList) {
            int numCards = this.size();
            Rank wildCardRank = MeldedCardList.this.roundOf;
            Permuter indexes = new Permuter(numCards);
            CardList wildCards = new CardList(numCards);
            CardList nonWildCards = new CardList(numCards);
            //although not explicitly used, this sorts into descending rank followed by wild cards which could speed up permutations?
            sortAndSeparateWildCards(wildCardRank, this, nonWildCards, wildCards);

            int bestValuation=-1;
            int[] cardListIdx;
            //Loop over all valid permutations - break as soon as we confirm a meld
            for (cardListIdx = indexes.getNext(); cardListIdx != null; cardListIdx = indexes.getNext()) {
                //if lastMelded was a non-wild card, then these are just that card's Rank and Suit
                //if it was a wild card, then they are the "substitute" value(s) the wild card plays
                Rank rankMeldRank = null;
                Rank sequenceMeldLastRank = null;
                Suit sequenceMeldSuit = null;
                boolean isSequenceMeld = true;
                boolean isRankMeld = true;
                boolean isBrokenSequence = true;
                Meld testMeld = new Meld(this.playerHandComparator, MeldComplete.SINGLES);
                Card testCard = null;
                MeldedCardList permHand = new MeldedCardList(wildCardRank,playerHandComparator, this);


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
                        if (testCard.isWildCard(wildCardRank) || (rankMeldRank == null)) testIsMelding = true;
                        else if (testCard.isSameRank(rankMeldRank)) {
                            testIsMelding = true;
                            testMeld.meldType = MeldType.RANK;
                            isSequenceMeld = false; //now we know it's a Rank meld and not a Sequence meld
                        }
                        else isRankMeld = false;
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
                        permHand.addDecomposition(isFinalRound, testMeld);
                        testMeld.clear();
                        testMeld.add(testCard);
                        isRankMeld = true;
                        isSequenceMeld = true;
                        //Don't do pruning at this point any more to allow scoring algorithm to be more complicated
                    }
                }//for iCard=1..numCards (testing permutation)
                //anything left over in testMeld needs to be added to unMelded or Melded appropriately
                permHand.addDecomposition(isFinalRound, testMeld);

                //reset the bestPermutation if this is a lower score
                //use the player's specific criteria (ComputerPlayer or SmarterComputerPlayer)
                if ((bestValuation == -1) || playerHandComparator.isFirstBetterThanSecond(permHand, bestMeldedCardList, isFinalRound)) {
                    bestValuation = permHand.calculateValueAndScore(isFinalRound);
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

        @Deprecated
        protected int decomposeAndCheck(final boolean isFinalRound) {
            //new copy of best found so far - not used here
            MeldedCardList bestHand = new MeldedCardList(MeldedCardList.this.roundOf, MeldedCardList.this.playerHandComparator);
            return decomposeAndCheck(isFinalRound, bestHand);
        }

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

        //add to a new meld just created, or to an existing meld and remove from other melds, partial melds, or singles
        // allows for dragging and dropping back onto itself
        protected void addTo(final Card card) {
            //remove from unMelded, singles, or other melds
            for (CardList cl : MeldedCardList.this.melds) cl.remove(card);
            for (CardList cl : MeldedCardList.this.partialMelds) cl.remove(card);
            MeldedCardList.this.singles.remove(card);
            if (!this.contains(card)) this.add(card); //may remove and then re-add
            this.needToCheck = true;
        }

        MeldType getMeldType() {
            return meldType;
        }

        void setMeldComplete(MeldComplete meldComplete) {
            this.meldComplete = meldComplete;
        }

        void setMeldType(MeldType meldType) {
            this.meldType = meldType;
        }
    }//end class Meld

    /* STATIC METHODS associated with Meld (but Inner classes can't contain static methods */

    //Helper for meldUsingHeuristics
    //TODO:A Would like to merge with other setCheckedAndValid - when we move these to right context
    static void setCheckedAndValid (final ArrayList<Meld> melds) {
        for (Meld meld : melds) {
            meld.setCheckedAndValid();
            meld.meldComplete = MeldComplete.FULL;
        }
    }

    //Helper for FiveKings
    static boolean isValidMeld(final CardList cl) {
        return (cl instanceof Meld) && ((Meld)cl).isValidFullMeld;
    }

    static String getString(final ArrayList<Meld> meldsOrUnMelds) {
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
    static int getCardScore(Card card, Rank wildCardRank, boolean isFinalRound) {
        final int cardScore;
        //if final round, then rank wildCard=20, Joker=50, otherwise Rank value
        if (isFinalRound) cardScore = card.getCardValue(wildCardRank);
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
        for (Card card:cardList) if (contains(containingMelds,card)) return true;
        return false;
    }


}//private class Hand