package com.example.jeffrey.fivekings;

import java.util.ArrayList;
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
 * TODO:B Extend for how many rounds we can use permutations by combining all wild cards (one combination)
 * TODO:C Discard strategy: Don't discard what others want (wild cards, or cards they picked up)
 */
class Hand {
    //all your cards, excluding what you picked up
    private CardList cards;
    // the cards melded onto the table
    private ArrayList<CardList> melds;
    // and those left over (which count against the score)
    private ArrayList<CardList> unMelded;

    //Default constructor is from CardList
    Hand() {
        cards = new CardList();
        melds = new ArrayList<>();
        unMelded = new ArrayList<>();
    }

    int meldAndScore(Rank wildCardRank, boolean usePermutations, boolean isFinalScore, Card addedCard, EvaluationWrapper cardWrapper) {
        int score=0;
        if (usePermutations) score = meldAndScoreUsingPermutations(wildCardRank, isFinalScore, addedCard, cardWrapper);
        else score = meldAndScoreUsingHeuristics(wildCardRank, isFinalScore, addedCard, cardWrapper);
        return score;
    }

    /* v2 values keeping pairs and potential sequences, because otherwise we'll throw away a King from a pair of Kings
Score using
    1. Maximize melds: 3 or more rank or sequence melds - maximize this
    2. Maximize partialMelds: pairs and broken sequences (for now just +/-1) - want to maximize number of cards in this
    3. Minimize unMelded: remaining singles - minimize the score of this by throwing away the
In the final scoring, calculate partialMelds and unMelded value
TODO:B Account for the overlap between partialMelds and partialSequences
TODO:B Add in more than one sequence, and also broken sequences
 */
    int meldAndScoreUsingHeuristics(Rank wildCardRank, boolean isFinalScore, Card addedCard, EvaluationWrapper cardWrapper) {
        //cards.size()+1 because we add the addedCard (discardPile or drawPile)
        if (addedCard == null) throw new RuntimeException("Hand.useDrawOrDiscard: addedCard is null");
        //Log.d(Game.APP_TAG,"Entering meldAndScoreUsingHeuristics");
        CardList cardsWithAdded = new CardList(cards);
        cardsWithAdded.add(addedCard);
        final int numCards = cardsWithAdded.size();

        //list of potential melds (pairs and broken sequences) - could be many of these
        //because a card can be in the list multiple times
        ArrayList<CardList> partialMelds = new ArrayList<>(numCards);
        ArrayList<CardList> fullMelds = new ArrayList<>(numCards);
        ArrayList<CardList> partialSequences = new ArrayList<>(numCards);
        ArrayList<CardList> fullSequences = new ArrayList<>(numCards);


        //separate out any wildcards
        //Log.d(Game.APP_TAG,"---Separate out wildcards");
        CardList wildCards = new CardList(numCards);
        for (Card card : cardsWithAdded) {
            if (card.isWildCard(wildCardRank)) wildCards.add(card);
        }

        //find matches in each rank
        //Log.d(Game.APP_TAG,"---find matches in each rank");
        CardList rankMatch = new CardList(numCards);
        for (Rank rank:Rank.values()) {
            rankMatch.clear();
            for (Card card : cardsWithAdded) {
                if (!card.isWildCard(wildCardRank) && card.isSameRank(rank)) rankMatch.add(card);
            }
            //add pairs or better to lists
            if (rankMatch.size()>=3) fullMelds.add((CardList)rankMatch.clone());
            else if (2 == rankMatch.size()) partialMelds.add((CardList)rankMatch.clone());
        }//end-for Ranks

        //for now just one sequence in each suit with no break (ascending only)
        //TODO:A handle sequence in any order, and put broken sequences into partialSequences list (can be filled by wild cards)
        //Log.d(Game.APP_TAG,"---find matches in each sequence");
        CardList sequenceMatch = new CardList(numCards);
        Card lastCard=null;
        for (Suit suit:Suit.values()) {
            sequenceMatch.clear();
            lastCard = null;
            for (Card card : cardsWithAdded) {
                //if the card is already in a full meld, then skip it
                if (!card.isWildCard(wildCardRank) && card.isSameSuit(suit) && !contains(fullMelds,card)) {
                    if ((0 == sequenceMatch.size()) || (1 == card.getRankDifference(lastCard))) {
                        sequenceMatch.add(card);
                        lastCard = card;
                    }
                }//end-if same suit
            }
            if (sequenceMatch.size()>=3) fullSequences.add((CardList)sequenceMatch.clone());
            else if (sequenceMatch.size()>1) partialSequences.add((CardList)sequenceMatch.clone());
        }//end for Suits

        // Use extra wildcards to turn partial melds into full etc.
        //Log.d(Game.APP_TAG,"---see if we can expand partial melds");
        if (!wildCards.isEmpty()) {
            for (Iterator<CardList> iterator = partialMelds.iterator(); iterator.hasNext(); ) {
                CardList rankMeld = iterator.next();
                if (2 == rankMeld.size()) {
                    rankMeld.add(wildCards.get(0));
                    //move rankMeld from partial to fullMelds
                    fullMelds.add((CardList) rankMeld.clone());
                    iterator.remove();
                    wildCards.remove(0);
                    if (wildCards.isEmpty()) break;
                }
            }
        }
        if (!wildCards.isEmpty()) {
            //See if we can turn partialSequences into full by adding wild cards
            //Log.d(Game.APP_TAG, "---see if we can expand partial sequences");
            for (Iterator<CardList> iterator = partialSequences.iterator(); iterator.hasNext(); ) {
                CardList sequenceMeld = iterator.next();
                if (2 == sequenceMeld.size()) {
                    sequenceMeld.add(wildCards.get(0));
                    fullSequences.add((CardList) sequenceMeld.clone());
                    iterator.remove();
                    wildCards.remove(0);
                    if (wildCards.isEmpty()) break;
                }
            }
        }

        //TODO:A If we have excess wildcards, use them up with singles or with extending existing melds
        //TODO:A Also have problem with an excess card left

        //Score and sort cardsWithAdded - valuing partial melds and sequences
        //Intermediate scoring:
        // cardScore = face value if not in full-meld, full-sequence, or wildcard
        //              x 1/2 if in partialMeld x1/2 if in partialSequence (so x1/4 if in both)
        //Final scoring: Discard all partial melds/sequences and convert to singles
        //Log.d(Game.APP_TAG,"---scoring");
        CardList singles = new CardList(numCards);
        int cardScore=0;
        int intermediateScore=0;
        for (Card card:cardsWithAdded) {
            singles.add(card);
            cardScore = card.getScore(wildCardRank, isFinalScore);
            if (wildCards.contains(card)) singles.remove(card);
            if (contains(fullMelds,card) || contains(fullSequences,card)) {
                cardScore = 0;
                singles.remove(card);
            }
            if (!isFinalScore) {
                if (contains(partialMelds, card)) {
                    cardScore *= 0.5;
                    singles.remove(card);
                }
                if (contains(partialSequences, card)) {
                    cardScore *= 0.5;
                    singles.remove(card);
                }
            }
            intermediateScore += cardScore;
        }
        //Now find most expensive card to discard - if there aren't any cardsWithAdded then pick highest value card
        //Log.d(Game.APP_TAG,"---find discard");
        Card bestDiscard = null;
        if (0 == singles.size()) bestDiscard = addedCard; //TODO: Will have to adjust any melds using this
        else {
            bestDiscard = singles.getHighestScoreCard(wildCardRank, isFinalScore);
            singles.remove(bestDiscard);
        }

        //ArrayList of CardLists (so we can separate melds from each other)
        ArrayList<CardList> bestMelds = new ArrayList<>(fullMelds);
        bestMelds.addAll(fullSequences);
        ArrayList<CardList> bestUnMelded = new ArrayList<>();
        //for intermediate scoring, we show partial melds and sequences
        bestUnMelded.add(singles);
        bestUnMelded.add(wildCards);
        if (!isFinalScore) {
           bestUnMelded.addAll(partialMelds);
           bestUnMelded.addAll(partialSequences);
        }
        cardWrapper.setCardToDiscard(bestDiscard);
        cardWrapper.setBestMelds(bestMelds);
        cardWrapper.setBestUnMelded(bestUnMelded);
        cardWrapper.setScore(intermediateScore);
        //Log.d(Game.APP_TAG,"---exiting meldAndScoreUsingHeuristics");
        return intermediateScore;
    }//end int meldAndScoreUsingHeuristics

    //delegate method to look for card in ArrayList<CardList> (otherwise we'd have to override ArrayList)
    private static boolean contains (ArrayList<CardList> cardLists, Card card) {
        for (CardList cardList : cardLists)
            if (cardList.contains(card)) return true;
        return false;
    }

    private static int getScore(ArrayList<CardList> cardLists, Rank wildCardRank, boolean isFinalScore) {
        if (null == cardLists) return 0;
        int score=0;
        for (CardList cardList : cardLists) score += cardList.getScore(wildCardRank, isFinalScore);
        return score;
    }

    /* Hand evaluation - v1
    Consider all permutations (shouldn't be too expensive) and return -ve value of unmelded
    This is the sledgehammer approach (rather than trying to heuristically meld)
    Everything melded gives the maximum evaluation of 0
     */
    private int meldAndScoreUsingPermutations(Rank wildCardRank, boolean isFinalScore, Card addedCard, EvaluationWrapper cardWrapper) {
        //numCards = cards.size()+1 because we add the addedCard (discardPile or drawPile)
        final int numCards = cards.size()+1;

        //new copy of best found so far
        ArrayList<CardList> bestPermutationMelded=null;
        ArrayList<CardList> bestPermutationUnMelded=null;
        Card bestPermutationDiscard=null;
        int minUnMeldedScore=-1;

        if (addedCard == null) throw new RuntimeException("Hand.useDrawOrDiscard: addedCard is null");

        //v1: Consider all permutations and use early pruning (based on unMelded score)
        //Generate permutations - because all permutations are tested, we can just look for melds and sequences in order
        //Create a permuter for each available card slot - a copy of the hand + the added card being tested
        CardList[] cardLists = new CardList[numCards];

        for (int i = 0; i < numCards; i++) {
            cardLists[i] = new CardList(numCards);
            cardLists[i].addAll(this.cards);
            cardLists[i].add(addedCard); //this is the card we are testing (from the Discard or Draw pile)
        }
        //v2: generate the next permutation on each call to getNext
        int allPermutations=0;
        Permuter indexes = new Permuter(numCards);
        int[] cardListIdx = null;

        CardList testMeld = new CardList();
        Card testCard = null;
        ArrayList<CardList> permutationUnMelded = new ArrayList<>();
        ArrayList<CardList> permutationMelded = new ArrayList<>();

        //Loop over all valid permutations; the card at numCards is considered to be what we will discard
        for (cardListIdx = indexes.getNext(); cardListIdx != null; cardListIdx = indexes.getNext()) {
            allPermutations++;
            permutationUnMelded.clear();
            permutationMelded.clear();

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
            and require that the first card not be a wildcard (because such a permutation will turn up later
            and this eliminates the possibility of Wild-3-4
*/
            lastMeldedCard = cardLists[0].get(cardListIdx[0]);
            if (lastMeldedCard.isWildCard(wildCardRank)) continue;
            testMeld.clear();
            testMeld.add(lastMeldedCard);
            for (int iCard = 1; iCard < numCards - 1; iCard++) {
                /*
                * if this is a wildcard or lastMeldedCard is a wildcard
                * or cardLists[iCard].cards.get(cardListIdx[iCard]) melds in rank (any suit) or seq (same suit)
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

                testCard = cardLists[iCard].get(cardListIdx[iCard]);
                //this convoluted logic is because BOTH isRankMeld and isSequenceMeld could be true in a permutation like Q-Wild-Wild
                boolean testIsMelding = false;
                if (isRankMeld) {
                    if (testCard.isWildCard(wildCardRank))
                        testIsMelding = true; //any wildcard is fine for a Rank meld
                    else if (rankMeldRank == null) testIsMelding = true;
                    else if (testCard.getRank() == rankMeldRank) { //same Rank (e.g. Queens)
                        testIsMelding = true;
                        isSequenceMeld = false; //now we know it's a Rank meld
                    } else isRankMeld = false;
                }
                if (isSequenceMeld) {
                    if (sequenceMeldLastRank == null) testIsMelding = true;
                        //can't be a sequenceMeld if the lastRank is a King (nothing greater)
                    else if (sequenceMeldLastRank.getRankValue() >= Rank.KING.getRankValue())
                        isSequenceMeld = false;
                        //for a sequence meld, wildcard is fine *provided* sequenceMeldLastRank isn't at the K rank
                    else if (testCard.isWildCard(wildCardRank)) testIsMelding = true;
                    else if ((testCard.getSuit() == sequenceMeldSuit) && (testCard.getRank() == sequenceMeldLastRank.getNext())) {//same Suit and next in sequence
                        testIsMelding = true;
                        isRankMeld = false; //now we know it's a sequence meld
                    } else isSequenceMeld = false;
                }

                if (testIsMelding) testMeld.add(testCard);
                else {// testCard doesn't fit the testMeld - now check if testMeld has fewer than 3 cards then we can move it to unMelded
                    if (testMeld.size() >= 3)
                        permutationMelded.add((CardList)testMeld.clone());
                    else permutationUnMelded.add((CardList)testMeld.clone());
                    testMeld.clear();
                    testMeld.add(testCard);
                    isRankMeld = true;
                    isSequenceMeld = true;
                    //if the unmelded score is already bigger than previous minimum, we can move to next permutation
                    if ((minUnMeldedScore != -1) && (getScore(permutationUnMelded,wildCardRank, isFinalScore) >= minUnMeldedScore))
                        break;
                }
            }//for iCard=1..numCards (testing permutation)
            //anything left over in testMeld needs to be added to unMelded or Melded appropriately
            if (testMeld.size() < 3) permutationUnMelded.add((CardList)testMeld.clone());
            else permutationMelded.add((CardList)testMeld.clone());
            //reset the bestPermutation if this is a lower score
            //on normal round, Jokers and wild cards are evaluated as 0; in the last licks round they count full value
            if ((minUnMeldedScore == -1) || (getScore(permutationUnMelded,wildCardRank, isFinalScore) < minUnMeldedScore)) {
                minUnMeldedScore = getScore(permutationUnMelded,wildCardRank, isFinalScore);
                //use copy constructor because permutationMelded etc will continue to change on subsequent permutations
                bestPermutationMelded = new ArrayList<>(permutationMelded);
                bestPermutationUnMelded  = new ArrayList<>(permutationUnMelded);
                //discard is the right-most card in the permutations - needs to be a pointer to the actual card for later discarding
                bestPermutationDiscard = cardLists[numCards - 1].get(cardListIdx[numCards - 1]);
            }
        }//end for cardListIdx in all permutations of cards

        cardWrapper.setCardToDiscard(bestPermutationDiscard);
        cardWrapper.setBestMelds(bestPermutationMelded);
        cardWrapper.setBestUnMelded(bestPermutationUnMelded);
        int score = getScore(bestPermutationUnMelded,wildCardRank, isFinalScore);
        cardWrapper.setScore(score);
        //Log.d(Game.APP_TAG, "Examined "+allPermutations+" permutations");
        return score;
    }//end int meldAndScore


    protected int getScore(Rank wildCardRank, boolean isFinalScore){
        //should never be null, but if it is then score = 0
        if (unMelded == null) return 0;
        return getScore(unMelded, wildCardRank, isFinalScore);
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
        for (int iCard=0; iCard < numberToDeal; iCard++)
            cards.add(cardsToDeal.deal());
        return true;
    }

    //TODO: Combine these two methods depending on which is calling them
    String getUnMeldedString() {
        StringBuffer unMeldedString = new StringBuffer();
        for (CardList unMelds:unMelded) {
            unMeldedString.append(unMelds.getString());
        }
        return unMeldedString.toString();
    }

    String getMeldedString() {
        StringBuffer meldedString = new StringBuffer();
        for (CardList melds:this.melds) {
            meldedString.append(melds.getString());
        }
        return meldedString.toString();
    }



    void setMelds(ArrayList<CardList> melds) {
        this.melds = new ArrayList<>(melds);
    }

    void setUnMelded(ArrayList<CardList> unMelded) {
        this.unMelded = new ArrayList<>(unMelded);
    }
}//class Hand
