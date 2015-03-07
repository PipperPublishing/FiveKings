package com.example.jeffrey.fivekings;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
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
                TODO:A Is there a different parameter for positioning FrameLayouts relative to each other?
    3/3/2015    Set CARD_OFFSET_RATIO to be 20% of INTRINSIC_WIDTH (preset in CardView)
    3/3/2015    Vibrate Discard and Draw Piles to show you are meant to draw - replace InfoLine with Toast on first few rounds
    3/3/2015    Animate pick from Draw or Discard Piles
                Comment out infoLine - add back in information as needed
                Enable/Disable Draw and Discard Piles just sets clickable or not
    3/5/2015    Improved animation to show cards picked for humans and faulty animation for Computer
    3/6/2015    Switch to using Animator for Computer Discard
    3/7/2015    First code for saving instance State when app gets Stopped - NOT WORKING
                On LongClick of Play button, pop up a confirmation dialog before starting a new game
*/

public class FiveKings extends Activity {
    static final float CARD_OFFSET_RATIO = 0.18f;
    static final float MELD_OFFSET_RATIO = 0.3f;
    static final int TOAST_X_OFFSET = 20;
    static final int TOAST_Y_OFFSET = +250;
    static final Rank HELP_ROUNDS=Rank.THREE;

    // Dynamic elements of the interface
    private Game mGame=null;
    private Button mPlayButton;
    private TextView mLastRoundScores;
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
    private TextView mInfoLine;
    private CardView mDiscardPile;
    private ImageButton mDrawPileButton;
    private RelativeLayout mCurrentCards;
    private RelativeLayout mCurrentMelds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_five_kings);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

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

        mLastRoundScores = (TextView)findViewById(R.id.after_round);
        mCurrentRound = (TextView)findViewById(R.id.current_round);
        mGameInfoToast = Toast.makeText(this, R.string.blank,Toast.LENGTH_SHORT);
        mGameInfoToast.setGravity(Gravity.TOP|Gravity.LEFT,TOAST_X_OFFSET,TOAST_Y_OFFSET);
        mInfoLine = (TextView) findViewById(R.id.info_line);
        mInfoLine.setTypeface(null, Typeface.ITALIC);
        mDiscardPile = (CardView)findViewById(R.id.discardPile);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDiscardPile(v);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());
        mDrawPileButton = (ImageButton)findViewById(R.id.drawPile);
        mDrawPileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDrawPile(v);
            }
        });
        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

        disableDrawDiscard();

    }//end onCreate

    static final String ROUND_OF="ROUND_OF";


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(ROUND_OF, mGame.getRoundOf().ordinal());


        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //savedInstanceState.getInt(ROUND_OF, mGame.getRoundOf().ordinal());
    }

 /*   EVENT HANDLERS*/
    //Event handler for [Save] in add player dialog
 public void addEditPlayerClicked(String playerName, boolean isHuman, boolean addingFlag, int iPlayer) {
     if (addingFlag) mGame.addPlayer(playerName, isHuman);
     else mGame.updatePlayer(playerName, isHuman, iPlayer);
     showPlayerScores(mGame.getPlayers());
 }

    //the Event handler for button presses on [Play]
    private void playGameClicked(){
        setWidgetsByGameState();

        if (null== mGame) {
            mGame = new Game(this); //also sets gameState=ROUND_START
            this.mCurrentRound.setText(resFormat(R.string.current_round,mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            showPlayerScores(mGame.getPlayers());
            showAddPlayers();
        }

        //User pressed [New Game] button (currently by long-pressing Play)
        else if (GameState.NEW_GAME == mGame.getGameState()) {
            mGame.init();
            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            showPlayerScores(mGame.getPlayers());
            showAddPlayers();
        }

        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer(null).getName()));
            //Show blank screen with discard pile showing
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            showDiscardPileCard();
        }

        else if (GameState.TAKE_COMPUTER_TURN == mGame.getGameState()) {
            StringBuilder turnInfo = new StringBuilder(100);

            turnInfo.setLength(0);
            boolean pickedDiscardPile = mGame.takeComputerTurn(getText(R.string.computerTurnInfo).toString(), turnInfo);
            mGameInfoToast.setText(turnInfo);
            mGameInfoToast.show();

            if (mGame.getPlayer() == mGame.getPlayerWentOut()) {
                mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(),mGame.getPlayerWentOut().getName()));
                mGameInfoToast.show();
            }
            //at this point the DiscardPile still shows the old card
            //animate... also calls syncDiscardCardsMelds and checkEndRound() in the OnAnimationEnd listener
            animateComputerPickUpAndDiscard(pickedDiscardPile); // also calls syncDiscardCardsMelds
        }

        else if (GameState.TURN_START == mGame.getGameState()) {
            mGame.rotatePlayer();
            syncDiscardMeldsCards();

            if (mGame.getPlayer().isHuman()) {
                showHint(resFormat(R.string.yourCardsAndMelds, mGame.getPlayer().getName()));
                disablePlayButton();
                enableDrawDiscard();
                animatePiles();
                mGame.setGameState(GameState.TAKE_HUMAN_TURN);
                setWidgetsByGameState();
            }
            else {
                //mInfoLine.setText(resFormat(R.string.computerCardsAndMelds, mGame.getPlayer().getName()));
                this.mPlayButton.setText(resFormat(R.string.takePlayerTurn, mGame.getPlayer().getName()));
                mGame.setGameState(GameState.TAKE_COMPUTER_TURN);
            }
        }//end TURN_START

        else if (GameState.ROUND_END == mGame.getGameState()) {
            mLastRoundScores.setText(resFormat(R.string.scores_after, mGame.getRoundOf().getString()));
            mGame.endRound();
            mGameInfoToast.setText(R.string.displayScores);
            mGameInfoToast.show();
            showPlayerScores(mGame.getPlayers());
            if (GameState.ROUND_START == mGame.getGameState()) this.mPlayButton.setText(resFormat(R.string.nextRound,mGame.getRoundOf().getString()));
        }

        else if (GameState.GAME_END == mGame.getGameState()) {
            this.mPlayButton.setText(getText(R.string.newGame));
            mGame.logFinalScores();
            mGame.setGameState(GameState.NEW_GAME);
        }
    }

    //Event handler for clicks on Draw Pile
    private void clickedDrawPile(View v) {
        clickedDrawOrDiscard(Game.USE_DRAW_PILE);
    }
    //Event handler for clicks on Discard Pile
    private void clickedDiscardPile(View v) {
        clickedDrawOrDiscard(Game.USE_DISCARD_PILE);
    }

    private void clickedDrawOrDiscard(boolean useDiscardPile) {
        StringBuilder turnInfo = new StringBuilder(100);
        mGame.setGameState(GameState.HUMAN_PICKED_CARD);
        setWidgetsByGameState();

        turnInfo.setLength(0);
        //also sets GameState to END_HUMAN_TURN
        mGame.takeHumanTurn(getText(R.string.humanTurnInfo).toString(), turnInfo, useDiscardPile);
        //at this point the DiscardPile still shows the old card if you picked it
        //handles animating the card off the appropriate pile and making it appear in the hand
        animateHumanPickUp(useDiscardPile);
        showHint(turnInfo.toString());
    }


    //Event Handler for clicks on cards or melds - called from DiscardPile drag listener
    boolean discardedCard(int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        setWidgetsByGameState();
        if (GameState.END_HUMAN_TURN == mGame.getGameState()) {
            if (!mGame.getPlayer().isHuman()) throw new RuntimeException("discardedCard: player is not Human");
            Player playerWentOut = mGame.endHumanTurn(foundCardView.getCard());
            if (mGame.getPlayer() == playerWentOut) {
                mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(), playerWentOut.getName()));
                mGameInfoToast.show();
            }
            syncDiscardMeldsCards();

            //returns false if we've reached the player who went out again
            mGame.checkEndRound();
            if (GameState.ROUND_END == mGame.getGameState()) mPlayButton.setText(getText(R.string.showScores));
            else mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer(null).getName()));
        }
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    //to add to a existing meld, drag to an existing meld
    boolean makeNewMeld(int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //create trialMeld (one card at a time for now)

        mGame.getPlayer().makeNewMeld(foundCardView.getCard());
        syncDiscardMeldsCards();
        return true;
    }

    //Don't test for valid meld; that is done with evaluateMelds
    boolean addToMeld(CardList meld, int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //add to existing meld

        mGame.getPlayer().addToMeld(meld, foundCardView.getCard());
        syncDiscardMeldsCards();
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
        if (this.mDrawPileButton != null) {
            this.mDrawPileButton.setEnabled(true);
            this.mDrawPileButton.setClickable(true);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setEnabled(true);
            this.mDiscardPile.setClickable(true);
        }
    }
    private void disableDrawDiscard() {
        if (this.mDrawPileButton != null) {
            this.mDrawPileButton.setClickable(false);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setClickable(false);
        }
    }
    private void setWidgetsByGameState() {
        if (mGame == null) return;
        switch (mGame.getGameState()) {
            case NEW_GAME:
            case ROUND_START:
            case TURN_START:
            case TAKE_COMPUTER_TURN:
            case END_HUMAN_TURN:
            case ROUND_END:
            case GAME_END:
            default:
                enablePlayButton();
                disableDrawDiscard();
                break;
            case TAKE_HUMAN_TURN:
                disablePlayButton();
                enableDrawDiscard();
                break;
            case HUMAN_PICKED_CARD:
                disablePlayButton();
                disableDrawDiscard();
                break;
        }
    }

    /* ANIMATION ROUTINES */
    //shakes the Draw and Discard Piles to indicate that you should draw from them
    //TODO:A The alpha fade applied to the DiscardPile on moving a card cancels the shake animation
    private void animatePiles() {
        Animation spinCardAnimation = AnimationUtils.loadAnimation(this, R.anim.card_shake);
        this.mDrawPileButton.startAnimation(spinCardAnimation);
        this.mDiscardPile.startAnimation(spinCardAnimation);
    }

    private void animateHumanPickUp(boolean pickedDiscardPile) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());

        if (pickedDiscardPile) {
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
            pileLp.addRule(RelativeLayout.ALIGN_LEFT,mDrawPileButton.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDrawPileButton.getId());
            pileCardView.setLayoutParams(pileLp);
        }
        drawAndDiscardPiles.addView(pileCardView);

        this.mDrawPileButton.clearAnimation();
        this.mDiscardPile.clearAnimation();
        //Use the old-style Tween animation because we can define it in XML
        //TODO: Use two animations meeting in the middle (from Discard/Draw and to Hand)
        //For Computer this is pick, bounce, and discard; for Human it is pick and bounce card
        final AnimationSet pickAndBounce = new AnimationSet(false);
        final Animation pickedCardAnimation = AnimationUtils.loadAnimation(this,
                (pickedDiscardPile ? R.anim.from_discardpile : R.anim.from_drawpile));
        final Animation bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.card_bounce);
        pickAndBounce.addAnimation(pickedCardAnimation);
        pickAndBounce.addAnimation(bounceAnimation);
        pickAndBounce.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                syncDiscardMeldsCards();
                drawAndDiscardPiles.removeView(pileCardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        pileCardView.startAnimation(pickAndBounce);

/*      //attempt to use Overlays, but it didn't show
        ViewGroup vg = (ViewGroup)mDrawPileButton.getParent();
        ViewOverlay vo = mDrawPileButton.getOverlay();
        Drawable drawPileDrawable =mDrawPileButton.getDrawable();
        vo.add(drawPileDrawable);
        ViewGroupOverlay vgo = vg.getOverlay();
        vgo.add(discardPileCardView);
        vgo.add(drawPileCardView);
*/
    }
    private void animateComputerPickUpAndDiscard(boolean pickedDiscardPile) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());

        final CardView discardCardView = new CardView(this, mGame.getPlayer().getDiscard(),-1);
        final RelativeLayout.LayoutParams discardLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());
        discardLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
        discardLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDiscardPile.getId());
        discardCardView.setLayoutParams(discardLp);
        discardCardView.setAlpha(0.0f); //start it off invisible so we can animate it into view
        drawAndDiscardPiles.addView(discardCardView);

        //Create the fake card we will animate
        if (pickedDiscardPile) {
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
            pileLp.addRule(RelativeLayout.ALIGN_LEFT,mDrawPileButton.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDrawPileButton.getId());
            pileCardView.setLayoutParams(pileLp);
        }
        drawAndDiscardPiles.addView(pileCardView);

        //Use Animator animation because for Computer we animate pickup to hand
        //and then discard to Discard Pile (could be two different cards)
        final AnimatorSet pickAndDiscard = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedDiscardPile ? R.animator.from_discardpile : R.animator.from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);
        final Animator discardAnimator = AnimatorInflater.loadAnimator(this, R.animator.to_discardpile);
        discardAnimator.setTarget(discardCardView);
        pickAndDiscard.play(discardAnimator).after(pickedCardAnimator);
        pickAndDiscard.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                drawAndDiscardPiles.removeView(discardCardView);
                drawAndDiscardPiles.removeView(pileCardView);
                mGame.endTurn();
                syncDiscardMeldsCards();
                mGame.checkEndRound();

                if (GameState.ROUND_END == mGame.getGameState())
                    mPlayButton.setText(getText(R.string.showScores));
                else
                    mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getNextPlayer(null).getName()));
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


    private void syncDiscardMeldsCards() {
        showDiscardPileCard();
        //don't show computer cards unless SHOW_ALL_CARDS is set or final round
        if (mGame.getPlayer().isHuman() || Game.SHOW_ALL_CARDS || ((mGame.getPlayerWentOut() != null)
                    &&((mGame.getGameState() == GameState.TAKE_COMPUTER_TURN) || (mGame.getGameState() == GameState.ROUND_END)))) {
            int iBase = showCards(mGame.getPlayer().getHandUnMelded(),  mCurrentCards, 0, false, mGame.getPlayer().isHuman());
            showCards(mGame.getPlayer().getHandMelded(), mCurrentMelds, iBase, true,  mGame.getPlayer().isHuman());
        }
        else {
            showCardBacks(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
        }
    }
    private void showDiscardPileCard() {
        //TODO:B better would be card stack/list that automatically associates the right card
        // but right now we need to change the Card/Image for a fixed CardView (which is linked into the display)
        //this handles getDiscardPileCard == null
        mDiscardPile.setCard(this, mGame.getDiscardPileCard());
    }

    private void showHint(String mText) {
        if ((mGame.getRoundOf().getOrdinal() <= HELP_ROUNDS.getOrdinal()) && (mText.length() > 0)) {
            mGameInfoToast.setText(mText);
            mGameInfoToast.show();
        }
    }

    //open Add Player dialog
    private void showAddPlayers() {
        //pop open the Players dialog for the names
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(null, false, true, -1);
        epdf.show(getFragmentManager(),null);
    }

    private void showEditPlayer(String oldPlayerName, boolean oldIsHuman, int iPlayer) {
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(oldPlayerName, oldIsHuman, false, iPlayer);
        epdf.show(getFragmentManager(),null);
    }

    //just show the backs of the cards
    private void showCardBacks(int numCards, RelativeLayout cardsLayout, RelativeLayout meldsLayout) {
        ArrayList<CardView> cardLayers = new ArrayList<>(Game.MAX_CARDS);
        float xOffset=0f;
        cardsLayout.removeAllViews();
        meldsLayout.removeAllViews();
        for (int iCard=0; iCard<numCards; iCard++) {
            CardView cv = new CardView(this, CardView.sBitmapCardBack ); //TODO:B way of doing early initialization of this?
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
    }

    private int showCards(ArrayList<CardList> meldLists, RelativeLayout relativeLayout, int iViewBase, boolean showBorder, boolean isHuman) {
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
                if (Player.isValidMeld(cardList))
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


    //TODO:B Look at switching to ListView, especially since all Table Rows are the same
    //Also would allow us to get rid of this hacky behavior of storing hidden fields
    //since the ListView would contain the data we needed
    private void showPlayerScores(List<Player> players) {

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


    CardView findViewByIndex(int iCardView) {
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
            Log.e(mGame.APP_TAG, "discardedCard: No matching card found");
        }
        // else Toast.makeText(this, foundCardView.getCard().getCardString() + " discarded", Toast.LENGTH_SHORT).show();
        return foundCardView;
    }


    /* SMALL UTILITY METHODS */
    private TextView clone(TextView vFrom) {
        final TextView vTo = new TextView(this);
        cloneLayout(vFrom, vTo);
        return vTo;
    }

    static private void cloneLayout(TextView vFrom, TextView vTo) {
        vTo.setLayoutParams(vFrom.getLayoutParams());
        vTo.setPadding(vFrom.getPaddingLeft(), vFrom.getPaddingTop(), vFrom.getPaddingRight(), vFrom.getPaddingBottom());
    }

    private String resFormat(int resource, String param) {
        return String.format(getText(resource).toString(),param);
    }

}
