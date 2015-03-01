package com.example.jeffrey.fivekings;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jeffrey.fivekings.util.SystemUiHider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
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
 */

public class FiveKings extends Activity {
    static final float CARD_OFFSET =25.0f;
    static final float ADDITIONAL_MELD_OFFSET = 20.0f;
    static final float CARD_WIDTH = 50.0f;
    static final int TOAST_X_OFFSET = 20;
    static final int TOAST_Y_OFFSET = +250;

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
                playGameClicked(v);
            }
        });
        mPlayButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), R.string.startingGame, Toast.LENGTH_SHORT).show();
                mPlayButton.setText(getText(R.string.newGame));
                mGame.setGameState(GameState.NEW_GAME);
                playGameClicked(v);
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
        mDiscardPile.setClickable(false);
        mDiscardPile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDiscardPile(v);
            }
        });
        mDiscardPile.setOnDragListener(new DiscardPileDragEventListener());
        mDrawPileButton = (ImageButton)findViewById(R.id.drawPile);
        mDrawPileButton.setEnabled(false);
        mDrawPileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clickedDrawPile(v);
            }
        });
        mCurrentMelds = (RelativeLayout) findViewById(R.id.current_melds);
        mCurrentMelds.setOnDragListener(new CurrentMeldsLayoutDragListener());
        mCurrentCards = (RelativeLayout) findViewById(R.id.current_cards);

    }//end onCreate

 /*   EVENT HANDLERS*/
    //Event handler for [Save] in add player dialog
 public void addEditPlayerClicked(String playerName, boolean isHuman, boolean addingFlag, int iPlayer) {
     if (addingFlag) mGame.addPlayer(playerName, isHuman);
     else mGame.updatePlayer(playerName, isHuman, iPlayer);
     showPlayerScores(mGame.getPlayers());
 }

    //the Event handler for button presses on [Play]
    private void playGameClicked(View v){
        enablePlayDisableDrawDiscard(true);
        if (null== mGame) {
            mGame = new Game(this); //also sets gameState=ROUND_START
            this.mCurrentRound.setText(resFormat(R.string.current_round,mGame.getRoundOf().getString()));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            showPlayerScores(mGame.getPlayers());
            showAddPlayers();
        }

        //User pressed [New Game] button
        else if (GameState.NEW_GAME == mGame.getGameState()) {
            mGame.init();
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextRound, mGame.getRoundOf().getString()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            showPlayerScores(mGame.getPlayers());
        }

        //starting a round
        else if (GameState.ROUND_START == mGame.getGameState()) {
            mGame.initRound();
            this.mCurrentRound.setText(resFormat(R.string.current_round, mGame.getRoundOf().getString()));
            mInfoLine.setText(getText(R.string.blank));
            this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            mCurrentCards.removeAllViews();
            mCurrentMelds.removeAllViews();
            syncDiscardMeldsCards(null);
        }

        else if (GameState.TAKE_COMPUTER_TURN == mGame.getGameState()) {
            StringBuilder turnInfo = new StringBuilder(100);
            Player playerWentOut=null;

            turnInfo.setLength(0);
            playerWentOut = mGame.takeComputerTurn(getText(R.string.computerTurnInfo).toString(), turnInfo);

            if (playerWentOut != null) {
                mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(),playerWentOut.getName()));
                mGameInfoToast.show();
            }
            syncDiscardMeldsCards(turnInfo);
            //returns false if we've reached the player who went out again
            if (mGame.endTurn()) this.mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            else this.mPlayButton.setText(getText(R.string.showScores));
        }

        else if (GameState.TURN_START == mGame.getGameState()) {
            syncDiscardMeldsCards(null);

            if (mGame.getPlayer().isHuman()) {
                mInfoLine.setText(resFormat(R.string.yourCardsAndMelds, mGame.getPlayer().getName()));
                enablePlayDisableDrawDiscard(false);
                mGame.setGameState(GameState.TAKE_HUMAN_TURN);
            }
            else {
                mInfoLine.setText(resFormat(R.string.computerCardsAndMelds, mGame.getPlayer().getName()));
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

    //Event handler for clicks on Discard Pile
    private void clickedDiscardPile(View v) {
        drawOrDiscard(Game.USE_DISCARD_PILE);
    }

    //Event handler for clicks on Draw Pile
    private void clickedDrawPile(View v) {
        drawOrDiscard(Game.USE_DRAW_PILE);
    }

    private void drawOrDiscard(boolean useDiscardPile) {
        StringBuilder turnInfo = new StringBuilder(100);

        turnInfo.setLength(0);
        mGame.takeHumanTurn(getText(R.string.humanTurnInfo).toString(), turnInfo, useDiscardPile); //also sets GameState to END_HUMAN_TURN
        enablePlayDisableDrawDiscard(false, false); //disable drawing again from the Discard Pile
        syncDiscardMeldsCards(turnInfo);
    }


//Event Handler for clicks on cards or melds - called from DiscardPile drag listener
    boolean discardedCard(int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);

        if (GameState.END_HUMAN_TURN == mGame.getGameState()) {
            if (!mGame.getPlayer().isHuman()) throw new RuntimeException("discardedCard: player is not Human");
            Player playerWentOut = mGame.endHumanTurn(foundCardView.getCard());
            if (playerWentOut != null) {
                mGameInfoToast.setText(String.format(getText(R.string.wentOut).toString(), playerWentOut.getName()));
                mGameInfoToast.show();
            }
            syncDiscardMeldsCards(null);

            //returns false if we've reached the player who went out again
            if (mGame.endTurn()) mPlayButton.setText(resFormat(R.string.nextPlayer, mGame.getPlayer().getName()));
            else mPlayButton.setText(getText(R.string.showScores));

            enablePlayDisableDrawDiscard(true);
        }
        return true;
    }

    //makeNewMeld is called if you drag onto the mCurrentMelds layout - we're creating a new meld
    // later we'll allow moving between melds and reordering the cards
    //to add to a existing meld, drag to an existing meld
    boolean makeNewMeld(int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //create trialMeld (one card at a time for now)

        mGame.getPlayer().makeNewMeld(foundCardView.getCard());
        syncDiscardMeldsCards(null);
        return true;
    }

    //Don't test for valid meld; that is done with evaluateMelds
    boolean addToMeld(CardList meld, int iCardView) {
        CardView foundCardView= findViewByIndex(iCardView);
        if (foundCardView == null) return false;
        //add to existing meld

        mGame.getPlayer().addToMeld(meld, foundCardView.getCard());
        syncDiscardMeldsCards(null);
        return true;
    }



    CardView findViewByIndex(int iCardView) {
        CardView foundCardView=null;
        //find the view we coded with this index - have to loop thru nested layouts
        for (int iNestedLayout = 0; iNestedLayout < mCurrentCards.getChildCount(); iNestedLayout++) {
            RelativeLayout nestedLayout = (RelativeLayout)mCurrentCards.getChildAt(iNestedLayout);
            if (null == nestedLayout) break;
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
                RelativeLayout nestedLayout = (RelativeLayout) mCurrentMelds.getChildAt(iNestedLayout);
                if (null == nestedLayout) break;
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

    /* COMMON METHODS FOR MANAGING UI ELEMENTS */
    //TODO:A: Also enable/disable dragging of discard when not human on discard step
    private void enablePlayDisableDrawDiscard(boolean enable) {
        if (this.mPlayButton != null) {
            this.mPlayButton.setEnabled(enable);
            this.mPlayButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
        if (this.mDrawPileButton != null) this.mDrawPileButton.setEnabled(!enable);
        if (this.mDiscardPile != null) this.mDiscardPile.setClickable(!enable);
    }
    private void enablePlayDisableDrawDiscard(boolean enablePlay, boolean enablePiles) {
        enablePlayDisableDrawDiscard(enablePlay);
        if (this.mDrawPileButton != null) this.mDrawPileButton.setEnabled(enablePiles);
        if (this.mDiscardPile != null) this.mDiscardPile.setClickable(enablePiles);
        //cycle alpha animation to show you can pick from them - hack until we can do highlighting
        //TODO:A This is not showing up at all
        final AlphaAnimation alphaFade = new AlphaAnimation(0.0F, 1.0F);
        if (enablePiles) {
            alphaFade.setDuration(1000); // Make animation instant
            alphaFade.setRepeatCount(Animation.INFINITE);
            alphaFade.setRepeatMode(Animation.REVERSE);
            mDrawPileButton.startAnimation(alphaFade);
            mDiscardPile.startAnimation(alphaFade);
        }
        else {
            mDrawPileButton.clearAnimation();
            mDiscardPile.clearAnimation();
        }
    }

    private void syncDiscardMeldsCards(StringBuilder turnInfo) {
        //Show card on discard pile (changes because of this player's play)
        //TODO:A Make DiscardPile a stacked list of clickable cards so the correct one automatically shows
        mDiscardPile.setImageDrawable(mGame.getDiscardPileDrawable(this));

        //don't show computer cards unless SHOW_ALL_CARDS is set or final round
        if (mGame.getPlayer().isHuman() || Game.SHOW_ALL_CARDS
                ||((GameState.TAKE_COMPUTER_TURN == mGame.getGameState()) && (mGame.getPlayerWentOut()!=null))) {
            int iBase = showCards(mGame.getPlayer().getHandUnMelded(),  mCurrentCards, 0);
            showCards(mGame.getPlayer().getHandMelded(), mCurrentMelds, iBase);
        }
        else {
            showCardBacks(mGame.getRoundOf().getRankValue(), mCurrentCards, mCurrentMelds);
        }
        mInfoLine.setText(turnInfo);
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
            xOffset += CARD_OFFSET;
        }
        //bit of a hack: we now adjust everything by - 1/2*(width of stacked image) to center it
        // add CARD_WIDTH because the total width of the stacked image is sum(offsets) + CARD_WIDTH
        // subtract CARD_OFFSET and ADDITIONAL_MELD_OFFSET because we added these at the end of the loop (but they're not in the layout)
        xOffset = xOffset - CARD_OFFSET - ADDITIONAL_MELD_OFFSET + CARD_WIDTH;
        if (!cardLayers.isEmpty()) {
            for (CardView cv : cardLayers) {
                cv.setTranslationX(cv.getTranslationX()-0.5f*xOffset);
                cardsLayout.addView(cv);
            }
        }
    }

    //show melds and unmelded as ImageViews
    //TODO:A Don't need to set listeners for non-Human cards
    private int showCards(ArrayList<CardList> meldLists, RelativeLayout relativeLayout, int iViewBase) {
        ArrayList<CardView> cardLayers = new ArrayList<>(Game.MAX_CARDS);
        float xCardOffset=0f;
        float xMeldOffset=0f;
        int iView=iViewBase;
        relativeLayout.removeAllViews();
        for (CardList cardList : meldLists) {
            RelativeLayout nestedLayout = new RelativeLayout(this);
            nestedLayout.setTag(cardList); //so we can retrieve which meld a card is dragged onto
            nestedLayout.setLayoutParams(relativeLayout.getLayoutParams());
            nestedLayout.setTranslationX(xMeldOffset);
            xMeldOffset += ADDITIONAL_MELD_OFFSET;
            for (Card card : cardList.getCards()) {
                CardView cv = new CardView(this, card, iView );
                cv.setTag(iView); //allows us to pass the index into the dragData without dealing with messy object passing
                iView++;
                cv.setTranslationX(xCardOffset);
                xCardOffset += CARD_OFFSET;
                cv.bringToFront();
                cv.setClickable(false); //TODO:A: Allow selection by clicking for multi-drag?
                cv.setOnDragListener(new CardViewDragEventListener() );
                cv.setOnLongClickListener(new View.OnLongClickListener() {
                    // Defines the one method for the interface, which is called when the View is long-clicked
                    public boolean onLongClick(View v) {
                        // Create a new ClipData using the tag as a label, the plain text MIME type, and
                        // the string containing the View index. This will create a new ClipDescription object within the
                        // ClipData, and set its MIME type entry to "text/plain"
                        ClipData dragData = ClipData.newPlainText("Discard", v.getTag().toString());
                        View.DragShadowBuilder myShadow = new CardViewDragShadowBuilder(v);

                        final AlphaAnimation alphaFade = new AlphaAnimation(0.5F, 0.5F);
                        alphaFade.setDuration(0); // Make animation instant
                        alphaFade.setFillAfter(true); // Tell it to persist after the animation ends
                        v.startAnimation(alphaFade);
                        // Starts the drag
                        v.startDrag(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                null,      // no need to use local data
                                0          // flags (not currently used, set to 0)
                        );
                        return true;
                    }//end onClick
                });

                cardLayers.add(cv);
                nestedLayout.addView(cv);
                nestedLayout.setOnDragListener(new CurrentMeldDragListener());
            }//end cards in meld
            relativeLayout.addView(nestedLayout);
        }
        //bit of a hack: we now adjust everything by - 1/2*(width of stacked image) to center it
        // add CARD_WIDTH because the total width of the stacked image is sum(offsets) + CARD_WIDTH
        // subtract CARD_OFFSET and ADDITIONAL_MELD_OFFSET because we added these at the end of the loop (but they're not in the layout)
        //TODO:A: This is wrong now that we are using nested layouts
       /* xCardOffset = xCardOffset - CARD_OFFSET - ADDITIONAL_MELD_OFFSET + CARD_WIDTH;
        if (!cardLayers.isEmpty()) {
            for (CardView cv : cardLayers) {
                cv.setTranslationX(cv.getTranslationX() - 0.5f * xCardOffset);
            }
        }*/
        return iView; //used as the starting index for mCurrentMelds
    }//end showCards


    //TODO:A Look at switching to ListView, especially since all Table Rows are the same
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
