package com.example.jeffrey.fivekings;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/15/2015    Moved discardFromHand here - actually does discard
 *
 */
class HumanPlayer extends Player {
    HumanPlayer(final String name) {
        super(name);
    }
    HumanPlayer(final Player player) {
        super(player);
    }

    @Override
    final boolean initAndDealNewHand(final DrawAndDiscardPiles.DrawPile drawPile,final Rank roundOf, final Hand.MeldMethod method) {
        super.initAndDealNewHand(drawPile, roundOf, method);
        this.hand.calculateValueAndScore(false);
        return true;
    }

    //Human player uses this version which adds the addedCard (the card a human picked) and checks the melds
    final void addAndEvaluate(final boolean isFinalRound, final Card addedCard) {
        addCardToHand(addedCard);
        checkMeldsAndEvaluate(isFinalRound);
    }

    final int checkMeldsAndEvaluate(final boolean isFinalRound) {
        //sets the valuation of each meld (0 for a valid meld)
        for (Hand.Meld meld : hand.melds) meld.check(isFinalRound);
        this.hand.calculateValueAndScore(isFinalRound);
        return this.hand.getValueOrScore(isFinalRound);
    }

    private boolean addCardToHand(final Card card) {
        if (card != null) {
            checkHandSize();
            hand.add(card); //calls syncCardsAndMelds
        }
        return (card != null);
    }

    @Override
    final Card discardFromHand(final Card cardToDiscard) {
        if (cardToDiscard != null) {
            hand.discardFrom(cardToDiscard); //calls syncCardsAndMelds
            checkHandSize();
        }
        return cardToDiscard;
    }


    final boolean makeNewMeld(final Card card) {
        hand.makeNewMeld(card);
        return true;
    }
    final void addToMeld(final CardList meld, Card card) {
        ((Hand.Meld)meld).addTo(card);
    }

    final boolean isHuman() {return true;}
}
