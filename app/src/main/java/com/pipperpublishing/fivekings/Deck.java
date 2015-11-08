package com.pipperpublishing.fivekings;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.pipperpublishing.fivekings.view.FiveKings;

/**
 * Created by Jeffrey on 2/20/2015.
 * Encapsulates the draw and discard pile so that we can redeal the Draw pile when it runs out
 * 2/24/2015    Moved hand deal to here to encapsulate it - just deals as many cards as requested
 * 3/20/2015    Moved Deck here as well
 * 3/21/2015    Dealing CardList implements shuffle, deal, peekNext
 */
/**
 * Created by Jeffrey on 1/22/2015.
 * 2/3/2015 Removed suit NONE so simplified
 * 2/4/2015 Pulled Jokers into Card; also add two copies of each card
 * 2/14/2015 Pass application context so we can read drawables
 * 2/17/2015 Converted to a singleton
 * 3/13/2015    Impact of sub-classing Jokers
 *              Removed context
 * 3/19/2015    Made Deck the outer class of DrawPile and DiscardPile
 * 10/20/2015   Made discardPile and drawPile private and replaced calls appropriately
 * 11/8/2015    Lots of crashes from Deck parceling; make Deck parcelable
 *              To recreate, retrieve the singleton Deck using getInstance and then unparcel DiscardPile and DrawPile
 * 11/9/2015    Because you can only have one static CREATOR, it has to unmarshal DiscardPile and DrawPile itself
 *              Seems like there will be a new static instance of the deck on recreation so we should
 *              replace those cards with the saved ones
 */
/*TODO:A Eliminate the Deck and just use Drawpile? This would remove the post-parceling problem of the Deck referencing
  cards that are different instances from the ones in the Draw/Discard pile. However we would need to reconstitute
  the Deck from the Hands after a round

  Alternatively we could reconstitute the Deck after unparceling instead
    */
public class Deck extends DealingCardList implements Parcelable {
    private DiscardPile discardPile;
    private DrawPile drawPile;

    //private because we use singleton pattern to do static initialization
    private Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                this.add(new Card(suit, rank));
                this.add(new Card(suit, rank));
            }
        }//end for suits
        //add six Jokers
        for (int iJoker=1; iJoker<=6; iJoker++) this.add(new Joker());

        initDrawAndDiscardPile();
    }

    private static class DeckHolder {
        private static final Deck deck = new Deck();
    }

    static public Deck getInstance() {
        return DeckHolder.deck;
    }

    final void initDrawAndDiscardPile() {
        drawPile = new DrawPile(); //also copies deck to drawPile
        discardPile = new DiscardPile();
    }

    final void dealToDiscard() {
        discardPile.add(drawPile.deal());
    }

    final Card drawFromDiscardPile() {
        return discardPile.deal();
    }
    final Card drawFromDrawPile() {
        return drawPile.deal();
    }
    final CardList drawFromDrawPile(final int numToDeal) {
        return drawPile.deal(numToDeal);
    }
    final Card peekFromDiscardPile() {
        return discardPile.peekNext();
    }
    final Card peekFromDrawPile() {
        return drawPile.peekNext();
    }
    final public void addToDiscardPile(final Card discard) {
        discardPile.add(discard);
    }


    /* PARCELABLE read/write methods */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeList(drawPile);
        parcel.writeList(discardPile);
    }


    public static final Parcelable.Creator<Deck> CREATOR = new Parcelable.Creator<Deck>() {
        @Override
        public Deck createFromParcel(Parcel parcel) {
            //when we get static deck, it should also include drawPile and discardPile
            Deck staticDeck = Deck.getInstance();
            Log.d(FiveKings.APP_TAG, "Elements of deck: "+staticDeck.getString());
            /*at this point we have an unshuffled deck which has been copied to drawPile
             Now must replace the drawPile elements with the saved ones (which are completely
             new objects not referencing the ones in Deck) - that will be fixed on the following round
             */
            staticDeck.drawPile.clear();
            parcel.readList(staticDeck.drawPile, Card.class.getClassLoader());
            Log.d(FiveKings.APP_TAG, "Elements of drawpile: " + staticDeck.drawPile.getString());
            parcel.readList(staticDeck.discardPile, Card.class.getClassLoader());
            Log.d(FiveKings.APP_TAG, "Elements of discardpile: " + staticDeck.discardPile.getString());
            return staticDeck;
        }

        @Override
        public Deck[] newArray(int size) {
            return new Deck[size];
        }
    };


    /**
     * INNER CLASS DrawPile
     */
    private class DrawPile extends DealingCardList {
        //this constructor is for copying the Deck to the DrawPile
        private DrawPile(){
            super();
            this.clear();
            this.addAll(Deck.this);
        }

        @Override
        final public Card deal() {
            reDealIfEmpty();
            return super.deal();
        }

        @Override
        final public Card peekNext() {
            reDealIfEmpty();
            return super.peekNext();
        }

        CardList deal(final int numToDeal) {
            CardList returnedCards = new CardList(numToDeal);
            for (int iCard=0; iCard < numToDeal; iCard++) returnedCards.add(deal());
            return returnedCards;
        }

        private void reDealIfEmpty () {
            if (this.isEmpty()) {
                Log.d(FiveKings.APP_TAG, "DrawAndDiscardPiles.DrawPile.reDealIfEmpty: Redealing Discard Pile");
                this.clear();
                //I don't think we can do a new here, because this would be left pointing to the empty drawPile...or maybe not
                this.addAll(discardPile);
                this.shuffle();
                discardPile.clear();
                dealToDiscard();
            }
        }

        /* Constructor for parceling recreation */
        private DrawPile(Parcel parcel) {
            parcel.readList(this,Card.class.getClassLoader());
        }

    }//end DrawPile

    /* INNER CLASS DiscardPile */
    private class DiscardPile extends DealingCardList {
        private DiscardPile() {
            super();
            this.clear();
        }

        @Override
        //Override deal because the DiscardPile is actually LIFO
        final public Card deal() {
            if (this.isEmpty()) throw new RuntimeException(FiveKings.APP_TAG + "DiscardPile.deal: is empty");
            return this.remove(this.size() - 1);
        }

        @Override
        final public Card peekNext() {
            final Card nextCard;
            //DiscardPile can run out in the middle of a turn (when you draw the last card)
            //but it only affects display (a blank space where the pile was)
            if (this.isEmpty()) {
                Log.i(FiveKings.APP_TAG, "DiscardPile.peekNext: is empty");
                nextCard = null;
            }
            else nextCard = this.get(this.size() - 1);

            return nextCard;
        }

        /* Constructor for parceling recreation */
        private DiscardPile(Parcel parcel) {
            parcel.readList(this,Card.class.getClassLoader());
        }


    }//end DiscardPile


}
