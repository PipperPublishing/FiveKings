package com.example.jeffrey.fivekings;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by Jeffrey on 1/22/2015.
 * Initial version handling of permutations (each call): nPn-1=n!, where n=round+1
 *  3:  Examined 229 of which 24 were valid (4P3 = 4!)
 *  4:  Examined 2931 of which 120 were valid
 *  5:  Examined 44791 of which 720 were valid
 *  6:  Examined 800668 of which 5040 were valid
 *  7:  Examined 16434825 of which 40320 were valid
 * 1/28/2015 v2 Implement Heap's algorithm inline- see http://www.cs.princeton.edu/~rs/talks/perms.pdf
 * 1/29/2015 Implement Johnson-Trotter algorithm which can act as an iterator (so storage is less of a problem)
 * 1/30/2015 move to a different evaluation method which counts pairs and possible sequences
 * 2/1/2015 Make melds, unMelded ArrayList<CardList> so we can separate melds visually
 * 2/5/2015 Eliminate using wrapper, switch to unified hand evaluation
 * TODO:B Extend for how many rounds we can use permutations by eliminating equivalent permutations (e.g. K*-KS = KS=K*)
 * TODO:C Discard strategy: Don't discard what others want (wild cards, or cards they picked up)
 * TODO:C Should be able to merge large chunks of Heuristics and Permutations
 * TODO: Current problems:- need to check that partial and full melds don't overlap
  */
class Hand {
    //all your cards, excluding what you picked up
    private CardList cards;
    // these following values are the LATEST meld/evaluate attempt and so are mutable
    // Player saves key values before trying different strategies
    // the cards melded onto the table
    private ArrayList<CardList> melds;
    // and those unmelded (which count against the score)
    private ArrayList<CardList> partialMelds;
    private CardList singles;
    private int intermediateHandValue; //looks at partial melds and sequences at 1/2 value
    private int finalHandScore; //looks at full value of cards not in melds
    private Card lastDiscard;

    //Default constructor is from CardList
    Hand() {
        cards = new CardList();
        melds = new ArrayList<>();
        partialMelds = new ArrayList<>();
        singles = new CardList(); //set to cards in dealNew
        intermediateHandValue = 0;
        finalHandScore=0;
        lastDiscard=null;
    }

    int meldAndEvaluate(Rank wildCardRank, boolean usePermutations, boolean isFinalScore, Card addedCard) {
        int valuation;
        if (usePermutations) valuation = meldAndEvaluateUsingPermutations(wildCardRank, isFinalScore, addedCard);
        else valuation = meldAndEvaluateUsingHeuristics(wildCardRank, isFinalScore, addedCard);
        return valuation;
    }

    /* v2 values keeping pairs and potential sequences, because otherwise we'll throw away a King from a pair of Kings
Evaluate using
    1. Maximize melds: 3 or more rank or sequence melds - maximize this
    2. Maximize partialMelds: pairs and broken sequences (for now just +/-1) - want to maximize number of cards in this
    3. Minimize unMelded: remaining singles - minimize the score of this by throwing away the
In the final scoring, calculate partialMelds and unMelded value
Evaluation accounts for hand potential, but Scoring is just what's left after melding
TODO:B Account for the overlap between partialMelds and partialSequences
TODO:B Add in more than one sequence
TODO:B Loop over fullMeld and fullSequence alternatives (use perms?)
 */
    int meldAndEvaluateUsingHeuristics(Rank wildCardRank, boolean isFinalScore, Card addedCard) {
        //cards.size()+1 because we add the addedCard (discardPile or drawPile)
        if (addedCard == null) throw new RuntimeException("Hand.useDiscardPile: addedCard is null");
        //Log.d(Game.APP_TAG,"Entering meldAndEvaluateUsingHeuristics");
        CardList cardsWithAdded = new CardList(cards);
        cardsWithAdded.add(addedCard);
        final int numCards = cardsWithAdded.size();

        //list of potential melds (pairs and broken sequences) - could be many of these
        //because a card can be in the list multiple times
        ArrayList<CardList> fullRankMelds = new ArrayList<>(numCards);
        ArrayList<CardList> fullSequences = new ArrayList<>(numCards);
        ArrayList<CardList> partialRankMelds = new ArrayList<>(numCards);
        ArrayList<CardList> partialSequences = new ArrayList<>(numCards);

        //separate out any wildcards
        //TODO:B create separate list without wildcards - would remove the checks we have to do
        //Log.d(Game.APP_TAG,"---Separate out wildcards");
        CardList wildCards = new CardList(numCards);
        for (Card card : cardsWithAdded) {
            if (card.isWildCard(wildCardRank)) wildCards.add(card);
        }



        //Rank Matches - loop from highest to lowest rank
        //Log.d(Game.APP_TAG,"---find matches in each rank");
        CardList sortedCards = new CardList(cardsWithAdded);
        Collections.sort(sortedCards, Card.cardComparatorRankFirstDesc);
        CardList rankMatch = new CardList(numCards);
        rankMatch.clear();
        //will meld larger cards first because of the comparator above
        for (Card card : sortedCards) {
            //if the card is already in a full meld, then skip it
            if (!card.isWildCard(wildCardRank) && !contains(fullRankMelds,card)) {
                if (rankMatch.isEmpty() || (card.isSameRank(rankMatch.get(rankMatch.size()-1)))) {
                    rankMatch.add(card);
                }
                else {
                    //not same rank; record and restart the sequence
                    addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalScore);
                    rankMatch.clear();
                    rankMatch.add(card);
                }//end-if same rank
            }
        }
        addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalScore);

        //Sequences - for now full sequences (3*-4*-5*) or broken pairs (e.g. 3*-5*) in partialSequences list
        //Log.d(Game.APP_TAG,"---find matches in each sequence");
        Collections.sort(sortedCards, Card.cardComparatorSuitFirst);
        CardList sequenceMatch = new CardList(numCards);
        for (Suit suit:Suit.values()) {
            sequenceMatch.clear();
            for (Card card : sortedCards) {
                //if the card is already in a full meld or sequence, then skip it
                if (!card.isWildCard(wildCardRank) && card.isSameSuit(suit) && !contains(fullRankMelds,card) && !contains(fullSequences,card)) {
                    if (sequenceMatch.isEmpty() || (1 == card.getRankDifference(sequenceMatch.get(sequenceMatch.size()-1)))) {
                        sequenceMatch.add(card);
                    }
                        //broken sequence; record but put into partial sequences AND into next sequence (unless we used aa wildcard to make it full)
                    else if ((1 == sequenceMatch.size()) && (2== card.getRankDifference(sequenceMatch.get(sequenceMatch.size()-1)))) {
                        sequenceMatch.add(card);
                        boolean madePartialIntoFull = addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalScore);
                        sequenceMatch.clear();
                        if (!madePartialIntoFull) sequenceMatch.add(card);
                    }
                    else{
                        //not adjacent; record and restart the sequence
                        addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalScore);
                        sequenceMatch.clear();
                        sequenceMatch.add(card);
                    }
                }//end-if same suit
            }
            addWildcardsAndRecord(fullSequences,partialSequences,sequenceMatch,wildCards,isFinalScore);
        }//end for Suits

        //Go back and check if partial rank melds overlap with full sequences; if so, drop the partial rank meld
        for (Iterator<CardList> iterator = partialRankMelds.iterator(); iterator.hasNext(); ) {
            CardList rankMeld = iterator.next();
            if (contains(fullSequences, rankMeld)) iterator.remove();
        }


        //TODO:A if we had two wildcards + a single card, meld them

        //if there are remaining wildcards, keep adding them to existing melds until we run out
        while (!wildCards.isEmpty() && (!fullRankMelds.isEmpty() || !fullSequences.isEmpty())) {
            //Log.d(Game.APP_TAG, "---extend existing melds/sequence");
            for (Iterator<CardList> iterator = fullRankMelds.iterator(); iterator.hasNext(); ) {
                CardList rankMeld = iterator.next();
                rankMeld.add(wildCards.get(0));
                wildCards.remove(0);
                if (wildCards.isEmpty()) break;
            }
            if (!wildCards.isEmpty()) {
                for (Iterator<CardList> iterator = fullSequences.iterator(); iterator.hasNext(); ) {
                    CardList sequenceMeld = iterator.next();
                    sequenceMeld.add(wildCards.get(0));
                    wildCards.remove(0);
                    if (wildCards.isEmpty()) break;
                }
            }
        }

        //ArrayList of CardLists (so we can separate melds from each other)
        melds = new ArrayList<>(fullRankMelds);
        melds.addAll(fullSequences);

        // For final scoring (last licks) we don't show partial melds (they all go into singles)
        // For intermediate scoring they count reduced and we don't show in singles
        if (isFinalScore) {
            //partialMelds should always already be clear (because checked earlier)
            if (!partialRankMelds.isEmpty() || !partialSequences.isEmpty())
                Log.e(Game.APP_TAG, "meldAndEvaluateUsingHeuristics: partialMelds not empty in final scoring");
            partialMelds.clear();
        }else {
            partialMelds = new ArrayList<>(partialRankMelds);
            partialMelds.addAll(partialSequences);
        }
        //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
        //TODO:A SHouldn't be any wildcards left - but possible they will not get included here (if there wen't any melds)
        singles = new CardList(cardsWithAdded);
        for (Card card:cardsWithAdded) {
            if (contains(melds,card)) singles.remove(card);
            if (!isFinalScore && contains(partialMelds,card)) singles.remove(card);
        }

        //Now find most expensive single card to discard - if there aren't any singles then pick highest value partialMeld
        //Log.d(Game.APP_TAG,"---find discard");
        lastDiscard=null;
        Collections.sort(sortedCards, Card.cardComparatorRankFirstDesc);
        if (!singles.isEmpty()) {
            lastDiscard = singles.getHighestScoreCard(wildCardRank, isFinalScore);
            singles.remove(lastDiscard);
        }//end if singles are not empty
        else if (!partialMelds.isEmpty()) {
            //eliminate a partialMeld containing the first high value card (sorted descending)
            for (Card card : sortedCards) {
                if (!card.isWildCard(wildCardRank) && contains(partialMelds, card)) {
                    lastDiscard = card;
                    break;
                }
            }
            if (null == lastDiscard)
                throw new RuntimeException("meldAndEvaluateUsingHeuristics: no discard found in partialMelds");
            for (Iterator<CardList> iterator = partialMelds.iterator(); iterator.hasNext(); ) {
                CardList partialMeld = iterator.next();
                if (partialMeld.contains(lastDiscard)) {
                    singles.addAll(partialMeld);
                    singles.remove(lastDiscard);
                    iterator.remove();
                    break;
                }
            }
        }//end-else partialMelds is not empty
        else {//have to eliminate a fullMeld - here you want to sort ascending
            Collections.sort(sortedCards, Card.cardComparatorRankFirstAsc);
            for (Card card : sortedCards) {
                if (!card.isWildCard(wildCardRank) && contains(melds, card)) {
                    lastDiscard = card;
                    break;
                }
            }
            if (null == lastDiscard)
                throw new RuntimeException("meldAndEvaluateUsingHeuristics: no discard found in melds");
            for (Iterator<CardList> iterator = melds.iterator(); iterator.hasNext(); ) {
                CardList rankMeldSeq = iterator.next();
                if (rankMeldSeq.contains(lastDiscard)) {
                    singles.addAll(rankMeldSeq);
                    singles.remove(lastDiscard);
                    iterator.remove();
                    break;
                }
            }
        }


        //Evaluate this; don't pass full melds/sequences because they don't count in scoring
        intermediateHandValue = calculateHandValue(wildCardRank, isFinalScore, cardsWithAdded, partialMelds, singles);

        //Log.d(Game.APP_TAG,"---exiting meldAndEvaluateUsingHeuristics");
        return intermediateHandValue;
    }//end int meldAndEvaluateUsingHeuristics

    //save test if full; see if you can pad it to a full with wildcards and keep/discard if not
    private boolean addWildcardsAndRecord(ArrayList<CardList> fulls, ArrayList<CardList> partials, CardList test, CardList wildCards, boolean isFinalScore) {
        boolean madePartialIntoFull=false;
        if (test.size()>=3) fulls.add((CardList)test.clone());
        else if (2 == test.size()) {
            if (wildCards.isEmpty()) {
                if (!isFinalScore) partials.add((CardList) test.clone());
            }
            else {
                test.add(wildCards.get(0));
                wildCards.remove(0);
                fulls.add((CardList) test.clone());
                madePartialIntoFull = true;
            }
        }
        return madePartialIntoFull;
    }


    /* Hand evaluation - v1
        Consider all permutations (shouldn't be too expensive) and return -ve value of unmelded
        This is the sledgehammer approach (rather than trying to heuristically meld)
        Everything melded gives the maximum evaluation of 0
        TODO:B Look at descending sequences so we can throw them out sooner
        TODO:B Eliminate looking at other perms that are equivalent score (e.g. (K* KS KH) = (KS K* KH)) - use a hash?
         */
    private int meldAndEvaluateUsingPermutations(Rank wildCardRank, boolean isFinalScore, Card addedCard) {
        if (addedCard == null) throw new RuntimeException("Hand.useDiscardPile: addedCard is null");

        CardList cardsWithAdded = new CardList(cards);
        cardsWithAdded.add(addedCard);
        //numCards = cards.size()+1 because we add the addedCard (discardPile or drawPile)
        final int numCards = cardsWithAdded.size();

        //new copy of best found so far
        ArrayList<CardList> bestPermutationMelded=null;
        ArrayList<CardList> bestPermutationUnMelded=null;
        CardList bestPermutationSingles=null;
        Card bestPermutationDiscard=null;
        int bestValuation=-1;

/*        v1: Consider all permutations and use early pruning (based on unMelded score)
        Generate permutations - because all permutations are tested, we can just look for melds and sequences in order
        Create a permuter index for each available card slot
        v2: generate the next permutation on each call to getNext
*/
        int allPermutations=0;
        Permuter indexes = new Permuter(numCards);
        int[] cardListIdx = null;

        CardList testMeld = new CardList();
        Card testCard = null;
        ArrayList<CardList> permutationUnMelded = new ArrayList<>();
        ArrayList<CardList> permutationMelded = new ArrayList<>();
        CardList permutationSingles = new CardList();

        //Loop over all valid permutations; the card at numCards is considered to be what we will discard
        for (cardListIdx = indexes.getNext(); cardListIdx != null; cardListIdx = indexes.getNext()) {
            allPermutations++;
            permutationUnMelded.clear();
            permutationMelded.clear();
            permutationSingles.clear();

            //if lastMelded was a non-wild card, then these are just that card's Rank and Suit
            //if it was a wild card, then they are the "substitute" value(s) the wild card plays
            Card lastMeldedCard = null;
            Rank rankMeldRank = null;
            Rank sequenceMeldLastRank = null;
            Suit sequenceMeldSuit = null;
            boolean isSequenceMeld = true;
            boolean isRankMeld = true;

/*           note *this* loop is over the cards in the permutation to see whether they can be melded and excludes the [numCards-1] card which will be discarded
            We only look for melds in order (including ascending sequences) because at least one permutation will have that if it exists
*/
            lastMeldedCard = cardsWithAdded.get(cardListIdx[0]);
            testMeld.clear();
            testMeld.add(lastMeldedCard);
            for (int iCard = 1; iCard < numCards - 1; iCard++) {
                /*
                * if this is a wildcard or lastMeldedCard is a wildcard
                * or cardsWithAdded[cardListIdx[iCard] melds in rank (any suit) or seq (same suit)
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

                testCard = cardsWithAdded.get(cardListIdx[iCard]);
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
                    else if (testCard.getRank().isLowestRank() && lastMeldedCard.isWildCard(wildCardRank)) isSequenceMeld = false;
                    //same Suit and next in sequence
                    else if (testCard.isSameSuit(sequenceMeldSuit) && testCard.isSameRank(sequenceMeldLastRank.getNext())) {
                        testIsMelding = true;
                        isRankMeld = false; //now we know it's a sequence meld
                    } else isSequenceMeld = false;
                }

                if (testIsMelding) testMeld.add(testCard);
                else {// testCard doesn't fit the testMeld - now check if testMeld has fewer than 3 cards then we can move it to unMelded
                    if (testMeld.size() >= 3)
                        permutationMelded.add((CardList)testMeld.clone());
                    else if (2 == testMeld.size())
                        permutationUnMelded.add((CardList)testMeld.clone());
                    else
                        permutationSingles.add(testMeld.get(0));
                    testMeld.clear();
                    testMeld.add(testCard);
                    isRankMeld = true;
                    isSequenceMeld = true;
                    //if the unmelded score is already bigger than previous minimum, we can move to next permutation
                    if ((bestValuation != -1) && (calculateHandValue(wildCardRank, isFinalScore, cardsWithAdded, permutationUnMelded, permutationSingles) >= bestValuation))
                        break;
                }
            }//for iCard=1..numCards (testing permutation)
            //anything left over in testMeld needs to be added to unMelded or Melded appropriately
            if (testMeld.size() >= 3)
                permutationMelded.add((CardList)testMeld.clone());
            else if (2 == testMeld.size())
                permutationUnMelded.add((CardList)testMeld.clone());
            else
                permutationSingles.add(testMeld.get(0));

            //reset the bestPermutation if this is a lower score
            //on normal round, Jokers and wild cards are evaluated as 0; in the last licks round they count full value
            if ((bestValuation == -1) || (calculateHandValue(wildCardRank, isFinalScore, cardsWithAdded, permutationUnMelded, permutationSingles) < bestValuation)) {
                bestValuation = calculateHandValue(wildCardRank, isFinalScore,cardsWithAdded, permutationUnMelded, permutationSingles);
                //use copy constructor because permutationMelded etc will continue to change on subsequent permutations
                bestPermutationMelded = new ArrayList<>(permutationMelded);
                bestPermutationUnMelded  = new ArrayList<>(permutationUnMelded);
                bestPermutationSingles = new CardList(permutationSingles);
                //discard is the right-most card in the permutations - needs to be a pointer to the actual card for later discarding
                bestPermutationDiscard = cardsWithAdded.get(cardListIdx[numCards - 1]);
            }
        }//end for cardListIdx in all permutations of cards

        //set member variables
        melds = bestPermutationMelded;
        partialMelds = bestPermutationUnMelded;
        lastDiscard = bestPermutationDiscard;

        //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
        singles = new CardList(cardsWithAdded);
        for (Card card:cardsWithAdded) {
            if ((contains(bestPermutationMelded,card) || (card == lastDiscard)) || (!isFinalScore && contains(partialMelds,card))){
                singles.remove(card);
            } else if (card.isWildCard(wildCardRank)) Log.e(Game.APP_TAG,"meldAndEvaluateUsingPermutations: Unmelded wildcards remaining");
        }
        if (isFinalScore) partialMelds.clear();

        intermediateHandValue = calculateHandValue(wildCardRank, isFinalScore,cardsWithAdded, partialMelds, singles);

        //Log.d(Game.APP_TAG, "Examined "+allPermutations+" permutations");
        return intermediateHandValue;
    }//end int meldAndEvaluateUsingPermutations


    /*    Score and sort cardsWithAdded - valuing partial melds and sequences
        Intermediate scoring:
        cardValue = face value if not in full-meld, full-sequence, or wildcard
                      x 1/2  face value if in partialMeld
        Final scoring: Discard all partial melds/sequences and convert to singles (already done)
                        But also record this at all times for final round scoring
        */
    private int calculateHandValue(Rank wildCardRank, boolean isFinalScore,  CardList cardsWithAdded, ArrayList<CardList> partialMelds, CardList singles) {
        float handValue = 0.0f;
        finalHandScore=0;
        for (Card card : cardsWithAdded) {
            float cardValue = card.getScore(wildCardRank, isFinalScore);
            //if Final, then singles contains everything in partialMelds at full value
            //otherwise reduce it by 1/2 if partially melded
            if (contains(partialMelds,card) || singles.contains(card)) {
                finalHandScore += cardValue;
                if (!isFinalScore && contains(partialMelds, card)) handValue += 0.5 * cardValue;
                else handValue += cardValue;
            }
        }
        return (int)handValue;
    }


    //delegate method to look for card in ArrayList<CardList> (otherwise we'd have to override ArrayList)
    private static boolean contains (ArrayList<CardList> cardLists, Card card) {
        if (null == cardLists) return false;
        for (CardList cardList : cardLists)
            if (cardList.contains(card)) return true;
        return false;
    }
    private static boolean contains (ArrayList<CardList> containingCardLists, CardList cardList) {
        if (null == containingCardLists) return false;
        for (Card card:cardList) if (contains(containingCardLists,card)) return true;
        return false;
    }


    protected int getHandValueOrScore(boolean isFinalScore){
        if (isFinalScore) return this.finalHandScore;
        else return this.intermediateHandValue;
    }


    Card discardFrom(Card discardedCard){
        cards.remove(discardedCard);
        return discardedCard;
    }

    void add(Card addedCard) {
        cards.add(addedCard);
    }

    boolean dealNew(CardList cardsToDeal, int numberToDeal) {
        if ((cardsToDeal == null) || (numberToDeal <= 0)) return false;
        cards.clear();
        for (int iCard=0; iCard < numberToDeal; iCard++) cards.add(cardsToDeal.deal());
        singles = (CardList)cards.clone();
        return true;
    }

    //GETTERS and SETTERS
    //TODO: Combine these two methods depending on which is calling them
    String getUnMeldedString() {
        StringBuffer unMeldedString = new StringBuffer();
        if (null != partialMelds) {
            for (CardList unMelds : partialMelds) {
                unMeldedString.append(unMelds.getString());
            }
        }
        return unMeldedString.toString();
    }
    String getSingles() {
        return singles.getString();
    }

    String getMeldedString() {
        StringBuffer meldedString = new StringBuffer();
        if (null != melds) {
            for (CardList melds : this.melds) {
                meldedString.append(melds.getString());
            }
        }
        return meldedString.toString();
    }


    public Card getLastDiscard() {
        return lastDiscard;
    }

}//class Hand
