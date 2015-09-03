package com.pipperpublishing.fivekings;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.pipperpublishing.fivekings.util.Utilities;

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
    4/6/2015    Display hints if you click in the wrong place
    4/7/2015    Delete Deprecated methods
                explodeHands shows winner's hand spiraling out
    4/8/2015    explodeHand shows deck spiraling out randomly from deal position
    4/9/2015    Added enable/disableDragToDiscardPile to prevent drag and drop before you've picked
                Return AnimatorSet from animateDealing so you can cancel it with Play button
                Change showCards to disable dragging after Final Turn
    6/3/2015    showCards: Compute height of cards as 50% of RelativeLayout
    6/6/2015    Added CARD_SCALING (the ratio of the card height to that of the Meld Layout so that the cards can be arranged +/- 50% offset)
    6/11/2015   Change from DECK_SCALING to use actual scaled height of deck (that is dynamically controlled by the LinearLayout it is placed in)
    6/17/2015   Try adding Discard and DrawPile programmatically to solve weird translation problem
                Use setAdjustViewBounds and position relative to spacer
    8/25/2015   Read/set settings for ShowComputerHands and AnimateDealing
    8/26/2015   Show Help unless ShowHelp setting false; also have a checkbox in the dialog?
    8/29/2015   Implement onSave/onRestore Instance State to recreate correct layout AND correct round etc.
    8/30/2015   Eliminate showIntro and get it directly from settings
    9/2/2015    Get scaledCardHeight (assumed to be the same for mCurrentCards and mCurrentMelds) when window first is focused
                Add flag needToRefreshCardsMelds to ensure relayout on Orientation change etc.
                Add flag isAfterDealing to indicate whether we should show a card or not
                Redo animateHumanPickup and animateRoundScore to use actual positions on screen
    9/3/2015    We *should * override onSave/onRestore for each custom View to bring it back correctly (and handling layout changes there)
                Instead, we're refreshing the dynamic elements which makes this fragile

*/

public class FiveKings extends Activity {
    static final String SETTINGS_NAME="SettingsFile";
    static final String SHOW_COMPUTER_HANDS_SETTING="showComputerHands";
    static final String ANIMATE_DEALING_SETTING="animateDealing";
    static final String SHOW_HELP="showIntro";

    static final float CARD_OFFSET_RATIO = 0.2f;
    static final float CARD_SCALING = 0.6f; //in the meld area, the cards are 1.5 x CARD_SCALING + a margin for the border, to fit in the meld area
    static final float X_MELD_OFFSET_RATIO = 0.3f;
    static final float Y_MELD_OFFSET_RATIO = 0.25f; //each meld is +/-25% of card height from the center-line
    //static final int TOAST_X_OFFSET = 20;
    //static final int TOAST_Y_OFFSET = +600;
    static final Rank HELP_ROUNDS=Rank.FOUR;
    static final float ALMOST_TRANSPARENT_ALPHA=0.1f;
    static final float HALF_TRANSPARENT_ALPHA=0.5f;
    static final float THIRD_TRANSPARENT_ALPHA=0.3f;
    static final float INVISIBLE_ALPHA=0.0f;

    private static final int ANIMATION_2S =2000;
    private static final int ANIMATION_1S = 1000;
    private static final int ANIMATION_500MS =500;
    private static final int ANIMATION_100MS =100;

    private static final int DISCARDPILE_VIEW_ID=1000;
    private static final int DRAWPILE_VIEW_ID=1001;

    // Dynamic elements of the interface
    //TODO:A could probably merge Game up into FiveKings to simplify things
    private Game mGame=null;
    private float scaledCardHeight;
    private boolean needToRefreshCardsMelds =true;
    private boolean isAfterDealing =false;

    private ProgressBar spinner;
    private Button mPlayButton;
    private TableLayout mScoreDetail;
    private View mScoreDetailView;
    private Toast mHintToast;
    private String mHint;
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
    private AnimatorSet dealAnimatorSet;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    //Note that we merge onRestoreSavedInstanceState in here because of logic related to restoring Game state
    protected void onCreate(Bundle savedInstanceState) {
        //auto-magically recreates static elements (defined in the XML)
        super.onCreate(savedInstanceState);

        //invalidated by a re-layout
        needToRefreshCardsMelds = true;

        //This section does not differ whether it's a new instance or a resumed one
        setContentView(R.layout.activity_five_kings);

        //Initialize other dynamic elements of the UI (e.g. handlers) or get references for later manipulation

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

        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

        spinner = (ProgressBar)findViewById(R.id.progressCircle);
        spinner.setVisibility(View.GONE);

        // This section differs whether this is being newly CREATED (Bundle is null) or RESUMED
        if (savedInstanceState == null) {
            dealAnimatorSet = null;
            mGame = new Game();
            setupDrawAndDiscardPiles(); //needs deck created in Game
            showHideDrawAndDiscardPiles(false);
            disableDrawDiscardClick();
            mHint = getText(R.string.toStartGameHint).toString();
            isAfterDealing = false;
        }
        else {
            mGame = savedInstanceState.getParcelable("mGame");
            mHint = savedInstanceState.getString("mHint", getText(R.string.toStartGameHint).toString());
            isAfterDealing = savedInstanceState.getBoolean("isAfterDealing", isAfterDealing);

            //Show/Hide state of Draw and Discard pile is whatever it was when saved
            setupDrawAndDiscardPiles();
            mGame.relayoutPlayerMiniHands(this); //because these are completely dynamic
        }

        mHintToast = Toast.makeText(this,mHint, Toast.LENGTH_SHORT);
        mHintToast.show();

        //get current settings from SharedPreferences if they exist
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        mGame.setShowComputerCards(settings.getBoolean(SHOW_COMPUTER_HANDS_SETTING, false));
        mGame.setAnimateDealing(settings.getBoolean(ANIMATE_DEALING_SETTING, true));

        //Note we cannot actually refresh the card display here because the Views have not been measured yet
        // and so getHeight returns 0 (which we use for dynamic scaling of card heights)
    }//end onCreate

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // saves the View hierarchy state
        /* We *should * override onSave/onRestore for each custom View to bring it back correctly (and handling layout changes there)
        Instead, we're refreshing the dynamic elements which makes this fragile */

        super.onSaveInstanceState(savedInstanceState);

        //save Game info
        savedInstanceState.putParcelable("mGame", mGame);
        savedInstanceState.putString("mHint", mHint);
        savedInstanceState.putBoolean("isAfterDealing", isAfterDealing);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            //get the scaledCardHeight to fit the melds/cards 50% overlapping
            //set it here because we don't know height until the window is focused
            scaledCardHeight = CARD_SCALING * mCurrentCards.getHeight();

            if (needToRefreshCardsMelds && (mGame != null)) {
                mGame.relayoutPlayerMiniHands(this);
                //FIXME: First parameter depends on COmputer||showCardsAlways or Human  - override in those
                updateHandsAndCards(true, mGame.isFinalTurn());
                needToRefreshCardsMelds = false;
            }
            //if we resume from a screen with the hands dealt, then we need to reset that
            //FIXME: This should be handled totally within the miniHands (they should know that a card is showing)
            //so that when you re-lay them out they set the card showing correctly
            if (isAfterDealing) this.dealAnimatorSet = animateDealing(mGame.getRoundOf().getRankValue(), mGame.numPlayers(), false);
        }
    }

    /*------------------*/
    /*   EVENT HANDLERS */
    /*------------------*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
                SettingsDialogFragment settingsDialogFragment = SettingsDialogFragment.newInstance(mGame.isShowComputerCards(),mGame.isAnimateDealing(), settings.getBoolean(SHOW_HELP, true));
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
                IntroBox.show(FiveKings.this);
                return true;
            case R.id.action_about:
                AboutBox.show(FiveKings.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //TODO:A Seems like we could eliminate one set of setting variables and get them directly from the Preferences
    public void setSettings(final boolean showComputerHands, final boolean animateDealing, boolean showHelp) {
        //set locally...
        mGame.setShowComputerCards(showComputerHands);
        mGame.setAnimateDealing(animateDealing);
        //...and also store in preferences
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SHOW_COMPUTER_HANDS_SETTING, showComputerHands);
        editor.putBoolean(ANIMATE_DEALING_SETTING, animateDealing);
        editor.putBoolean(SHOW_HELP, showHelp);

        // Commit the edits!
        editor.commit();
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
            mGame.updatePlayerMiniHands();
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
        if (dealAnimatorSet != null) dealAnimatorSet.cancel();
        if (GameState.NEW_GAME == mGame.getGameState()) {
            newGame();
        }
        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            mGame.resetPlayerMiniHandsToRoundStart();
            this.mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            this.dealAnimatorSet = animateDealing(mGame.getRoundOf().getRankValue(), mGame.numPlayers(), mGame.isAnimateDealing());
            mGame.getNextPlayer().findBestHandStart(mGame.isFinalTurn(), mGame.peekDiscardPileCard());
            //Show blank screen with discard pile showing - now in onAnimationEnd
            setmHint(R.string.clickMovingHandHint);
        }

        else showHint(null, true); //force show the current hint
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
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        if (settings.getBoolean(SHOW_HELP, true)) IntroBox.show(FiveKings.this);
        return true;
    }

    void endTurnCheckRound(boolean showCards) {
        mGame.endCurrentPlayerTurn();
        updateHandsAndCards(showCards, mGame.isFinalTurn());
        if (mGame.isFinalTurn()) {
            // addToCumulativeScore() moved from animateRoundScore, because logging in checkEndRound continues before animation completes
            // maintain a separate displayed score in the miniHand
            mGame.getCurrentPlayer().addToCumulativeScore();
            animateRoundScore(mGame.getCurrentPlayer());
        }
        mGame.checkEndRound();
        GameState gameState = mGame.getGameState();
        if (GameState.ROUND_START == gameState) {
            mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            showHint(resFormat(R.string.displayScores, null), false);
            setmHint(R.string.nextRoundHint);
        }
        else if (GameState.TURN_START == gameState) {
            mGame.findBestHandStart();
            animatePlayerMiniHand(mGame.getNextPlayer().getMiniHandLayout());
            setmHint(R.string.clickMovingHandHint);
        }
        else if (GameState.GAME_END == gameState) {
            mPlayButton.setText(R.string.newGame);
            explodeHand(mGame.getWinner());
            mGame.logFinalScores();
            mGame.setGameState(GameState.NEW_GAME);
            setmHint(R.string.toStartGameHint);
        }
    }


    /*---------------------------------------------------*/
    /* Event Handlers for clicks/drags on cards or melds */
    /*---------------------------------------------------*/
    boolean discardedCard(final int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        mGame.setHandDiscard(foundCardView.getCard());
        //turn off the ability of DiscardPile to accept drags
        disableDragToDiscardPile();
        endTurnCheckRound(true);
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    //to add to a existing meld, drag to an existing meld
    //TODO:A Move this into the Player because we could be dragging when CurrentPlayer is not Human
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
            Log.e(Game.APP_TAG, "findViewByIndex: No matching card found");
        }
        // else Toast.makeText(this, foundCardView.getCard().getCardString() + " discarded", Toast.LENGTH_SHORT).show();
        return foundCardView;
    }


    /*---------------------------------------------------*/
    /* COMMON METHODS FOR ENABLING/DISABLING UI ELEMENTS */
    /*---------------------------------------------------*/
    void enableDragToDiscardPile() {
        mDiscardPile.setAcceptDrag(true);
    }
    void disableDragToDiscardPile() {
        mDiscardPile.setAcceptDrag(false);
    }

    //just prevent clicking, but for animations we need the buttons to be enabled
    void enableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setEnabled(true);
            this.mDrawPile.setClickable(true);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setEnabled(true);
            this.mDiscardPile.setClickable(true);
        }
        animatePiles();
        setmHint(R.string.pickFromDrawOrDiscardHint);
        showHint(null, false);
    }
    void disableDrawDiscardClick() {
        if (this.mDrawPile != null) {
            this.mDrawPile.setClickable(false);
        }
        if (this.mDiscardPile != null) {
            this.mDiscardPile.setClickable(false);
        }
        setmHint(R.string.meldAndDragDiscardHint);
        showHint(null, false);
    }


    /*--------------------*/
    /* ANIMATION ROUTINES */
    /*--------------------*/
    void showSpinner(final boolean show) {
        spinner.setVisibility(show ? View.VISIBLE : View.GONE);
        spinner.invalidate();
    }

    private AnimatorSet animateDealing(final int numCards, final int numPlayers, boolean doDealing) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        showHideDrawAndDiscardPiles(false);

        //the template card that animates from the center point to the hands
        final CardView deck = new CardView(this, CardView.sBlueBitmapCardBack);
        final CardView dealtCardView = new CardView(this, CardView.sBlueBitmapCardBack);
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(this.getDrawPileWidth(), this.getDrawPileHeight());
        pileLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        deck.setLayoutParams(pileLp);
        drawAndDiscardPiles.addView(deck);

        final AnimatorSet dealSet = new AnimatorSet();
        dealSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mPlayButton.setText(R.string.skipDealing);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mPlayButton.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
                drawAndDiscardPiles.removeView(deck);
                drawAndDiscardPiles.removeView(dealtCardView);
                afterDealing();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                //Don't use showHint, because the text gets overridden in the calling thread
                Toast.makeText(FiveKings.this, R.string.toDisableDealing, Toast.LENGTH_LONG).show();
            }
        });

        //The card is returned to the home point with the first portion of deal_from_drawpile
        //so that each card looks like it's coming from the center drawpile
        Animator dealtCardAnimator = AnimatorInflater.loadAnimator(this, R.animator.deal_from_drawpile);
        Animator lastDealtCardAnimator = null;
        ObjectAnimator alphaAnimator;

        //this is the card we animate
        //if doDealing is false, we only do the Alpha animation to show the cards
        dealtCardView.setLayoutParams(pileLp);
        drawAndDiscardPiles.addView(dealtCardView);
        for (int iCard=0; iCard<numCards; iCard++) {
            for (int iPlayer = 0; iPlayer < numPlayers; iPlayer++) {
                if (doDealing) {
                    dealtCardAnimator = dealtCardAnimator.clone();
                    dealtCardAnimator.setTarget(dealtCardView);
                    //offsets where the cards are dealt according to player
                    ObjectAnimator playerOffsetXAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationX", mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getTranslationX());
                    ObjectAnimator playerOffsetYAnimator = ObjectAnimator.ofFloat(dealtCardView, "TranslationY", mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getTranslationY());
                    if (lastDealtCardAnimator == null)
                        dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator);
                    else
                        dealSet.play(dealtCardAnimator).with(playerOffsetXAnimator).with(playerOffsetYAnimator).after(lastDealtCardAnimator);
                    lastDealtCardAnimator = dealtCardAnimator;
                }
                //if this is the first card, then add an alpha animation to show the blank hand space being replaces by a card
                if (iCard == 0) {
                    CardView playerHandCard = mGame.getPlayerByIndex(iPlayer).getMiniHandLayout().getCardView();
                    alphaAnimator = ObjectAnimator.ofFloat(playerHandCard,"Alpha",ALMOST_TRANSPARENT_ALPHA, 1.0f);
                    alphaAnimator.setDuration(ANIMATION_100MS);
                    if (doDealing) dealSet.play(dealtCardAnimator).with(alphaAnimator);
                    else {
                        dealSet.play(alphaAnimator);
                        lastDealtCardAnimator = alphaAnimator;
                    }
                }

            }//end for iPlayer
        }//end for numCards
        dealSet.start();
        return dealSet;
    }//end animateDealing

    private void afterDealing() {
        mGame.updatePlayerMiniHands();
        showHideDrawAndDiscardPiles(true);
        animatePlayerMiniHand(mGame.getNextPlayer().getMiniHandLayout());
        isAfterDealing = true;
    }

    private void setupDrawAndDiscardPiles() {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final Space spacer = (Space) findViewById(R.id.spacer);

        //set up Discard Pile
        final RelativeLayout.LayoutParams discardPileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDiscardPile = new CardView(this, mGame.peekDiscardPileCard(), DISCARDPILE_VIEW_ID);
        mDiscardPile.setId(DISCARDPILE_VIEW_ID);
        discardPileLp.addRule(RelativeLayout.RIGHT_OF, spacer.getId());
        discardPileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mDiscardPile.setLayoutParams(discardPileLp);
        mDiscardPile.setAdjustViewBounds(true);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.clickedDrawOrDiscard(FiveKings.this, Game.PileDecision.DISCARD_PILE);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());
        showDiscardPileCard();
        drawAndDiscardPiles.addView(mDiscardPile);

        //set up Draw Pile
        final RelativeLayout.LayoutParams drawPileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDrawPile = new CardView(this, CardView.sBlueBitmapCardBack);
        mDrawPile.setId(DRAWPILE_VIEW_ID);
        drawPileLp.addRule(RelativeLayout.LEFT_OF, spacer.getId());
        drawPileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mDrawPile.setLayoutParams(drawPileLp);
        mDrawPile.setAdjustViewBounds(true);
        mDrawPile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.clickedDrawOrDiscard(FiveKings.this, Game.PileDecision.DRAW_PILE);
            }
        });
        drawAndDiscardPiles.addView(mDrawPile);

    }

    private void showHideDrawAndDiscardPiles(final boolean show) {
        mDiscardPile.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        mDrawPile.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    //shakes the Draw and Discard Piles to indicate that you should draw from them
    //TODO:A The alpha fade applied to the DiscardPile on moving a card cancels the shake animation
    private void animatePiles() {
        Animation spinCardAnimation = AnimationUtils.loadAnimation(this, R.anim.card_shake);
        this.mDrawPile.startAnimation(spinCardAnimation);
        this.mDiscardPile.startAnimation(spinCardAnimation);
    }

    void animateHumanPickUp(final Game.PileDecision pickedFrom) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final Space spacer = (Space) findViewById(R.id.spacer);

        //Create the fake card we will animate - copied from animateComputerPickUp
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (pickedFrom == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.RIGHT_OF,spacer.getId());
            pileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            pileCardView.setLayoutParams(pileLp);
            //show the card underneath the one we are animating
            showDiscardPileCard();
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            pileCardView = new CardView(this, CardView.sBlueBitmapCardBack);
            pileLp.addRule(RelativeLayout.LEFT_OF, spacer.getId());
            pileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            pileCardView.setLayoutParams(pileLp);
        }
        pileCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(pileCardView);
        drawAndDiscardPiles.setClipChildren(false);
        drawAndDiscardPiles.setClipToPadding(false);
        //stop the cards jiggling since we have now picked
        this.mDrawPile.clearAnimation();
        this.mDiscardPile.clearAnimation();

        //Use Animator animation to combine shrinking behavior (in XML) with translation
        final AnimatorSet pickUpSet = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedFrom == Game.PileDecision.DISCARD_PILE ? R.animator.from_discardpile : R.animator.from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);

        // Now add translation based on center of currentCards
        int mCurrentCardsLocation[] = new int[2];
        mCurrentCards.getLocationInWindow(mCurrentCardsLocation);
        int spacerLocation[] = new int[2];
        spacer.getLocationInWindow(spacerLocation);
        final float translationX = (mCurrentCardsLocation[0] + mCurrentCards.getWidth()/2) - spacerLocation[0];
        final float translationY = (mCurrentCardsLocation[1] + mCurrentCards.getHeight()/2) - spacerLocation[1];
        // adjustment accounts for different position of Draw and Discard PIle
        float adjustmentX = 0.5f * (this.getDrawPileWidth() + PlayerMiniHandLayout.SPACER_WIDTH);
        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX",
                translationX+ (pickedFrom == Game.PileDecision.DISCARD_PILE ? -adjustmentX : +adjustmentX));
        pickedCardXAnimator.setDuration(ANIMATION_1S);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", translationY);
        pickedCardYAnimator.setDuration(ANIMATION_1S);
        pickUpSet.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        pickUpSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                updateHandsAndCards(true, false); //true because this is human
                //remove extra cards created for the purpose of animation
                drawAndDiscardPiles.removeView(pileCardView);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });
        pickUpSet.start();

    }//end animateHumanPickUp

    /* replaced with new version that calculates where card should go
    void animateHumanPickUp(final Game.PileDecision pickedPile) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);


        if (pickedPile == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT,mDiscardPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM, mDiscardPile.getId());
            //show the card underneath the one we are animating
            showDiscardPileCard();
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            //doesn't show the card until it appears in your hand
            pileCardView = new CardView(this, mGame.getDrawnCard(),-1);
            pileLp.addRule(RelativeLayout.ALIGN_LEFT, mDrawPile.getId());
            pileLp.addRule(RelativeLayout.ALIGN_BOTTOM, mDrawPile.getId());
        }
        pileCardView.setLayoutParams(pileLp);
        pileCardView.setAdjustViewBounds(true);
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
*/

    void animateComputerPickUpAndDiscard(final PlayerMiniHandLayout playerMiniHandLayout, final Game.PileDecision pickedFrom) {
        final RelativeLayout drawAndDiscardPiles = (RelativeLayout)findViewById(R.id.draw_and_discard_piles);
        final Space spacer = (Space) findViewById(R.id.spacer);

        final CardView discardCardView = new CardView(this, mGame.getCurrentPlayer().getHandDiscard(),-1);
        final RelativeLayout.LayoutParams discardLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        discardLp.addRule(RelativeLayout.RIGHT_OF, spacer.getId());
        discardLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        discardCardView.setLayoutParams(discardLp);
        discardCardView.setAlpha(INVISIBLE_ALPHA);
        discardCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(discardCardView);


        //Create the fake card we will animate
        final CardView pileCardView;
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (pickedFrom == Game.PileDecision.DISCARD_PILE) {
            //mDiscardPile still shows the old card if we used it, so we can copy it and then update the pile underneath
            //create a copy of the Discard pile and position over the current Discard Pile
            pileCardView = new CardView(this,mDiscardPile);
            pileLp.addRule(RelativeLayout.RIGHT_OF,spacer.getId());
            pileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            pileCardView.setLayoutParams(pileLp);
            //show the card underneath the one we are animating
            showDiscardPileCard();
        }
        else {
            //create a copy of the top Draw pile card and position over the current pile
            pileCardView = new CardView(this, CardView.sBlueBitmapCardBack);
            pileLp.addRule(RelativeLayout.LEFT_OF, spacer.getId());
            pileLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            pileCardView.setLayoutParams(pileLp);
        }
        pileCardView.setAdjustViewBounds(true);
        drawAndDiscardPiles.addView(pileCardView);

        //Use Animator animation because for Computer we animate pickup to hand
        //and then discard to Discard Pile (could be two different cards)
        final AnimatorSet pickAndDiscard = new AnimatorSet();
        final Animator pickedCardAnimator = AnimatorInflater.loadAnimator(this,
                (pickedFrom == Game.PileDecision.DISCARD_PILE ? R.animator.from_discardpile : R.animator.from_drawpile));
        pickedCardAnimator.setTarget(pileCardView);

        final float translationX = playerMiniHandLayout.getTranslationX();
        /* the original translationX was from the center-horizontal of the fullscreen layout
        but that translation needs to be adjusted by +/-0.5 x (DrawPileWidth + SpacerWidth) for drawing from DiscardPile or DrawPile
         */
        float adjustmentX = 0.5f * (this.getDrawPileWidth() + PlayerMiniHandLayout.SPACER_WIDTH);
        final float translationY = playerMiniHandLayout.getTranslationY();
        final ObjectAnimator pickedCardXAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationX",
                translationX+ (pickedFrom == Game.PileDecision.DISCARD_PILE ? -adjustmentX : +adjustmentX));
        pickedCardXAnimator.setDuration(ANIMATION_500MS);
        final ObjectAnimator pickedCardYAnimator = ObjectAnimator.ofFloat(pileCardView, "TranslationY", translationY);
        pickedCardYAnimator.setDuration(ANIMATION_500MS);
        pickAndDiscard.play(pickedCardAnimator).with(pickedCardXAnimator).with(pickedCardYAnimator);

        /* Here we are always animating back to DiscardPile so the adjustment is always negative*/
        final Animator discardAnimator = AnimatorInflater.loadAnimator(this, R.animator.to_discardpile);
        discardAnimator.setTarget(discardCardView);
        final ObjectAnimator discardXAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationX",translationX-adjustmentX,0f );
        discardXAnimator.setDuration(ANIMATION_1S);
        final ObjectAnimator discardYAnimator = ObjectAnimator.ofFloat(discardCardView, "TranslationY", translationY,0f);
        discardYAnimator.setDuration(ANIMATION_1S);
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

        // Now add translation based on center of currentMelds
        int mCurrentMeldsLocation[] = new int[2];
        mCurrentMelds.getLocationInWindow(mCurrentMeldsLocation);
        int miniHandLayoutLocation[] = new int[2];
        currentPlayer.getMiniHandLayout().getLocationInWindow(miniHandLayoutLocation);
        final float translationX = miniHandLayoutLocation[0] - (mCurrentMeldsLocation[0] + mCurrentMelds.getWidth()/2);
        final float translationY = miniHandLayoutLocation[1] - (mCurrentMeldsLocation[1] + mCurrentMelds.getHeight()/2);

        final ObjectAnimator scoreXAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationX", -translationX, 0f);
        final ObjectAnimator scoreYAnimator = ObjectAnimator.ofFloat(roundScoreView, "TranslationY", -translationY, 0f);

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
        scoreXAnimator.setDuration(ANIMATION_2S);
        scoreYAnimator.setDuration(ANIMATION_2S);
        scaleAndTranslateSet.play(scoreXAnimator).with(scoreYAnimator).with(scaleAnimator);
        scaleAndTranslateSet.start();
    }

    void animatePlayerMiniHand(PlayerMiniHandLayout playerMiniHandLayout) {
        final Animation bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.hand_click_me);
        playerMiniHandLayout.getCardView().startAnimation(bounceAnimation);
    }
    void cancelAnimatePlayerMiniHand(PlayerMiniHandLayout playerMiniHandLayout) {
        playerMiniHandLayout.getCardView().clearAnimation();
    }

    private void explodeHand(final Player winningPlayer) {
        final RelativeLayout fullScreenContent = (RelativeLayout)findViewById(R.id.fullscreen_content);

        //get's the location of a point between the Draw and Discard pile (source for dealt cards)
        final RelativeLayout.LayoutParams pileLp = new RelativeLayout.LayoutParams(this.getDrawPileWidth(),this.getDrawPileHeight());
        pileLp.addRule(RelativeLayout.CENTER_HORIZONTAL);

        mCurrentMelds.removeAllViews();
        mCurrentCards.removeAllViews();
        mDrawPile.setVisibility(View.INVISIBLE);
        mDiscardPile.setVisibility(View.INVISIBLE);
        final AnimatorSet explodeSet = new AnimatorSet();
        Animator cardAnimator = AnimatorInflater.loadAnimator(this, R.animator.spiral_outwards);
        AnimatorSet lastCardAnimatorSet=null;

        //pile the cards in the middle of the screen and then animate them
        final Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        float height =size.y;

        //deal the complete deck - have to clone it because otherwise we can't continue to next game
        Deck deck2 = (Deck) mGame.getDeck().clone();
        while (deck2.peekNext() != null) {
            AnimatorSet cardAnimatorSet = new AnimatorSet();
            Card card = deck2.deal();
            cardAnimator = cardAnimator.clone();
            addExplodeTranslation(explodeSet, cardAnimatorSet, lastCardAnimatorSet, cardAnimator,
                    card, pileLp, height, fullScreenContent);
            lastCardAnimatorSet = cardAnimatorSet;
        }
        explodeSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                new AlertDialog.Builder(FiveKings.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.congratulationsName)
                        .setMessage(String.format(getText(R.string.congratulationsDetails).toString(), winningPlayer.getName(), String.valueOf(winningPlayer.getCumulativeScore())))
                        .setPositiveButton(R.string.newGame, null)
                        .show();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }
        });
        explodeSet.start();
    }
    private void addExplodeTranslation(final AnimatorSet explodeSet, final AnimatorSet cardAnimatorSet, final AnimatorSet lastCardAnimatorSet, final Animator cardAnimator,
                                       final Card card, RelativeLayout.LayoutParams pileLp, final float height , final RelativeLayout fullScreenContent) {
        CardView cardView = new CardView(this, card, -1);
        cardView.setLayoutParams(pileLp);
        fullScreenContent.addView(cardView);
        cardAnimator.setTarget(cardView);
        double randomAngle = Math.toRadians(45 + 90 * Math.random());
        //animate them down from the DrawPile and off the screen
        float translationX = (float) (2*height * Math.cos(randomAngle));
        float translationY = (float) (2*height * Math.sin(randomAngle)) ;
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(cardView, "TranslationX", translationX);
        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(cardView, "TranslationY", translationY);

        //want effect of cards flying out one after another
        cardAnimatorSet.setDuration(3 * ANIMATION_100MS);
        cardAnimatorSet.play(cardAnimator).with(xAnimator).with(yAnimator);

        if (lastCardAnimatorSet == null) explodeSet.play(cardAnimatorSet);
        //every 4/5 (approx) we split the group to provide a more continuous experience
        else if (Math.random() < 0.8) {
            explodeSet.play(cardAnimatorSet).after(lastCardAnimatorSet);
        }else {
            explodeSet.play(cardAnimatorSet).with(lastCardAnimatorSet);
        }
    }

    /*----------------------------------------------*/
    /* SHOW CONTENTS OF YOUR HAND                   */
    /*----------------------------------------------*/
    void updateHandsAndCards(final boolean showCards, boolean afterFinalTurn) {
        showDiscardPileCard();
        //automatically shows all round scores if final round
        //TODO:A If updateHandsAndCards is called once per player, then shouldn't be updating all miniHands
        mGame.updatePlayerMiniHands();

        if (mGame.getCurrentPlayer() != null) {
            if (mGame.getCurrentPlayer() == mGame.getPlayerWentOut()) {
                showHint(String.format(getText(R.string.wentOut).toString(), mGame.getPlayerWentOut().getName()), true);
            }
            //showCards is true if being called from Human Player or by Computer Player with showComputerCards set
            //last parameter controls whether we allow dragging and show the border to drag to create new melds
            if (showCards || afterFinalTurn) {
                int iBase = showCards(mGame.getCurrentPlayer().getHandUnMelded(), mCurrentCards, 0, false,
                        !afterFinalTurn && mGame.getCurrentPlayer().isHuman());
                showCards(mGame.getCurrentPlayer().getHandMelded(), mCurrentMelds, iBase, true,
                        !afterFinalTurn && mGame.getCurrentPlayer().isHuman());
            } else {
                //now just clears the cards&melds area
                showNothing(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
            }
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

    private int showCards(final ArrayList<CardList> meldLists, final RelativeLayout relativeLayout, final int iViewBase, final boolean showBorder, final boolean allowDragging) {
        int iView=iViewBase;
        float cardScaleFactor = scaledCardHeight / CardView.INTRINSIC_HEIGHT ;
        int xMeldOffset=0;
        int xCardOffset=0;
        int yMeldOffset= (int) (+Y_MELD_OFFSET_RATIO * scaledCardHeight);

        relativeLayout.removeAllViews();
        for (CardList cardList : meldLists) {
            if (cardList.isEmpty()) continue;
            RelativeLayout nestedLayout = new RelativeLayout(this);
            nestedLayout.setTag(cardList); //so we can retrieve which meld a card is dragged onto
            //Create and add an invisible view that fills the necessary space for all cards in this meld
            CardView cvTemplate = new CardView(this, CardView.sBlueBitmapCardBack);
            cvTemplate.setVisibility(View.INVISIBLE);
            //the final width is the first card + the offsets of the others x the height scale factor
            nestedLayout.addView(cvTemplate, (int) ((CardView.INTRINSIC_WIDTH + (cardList.size() - 1) * CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH) * cardScaleFactor), (int) scaledCardHeight);
            nestedLayout.setTranslationX(xMeldOffset + xCardOffset);
            nestedLayout.setTranslationY(yMeldOffset);
            xMeldOffset += cardScaleFactor * ((X_MELD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH) + xCardOffset);
            yMeldOffset = -yMeldOffset;


            if (showBorder) {
                //if this is actually a validated meld, outline in solid green
                if (MeldedCardList.isValidMeld(cardList))
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.solid_green_border));
                else
                    nestedLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            }
            xCardOffset=0;
            for (Card card : cardList) {
                CardView cv = new CardView(this, card, iView );
                cv.setTag(iView); //allows us to pass the index into the dragData without dealing with messy object passing
                iView++;
                cv.setTranslationX(xCardOffset);
                xCardOffset += cardScaleFactor * (CARD_OFFSET_RATIO * CardView.INTRINSIC_WIDTH);
                cv.bringToFront();
                cv.setClickable(false); //TODO:B: Allow selection by clicking for multi-drag?
                if (allowDragging) {//no dragging of computer cards or Human cards after final turn
                    cv.setOnDragListener(new CardViewDragEventListener());
                    cv.setOnTouchListener(new View.OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
                            // Create a new ClipData using the tag as a label, the plain text MIME type, and
                            // the string containing the View index. This will create a new ClipDescription object within the
                            // ClipData, and set its MIME type entry to "text/plain"
                            ClipData dragData = ClipData.newPlainText("Discard", v.getTag().toString());
                            View.DragShadowBuilder cardDragShadow = new CardViewDragShadowBuilder(v);

                            v.startAnimation(Utilities.instantFade(FiveKings.HALF_TRANSPARENT_ALPHA, FiveKings.HALF_TRANSPARENT_ALPHA));
                            // Starts the drag
                            v.startDrag(dragData,cardDragShadow, null,0);
                            return true;
                        }//end onClick
                    });
                }//end if allowDragging

                nestedLayout.addView(cv, (int) (cardScaleFactor*CardView.INTRINSIC_WIDTH), (int) scaledCardHeight);
            }//end cards in meld
            if (allowDragging) nestedLayout.setOnDragListener(new CurrentMeldDragListener());
            relativeLayout.addView(nestedLayout);
        }
        if (showBorder && allowDragging) {
            //Now add a "Drag here to create a new meld" space, although the handler will actually be the CurrentMeldLayoutListener
            TextView newMeldSpace = new TextView(this);
            newMeldSpace.setText(R.string.dragYourCards);
            newMeldSpace.setLines(5);
            //Move this a full card's space over from last card
            newMeldSpace.setTranslationX(xMeldOffset+ cardScaleFactor* CardView.INTRINSIC_WIDTH*(1- X_MELD_OFFSET_RATIO)+xCardOffset);
            newMeldSpace.setTranslationY(yMeldOffset);
            newMeldSpace.setTypeface(null, Typeface.ITALIC);
            newMeldSpace.setBackgroundDrawable(getResources().getDrawable(R.drawable.dashed_border));
            newMeldSpace.setEnabled(false);
            relativeLayout.addView(newMeldSpace,(int)(cardScaleFactor* CardView.INTRINSIC_WIDTH), (int) scaledCardHeight);
        }
        return iView; //used as the starting index for mCurrentMelds
    }//end showCards



    private void showDiscardPileCard() {
        //TODO:B better would be card stack/list that automatically associates the right card
        // but right now we need to change the Card/Image for a fixed CardView (which is linked into the display)
        //this handles peekDiscardPileCard == null
        mDiscardPile.setCard(this, mGame.peekDiscardPileCard());
    }

    //shows the existing hint, unless you pass specific text
    void showHint(final String mText, boolean forceShow) {
        if ((mGame != null) && (mGame.getRoundOf() != null)
                && (forceShow || (mGame.getRoundOf().getOrdinal() <= HELP_ROUNDS.getOrdinal()))) {
            mHintToast.setText(mText==null ? mHint : mText);
            mHintToast.show();
        }
    }

    //open Add Player dialog
    void showAddPlayers() {
        if ((mGame != null) && (mGame.getRoundOf() != null) && (mGame.getRoundOf() != Rank.getLowestRank())) {
            showHint(getText(R.string.cantAdd).toString(), true);
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

    int getDrawPileHeight() {
        return this.mDrawPile.getHeight();
    }

    int getDrawPileWidth() {
        return this.mDrawPile.getWidth();
    }


    void setmHint(String mHint) {
        this.mHint = mHint;
    }
    void setmHint(int resource) {
        this.mHint = getText(resource).toString();
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
