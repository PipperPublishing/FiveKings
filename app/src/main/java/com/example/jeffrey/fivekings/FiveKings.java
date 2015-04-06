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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
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
    3/22/2015   Impact of encapsulating all player management in PlayerList
    3/31/2015   Removed mCurrentRound (showing it on the moved Play button)
                Moved clickedDrawOrDiscard to Game
                Relayout miniHands on add/delete of players
                Moved common code to endTurnCheckRound
                Replaced syncDisplay with updateHandsAndCards
    4/1/2015    Removed syncDisplay and ROUND_END
    4/2/2015    First cut at Settings menu
    4/3/2015    Settings: showComputerHands and animateDealing
                Move Game creation to onCreate; could also make it singleton
    4/5/2015    Start threaded findBestHand as soon as previous player (or round start) is done.
    4/6/2015    Implement more menu items, including Player, and remove Add Player on screen
*/

public class FiveKings extends Activity {
    static final float CARD_OFFSET_RATIO = 0.18f;
    static final float MELD_OFFSET_RATIO = 0.3f;
    //static final int TOAST_X_OFFSET = 20;
    //static final int TOAST_Y_OFFSET = +600;
    static final Rank HELP_ROUNDS=Rank.FOUR;
    static final float ALMOST_TRANSPARENT_ALPHA=0.1f;
    static final float HALF_TRANSPARENT_ALPHA=0.5f;
    static final float THIRD_TRANSPARENT_ALPHA=0.3f;
    static final float INVISIBLE_ALPHA=0.0f;

    private static final int LONG_ANIMATION=2000;
    private static final int MEDIUM_ANIMATION=500;
    private static final int SHORT_ANIMATION=100;

    // Dynamic elements of the interface
    private Game mGame=null;

    private ProgressBar spinner;
    private String mHint;
    private Button mPlayButton;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_five_kings);

        mHint = getText(R.string.toStartGame).toString();
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
                checkNewGame(); //pop up check dialog
                return true;
            }
        });

        mGameInfoToast = Toast.makeText(this, mHint, Toast.LENGTH_SHORT);
        mGameInfoToast.show();
        mDiscardPile = (CardView)findViewById(R.id.discardPile);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.clickedDrawOrDiscard(FiveKings.this, Game.PileDecision.DISCARD_PILE);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());

        mDrawPile = (CardView)findViewById(R.id.drawPile);
        mDrawPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.clickedDrawOrDiscard(FiveKings.this, Game.PileDecision.DRAW_PILE);
            }
        });

        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

/*        PlayerMiniHandLayout addPlayerMiniHandLayout = new PlayerMiniHandLayout(this);
        final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);
        fullScreenContent.addView(addPlayerMiniHandLayout);*/

        disableDrawDiscardClick();

        spinner = (ProgressBar)findViewById(R.id.progressCircle);
        spinner.setVisibility(View.GONE);

        if (null == mGame) mGame = new Game();
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


    /*------------------*/
    /*   EVENT HANDLERS */
    /*------------------*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                SettingsDialogFragment settingsDialogFragment = SettingsDialogFragment.newInstance(mGame.isShowComputerCards(),mGame.isAnimateDealing());
                settingsDialogFragment.show(getFragmentManager(), null);
                return true;
            case R.id.action_add_player:
                showAddPlayers();
                return true;
            case R.id.action_edit_player:
            case R.id.action_delete_player:
                Toast.makeText(this,R.string.edit_delete_hint,Toast.LENGTH_LONG).show();
                return false;
            case R.id.action_new_game:
                return checkNewGame();
            case R.id.action_help:
                HelpBox.show(FiveKings.this);
                return true;
            case R.id.action_about:
                AboutBox.show(FiveKings.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setSettings(final boolean showComputerHands, final boolean animateDealing) {
        mGame.setShowComputerCards(showComputerHands);
        mGame.setAnimateDealing(animateDealing);
    }

    //Event handler for [Save] in add player dialog
    public void addEditPlayerClicked(final String playerName, final boolean isHuman, final boolean addingFlag, final int iPlayerUpdate) {
         //if we add a player, we need to remove and re-add the hand layouts
        if (addingFlag) {
            if ((mGame.getRoundOf() != null) && (mGame.getRoundOf() != Rank.getLowestRank())) {
                Log.e(Game.APP_TAG, "Can't add players after Round of 3's");
                return;
            }
            mGame.addPlayer(playerName, isHuman);
            mGame.relayoutPlayerMiniHands(this);
        } else {
            mGame.updatePlayer(playerName, isHuman, iPlayerUpdate);
            mGame.updatePlayerMiniHands(false);
        }
    }
    public void deletePlayerClicked(final int iPlayerToDelete) {
        //pop up an alert to verify
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.confirmDelete)
                .setMessage(resFormat(R.string.areYouSureDelete, mGame.getPlayerByIndex(iPlayerToDelete).getName()))
                .setPositiveButton(R.string.yesDelete, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //have to be careful of order and save the layout to delete from the screen
                        mGame.deletePlayer(iPlayerToDelete, FiveKings.this);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    //the Event handler for button presses on [Play]
    private void playGameClicked(){
        setWidgetsByGameState();

        if (GameState.NEW_GAME == mGame.getGameState()) {
            newGame();
        }
        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            mGame.resetPlayerMiniHandsToRoundStart();
            if (mGame.isAnimateDealing()) {
                animateDealing(mGame.getRoundOf().getRankValue(), mGame.numPlayers());
            }
            else {
                afterDealing();
            }
            mGame.getNextPlayer().findBestHandStart(mGame.isFinalTurn(), mGame.peekDiscardPileCard());
            this.mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            //Show blank screen with discard pile showing - now in onAnimationEnd
        }

        else if (GameState.GAME_END == mGame.getGameState()) {
            this.mPlayButton.setText(getText(R.string.newGame));
            mGame.logFinalScores();
            mGame.setGameState(GameState.NEW_GAME);
        }
        else {
            Toast.makeText(this, R.string.clickComputerHand,Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkNewGame() {
        new AlertDialog.Builder(this)
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
                        newGame();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        return false;
    }

    private boolean newGame() {
        //User pressed [New Game] button (currently by long-pressing Play), or be finishing a previous game
        mGame.removePlayerMiniHands(this); // have to do this before we reset the players
        mGame.init();
        this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
        mCurrentCards.removeAllViews();
        mCurrentMelds.removeAllViews();
        mGame.relayoutPlayerMiniHands(this);
        showAddPlayers();
        return true;
    }

    void endTurnCheckRound(boolean showCards) {
        mGame.endTurn();
        updateHandsAndCards(showCards, mGame.isFinalTurn());
        if (mGame.isFinalTurn()) {
            // addToCumulativeScore() moved from animateRoundScore, because logging in checkEndRound continues before animation completes
            // maintain a separate displayed score in the miniHand
            mGame.getCurrentPlayer().addToCumulativeScore();
            animateRoundScore(mGame.getCurrentPlayer());
        }
        mGame.checkEndRound();
        if (GameState.ROUND_START == mGame.getGameState()) {
            mPlayButton.setText(resFormat(R.string.nextRound,mGame.getRoundOf().getString()));
            showHint(resFormat(R.string.displayScores, null));
            mGame.updatePlayerMiniHands(false);
            enablePlayButton();
        }
        else if (GameState.TURN_START == mGame.getGameState()) {
            mGame.getNextPlayer().findBestHandStart(mGame.isFinalTurn(), mGame.peekDiscardPileCard()); //if Human does nothing
            animatePlayerMiniHand(mGame.getNextPlayer().getMiniHandLayout());
        }
        else if (GameState.GAME_END == mGame.getGameState()) {
            mPlayButton.setText(R.string.newGame);
            enablePlayButton();
        }
    }


    /*---------------------------------------------------*/
    /* Event Handlers for clicks/drags on cards or melds */
    /*---------------------------------------------------*/
    boolean discardedCard(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        setWidgetsByGameState();
        mGame.setHandDiscard(foundCardView.getCard());
        endTurnCheckRound(true);
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    //to add to a existing meld, drag to an existing meld
    boolean makeNewMeld(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //create trialMeld (one card at a time for now)

        ((HumanPlayer)mGame.getCurrentPlayer()).makeNewMeld(foundCardView.getCard());
        updateHandsAndCards(true, false);
        return true;
    }

    //Don't test for valid meld; that is done with evaluateMelds
    boolean addToMeld(final CardList meld, final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //add to existing meld

        ((HumanPlayer)mGame.getCurrentPlayer()).addToMeld(meld, foundCardView.getCard());
        updateHandsAndCards(true, false); //SHOW is true because this is a human hand
        return true;
    }

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


    /*---------------------------------------------------*/
    /* COMMON METHODS FOR ENABLING/DISABLING UI ELEMENTS */
    /*---------------------------------------------------*/
    @Deprecated
    private void enablePlayButton() {
        if (this.mPlayButton != null) {
            this.mPlayButton.setClickable(true);
        }
    }
    @Deprecated
    private void disablePlayButton() {
        if (this.mPlayButton != null) {
            //Always clickable
            //this.mPlayButton.setClickable(false);
        }
    }
    //just prevent clicking, but for animations we need the buttons to be enabled
    private void enableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setEnabled(true);
            this.mDrawPile.setClickable(true);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setEnabled(true);
            this.mDiscardPile.setClickable(true);
        }
    }
    void disableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setClickable(false);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setClickable(false);
        }
    }


    void setWidgetsForHuman(final String playerName) {
        showHint(resFormat(R.string.yourCardsAndMelds, playerName));
        disablePlayButton();
        enableDrawDiscardClick();
        animatePiles();
    }

    void setWidgetsByGameState() {
        switch (mGame.getGameState()) {
            case ROUND_START:
            case TURN_START:
            case TAKE_TURN:
            case HUMAN_PICKED_CARD:
                disablePlayButton();
                disableDrawDiscardClick();
                break;
            case END_HUMAN_TURN:
                enablePlayButton();
                disableDrawDiscardClick();
                mGame.getCurrentPlayer().getMiniHandLayout().setGreyedOut(false);
                break;

            case NEW_GAME:
            case GAME_END:
            default:
                enablePlayButton();
                disableDrawDiscardClick();
                break;
        }
    }

    /*--------------------*/
    /* ANIMATION ROUTINES */
    /*--------------------*/
    void showSpinner(final boolean show) {
        spinner.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        spinner.invalidate();
    }

    private void animateDealing(final int numCards, final int numPlayers) {

        final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);
        mDrawPile.setVisibility(View.INVISIBLE);
        mDiscardPile.setVisibility(View.INVISIBLE);

        //the template card that animates from the center point to the hands
        final CardView deck = new CardView(this, CardView.sBlueBitmapCardBack);
        final CardView dealtCardView = new CardView(this, CardView.sBlueBitmapCardBack);
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams((int)(CardView.INTRINSIC_WIDTH* PlayerMiniHandLayout.DECK_SCALING),
                                                                                    (int)(CardView.INTRINSIC_HEIGHT* PlayerMiniHandLayout.DECK_SCALING));
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
                afterDealing();
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
                ObjectAnimator playerOffsetXAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationX", mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getTranslationX());
                ObjectAnimator playerOffsetYAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationY", mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getTranslationY());
                if (lastDealtCardAnimator == null) dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator);
                else dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator).after(lastDealtCardAnimator);
                //if this is the first card, then add an alpha animation to show the blank hand space being replaces by a card
                if (iCard == 0) {
                    CardView playerHandCard = mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getCardView();
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

    private void afterDealing() {
        mDrawPile.setVisibility(View.VISIBLE);
        mDiscardPile.setVisibility(View.VISIBLE);
        showDiscardPileCard();
        mGame.updatePlayerMiniHands(false);
        setWidgetsByGameState();
        animatePlayerMiniHand(mGame.getNextPlayer().getMiniHandLayout());
    }


    //shakes the Draw and Discard Piles to indicate that you should draw from them
    //TODO:A The alpha fade applied to the DiscardPile on moving a card cancels the shake animation
    private void animatePiles() {
        Animation spinCardAnimation = AnimationUtils.loadAnimation(this, R.anim.card_shake);
        this.mDrawPile.startAnimation(spinCardAnimation);
        this.mDiscardPile.startAnimation(spinCardAnimation);
    }

    void animateHumanPickUp(final Game.PileDecision pickedPile) {
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
                updateHandsAndCards(true, false); //true because this is human
                drawAndDiscardPiles.removeView(pileCardView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        pileCardView.startAnimation(pickAndBounce);
    }


    void animateComputerPickUpAndDiscard(final PlayerMiniHandLayout playerMiniHandLayout, final Game.PileDecision pickedFrom) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());

        final CardView discardCardView = new CardView(this, mGame.getCurrentPlayer().getHandDiscard(),-1);
        final RelativeLayout.LayoutParams discardLp = new RelativeLayout.LayoutParams(mDiscardPile.getLayoutParams());
        discardLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
        discardLp.addRule(RelativeLayout.ALIGN_BOTTOM,mDiscardPile.getId());
        discardCardView.setLayoutParams(discardLp);
        discardCardView.setAlpha(INVISIBLE_ALPHA); //start it off invisible so we can animate it into view
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
            pileCardView = new CardView(this, CardView.sBlueBitmapCardBack);
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

        final float translationX = playerMiniHandLayout.getTranslationX();
        //These translations need to be adjusted according to whether we are drawing from Draw or Discard pile (+ an adjustment for the gap)
        float adjustmentX = (float)(0.5 * CardView.INTRINSIC_WIDTH * PlayerMiniHandLayout.DECK_SCALING + 10f);
        adjustmentX = (pickedFrom == Game.PileDecision.DISCARD_PILE ? -adjustmentX : +adjustmentX);
        final float translationY = playerMiniHandLayout.getTranslationY();
        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX",translationX+adjustmentX );
        pickedCardXAnimator.setDuration(MEDIUM_ANIMATION);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", translationY);
        pickedCardYAnimator.setDuration(MEDIUM_ANIMATION);
        pickAndDiscard.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        final Animator discardAnimator = AnimatorInflater.loadAnimator(this, R.animator.to_discardpile);
        discardAnimator.setTarget(discardCardView);
        adjustmentX = (float)(-0.5 * CardView.INTRINSIC_WIDTH * PlayerMiniHandLayout.DECK_SCALING );
        final ObjectAnimator discardXAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationX",translationX+adjustmentX,0f );
        discardXAnimator.setDuration(2*MEDIUM_ANIMATION);
        final ObjectAnimator discardYAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationY", translationY,0f);
        discardYAnimator.setDuration(2*MEDIUM_ANIMATION);
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
                endTurnCheckRound(mGame.isShowComputerCards());
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
        //set roundScore to visible and show it in AnimationStart

        final TextView roundScoreView = currentPlayer.getMiniHandLayout().getRoundScoreView();
        final AnimatorSet scaleAndTranslateSet = new AnimatorSet();
        final Animator scaleAnimator = AnimatorInflater.loadAnimator(this,R.animator.scale_score);
        scaleAnimator.setTarget(roundScoreView);

        final float translationX = currentPlayer.getMiniHandLayout().getTranslationX();
        final float translationY = currentPlayer.getMiniHandLayout().getTranslationY();
        //translate from the middle of the screen in X, and about 400f in Y (TODO:A measure from melds)
        final ObjectAnimator scoreXAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationX", -translationX, 0f);
        final ObjectAnimator scoreYAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationY", +400f, 0f);

        scaleAndTranslateSet.addListener(new Animator.AnimatorListener() {
                 @Override
                 public void onAnimationStart(Animator animator) {
                     currentPlayer.updatePlayerMiniHand(true, false);
                 }

                 @Override
                 public void onAnimationEnd(Animator animator) {
                     currentPlayer.updatePlayerMiniHand(true, true); //to show cumulative score when roundScore arrives
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

    void animatePlayerMiniHand(PlayerMiniHandLayout playerMiniHandLayout) {
        final Animation bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.hand_click_me);
        playerMiniHandLayout.getCardView().startAnimation(bounceAnimation);
    }



    /*----------------------------------------------*/
    /* SHOW CONTENTS OF YOUR HAND                   */
    /*----------------------------------------------*/
    void updateHandsAndCards(final boolean showCardsAlways, boolean afterFinalTurn) {
        showDiscardPileCard();
        //automatically shows all round scores if final round
        mGame.updatePlayerMiniHands(false);

        if (mGame.getCurrentPlayer() == mGame.getPlayerWentOut()) {
            mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(),mGame.getPlayerWentOut().getName()));
            mGameInfoToast.show();
        }
        //showCards is true if being called from Human Player or by Computer Player with showComputerCards set
        if (showCardsAlways || afterFinalTurn) {
            int iBase = showCards(mGame.getCurrentPlayer().getHandUnMelded(), mCurrentCards, 0, false, mGame.getCurrentPlayer().isHuman());
            showCards(mGame.getCurrentPlayer().getHandMelded(), mCurrentMelds, iBase, true, mGame.getCurrentPlayer().isHuman());
        }else {
            //now just clears the cards&melds area
            showNothing(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
        }
    }

    //shows nothing (mini-hand has the draw or discard)
    private void showNothing(final int numCards, final RelativeLayout cardsLayout, final RelativeLayout meldsLayout) {
        cardsLayout.removeAllViews();
        meldsLayout.removeAllViews();
        /* show just the backs of cards
        ArrayList<CardView> cardLayers = new ArrayList<>(Game.MAX_CARDS);
        float xOffset=0f;
        for (int iCard=0; iCard<numCards; iCard++) {
            CardView cv = new CardView(this, CardView.sBlueBitmapCardBack );
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
        int yMeldOffset= (int) (+MELD_OFFSET_RATIO * CardView.INTRINSIC_HEIGHT);
        int iView=iViewBase;
        relativeLayout.removeAllViews();

        for (CardList cardList : meldLists) {
            if (cardList.isEmpty()) continue;
            RelativeLayout nestedLayout = new RelativeLayout(this);
            nestedLayout.setTag(cardList); //so we can retrieve which meld a card is dragged onto
            //Create and add an invisible view that fills the necessary space for all cards in this meld
            CardView cvTemplate = new CardView(this, CardView.sBlueBitmapCardBack);
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



    private void showDiscardPileCard() {
        //TODO:B better would be card stack/list that automatically associates the right card
        // but right now we need to change the Card/Image for a fixed CardView (which is linked into the display)
        //this handles peekDiscardPileCard == null
        mDiscardPile.setCard(this, mGame.peekDiscardPileCard());
    }

    void showHint(final String mText) {
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
            //pop open the Players dialog for the names
            EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(null, false, true, -1);
            epdf.show(getFragmentManager(), null);
        }
    }

    void showEditPlayer(final String oldPlayerName, final boolean oldIsHuman, final int iPlayer) {
        EnterPlayersDialogFragment epdf = EnterPlayersDialogFragment.newInstance(oldPlayerName, oldIsHuman, false, iPlayer);
        epdf.show(getFragmentManager(),null);
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


    /* DEPRECATED METHODS */
    //Keeping this code in case we reintroduce and end-of-game score list
    @Deprecated
    private void showPlayerScores2(List<Player> players) {

        if (null == mScoreDetail) {
            //mScoreDetail = (TableLayout) findViewById(R.id.scoreDetail);
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


}
