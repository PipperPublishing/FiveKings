package com.pipperpublishing.fivekings;

/**
 * Created by Jeffrey on 2/14/2015.
 * 4/1/2015 Removed ROUND_END
 * 9/30/2015    Added ROUND_READY_TO_DEAL and ROUND_END to help with restarting because of orientation change etc
 */
enum GameState {
    NEW_GAME, ROUND_START, ROUND_READY_TO_DEAL, TURN_START, TAKE_TURN,
    HUMAN_PICKED_CARD, END_HUMAN_TURN, ROUND_END, GAME_END
};
