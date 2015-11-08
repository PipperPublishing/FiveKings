package com.pipperpublishing.fivekings;

/**
 * Created by Jeffrey on 2/14/2015.
 * 4/1/2015 Removed ROUND_END
 * 9/30/2015    Added ROUND_READY_TO_DEAL and ROUND_END to help with restarting because of orientation change etc
 * 10/18/2015   Added TURN_END to help with restart
 */
public enum GameState {
    NEW_GAME, ROUND_START, ROUND_READY_TO_DEAL, TURN_START,
    HUMAN_PICKED_CARD, HUMAN_READY_TO_DISCARD, TURN_END, ROUND_END, GAME_END
};
