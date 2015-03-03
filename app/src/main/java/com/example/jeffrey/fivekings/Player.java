package com.example.jeffrey.fivekings;

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
 * 2/17/2015 Don't use relativePosition for now
 * 2/17/2015    Added isHuman to control when you can click on piles
 *              Added checkHandSize() to make sure we have the right number of cards
 * 2/24/2015    Move Hand to be inner class
 *              Eliminated meldAndEvaluateAsIs, evaluateIfFirst by melding when we deal the hand
 * 2/26/2015    getUnMelded returns melds and singles unrolled into one CardList for display
 * 2/27/2015    Added addToMeld and makeNewMeld; include checking for dropping back onto itself
 * 2/28/2015    Added Meld definition to record valuation (tells us whether it's a valid meld)
 * 3/3/2015     Added static method to check if a CardList is a valid meld

 */
class Player {
    private boolean isHuman; //interact allowing clicking of Draw and Discard piles
    private String name;
    //dealer rotates every round, but relativePosition says where this player sits relative to others
    private int relativePosition;
    private int roundScore;
    private int cumulativeScore;
    private Hand hand;
    private Card bestDiscard;

    Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
        this.relativePosition = 0; //not meaningful for now
        initGame();
    }

    boolean initGame() {
        cumulativeScore = 0;
        return true;
    }

    boolean initAndDealNewHand(DrawAndDiscardPiles.DrawPile drawPile,Rank roundOf, boolean usePermutations) {
        this.roundScore = 0;
        this.bestDiscard = null;
        this.hand = new Hand(drawPile, roundOf);
        //TODO:A should sort
        // TODO:B AutoMeld if requested
        if (!this.isHuman) hand.meldAndEvaluate(usePermutations, false);
        return true;
    }

    static final Comparator<Player> playerComparatorByScoreDesc = new Comparator<Player>() {
        @Override
        public int compare(Player lhs, Player rhs) {
            return lhs.cumulativeScore - rhs.cumulativeScore;
        }
    };

    boolean isOut() {
        return (hand.getValueOrScore(true) == 0);
    }
    //Computer has to use this version which loops through possible discards to find the best one
    boolean findBestHand(boolean usePermutations, boolean isFinalScore, Card addedCard) {
        bestDiscard = null;
        Hand bestHand = this.hand;
        //Loop over possible discards, so that now addAndEvaluate just looks at your hand without added
        //in fact, each loop the actual hand is different (including hand.cards) and will be saved if best
        CardList cardsWithAdded = new CardList(hand.cards);
        cardsWithAdded.add(addedCard);
        for (Card disCard : cardsWithAdded) {
            CardList cards = new CardList(cardsWithAdded);
            cards.remove(disCard);
            Hand testHand = new Hand(this.hand, cards); //creates new hand with replaced cards
            if (testHand.meldAndEvaluate(usePermutations, isFinalScore) < bestHand.getValueOrScore(isFinalScore)) {
                bestDiscard = disCard;
                bestHand = testHand;
            }
        }//end for loop over possible discards
        // set a discard means we improved the current score
        if (bestDiscard != null) {
            this.hand = bestHand;
            return true;
        } else {
            //TODO:B: This is a hack
            bestDiscard = addedCard;
            return false;
        }
    }

    //this version just melds the current hand
    @Deprecated
    void meldAndEvaluate(boolean usePermutations, boolean isFinalScore) {
        hand.meldAndEvaluate(usePermutations, isFinalScore);
    }

    //Human player uses this version which adds the addedCard (the card a human picked) and checks the melds
    void addAndEvaluate(boolean usePermutations, boolean isFinalRound, Card addedCard) {
        {
            addCardToHand(addedCard);
            checkMeldsAndEvaluate(isFinalRound);
            return;
        }

/*
        CardList cardsWithAdded = new CardList(hand.cards);
        cardsWithAdded.add(addedCard);
        Hand testHand = new Hand(this.hand, cardsWithAdded);
        testHand.addAndEvaluate(usePermutations, isFinalScore);
        this.hand = testHand;
*/
    }

    int checkMeldsAndEvaluate(boolean isFinalScore) {
        int bestValuation=0;

        for (Meld meld : hand.melds) {
            //get the minimum score of this meld
            bestValuation += meld.check(isFinalScore);
        }//end for all melds
        return hand.calculateValue(isFinalScore);
    }


    Card discardFromHand(Card cardToDiscard) {
        if (cardToDiscard == null) return null;
        hand.discardFrom(cardToDiscard); //calls syncCardsAndMelds
        checkHandSize();
        return cardToDiscard;
    }

    boolean addCardToHand(Card card) {
        if (card == null) return false;
        checkHandSize();
        hand.add(card); //cal;s syncCardsAndMelds
        return true;
    }

    boolean makeNewMeld(Card card) {
        hand.makeNewMeld(card);
        return true;
    }
    void addToMeld(CardList meld, Card card) {
        ((Meld)meld).addTo(card);
    }

    private void checkHandSize() throws RuntimeException{
        if (!hand.checkSize()) throw new RuntimeException(Game.APP_TAG + "checkHandSize: Hand length is too short/long");
    }

    void update(String updatedName, boolean isHuman) {
        this.name = updatedName;
        this.isHuman = isHuman;
    }

    //Player GETTERS and SETTERS
    int addToCumulativeScore() {
        roundScore = hand.getValueOrScore(true);
        cumulativeScore += roundScore;
        return cumulativeScore;
    }

    String getName() {
        return name;
    }

    Card getDiscard() {
        return this.bestDiscard;
    }

    String getMeldedString(boolean withBraces){
        StringBuilder mMelds = new StringBuilder("Melds ");
        if (withBraces) mMelds.append("{");
        mMelds.append(hand.getMeldedString());
        if (withBraces) mMelds.append("} ");
        return mMelds.toString();
    }

    String getPartialAndSingles(boolean withBraces) {
        String unMelded = hand.getUnMeldedString();
        String singles = hand.getSinglesString();
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
   int getHandValueOrScore(boolean isFinalScore) {
        return hand.getValueOrScore(isFinalScore);
    }


    int getRoundScore() {
        return roundScore;
    }

    ArrayList<CardList> getHandMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getMelded());
        return combined;
    }

    //TODO:A: Not unrolling these right now (Human doesn't see this)
    //because otherwise we don't know what to add back to
    //have to eliminate "combined"
    ArrayList<CardList> getHandUnMelded() {
        ArrayList<CardList> combined = new ArrayList<>();
        combined.addAll(hand.getUnMelded());
        combined.add(hand.getSingles());
        return combined;
    }

    CardList getSingles() {
        return hand.getSingles();
    }

    int getCumulativeScore() {
        return cumulativeScore;
    }

    boolean isHuman() {
        return isHuman;
    }

    /** INNER CLASS: Hand
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
     * TODO:A Partial melds should only include one instance of each card (not 10C-10D and 10C-JC)
     TODO:B Account for the overlap between partialMelds and partialSequences
     * TODO:B Current problems:- need to check that partial and full melds don't overlap, also partial Melds and sequences
     * TODO:C Should be able to merge large chunks of Heuristics and Permutations

     */
    private class Hand {
        private final Rank roundOf; //how many cards you should have
        //all your cards, excluding what you picked up
        private CardList cards;
        // the cards melded onto the table
        private ArrayList<Meld> melds;
        // and those unmelded (partialMelds and singles which count against the score)
        private ArrayList<Meld> partialMelds;
        private Meld singles;
        private int intermediateValue; //looks at partial melds and sequences at adjusted value
        private int finalScore; //looks at full value of cards not in melds

        private Hand(Rank roundOf) {
            this.roundOf = roundOf;
            this.cards = new CardList(roundOf.getRankValue());
            this.melds = new ArrayList<>();
            this.partialMelds = new ArrayList<>();
            this.singles = new Meld(roundOf.getRankValue());

            this.intermediateValue = 0;
            this.finalScore =0;
        }

        //deal and return hand
        private Hand(DrawAndDiscardPiles.DrawPile drawPile, Rank roundOf) {
            this(roundOf);
            if (drawPile == null) return;
            cards = drawPile.deal(roundOf.getRankValue());
            singles = new Meld(cards); //only for human really
        }

        //minimal constructor - copies and sets the cards (used in trying out different discards)
        //note that it doesn't copy other values which are not relevant
        private Hand(Hand hand, CardList cards) {
            this(hand.roundOf);
            this.cards = cards;
        }

        //used by Computer (or Human with [Meld] button)
        // the Hand this is being called on is the test hand, so we set here the member variables
        private int meldAndEvaluate(boolean usePermutations, boolean isFinalRound) {
            int valuation;
            if (usePermutations) meldBestUsingPermutations(isFinalRound);
            else meldUsingHeuristics(isFinalRound);
            return calculateValue(isFinalRound);
        }


        /* v2 values keeping pairs and potential sequences, because otherwise we'll throw away a King from a pair of Kings
    Evaluate using
        1. Maximize melds: 3 or more rank or sequence melds - maximize this
        2. Maximize partialMelds: pairs and broken sequences (for now just +/-1) - want to maximize number of cards in this
        3. Minimize unMelded: remaining singles - minimize the score of this by throwing away the
    In the final scoring, calculate partialMelds and unMelded value
    Evaluation accounts for hand potential, but Scoring is just what's left after melding
     */
        private boolean meldUsingHeuristics(boolean isFinalScore) {
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
            Meld rankMatch = new Meld(numCards);
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
                        addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalScore);
                        rankMatch.clear();
                        rankMatch.add(card);
                    }//end-if same rank
                }
            }
            addWildcardsAndRecord(fullRankMelds,partialRankMelds,rankMatch,wildCards,isFinalScore);

            //Sequences - for now full sequences (3*-4*-5*) or broken pairs (e.g. 3*-5*) in partialSequences list
            //Log.d(Game.APP_TAG,"---find matches in each sequence");
            CardList cardsSortedBySuit = new CardList(nonWildCards);
            Collections.sort(cardsSortedBySuit, Card.cardComparatorSuitFirst); //e.g. 3S,5S,JS,6H,8H...
            Meld sequenceMatch = new Meld(numCards);
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
            for (Iterator<Meld> iterator = partialRankMelds.iterator(); iterator.hasNext(); ) {
                CardList rankMeld = iterator.next();
                if (contains(fullSequences, rankMeld)) iterator.remove();
            }

            //If we still have wildcards, meld a partial and then see if it can be expanded to full
            Meld meldOfSingles = new Meld(numCards);
            for (Card card : cardsSortedByRankDesc) {
                if (wildCards.isEmpty()) break;
                if (!contains(fullRankMelds,card) && !contains(fullSequences,card)) {
                    meldOfSingles.clear();
                    meldOfSingles.add(card);
                    meldOfSingles.add(wildCards.get(0));
                    wildCards.remove(0);
                    addWildcardsAndRecord(fullRankMelds,partialRankMelds,meldOfSingles,wildCards,isFinalScore);
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
            if (isFinalScore) {
                //partialMelds should always already be clear (because checked earlier)
                if (!partialRankMelds.isEmpty() || !partialSequences.isEmpty())
                    Log.e(Game.APP_TAG, "meldUsingHeuristics: partialMelds not empty in final scoring");
                partialMelds.clear();
            }else {
                partialMelds = new ArrayList<>(partialRankMelds);
                partialMelds.addAll(partialSequences);
            }
            //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
            singles = new Meld(nonWildCards);
            for (Card card:nonWildCards) {
                if (contains(melds,card)) singles.remove(card);
                if (!isFinalScore && contains(partialMelds,card)) singles.remove(card);
            }

            //don't need to find discard (we are looping over possible discards if this is a Computer turn)
            //and scoring is now done in the caller
            //Log.d(Game.APP_TAG,"---exiting meldUsingHeuristics");
            return true;
        }//end int meldUsingHeuristics

        //save test if full; see if you can pad it to a full with wildcards and keep/discard if not
        private boolean addWildcardsAndRecord(ArrayList<Meld> fulls, ArrayList<Meld> partials, Meld test, CardList wildCards, boolean isFinalScore) {
            boolean madePartialIntoFull=false;
            if (test.size()>=3) fulls.add((Meld)test.clone());
            else if (2 == test.size()) {
                if (wildCards.isEmpty()) {
                    if (!isFinalScore) partials.add((Meld) test.clone());
                }
                else {
                    test.add(wildCards.get(0));
                    wildCards.remove(0);
                    fulls.add((Meld) test.clone());
                    madePartialIntoFull = true;
                }
            }
            return madePartialIntoFull;
        }


        /* PERMUTATIONS - v1
            Consider all permutations (shouldn't be too expensive) and return value of unmelded
            This is the sledgehammer approach - we use this until it gets too slow and then switch to heuristics
            Everything melded gives the maximum evaluation of 0
             */
        private int meldBestUsingPermutations(boolean isFinalRound) {
            final int numCards = this.cards.size();
            final Rank wildCardRank = this.roundOf;

            Meld cardsCopy = new Meld(cards);

            //new copy of best found so far
            ArrayList<Meld> bestPermutationMelded=new ArrayList<>(numCards);
            ArrayList<Meld> bestPermutationUnMelded=new ArrayList<>(numCards);
            CardList bestPermutationSingles=new CardList(numCards);

            cardsCopy.check(isFinalRound, bestPermutationMelded, bestPermutationUnMelded, bestPermutationSingles);
            //set member variables
            this.melds = bestPermutationMelded;
            this.partialMelds = bestPermutationUnMelded;

            //Clean up what is now left in singles  - for final scoring we put wildcards and partial melds/sequences into singles
            singles = new Meld(cardsCopy);
            for (Card card:cardsCopy) {
                if (contains(bestPermutationMelded,card) || (!isFinalRound && contains(partialMelds,card))){
                    singles.remove(card);
                } else if (card.isWildCard(wildCardRank)) Log.e(Game.APP_TAG,"meldBestUsingPermutations: Unmelded wildcards remaining");
            }
            if (isFinalRound) partialMelds.clear();

            intermediateValue = calculateValue(isFinalRound);

            return intermediateValue;
        }//end int meldBestUsingPermutations


        //this version calculates for the whole hand
        //TODO:A: Merge versions of calculateMeldsValue so there's one place we do calculations
        private int calculateValue(boolean isFinalRound) {
            float handValue = 0.0f;
            finalScore =0;

            intermediateValue = calculateMeldsValue(this.roundOf, false, this.cards, this.melds, this.partialMelds, this.singles );
            finalScore = calculateMeldsValue(this.roundOf, true, this.cards, this.melds, this.partialMelds, this.singles );
            return isFinalRound ? finalScore : intermediateValue;
        }

        boolean makeNewMeld(Card card) {
            Meld newMeld = new Meld();
            newMeld.addTo(card);
            melds.add(newMeld);
            return true;
        }

        Card discardFrom(Card discardedCard){
            cards.remove(discardedCard);
            syncCardsAndMelds();
            return discardedCard;
        }

        void add(Card addedCard) {
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

        private boolean checkSize() {
            return roundOf.getRankValue() == cards.size();
        }

        //Hand GETTERS
        private int getValueOrScore(boolean isFinalScore){
            if (isFinalScore) return this.finalScore;
            else return this.intermediateValue;
        }

        private String getMeldedString() { return getString(this.melds);}

        private String getUnMeldedString() {return getString(this.partialMelds);}

        private String getSinglesString() {
            return singles.getString();
        }

        private ArrayList<Meld> getMelded() {
            return melds;
        }

        private ArrayList<Meld> getUnMelded() {
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

        private CardList getSingles() {return singles;}

        @Deprecated
            //now being done in syncCardsAndMelds
        CardList getSortedSingles() {
            CardList sortedCards = new CardList(singles);
            Collections.sort(sortedCards, Card.cardComparatorRankFirstDesc);
            return sortedCards;
        }


        private Rank getRoundOf() {
            return roundOf;
        }
    }//private class Hand

    /* INNER CLASS Meld */
    //TODO:B Seems like we should be able to make this part of Hand
    //or maybe should just separate it out
    private class Meld extends CardList {
        private int valuation; //0 if this is a valid meld
        private boolean needToCheck; //a card has been added or removed so we need to recheck
        private boolean isValidMeld=false;

        private Meld() {
            this(Game.MAX_CARDS);
        }

        private Meld(CardList cl) {
            this(cl.size());
            this.addAll(cl);
        }

        private Meld(int numCards) {
            super(numCards);
            this.valuation = 0;
            this.isValidMeld = false;
            this.needToCheck = true;
        }

        //find the best arrangement of this meld and break if we get zero
        //also sorts the meld rank first (which should help if it's a rank meld)
        //if it's not a single meld, will return the breakdown
        //it's the caller's responsibility to not call this with too long a "meld" (since permutation time goes up geometrically)
        //returns best arrangement and returns partial meld and singles components if it can't be fully melded
        private int check(boolean isFinalRound, ArrayList<Meld> bestPermutationMelds, ArrayList<Meld> bestPermutationPartialMelds, CardList bestPermutationSingles) {
            int numCards = this.size();
            Rank wildCardRank = hand.roundOf;
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
                Meld testMeld = new Meld();
                Card testCard = null;
                ArrayList<Meld> permutationPartialMelds = new ArrayList<>();
                ArrayList<Meld> permutationMelds = new ArrayList<>();
                CardList permutationSingles = new CardList();


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
                            permutationMelds.add((Meld) testMeld.clone());
                        else if (2 == testMeld.size())
                            permutationPartialMelds.add((Meld) testMeld.clone());
                        else
                            permutationSingles.add(testMeld.get(0));
                        testMeld.clear();
                        testMeld.add(testCard);
                        isRankMeld = true;
                        isSequenceMeld = true;
                        //Don't do pruning at this point any more to allow scoring algorithm to be more complicated
                    }
                }//for iCard=1..numCards (testing permutation)
                //anything left over in testMeld needs to be added to unMelded or Melded appropriately
                if (testMeld.size() >= 3)
                    permutationMelds.add((Meld) testMeld.clone());
                else if (2 == testMeld.size())
                    permutationPartialMelds.add((Meld) testMeld.clone());
                else if (1 == testMeld.size())
                    permutationSingles.add(testMeld.get(0));

                //reset the bestPermutation if this is a lower score
                //on normal round, Jokers and wild cards are evaluated as 0; in the last licks round they count full value
                if ((bestValuation == -1) || (calculateMeldsValue(wildCardRank, isFinalRound, this, null, permutationPartialMelds, permutationSingles) < bestValuation)) {
                    bestValuation = calculateMeldsValue(wildCardRank, isFinalRound, this, null, permutationPartialMelds, permutationSingles);
                    //use copy constructor because permutationMelds etc will continue to change on subsequent permutations
                    bestPermutationMelds.clear(); bestPermutationMelds.addAll(permutationMelds);
                    bestPermutationPartialMelds.clear(); bestPermutationPartialMelds.addAll(permutationPartialMelds);
                    bestPermutationSingles.clear(); bestPermutationSingles.addAll(permutationSingles);
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

            //all the bestPermutationMelds are known to be valid
            for (Meld meld : bestPermutationMelds) meld.setCheckedAndValid();
            this.valuation = bestValuation;
            this.needToCheck = false;
            //single valid meld?
            this.isValidMeld = (bestPermutationPartialMelds.isEmpty()) && (bestPermutationSingles.isEmpty());

            return bestValuation;
        }//end int check

        private int check(boolean isFinalScore) {
            //new copy of best found so far - not used here
            ArrayList<Meld> bestPermutationMelded= new ArrayList<>();
            ArrayList<Meld> bestPermutationUnMelded= new ArrayList<>();
            CardList bestPermutationSingles= new CardList();
            return check(isFinalScore, bestPermutationMelded, bestPermutationUnMelded, bestPermutationSingles);
        }

        //add to a new meld just created, or to an existing meld and remove from other melds, partial melds, or singles
        // allows for dragging and dropping back onto itself
        private void addTo(Card card) {
            //remove from unMelded, singles, or other melds
            for (CardList cl : hand.melds) cl.remove(card);
            for (CardList cl : hand.partialMelds) cl.remove(card);
            hand.singles.remove(card);
            if (!this.contains(card)) this.add(card); //may remove and then re-add
            this.needToCheck = true;
        }

        private void setCheckedAndValid() {
            this.isValidMeld = true;
            this.needToCheck = false;
            this.valuation = 0;
        }

    }//end class Meld

    /* STATIC METHODS */
    static boolean isValidMeld(CardList cl) {
        if (!(cl instanceof Meld)) return false;
        return ((Meld)cl).isValidMeld;
    }

    private static String getString(ArrayList<Meld> meldsOrUnMelds) {
        StringBuilder meldedString = new StringBuilder();
        if (null != meldsOrUnMelds) {
            for (CardList melds : meldsOrUnMelds) {
                meldedString.append(melds.getString());
            }
        }
        return meldedString.toString();
    }


    //delegate method to look for card in ArrayList<CardList> (otherwise we'd have to override ArrayList)
    private static boolean contains (ArrayList<Meld> melds, Card card) {
        if (null == melds) return false;
        for (Meld meld : melds)
            if (meld.contains(card)) return true;
        return false;
    }
    private static boolean contains (ArrayList<Meld> containingMelds, CardList cardList) {
        if (null == containingMelds) return false;
        for (Card card:cardList) if (contains(containingMelds,card)) return true;
        return false;
    }

    private static void sortAndSeparateWildCards(Rank wildCardRank,CardList cards, CardList nonWildCards, CardList wildCards) {
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

    /*    Score and sort cardsWithAdded - valuing partial melds and sequences
    Intermediate scoring:
    cardValue = face value if not in full-meld, full-sequence, or wildcard
                  x 1/2  face value if in partialMeld
    Final scoring: Discard all partial melds/sequences and convert to singles (already done)
                    But also record this at all times for final round scoring
    */
    //TODO:B: Rewrite this so it doesn't use cards and can be called statically
    private static int calculateMeldsValue(Rank wildCardRank, boolean isFinalRound, CardList cards, ArrayList<Meld> melds,  ArrayList<Meld> partialMelds, CardList singles) {
        float intermediateValue = 0.0f;
        int finalScore =0;
        for (Card card : cards) {
            float cardValue = card.getScore(wildCardRank, isFinalRound);
            //if Final, then singles contains everything in partialMelds at full value
            //otherwise reduce it by 1/2 if partially melded
            if (contains(partialMelds,card) || singles.contains(card)) {
                finalScore += cardValue;
                if (contains(partialMelds, card)) intermediateValue += 0.5 * cardValue;
                else intermediateValue += cardValue;
            }
        }
        //"Melds" can actually be attempts by human so need to be added (have previously been evaluated by Meld.check)
        if (melds != null) {
            for (Meld meld : melds) {
                finalScore += meld.valuation;
                intermediateValue += meld.valuation;
            }
        }
        return isFinalRound ? finalScore : (int)intermediateValue;
    }


}
