package com.example.jeffrey.fivekings;

/**
 * Re-created by Jeffrey on 3/16/2015.
 * 3/16/2015    Separated Hand out from Player (there is a PlayerHand sub-class which is an inner class)
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/** Hand - general manipulation of melds, partial Melds etc.
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
 * TODO:A Partial melds should only include one instance of each card (not 10C-10D and 10C-JC)
 TODO:B Account for the overlap between partialMelds and partialSequences
 * TODO:B Current problems:- need to check that partial and full melds don't overlap, also partial Melds and sequences
 * TODO:C Should be able to merge large chunks of Heuristics and Permutations
 * TODO:B Look at Hand and Meld as co-classes which use each other

 */
class Hand {
    protected final Rank roundOf; //how many cards you should have and what is wild

    //all your cards
    protected CardList cards;
    // and the cards organized into melds, partial melds and remaining singles
    protected ArrayList<Meld> melds;
    protected ArrayList<Meld> partialMelds;
    protected Meld singles;

    protected float intermediateValue; //looks at partial melds and sequences at adjusted value
    protected int finalScore; //looks at full value of cards not in melds

    protected HandComparator playerHandComparator;


    protected Hand(final Rank roundOf, final HandComparator playerHandComparator) {
        this.roundOf = roundOf;
        this.cards = new CardList(roundOf.getRankValue());
        this.melds = new ArrayList<>();
        this.partialMelds = new ArrayList<>();
        this.singles = new Meld(playerHandComparator);

        this.intermediateValue = 0;
        this.finalScore =0;

        this.playerHandComparator = playerHandComparator;
    }

    //replace cards only
    protected Hand(final Rank roundOf, final HandComparator playerHandComparator, CardList cards) {
        this(roundOf, playerHandComparator);
        if (cards != null) this.cards = cards;
    }

    private void copyFrom(Hand fromHand) {
        if (fromHand != null) {
            this.cards = fromHand.cards;
            this.melds = fromHand.melds;
            this.partialMelds = fromHand.partialMelds;
            this.singles = fromHand.singles;

            this.intermediateValue = fromHand.intermediateValue;
            this.finalScore = fromHand.finalScore;

            this.playerHandComparator = fromHand.playerHandComparator;
        }
    }

    //used by Computer (or eventually maybe Human with [Meld] button)
    // the Hand this is being called on is the test hand, so we set here the member variables
    protected int meldAndEvaluate(final MeldMethod method, final boolean isFinalRound) {
        if (method == MeldMethod.PERMUTATIONS) meldBestUsingPermutations(isFinalRound);
        else meldUsingHeuristics(isFinalRound);
        return this.calculateValueAndScore(isFinalRound);
    }


    protected int calculateValueAndScore(boolean isFinalRound) {
        this.intermediateValue = 0.0f;
        this.finalScore =0;
        for (Card card : this.cards) {
            float cardValue = getCardScore(card, this.roundOf, false);
            float cardScore = getCardScore(card, this.roundOf, true);
            //if Final, then singles contains everything in partialMelds at full value
            //otherwise reduce it by 1/2 if partially melded
            if (contains(partialMelds,card) || singles.contains(card)) {
                this.finalScore += cardScore;
                if (contains(partialMelds, card)) this.intermediateValue += 0.5 * cardValue;
                else this.intermediateValue += cardValue;
            }
        }
        //"Melds" can actually be attempts by human so need to be added (have previously been evaluated by Meld.check)
        if (melds != null) {
            for (Meld meld : melds) {
                finalScore += meld.valuation;
                intermediateValue += meld.valuation;
            }
        }
        return isFinalRound ? this.finalScore : (int)this.intermediateValue;
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



    /* HEURISTICS
    v2 values keeping pairs and potential sequences, because otherwise we'll throw away a King from a pair of Kings
Strategy is to:
    1. Maximize melds: 3 or more rank or sequence melds - maximize this
    2. Maximize partialMelds: pairs and broken sequences (for now just +/-1) - want to maximize number of cards in this
    3. Minimize unMelded: remaining singles - minimize the score of this by throwing away the
In the final scoring, calculate partialMelds and unMelded value
Evaluation accounts for hand potential, but Scoring is just what's left after melding
*/
    private boolean meldUsingHeuristics(final boolean isFinalRound) {
        //Log.d(Game.APP_TAG,"Entering meldUsingHeuristics");
        final int numCards = this.cards.size();
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
        sortAndSeparateWildCards(wildCardRank, this.cards, nonWildCards, wildCards);

        //Rank Matches - loop from highest to lowest rank
        //Log.d(Game.APP_TAG,"---find matches in each rank");
        CardList cardsSortedByRankDesc = new CardList(nonWildCards);
        Collections.sort(cardsSortedByRankDesc, Card.cardComparatorRankFirstDesc); //e.g. (K*,KH,QD,JC...)
        Meld rankMatch = new Meld(this.playerHandComparator);
        rankMatch.clear();
        //will meld larger cards first because of the comparator above
        for (Card card : cardsSortedByRankDesc) {
            //if the card is already in a full meld, then skip it
            // don't' have to check wildcards because we've removed them
            if (!contains(fullRankMelds,card)) {
                if (rankMatch.isEmpty() || (card.isSameRank(rankMatch.get(rankMatch.size()-1)))) {
                    rankMatch.add(card);
                }
                else {
                    //not same rank; record and restart the sequence
                    addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalRound);
                    rankMatch.clear();
                    rankMatch.add(card);
                }//end-if same rank
            }
        }
        addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalRound);

        //Sequences - for now full sequences (3*-4*-5*) or broken pairs (e.g. 3*-5*) in partialSequences list
        //Log.d(Game.APP_TAG,"---find matches in each sequence");
        CardList cardsSortedBySuit = new CardList(nonWildCards);
        Collections.sort(cardsSortedBySuit, Card.cardComparatorSuitFirst); //e.g. 3S,5S,JS,6H,8H...
        Meld sequenceMatch = new Meld(this.playerHandComparator);
        for (Suit suit:Suit.values()) {
            sequenceMatch.clear();
            for (Card card : cardsSortedBySuit) {
                //if the card is already in a full meld or sequence, then skip it
                if (card.isSameSuit(suit) && !contains(fullRankMelds,card) && !contains(fullSequences,card)) {
                    if (sequenceMatch.isEmpty() || (1 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size()-1)))) {
                        sequenceMatch.add(card);
                    }
                    //broken sequence; record but put into partial sequences AND into next sequence (unless we used aa wildcard to make it full)
                    else if ((1 == sequenceMatch.size()) && (2== card.getRankDifference(sequenceMatch.get(sequenceMatch.size()-1)))) {
                        sequenceMatch.add(card);
                        boolean madePartialIntoFull = addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalRound);
                        sequenceMatch.clear();
                        if (!madePartialIntoFull) sequenceMatch.add(card);
                    }
                    else{
                        //not adjacent; record and restart the sequence
                        addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalRound);
                        sequenceMatch.clear();
                        sequenceMatch.add(card);
                    }
                }//end-if same suit
            }
            addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalRound);
        }//end for Suits

        //Go back and check if partial rank melds overlap with full sequences; if so, drop the partial rank meld
        for (Iterator<Meld> iterator = partialRankMelds.iterator(); iterator.hasNext(); ) {
            CardList rankMeld = iterator.next();
            if (contains(fullSequences, rankMeld)) iterator.remove();
        }

        //If we still have 2+ wildcards, meld singles into a full meld - but don't create partials with a wildcard
        Meld meldOfSingles = new Meld(this.playerHandComparator);
        for (Card card : cardsSortedByRankDesc) {
            if (wildCards.size()<=1) break;
            if (!contains(fullRankMelds,card) && !contains(fullSequences,card)) {
                meldOfSingles.clear();
                meldOfSingles.add(card);
                meldOfSingles.add(wildCards.get(0));
                wildCards.remove(0);
                addWildcardsAndRecord(fullRankMelds,partialRankMelds,meldOfSingles,wildCards,isFinalRound);
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
        for (Meld meld : melds) meld.setCheckedAndValid();

        // For final scoring (last licks) we don't show partial melds (they all go into singles)
        // For intermediate scoring they count reduced and we don't show in singles
        if (isFinalRound) {
            //partialMelds should always already be clear (because checked earlier)
            if (!partialRankMelds.isEmpty() || !partialSequences.isEmpty())
                Log.e(Game.APP_TAG, "meldUsingHeuristics: partialMelds not empty in final scoring");
            partialMelds.clear();
        }else {
            partialMelds = new ArrayList<>(partialRankMelds);
            partialMelds.addAll(partialSequences);
        }
        //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
        // since the wildcards are no longer being melded into partials need to add them back here
        singles = new Meld(this.playerHandComparator, nonWildCards);
        for (Card card:nonWildCards) {
            if (contains(melds,card)) singles.remove(card);
            if (!isFinalRound && contains(partialMelds,card)) singles.remove(card);
        }
        singles.addAll(wildCards);

        //don't need to find discard (we are looping over possible discards if this is a Computer turn)
        //and scoring is now done in the caller
        //Log.d(Game.APP_TAG,"---exiting meldUsingHeuristics");
        return true;
    }//end int meldUsingHeuristics

    //save test if full; see if you can pad it to a full with wildcards and keep/discard if not
    private boolean addWildcardsAndRecord(final ArrayList<Meld> fulls, final ArrayList<Meld> partials, final Meld test, final CardList wildCards, final boolean isFinalRound) {
        boolean madePartialIntoFull=false;
        if (test.size()>=3) fulls.add((Meld)test.clone());
        else if (2 == test.size()) {
            if (!wildCards.isEmpty()) {
                test.add(wildCards.get(0));
                wildCards.remove(0);
                fulls.add((Meld) test.clone());
                madePartialIntoFull = true;
            } else if (isFinalRound) { //should not be called if we don't have enough wildcards
                Log.e(Game.APP_TAG,"addWildcardsAndRecord: wildCards.isEmpty and isFinalRound");
            } else partials.add((Meld) test.clone());
        }
        return madePartialIntoFull;
    }


    /* PERMUTATIONS - v1
        Consider all permutations (shouldn't be too expensive) and return value of unmelded
        This is the sledgehammer approach - we use this until it gets too slow and then switch to heuristics
        Everything melded gives the maximum evaluation of 0
         */
    private void meldBestUsingPermutations(final boolean isFinalRound) {
        final int numCards = this.cards.size();
        final Rank wildCardRank = this.roundOf;

        Meld cardsCopy = new Meld(this.playerHandComparator, cards);

        Hand bestHand = new Hand(this.roundOf, this.playerHandComparator);

        cardsCopy.check(isFinalRound, bestHand);
        //set member variables
        this.melds = bestHand.melds;
        this.partialMelds = bestHand.partialMelds;

        //TODO:A May not need this - are singles set correctly by Meld.check?
        //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
        this.singles = new Meld(this.playerHandComparator,cardsCopy);
        for (Card card:cardsCopy) {
            if (contains(this.melds,card) || (!isFinalRound && contains(this.partialMelds,card))){
                this.singles.remove(card);
            } else if (card.isWildCard(wildCardRank)) Log.e(Game.APP_TAG,"meldBestUsingPermutations: Unmelded wildcards remaining");
        }
        if (isFinalRound) partialMelds.clear();

    }//end meldBestUsingPermutations


    boolean makeNewMeld(final Card card) {
        Meld newMeld = new Meld(this.playerHandComparator);
        newMeld.addTo(card);
        melds.add(newMeld);
        return true;
    }

    Card discardFrom(final Card discardedCard){
        cards.remove(discardedCard);
        syncCardsAndMelds();
        return discardedCard;
    }

    void add(final Card addedCard) {
        cards.add(addedCard);
        syncCardsAndMelds();
    }


    //TODO:A: And update hand valuation (would need to pass isFinalScore or call twice)
    //Use iterators so we can remove current value
    private boolean syncCardsAndMelds() {
        //check that all hand.cards are in melds or singles (add to singles if not)
        for (Card card : cards) {
            boolean foundCard = singles.contains(card) || contains(melds, card) || contains(partialMelds, card);
            if (!foundCard) singles.add(card);
        }
        //check that all cards in melds, partialMelds, and singles are in hand.cards (remove if not)
        for (Iterator<Meld> mIterator = melds.iterator(); mIterator.hasNext(); ) {
            CardList cl = mIterator.next();
            for (Iterator<Card> cIter = cl.iterator(); cIter.hasNext();) {
                if (!cards.contains(cIter.next())) cIter.remove();
            }
            if (cl.isEmpty()) mIterator.remove();
        }
        for (Iterator<Meld> clIterator = partialMelds.iterator(); clIterator.hasNext(); ) {
            CardList cl = clIterator.next();
            for (Iterator<Card> cIter = cl.iterator(); cIter.hasNext();) {
                if (!cards.contains(cIter.next())) cIter.remove();
            }
            if (cl.isEmpty()) clIterator.remove();
        }
        for (Iterator<Card> cardIterator = singles.iterator(); cardIterator.hasNext();) {
            if (!cards.contains(cardIterator.next())) cardIterator.remove();
        }

        //and sort the singles
        Collections.sort(singles, Card.cardComparatorRankFirstDesc);
        return true;
    }

    //Hand GETTERS
    //TODO:A Should check if the hand has been evaluated and do it if needed
    protected int getValueOrScore(final boolean isFinalRound){
        if (isFinalRound) return this.finalScore;
        else return (int) this.intermediateValue;
    }

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



    /* 3/17/2015    Removed numCards argument; just initialize capacity as MAX_CARDS */
    static enum MeldMethod {PERMUTATIONS, HEURISTICS}

    /*------------------*/
    /* INNER CLASS Meld */
    /*------------------*/
    protected class Meld extends CardList {
        private int valuation; //0 if this is a valid meld
        private boolean needToCheck; //a card has been added or removed so we need to recheck
        private boolean isValidMeld=false;

        private HandComparator playerHandComparator;


        protected Meld(final HandComparator playerHandComparator, final CardList cl) {
            this(playerHandComparator);
            this.addAll(cl);
        }

        private Meld(final HandComparator playerHandComparator) {
            super(Game.MAX_CARDS);
            this.valuation = 0;
            this.isValidMeld = false;
            this.needToCheck = true;
            this.playerHandComparator = playerHandComparator;
        }

        //find the best arrangement of this meld and break if we get zero
        //also sorts the meld rank first (which should help if it's a rank meld)
        //if it's not a single meld, will return the breakdown
        //it's the caller's responsibility to not call this with too long a "meld" (since permutation time goes up geometrically)
        //returns best arrangement and returns partial meld and singles components if it can't be fully melded
        private int check(final boolean isFinalRound, final Hand bestHand) {
            int numCards = this.size();
            Rank wildCardRank = Hand.this.roundOf;
            Permuter indexes = new Permuter(numCards);
            CardList wildCards = new CardList(numCards);
            CardList nonWildCards = new CardList(numCards);
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
                Meld testMeld = new Meld(this.playerHandComparator);
                Card testCard = null;
                Hand permHand = new Hand(wildCardRank,playerHandComparator, this);


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
                        if (testCard.isWildCard(wildCardRank))
                            testIsMelding = true; //any wildcard is fine for a Rank meld
                        else if (rankMeldRank == null) testIsMelding = true;
                        else if (testCard.isSameRank(rankMeldRank)) { //same Rank (e.g. Queens)
                            testIsMelding = true;
                            isSequenceMeld = false; //now we know it's a Rank meld
                        } else isRankMeld = false;
                    }
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
                            isRankMeld = false; //now we know it's a sequence meld
                        } else isSequenceMeld = false;
                    }
                    //don't use else-if, because above block sets isSequenceMeld false
                    //testCard broke the sequence, but may still be a brokenSequence (e.g. 10C-QC)
                    if ((1 == testMeld.size()) && !isSequenceMeld && !isRankMeld && isBrokenSequence && (sequenceMeldLastRank != null)) {
                        testIsMelding = testCard.isSameSuit(sequenceMeldSuit) && (2 == testCard.getRankDifference(sequenceMeldLastRank));
                        isBrokenSequence = false; //don't want to add more cards
                    }

                    if (testIsMelding) testMeld.add(testCard);
                    else {// testCard doesn't fit the testMeld - now check if testMeld has fewer than 3 cards then we can move it to unMelded
                        if (testMeld.size() >= 3)
                            permHand.melds.add((Meld) testMeld.clone());
                        else if (2 == testMeld.size())
                            permHand.partialMelds.add((Meld) testMeld.clone());
                        else
                            permHand.singles.add(testMeld.get(0));
                        testMeld.clear();
                        testMeld.add(testCard);
                        isRankMeld = true;
                        isSequenceMeld = true;
                        //Don't do pruning at this point any more to allow scoring algorithm to be more complicated
                    }
                }//for iCard=1..numCards (testing permutation)
                //anything left over in testMeld needs to be added to unMelded or Melded appropriately
                if (testMeld.size() >= 3)
                    permHand.melds.add((Meld) testMeld.clone());
                else if (2 == testMeld.size())
                    permHand.partialMelds.add((Meld) testMeld.clone());
                else if (1 == testMeld.size())
                    permHand.singles.add(testMeld.get(0));

                //reset the bestPermutation if this is a lower score
                //use the player's specific criteria (ComputerPlayer or SmarterComputerPlayer)
                if ((bestValuation == -1) || playerHandComparator.isFirstBetterThanSecond(permHand, bestHand, isFinalRound)) {
                    bestValuation = permHand.calculateValueAndScore(isFinalRound);
                    //use Hand.copyFrom because bestHand is a (final) parameter
                    bestHand.copyFrom(permHand);
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
            //all the bestPermutationMelds are known to be valid
            for (Meld meld : bestHand.melds) meld.setCheckedAndValid();
            this.valuation = bestValuation;
            this.needToCheck = false;
            //single valid meld?
            this.isValidMeld = (bestHand.partialMelds.isEmpty()) && (bestHand.singles.isEmpty());

            return bestValuation;
        }//end int check

        protected int check(final boolean isFinalRound) {
            //new copy of best found so far - not used here
            Hand bestHand = new Hand(Hand.this.roundOf, Hand.this.playerHandComparator);
            return check(isFinalRound, bestHand);
        }

        //add to a new meld just created, or to an existing meld and remove from other melds, partial melds, or singles
        // allows for dragging and dropping back onto itself
        protected void addTo(final Card card) {
            //remove from unMelded, singles, or other melds
            for (CardList cl : Hand.this.melds) cl.remove(card);
            for (CardList cl : Hand.this.partialMelds) cl.remove(card);
            Hand.this.singles.remove(card);
            if (!this.contains(card)) this.add(card); //may remove and then re-add
            this.needToCheck = true;
        }

        private void setCheckedAndValid() {
            this.isValidMeld = true;
            this.needToCheck = false;
            this.valuation = 0;
        }

    }//end class Meld

    /* STATIC METHODS associated with Meld (but Inner classes can't contain static methods */

    static boolean isValidMeld(final CardList cl) {
        return (cl instanceof Meld) && ((Meld)cl).isValidMeld;
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

    private static void sortAndSeparateWildCards(final Rank wildCardRank,final CardList cards, final CardList nonWildCards, final CardList wildCards) {
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
    protected static boolean contains (final ArrayList<Meld> melds, Card card) {
        if (null == melds) return false;
        for (Meld meld : melds)
            if (meld.contains(card)) return true;
        return false;
    }
    private static boolean contains (final ArrayList<Meld> containingMelds, CardList cardList) {
        if (null == containingMelds) return false;
        for (Card card:cardList) if (contains(containingMelds,card)) return true;
        return false;
    }


}//private class Hand