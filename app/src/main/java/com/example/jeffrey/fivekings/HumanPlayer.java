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
    final boolean initAndDealNewHand(final Deck.DrawPile drawPile,final Rank roundOf, final MeldedCardList.MeldMethod method) {
        super.initAndDealNewHand(drawPile, roundOf, method);
        this.hand.calculateValueAndScore(false);
        return true;
    }

    //Human player uses this version which adds the addedCard (the card a human picked) and checks the melds
    final void addAndEvaluate(final boolean isFinalRound, final Card addedCard) {
        addCardToHand(addedCard);
        checkMeldsAndEvaluate(isFinalRound);
    }

    @Override
    final int checkMeldsAndEvaluate(final boolean isFinalRound) {
        //sets the valuation of each meld (0 for a valid meld)
        //throws away the decomposition, because correct melding is the HumanPlayer's responsibility
        //This allows for later implementation of automatic melding
        return hand.checkMeldsAndEvaluate(isFinalRound);
    }

    private boolean addCardToHand(final Card card) {
        if (card != null) {
            checkHandSize();
            hand.addAndSync(card); //calls syncCardsAndMelds
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
        ((MeldedCardList.Meld)meld).addTo(card);
    }

    @Override
    final boolean isHuman() {return true;}
}
