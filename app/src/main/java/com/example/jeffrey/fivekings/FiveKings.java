package com.example.jeffrey.fivekings;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jeffrey.fivekings.util.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* HISTORY
    2/10/2015   Display each round every time Play button is pressed
    2/12/2015   Display each player's hands and top of Discard Pile
    2/15/2015   Added GameState to provide State after each button press
    2/16/2015   Changed LayeredDrawables to FrameLayouts so they can be clicked
    2/17/2015   Fixed end-of-round button to be Show scores. Last button now says [New Game] and calls Game constructor
    2/17/2015   turnInfo line now uses parameterized string resource
    2/17/2015   Converted StringBuffer to StringBuilder throughout
    2/21/2015   Creating basic elements of Draw-and-Drop listener (for discarding)
    2/23/2015   Trigger drag on simple click of a card
                Set DiscardPile clickable or not (disabled means you can't drop on it)
    2/26/2015   Change GameInfo and other on-screen text to Toasts
                Don't show computer cards except in final round after turn
                Removed Toast/display for current round (have lots of hints in UI already)
                Moved UI element initialization into onCreate
                Create nested melds (RelativeLayouts) inside mCurrent Melds - findViewByIndex now has an extra loop
    3/1/2015    Use onTouch to start Drag rather than OnLongClick
    3/3/2015    Put dashed line border around melds; use invisible background to preset correct width
                Note that Layouts wrap content around child views *without* Translation (Translation does not adjust parent view width)
    3/3/2015    Set CARD_OFFSET_RATIO to be 20% of INTRINSIC_WIDTH (preset in CardView)
    3/3/2015    Vibrate Discard and Draw Piles to show you are meant to draw - replace InfoLine with Toast on first few rounds
    3/3/2015    Animate pick from Draw or Discard Piles
                Comment out infoLine - add back in information as needed
                Enable/Disable Draw and Discard Piles just sets clickable or not
    3/5/2015    Improved animation to show cards picked for humans and faulty animation for Computer
    3/6/2015    Switch to using Animator for Computer Discard
    3/6/2015    First code for saving instance State when app gets Stopped - NOT WORKING
                On LongClick of Play button, pop up a confirmation dialog before starting a new game
    3/6/2015    0.2.10:
                Disable Play Button during Computer Animation so you can't click it twice
                Reset Scores header correctly to blank
                First version of dealing animation at the beginning of each round
                For now we eliminate it, but next step would be to label each hand and then scale it to 100% on each hand
    3/6/2015    Add dealt cards to piles section and label with name and scores
    3/7/2015    Create Relative Layout for Player Hands, including card back, name, round score, and cumulative score
    3/10/2015   Save PlayerLayout in Player class
                Fixed dealing animation by moving duration to individual objectAnimator steps
                Computer Draw animation goes from pile to mini-hand
                Convert animateHuman... to grey out PlayerHand while you look at it
    3/11/2015   Dealt hands were starting off too small (base 75% card size is now considered 1.0 for scaleX/Y)
                Don't show Computer card backs
    3/11/2015   Don't show roundScore until final round when you've had your final play
    3/11/2015   v0.3.02: Add Delete player option in Edit dialog
                Add a transparent card with a plus on it next to the final player
    3/12/2015   Impact of subclassing of Player->HumanPlayer and ComputerPlayer
*/

public class FiveKings extends Activity {
    static final float CARD_OFFSET_RATIO = 0.18f;
    static final float MELD_OFFSET_RATIO = 0.3f;
    //static final int TOAST_X_OFFSET = 20;
    //static final int TOAST_Y_OFFSET = +600;
    static final Rank HELP_ROUNDS=Rank.THREE;
    static final float ALMOST_TRANSPARENT_ALPHA=0.1f;

    private static final int LONG_ANIMATION=2000;
    private static final int MEDIUM_ANIMATION=500;
    private static final int SHORT_ANIMATION=100;

    // Dynamic elements of the interface
    private Game mGame=null;
    private Button mPlayButton;
    private TextView mCurrentRound;
    private TableLayout mScoreDetail;
    private View mScoreDetailView;
    private Toast mGameInfoToast;
    private TableRow[] mScoreDetailRow = new TableRow[Game.MAX_PLAYERS];
    private TextView[] mPlayerNametv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerScoretv = new TextView[Game.MAX_PLAYERS];
    private TextView[] mPlayerCumScoretv = new TextView[Game.MAX_PLAYERS];
    private CheckBox[] mPlayerIsHuman = new CheckBox[Game.MAX_PLAYERS];
    private TextView[] mPlayerIndex = new TextView[Game.MAX_PLAYERS];
    private CardView mDiscardPile;
    private CardView mDrawPile;
    private RelativeLayout mCurrentCards;
    private RelativeLayout mCurrentMelds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_five_kings);

        //Initialize the other elements of the UI (moved here from playGameClicked
        //set up the playGameClicked handler for the Button
        //set the OnClickListener for the button - for some reason this doesn't reliably work from XML
        mPlayButton = (Button)findViewById(R.id.Play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playGameClicked();
            }
        });
        //If you longClick, can start a new Game but pops up a confirmation dialog
        mPlayButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.newGame)
                        .setMessage(R.string.areYouSure)
                        .setPositiveButton(R.string.newGame, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(), R.string.startingGame, Toast.LENGTH_LONG).show();
                                mPlayButton.setText(getText(R.string.newGame));
                                if (mGame != null) mGame.setGameState(GameState.NEW_GAME);
                                playGameClicked();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });

        mCurrentRound = (TextView)findViewById(R.id.current_round);
        mGameInfoToast = Toast.makeText(this, R.string.blank,Toast.LENGTH_SHORT);
        mDiscardPile = (CardView)findViewById(R.id.discardPile);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDrawOrDiscard(Game.PileDecision.DISCARD_PILE);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());
        mDrawPile = (CardView)findViewById(R.id.drawPile);
        mDrawPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickedDrawOrDiscard(Game.PileDecision.DRAW_PILE);
            }
        });
        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

        PlayerLayout addPlayerLayout = new PlayerLayout(this);
        final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);
        fullScreenContent.addView(addPlayerLayout);

        disableDrawDiscard();

    }//end onCreate

    static final String ROUND_OF="ROUND_OF";


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {


        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

 /*   EVENT HANDLERS*/
    //Event handler for [Save] in add player dialog
    public void addEditPlayerClicked(final String playerName, final boolean isHuman, final boolean addingFlag, final int iPlayerUpdate) {
         //if we add a player, we need to remove and re-add the hand layouts
         if (addingFlag) {
             if ((mGame != null) && (mGame.getRoundOf() != null) && (mGame.getRoundOf() != Rank.getLowestRank())) {
                 Log.e(Game.APP_TAG,"Can't add players after Round of 3's");
                 return;
             }
             if (mGame != null) mGame.addPlayer(playerName, isHuman);
             removeAndAddPlayerHands(null);
         }
         else {
             mGame.updatePlayer(playerName, isHuman, iPlayerUpdate);
             Player.updatePlayerHands(mGame.getPlayers(), mGame.getCurrentPlayer());
         }
    }
    public void deletePlayerClicked(final int iPlayerToDelete) {
        //pop up an alert to verify
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.confirmDelete)
                .setMessage(resFormat(R.string.areYouSureDelete, mGame.getPlayer(iPlayerToDelete).getName()))
                .setPositiveButton(R.string.yesDelete, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //have to be careful of order and save the layout to delete from the screen
                        PlayerLayout deletedPlayerLayout = mGame.getPlayer(iPlayerToDelete).getPlayerLayout();
                        mGame.deletePlayer(iPlayerToDelete);
                        removeAndAddPlayerHands(deletedPlayerLayout);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    //the Event handler for button presses on [Play]
    private void playGameClicked(){
        setWidgetsByGameState();

        if (null== mGame) {
            mGame = new Game(this); //also sets gameState=ROUND_START
            this.mCurrentRound.setText(resFormat(R.string.current_round,mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            showAddPlayers();
        }

        //User pressed [New Game] button (currently by long-pressing Play)
        else if (GameState.NEW_GAME == mGame.getGameState()) {
            mGame.init();
            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            showAddPlayers();
        }

        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            Player.resetAndUpdatePlayerHands(mGame.getPlayers(), mGame.getCurrentPlayer());
            //TODO:A When do we pass variables vs. just reference in the method??
            animateDealing(mGame.getRoundOf().getRankValue(), mGame.getPlayers());

            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer().getName()));
            //Show blank screen with discard pile showing - now in onAnimationEnd
        }

        else if (GameState.TAKE_COMPUTER_TURN == mGame.getGameState()) {
            StringBuilder turnInfo = new StringBuilder(100);

            turnInfo.setLength(0);
            Game.PileDecision pickedFrom = mGame.takeComputerTurn(getText(R.string.computerTurnInfo).toString(), turnInfo);
            showHint(turnInfo.toString());

            //at this point the DiscardPile still shows the old card
            //animate... also calls syncDisplay and checkEndRound() in the OnAnimationEnd listener
            animateComputerPickUpAndDiscard(mGame.getCurrentPlayerIndex(), pickedFrom); // also calls syncDiscardCardsMelds
        }

        else if (GameState.TURN_START == mGame.getGameState()) {
            mGame.rotatePlayer();
            syncDisplay();

            if (mGame.getCurrentPlayer().isHuman()) {
                showHint(resFormat(R.string.yourCardsAndMelds, mGame.getCurrentPlayer().getName()));
                disablePlayButton();
                enableDrawDiscard();
                animatePiles();
                mGame.setGameState(GameState.TAKE_HUMAN_TURN);
                setWidgetsByGameState();
            }
            else {
                this.mPlayButton.setText(resFormat(R.string.takePlayerTurn, mGame.getCurrentPlayer().getName()));
                mGame.setGameState(GameState.TAKE_COMPUTER_TURN);
            }
        }//end TURN_START

        else if (GameState.ROUND_END == mGame.getGameState()) {
            mGame.endRound();
            showHint(resFormat(R.string.displayScores, null));
            Player.updatePlayerHands(mGame.getPlayers(), mGame.getCurrentPlayer());
            if (GameState.ROUND_START == mGame.getGameState()) this.mPlayButton.setText(resFormat(R.string.nextRound,mGame.getRoundOf().getString()));
        }

        else if (GameState.GAME_END == mGame.getGameState()) {
            this.mPlayButton.setText(getText(R.string.newGame));
            mGame.logFinalScores();
            mGame.setGameState(GameState.NEW_GAME);
        }
    }


    private void clickedDrawOrDiscard(final Game.PileDecision drawOrDiscardPile) {
        StringBuilder turnInfo = new StringBuilder(100);
        mGame.setGameState(GameState.HUMAN_PICKED_CARD);
        setWidgetsByGameState();

        turnInfo.setLength(0);
        //also sets GameState to END_HUMAN_TURN
        mGame.takeHumanTurn(getText(R.string.humanTurnInfo).toString(), turnInfo, drawOrDiscardPile);
        //at this point the DiscardPile still shows the old card if you picked it
        //handles animating the card off the appropriate pile and making it appear in the hand
        animateHumanPickUp(drawOrDiscardPile);
        showHint(turnInfo.toString());
    }


    //Event Handler for clicks on cards or melds - called from DiscardPile drag listener
    boolean discardedCard(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        setWidgetsByGameState();
        //TODO:A Code in here could be merged with that in onAnimationEnd for computer discard
        //including moving setDiscard out of findBestHand up to this level
        //does it need to be set earlier for the Computer?
        if (GameState.END_HUMAN_TURN == mGame.getGameState()) {
            if (!mGame.getCurrentPlayer().isHuman()) throw new RuntimeException("discardedCard: player is not Human");
            mGame.getCurrentPlayer().setHandDiscard(foundCardView.getCard());
            if (mGame.endTurn() != null) animateRoundScore(mGame.getCurrentPlayer());
            syncDisplay();
            mGame.checkEndRound();
            if (GameState.ROUND_END == mGame.getGameState()) mPlayButton.setText(getText(R.string.addScores));
            else mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer().getName()));
        }
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    //to add to a existing meld, drag to an existing meld
    boolean makeNewMeld(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //create trialMeld (one card at a time for now)

        ((HumanPlayer)mGame.getCurrentPlayer()).makeNewMeld(foundCardView.getCard());
        syncDisplay();
        return true;
    }

    //Don't test for valid meld; that is done with evaluateMelds
    boolean addToMeld(final CardList meld, final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //add to existing meld

        ((HumanPlayer)mGame.getCurrentPlayer()).addToMeld(meld, foundCardView.getCard());
        syncDisplay();
        return true;
    }



    /* COMMON METHODS FOR MANAGING UI ELEMENTS */
    private void enablePlayButton() {
        if (this.mPlayButton != null) {
            this.mPlayButton.setEnabled(true);
            this.mPlayButton.setVisibility(View.VISIBLE);
        }
    }
    private void disablePlayButton() {
        if (this.mPlayButton != null) {
            this.mPlayButton.setEnabled(false);
            this.mPlayButton.setVisibility(View.INVISIBLE);
        }
    }
    //just prevent clicking, but for animations we need the buttons to be enabled
    private void enableDrawDiscard() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setEnabled(true);
            this.mDrawPile.setClickable(true);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setEnabled(true);
            this.mDiscardPile.setClickable(true);
        }
    }
    private void disableDrawDiscard() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setClickable(false);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setClickable(false);
        }
    }
    private void setWidgetsByGameState() {
        if (mGame == null) return;
        switch (mGame.getGameState()) {
            case TAKE_COMPUTER_TURN:
                disablePlayButton();
                disableDrawDiscard();
                break;
            case TAKE_HUMAN_TURN:
                disablePlayButton();
                enableDrawDiscard();
                mGame.getCurrentPlayer().getPlayerLayout().setGreyedOut(true);
                break;
            case HUMAN_PICKED_CARD:
                disablePlayButton();
                disableDrawDiscard();
                break;
            case END_HUMAN_TURN:
                enablePlayButton();
                disableDrawDiscard();
                mGame.getCurrentPlayer().getPlayerLayout().setGreyedOut(false);
                break;

            case NEW_GAME:
            case ROUND_START:
            case TURN_START:
            case ROUND_END:
            case GAME_END:
            default:
                enablePlayButton();
                disableDrawDiscard();
                break;
        }
    }

    /* ANIMATION ROUTINES */
    private void animateDealing(final int numCards, final List<Player> players) {

        final int numPlayers = players.size();
        final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);
        mDrawPile.setVisibility(View.INVISIBLE);
        mDiscardPile.setVisibility(View.INVISIBLE);

        //the template card that animates from the center point to the hands
        final CardView deck = new CardView(this, CardView.sBitmapCardBack);
        final CardView dealtCardView = new CardView(this, CardView.sBitmapCardBack);
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams((int)(CardView.INTRINSIC_WIDTH*PlayerLayout.DECK_SCALING),
                                                                                    (int)(CardView.INTRINSIC_HEIGHT*PlayerLayout.DECK_SCALING));
        pileLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        deck.setLayoutParams(pileLp);
        fullScreenContent.addView(deck);

        final AnimatorSet dealSet = new AnimatorSet();
        dealSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                fullScreenContent.removeView(deck);
                fullScreenContent.removeView(dealtCardView);
                mDrawPile.setVisibility(View.VISIBLE);
                mDiscardPile.setVisibility(View.VISIBLE);
                showDiscardPileCard();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });

        Animator dealtCardAnimator = AnimatorInflater.loadAnimator(this, R.animator.deal_from_drawpile);
        Animator lastDealtCardAnimator = null;
        ObjectAnimator alphaAnimator;

        //this is the card we animate
        dealtCardView.setLayoutParams(pileLp);
        fullScreenContent.addView(dealtCardView);
        for (int iCard=0; iCard<numCards; iCard++) {
            for (int iPlayer = 0; iPlayer < numPlayers; iPlayer++) {
                dealtCardAnimator = dealtCardAnimator.clone();
                dealtCardAnimator.setTarget(dealtCardView);
                //offsets where the cards are dealt according to player
                ObjectAnimator playerOffsetXAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationX", mGame.getPlayer(iPlayer).getPlayerLayout().getTranslationX());
                ObjectAnimator playerOffsetYAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationY", mGame.getPlayer(iPlayer).getPlayerLayout().getTranslationY());
                if (lastDealtCardAnimator == null) dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator);
                else dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator).after(lastDealtCardAnimator);
                //if this is the first card, then add an alpha animation to show the blank hand space being replaces by a card
                if (iCard == 0) {
                    CardView playerHandCard = mGame.getPlayer(iPlayer).getPlayerLayout().getCardView();
                    alphaAnimator = ObjectAnimator.ofFloat(playerHandCard,"Alpha",ALMOST_TRANSPARENT_ALPHA, 1.0f);
                    alphaAnimator.setDuration(SHORT_ANIMATION);
                    dealSet.play(dealtCardAnimator).with(alphaAnimator);
                }

                //The card is returned to the home point with the second portion of deal_from_drawpile

                lastDealtCardAnimator = dealtCardAnimator;
            }//end for iPlayer
        }//end for numCards
        dealSet.start();
    }//end animateDealing



    //shakes the Draw and Discard Piles to indicate that you should draw from them
    //TODO:A The alpha fade applied to the DiscardPile on moving a card cancels the shake animation
    private void animatePiles() {
        Animation spinCardAnimation = AnimationUtils.loadAnimation(this, R.anim.card_shake);
        this.mDrawPile.startAnimation(spinCardAnimation);
        this.mDiscardPile.startAnimation(spinCardAnimation);
    }

    private void animateHumanPickUp(final Game.PileDecision pickedPile) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());


        if (pickedPile == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDiscardPile.getId());
            pileCardView.setLayoutParams(pileLp);
            //show the card underneath the one we are animating
            showDiscardPileCard();
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            //doesn't show the card until it appears in your hand
            pileCardView = new CardView(this, mGame.getDrawnCard(),-1);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM, mDrawPile.getId());
            pileCardView.setLayoutParams(pileLp);
        }
        drawAndDiscardPiles.addView(pileCardView);

        this.mDrawPile.clearAnimation();
        this.mDiscardPile.clearAnimation();
        //Use the old-style Tween animation because we can define it in XML
        //TODO: Use two animations meeting in the middle (from Discard/Draw and to Hand)
        //For Computer this is pick, bounce, and discard; for Human it is pick and bounce card
        final AnimationSet pickAndBounce = new AnimationSet(false);
        final Animation pickedCardAnimation = AnimationUtils.loadAnimation(this,
                (pickedPile== Game.PileDecision.DISCARD_PILE) ? R.anim.from_discardpile : R.anim.from_drawpile);
        final Animation bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.card_bounce);
        pickAndBounce.addAnimation(pickedCardAnimation);
        pickAndBounce.addAnimation(bounceAnimation);
        pickAndBounce.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                syncDisplay();
                drawAndDiscardPiles.removeView(pileCardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        pileCardView.startAnimation(pickAndBounce);
    }


    private void animateComputerPickUpAndDiscard(final int iPlayer, final Game.PileDecision pickedFrom) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());

        final CardView discardCardView = new CardView(this, mGame.getCurrentPlayer().getHandDiscard(),-1);
        final RelativeLayout.LayoutParams discardLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());
        discardLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
        discardLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDiscardPile.getId());
        discardCardView.setLayoutParams(discardLp);
        discardCardView.setAlpha(0.0f); //start it off invisible so we can animate it into view
        drawAndDiscardPiles.addView(discardCardView);

        //Create the fake card we will animate
        if (pickedFrom == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDiscardPile.getId());
            pileCardView.setLayoutParams(pileLp);
            //show the card underneath the one we are animating
            showDiscardPileCard();
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            //doesn't show the card until it appears in your hand
            pileCardView = new CardView(this, CardView.sBitmapCardBack);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM, mDrawPile.getId());
            pileCardView.setLayoutParams(pileLp);
        }
        drawAndDiscardPiles.addView(pileCardView);

        //Use Animator animation because for Computer we animate pickup to hand
        //and then discard to Discard Pile (could be two different cards)
        final AnimatorSet pickAndDiscard = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedFrom == Game.PileDecision.DISCARD_PILE ? R.animator.from_discardpile : R.animator.from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);

        final float translationX = mGame.getPlayer(iPlayer).getPlayerLayout().getTranslationX();
        //These translations need to be adjusted according to whether we are drawing from Draw or Discard pile (+ an adjustment for the gap)
        float adjustmentX = (float)(0.5 * CardView.INTRINSIC_WIDTH * PlayerLayout.DECK_SCALING + 10f);
        adjustmentX = (pickedFrom == Game.PileDecision.DISCARD_PILE ? -adjustmentX : +adjustmentX);
        final float translationY = mGame.getPlayer(iPlayer).getPlayerLayout().getTranslationY();
        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX",translationX+adjustmentX );
        pickedCardXAnimator.setDuration(MEDIUM_ANIMATION);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", translationY);
        pickedCardYAnimator.setDuration(MEDIUM_ANIMATION);
        pickAndDiscard.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        final Animator discardAnimator = AnimatorInflater.loadAnimator(this, R.animator.to_discardpile);
        discardAnimator.setTarget(discardCardView);
        adjustmentX = (float)(-0.5 * CardView.INTRINSIC_WIDTH * PlayerLayout.DECK_SCALING );
        final ObjectAnimator discardXAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationX",translationX+adjustmentX,0f );
        discardXAnimator.setDuration(1000);
        final ObjectAnimator discardYAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationY", translationY,0f);
        discardYAnimator.setDuration(1000);
        pickAndDiscard.play(discardAnimator).with(discardXAnimator).with(discardYAnimator).after(pickedCardAnimator);

        pickAndDiscard.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //remove extra cards created for the purpose of animation
                drawAndDiscardPiles.removeView(discardCardView);
                drawAndDiscardPiles.removeView(pileCardView);
                if (mGame.endTurn() != null) animateRoundScore(mGame.getCurrentPlayer());
                syncDisplay();
                mGame.checkEndRound();

                if (GameState.ROUND_END == mGame.getGameState())
                    mPlayButton.setText(getText(R.string.addScores));
                else
                    mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer().getName()));
                enablePlayButton();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });
        pickAndDiscard.start();

    }//end animateComputerPickUpAndDiscard

    //show the score at 5x size, animating to the player hand
    void animateRoundScore(final Player currentPlayer) {
        //can't animate the roundScore without showing it
        currentPlayer.getPlayerLayout().setPlayedInFinalRound(true);

        final TextView roundScoreView = currentPlayer.getPlayerLayout().getRoundScoreView();
        final AnimatorSet scaleAndTranslateSet = new AnimatorSet();
        final Animator scaleAnimator = AnimatorInflater.loadAnimator(this,R.animator.scale_score);
        scaleAnimator.setTarget(roundScoreView);

        final float translationX = currentPlayer.getPlayerLayout().getTranslationX();
        final float translationY = currentPlayer.getPlayerLayout().getTranslationY();
        //translate from the middle of the screen in X, and about 400f in Y (TODO:A measure from melds)
        final ObjectAnimator scoreXAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationX", -translationX, 0f);
        final ObjectAnimator scoreYAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationY", +400f, 0f);

        //TODO:A replace with subclass alternative (where you don't need to override everything)
        scaleAndTranslateSet.addListener(new Animator.AnimatorListener() {
                 @Override
                 public void onAnimationStart(Animator animator) {
                 }

                 @Override
                 public void onAnimationEnd(Animator animator) {
                 }

                 @Override
                 public void onAnimationRepeat(Animator animator) {
                 }

                 @Override
                 public void onAnimationCancel(Animator animator) {
                 }
        });
        scoreXAnimator.setDuration(LONG_ANIMATION);
        scoreYAnimator.setDuration(LONG_ANIMATION);
        scaleAndTranslateSet.play(scoreXAnimator).with(scoreYAnimator).with(scaleAnimator);
        scaleAndTranslateSet.start();
    }


    /* METHODS FOR SETTING UP PIECES OF THE DISPLAY */
    void removeAndAddPlayerHands(PlayerLayout deletedPlayerLayout) {
        if (mGame != null) {
            final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);
            //remove the old ones first (with a new/deleted  player they have to be relaid out)
            //this one won't be deleted because we deleted it from the player list
            if (null != deletedPlayerLayout) fullScreenContent.removeView(deletedPlayerLayout);
            for (int iPlayer = 0; iPlayer < mGame.getPlayers().size(); iPlayer++) {
                PlayerLayout playerLayout = mGame.getPlayer(iPlayer).getPlayerLayout();
                fullScreenContent.removeView(playerLayout);
                mGame.getPlayer(iPlayer).removePlayerLayout();
                fullScreenContent.addView(mGame.getPlayer(iPlayer).addPlayerLayout(this, iPlayer, mGame.getPlayers().size()));
            }
        }
    }


    //set background of player hands, visible discard, and cards/melds
    private void syncDisplay() {
        showDiscardPileCard();
        //automatically shows all round scores if final round
        Player.updatePlayerHands(mGame.getPlayers(), mGame.getCurrentPlayer());

        if (mGame.getCurrentPlayer() == mGame.getPlayerWentOut()) {
            mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(),mGame.getPlayerWentOut().getName()));
            mGameInfoToast.show();
        }
        //don't show computer cards unless SHOW_ALL_CARDS is set or final round
        if (mGame.getCurrentPlayer().isHuman() || Game.SHOW_ALL_CARDS || ((mGame.getPlayerWentOut() != null)
                    &&((mGame.getGameState() == GameState.TAKE_COMPUTER_TURN) || (mGame.getGameState() == GameState.ROUND_END)))) {
            int iBase = showCards(mGame.getCurrentPlayer().getHandUnMelded(),  mCurrentCards, 0, false, mGame.getCurrentPlayer().isHuman());
            showCards(mGame.getCurrentPlayer().getHandMelded(), mCurrentMelds, iBase, true,  mGame.getCurrentPlayer().isHuman());
        }
        else {
            //now just clears the cards&melds area
            showNothing(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
        }
    }
    private void showDiscardPileCard() {
        //TODO:B better would be card stack/list that automatically associates the right card
        // but right now we need to change the Card/Image for a fixed CardView (which is linked into the display)
        //this handles getDiscardPileCard == null
        mDiscardPile.setCard(this, mGame.getDiscardPileCard());
    }

    private void showHint(final String mText) {
        if ((mGame.getRoundOf() != null) && (mGame.getRoundOf().getOrdinal() <= HELP_ROUNDS.getOrdinal()) && (mText.length() > 0)) {
            mGameInfoToast.setText(mText);
            mGameInfoToast.show();
        }
    }

    //open Add Player dialog
    void showAddPlayers() {
        if ((mGame != null) && (mGame.getRoundOf() != null) && (mGame.getRoundOf() != Rank.getLowestRank())) {
            mGameInfoToast.setText(R.string.cantAdd);
            mGameInfoToast.show();
        }else {
            removeAndAddPlayerHands(null); //will call again if we actually add any players
            //pop open the Players dialog for the names
            EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(null, false, true, -1);
            epdf.show(getFragmentManager(), null);
        }
    }

    void showEditPlayer(final String oldPlayerName, final boolean oldIsHuman, final int iPlayer) {
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(oldPlayerName, oldIsHuman, false, iPlayer);
        epdf.show(getFragmentManager(),null);
    }

    //just show the backs of the cards
    private void showNothing(final int numCards, final RelativeLayout cardsLayout, final RelativeLayout meldsLayout) {
        cardsLayout.removeAllViews();
        meldsLayout.removeAllViews();
        /*
        ArrayList<CardView> cardLayers = new ArrayList<>(Game.MAX_CARDS);
        float xOffset=0f;
        for (int iCard=0; iCard<numCards; iCard++) {
            CardView cv = new CardView(this, CardView.sBitmapCardBack );
            cv.setTranslationX(xOffset);
            cv.bringToFront();
            cv.setClickable(false);
            cardLayers.add(cv);
            xOffset += (CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
        }
        xOffset -= (CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
        if (!cardLayers.isEmpty()) {
            for (CardView cv : cardLayers) {
                cv.setTranslationX(cv.getTranslationX()-0.5f*xOffset);
                cardsLayout.addView(cv);
            }
        }
        */
    }

    private int showCards(final ArrayList<CardList> meldLists, final RelativeLayout relativeLayout, final int iViewBase, final boolean showBorder, final boolean isHuman) {
        int xMeldOffset=0;
        int xCardOffset=0;
        int yMeldOffset= (int) (+MELD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
        int iView=iViewBase;
        relativeLayout.removeAllViews();

        for (CardList cardList : meldLists) {
            if (cardList.isEmpty()) continue;
            RelativeLayout nestedLayout = new RelativeLayout(this);
            nestedLayout.setTag(cardList); //so we can retrieve which meld a card is dragged onto
            //Create and add an invisible view that fills the necessary space for all cards in this meld
            CardView cvTemplate = new CardView(this, CardView.sBitmapCardBack );
            cvTemplate.setVisibility(View.INVISIBLE);
            //the final width is the first card + the offsets of the others
            cvTemplate.setMinimumWidth((int) (CardView.INTRINSIC_WIDTH + (cardList.size() - 1) * CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH));
            nestedLayout.addView(cvTemplate);
            nestedLayout.setTranslationX(xMeldOffset+xCardOffset);
            nestedLayout.setTranslationY(yMeldOffset);
            xMeldOffset += (MELD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH) + xCardOffset;
            yMeldOffset = -yMeldOffset;


            if (showBorder) {
                //if this is actually a validated meld, outline in solid green
                if (MeldedCardList.isValidMeld(cardList))
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.solid_green_border));
                else
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            }
            xCardOffset=0;
            for (Card card : cardList.getCards()) {
                CardView cv = new CardView(this, card, iView );
                cv.setTag(iView); //allows us to pass the index into the dragData without dealing with messy object passing
                iView++;
                cv.setTranslationX(xCardOffset);
                xCardOffset += (CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
                cv.bringToFront();
                cv.setClickable(false); //TODO:B: Allow selection by clicking for multi-drag?
                if (isHuman) {//no dragging of computer cards
                    cv.setOnDragListener(new CardViewDragEventListener());
                    cv.setOnTouchListener(new View.OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
                            // Create a new ClipData using the tag as a label, the plain text MIME type, and
                            // the string containing the View index. This will create a new ClipDescription object within the
                            // ClipData, and set its MIME type entry to "text/plain"
                            ClipData dragData = ClipData.newPlainText("Discard", v.getTag().toString());
                            View.DragShadowBuilder myShadow = new CardViewDragShadowBuilder(v);

                            v.startAnimation(Utilities.instantFade(0.5f, 0.5f));
                            // Starts the drag
                            v.startDrag(dragData,  // the data to be dragged
                                    myShadow,  // the drag shadow builder
                                    null,      // no need to use local data
                                    0          // flags (not currently used, set to 0)
                            );
                            return true;
                        }//end onClick
                    });
                }//end if isHuman

                nestedLayout.addView(cv);
            }//end cards in meld
            if (isHuman) nestedLayout.setOnDragListener(new CurrentMeldDragListener());
            relativeLayout.addView(nestedLayout);
        }
        if (showBorder && isHuman) {
            //Now add a "Drag here to create a new meld" space, although the handler will actually be the CurrentMeldLayoutListener
            TextView newMeldSpace = new TextView(this);
            newMeldSpace.setText(R.string.dragYourCards);
            newMeldSpace.setLines(5);
            //Move this a full card's space over from last card
            newMeldSpace.setTranslationX(xMeldOffset+ CardView.INTRINSIC_WIDTH*(1-MELD_OFFSET_RATIO)+xCardOffset);
            newMeldSpace.setTranslationY(yMeldOffset);
            newMeldSpace.setTypeface(null, Typeface.ITALIC);
            newMeldSpace.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            newMeldSpace.setEnabled(false);
            relativeLayout.addView(newMeldSpace,CardView.INTRINSIC_WIDTH, CardView.INTRINSIC_HEIGHT);
        }
        return iView; //used as the starting index for mCurrentMelds
    }//end showCards

    @Deprecated
    private void showPlayerScores2(List<Player> players) {

        if (null == mScoreDetail) {
            mScoreDetail = (TableLayout) findViewById(R.id.scoreDetail);
            LayoutInflater  inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //provides the layout params for each row
            mScoreDetailView = inflater.inflate(R.layout.score_detail,null);
            //actual rows are added dynamically below (allows for adding new players)
        }//end if mScoreDetail not initialized

        List<Player> sortedPlayers = new ArrayList<Player>(players);
        Collections.sort(sortedPlayers, Player.playerComparatorByScoreDesc);
        for (int iPlayer=0; iPlayer<sortedPlayers.size(); iPlayer++) {
            //dynamically add this row, including the initial hard-coded players
            if (null == mScoreDetailRow[iPlayer]) {
                mScoreDetailRow[iPlayer] = new TableRow(this);
                mPlayerNametv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.player_name));
                mPlayerNametv[iPlayer].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View tv) {
                        //third argument says we are editing
                        //really ugly...
                        showEditPlayer(((TextView) tv).getText().toString(),
                                ((CheckBox) ((ViewGroup) tv.getParent()).getChildAt(3)).isChecked(),
                                Integer.parseInt(((TextView) ((ViewGroup) tv.getParent()).getChildAt(4)).getText().toString()));
                    }
                });

                mPlayerScoretv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.round_score));
                mPlayerCumScoretv[iPlayer] = clone((TextView) mScoreDetailView.findViewById(R.id.cum_score));
                //invisible checkbox to remember whether this player is human or not
                mPlayerIsHuman[iPlayer] = new CheckBox(this);
                mPlayerIsHuman[iPlayer].setVisibility(View.GONE);
                //invisible array selector
                mPlayerIndex[iPlayer] = new TextView(this);
                mPlayerIndex[iPlayer].setVisibility(View.GONE);

                //add the detail row view into the table layout
                mScoreDetail.addView(mScoreDetailRow[iPlayer]);
                mScoreDetailRow[iPlayer].addView(mPlayerNametv[iPlayer], 0);
                mScoreDetailRow[iPlayer].addView(mPlayerScoretv[iPlayer], 1);
                mScoreDetailRow[iPlayer].addView(mPlayerCumScoretv[iPlayer], 2);
                mScoreDetailRow[iPlayer].addView(mPlayerIsHuman[iPlayer], 3);//index 3 is important for this invisible checkbox
                mScoreDetailRow[iPlayer].addView(mPlayerIndex[iPlayer],4);
            }
            mPlayerNametv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getName()));
            mPlayerScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getRoundScore()));
            mPlayerCumScoretv[iPlayer].setText(String.valueOf(sortedPlayers.get(iPlayer).getCumulativeScore()));
            mPlayerIsHuman[iPlayer].setChecked(sortedPlayers.get(iPlayer).isHuman());
            mPlayerIndex[iPlayer].setText(String.valueOf(iPlayer));
        }//end for players
        //Bold the top (leading) player
        mPlayerNametv[0].setTypeface(null, Typeface.BOLD);
        mPlayerCumScoretv[0].setTypeface(null, Typeface.BOLD);
    }//end showPlayerScores


    CardView findViewByIndex(final int iCardView) {
        CardView foundCardView=null;
        //find the view we coded with this index - have to loop thru nested layouts
        for (int iNestedLayout = 0; iNestedLayout < mCurrentCards.getChildCount(); iNestedLayout++) {
            View rl = mCurrentCards.getChildAt(iNestedLayout);
            if ((rl == null) || !(rl instanceof RelativeLayout)) continue;
            RelativeLayout nestedLayout = (RelativeLayout)rl;
            for (int iView = 0; iView < nestedLayout.getChildCount(); iView++) {
                CardView cv = (CardView) nestedLayout.getChildAt(iView);
                if ((cv != null) && (cv.getViewIndex() == iCardView)) {
                    foundCardView = cv;
                    break;
                }
            }
            if (foundCardView != null) break;
        }
        if (foundCardView == null) { //less likely, but search melded cards too
            for (int iNestedLayout = 0; iNestedLayout < mCurrentMelds.getChildCount(); iNestedLayout++) {
                View rl = mCurrentMelds.getChildAt(iNestedLayout);
                //Allows for the fake "Drag here to form new meld" TextView
                if ((rl == null) || !(rl instanceof RelativeLayout)) continue;
                RelativeLayout nestedLayout = (RelativeLayout)rl;
                for (int iView = 0; iView < nestedLayout.getChildCount(); iView++) {
                    CardView cv = (CardView) nestedLayout.getChildAt(iView);
                    if (cv.getViewIndex() == iCardView) {
                        foundCardView = cv;
                        break;
                    }
                }
            }
        }
        if (foundCardView == null) {
            Toast.makeText(this, "No matching card found", Toast.LENGTH_SHORT).show();
            Log.e(Game.APP_TAG, "discardedCard: No matching card found");
        }
        // else Toast.makeText(this, foundCardView.getCard().getCardString() + " discarded", Toast.LENGTH_SHORT).show();
        return foundCardView;
    }

    /* GETTER * Yuck */

    Game getmGame() {
        return mGame;
    }

    /* SMALL UTILITY METHODS */
    private TextView clone(final TextView vFrom) {
        final TextView vTo = new TextView(this);
        cloneLayout(vFrom, vTo);
        return vTo;
    }

    static private void cloneLayout(final TextView vFrom, final TextView vTo) {
        vTo.setLayoutParams(vFrom.getLayoutParams());
        vTo.setPadding(vFrom.getPaddingLeft(), vFrom.getPaddingTop(), vFrom.getPaddingRight(), vFrom.getPaddingBottom());
    }

    private String resFormat(final int resource, final String param) {
        return String.format(getText(resource).toString(),param);
    }

}
