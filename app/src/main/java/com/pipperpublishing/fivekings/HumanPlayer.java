package com.pipperpublishing.fivekings;

import android.util.Log;

/**
 * Created by Jeffrey on 3/12/2015.
 * 3/15/2015    Moved discardFromHand here - actually does discard
 * 3/31/2015    Pushed takeTurn and logTurn here so that we don't have Human vs. Computer checks in Game
 * 10/12/2015   Pass drawnCard to animateHumanPickup to further separate logic from View
 *  * 10/18/2015   Change per player updateHandsAndCards to returning a showCards flag
 10/18/2015     Removed second click logic that would expose the hand if it was hidden
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
    final boolean initAndDealNewHand(final Deck.DrawPile drawPile,final Rank roundOf) {
        super.initAndDealNewHand(drawPile, roundOf);
        this.hand.calculateValueAndScore(false);
        return true;
    }

    //Human player uses this version which adds the addedCard (the card a human picked) and checks the melds
    final void addAndEvaluate(final boolean isFinalTurn, final Card addedCard) {
        addCardToHand(addedCard);
        checkMeldsAndEvaluate(isFinalTurn);
    }

    @Override
    final int checkMeldsAndEvaluate(final boolean isFinalTurn) {
        //sets the valuation of each meld (0 for a valid meld)
        //throws away the decomposition, because correct melding is the HumanPlayer's responsibility
        //This allows for later implementation of automatic melding
        return hand.checkMeldsAndEvaluate(isFinalTurn);
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


    /*----------------------------------------*/
    /* Game playing methods (moved from Game) */
    /*----------------------------------------*/
    @Override
    void prepareTurn(final FiveKings fKActivity) {
        turnState = TurnState.PLAY_TURN;
        //TODO:A Override a method which returns showComputerCards false or true depending on setting or human
        //This could then be in the base class prepareTurn

        fKActivity.updateHandsAndCards(showCards(fKActivity.getmGame().isShowComputerCards()), false);
        fKActivity.enableDrawDiscardClick(); //also animates piles and sets the hint
        this.getMiniHandLayout().getCardView().clearAnimation();
    }

    @Override
    //Human cards are always shown, unless the next player is also human and this is the end of the turn
    boolean showCards(final boolean isShowComputerCards) {
        return true;
    }

    @Override
    //TODO:A Bad smell from all those fKActivity calls
    void takeTurn(final FiveKings fKActivity, final Game.PileDecision drawOrDiscardPile, final Deck deck, final boolean isFinalTurn) {
        /* If you click on the Human hand a second time then takeTurn is called
        If a card was picked, then show the animation; if not, then show a hint
        */
        if (fKActivity.getmGame().getGameState() == GameState.HUMAN_PICKED_CARD) {
            final StringBuilder turnInfo = new StringBuilder(100);
            turnInfo.setLength(0);
            final String turnInfoFormat = fKActivity.getString(R.string.humanTurnInfo);

            logTurn(isFinalTurn);

            this.drawnCard = (drawOrDiscardPile == Game.PileDecision.DISCARD_PILE) ? deck.discardPile.deal() : deck.drawPile.deal();
            this.addAndEvaluate(isFinalTurn, this.drawnCard);
            turnInfo.append(String.format(turnInfoFormat, this.getName(), this.drawnCard.getCardString(),
                    (drawOrDiscardPile == Game.PileDecision.DISCARD_PILE) ? "Discard" : "Draw"));

            Log.d(Game.APP_TAG, turnInfo.toString());
            //don't meld, because it was done as part of evaluation

            fKActivity.getmGame().setGameState(GameState.HUMAN_READY_TO_DISCARD);
            //at this point the DiscardPile still shows the old card if you picked it
            //handles animating the card off the appropriate pile and making it appear in the hand
            fKActivity.animateHumanPickUp(drawOrDiscardPile,drawnCard);
        } else fKActivity.setShowHint(null, FiveKings.HandleHint.SHOW_HINT , true);
    }

    @Override
    void logTurn(final boolean isFinalTurn) {
        //Use final scoring (wild cards at full value) on last-licks turn (when a player has gone out)
        Log.d(Game.APP_TAG, "Player " + this.getName());
        String sValuationOrScore = (isFinalTurn) ? "Score=" : "Valuation=";
        Log.d(Game.APP_TAG, "before...... " + this.getMeldedString(true) + this.getPartialAndSingles(true) + " "
                + sValuationOrScore + this.getHandValueOrScore(isFinalTurn));
    }

}
