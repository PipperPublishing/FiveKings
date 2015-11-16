/*
 * Copyright Jeffrey Pugh (pipper.publishing@gmail.com) (c) 2015. All rights reserved.
 */

package com.pipperpublishing.fivekings.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.pipperpublishing.fivekings.R;

/**
 * Created by Jeffrey on 2/18/2015.
 * 2/18/2015 Captures player names at start of game
 * 3/11/2015    CLoses the dialog after each Add
 *              Include a Delete option in the Edit dialog
 */
public class EnterPlayersDialogFragment extends DialogFragment {
    private static final String PLAYER_ARG = "PLAYER_NAME";
    private static final String IS_HUMAN_ARG = "IS_HUMAN";
    private static final String IS_HARD_COMPUTER_ARG = "IS_HARD_COMPUTER";
    private static final String IS_EXPERT_COMPUTER_ARG = "IS_EXPERT_COMPUTER";
    private static final String ADDING_ARG = "ADDING";
    private static final String I_PLAYER_ARG = "I_PLAYER";


    //use newInstance to pass arguments to the Bundle which the dialog can access
    // apparently this is preferred to custom member fields and setters
    static EnterPlayersDialogFragment newInstance(final String oldPlayerName, final boolean oldIsHuman, boolean oldIsHardComputer, boolean oldIsExpertComputer, final boolean addingFlag, final int iPlayer) {
        EnterPlayersDialogFragment ePDF = new EnterPlayersDialogFragment();
        Bundle args = new Bundle();
        args.putString(PLAYER_ARG, oldPlayerName);
        args.putBoolean(IS_HUMAN_ARG, oldIsHuman);
        args.putBoolean(IS_HARD_COMPUTER_ARG, oldIsHardComputer);
        args.putBoolean(IS_EXPERT_COMPUTER_ARG, oldIsExpertComputer);
        args.putBoolean(ADDING_ARG, addingFlag);
        args.putInt(I_PLAYER_ARG, iPlayer);

        ePDF.setArguments(args);
        return ePDF;
    }

    @Override
    public Dialog onCreateDialog(final Bundle args) {
        //use getArguments because they were passed to the fragment
        final String playerName=getArguments().getString(PLAYER_ARG, "");
        final boolean isHuman=getArguments().getBoolean(IS_HUMAN_ARG, false);
        final boolean isHardComputer=getArguments().getBoolean(IS_HARD_COMPUTER_ARG, false);
        final boolean isExpertComputer=getArguments().getBoolean(IS_EXPERT_COMPUTER_ARG, true);
        final boolean addingFlag = getArguments().getBoolean(ADDING_ARG, false);
        final int iPlayer = getArguments().getInt(I_PLAYER_ARG, -1);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View tv = inflater.inflate(R.layout.five_kings_enter_players, null);
        builder.setView(tv)
                .setTitle(getText(addingFlag ? R.string.addPlayersTitle : R.string.editPlayersTitle))
                // Add action buttons
                .setNegativeButton(getText(addingFlag ? R.string.add : R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //do nothing here because otherwise the button won't get instantiated
                    }
                })
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EnterPlayersDialogFragment.this.getDialog().cancel();
                    }
                });
        if (!addingFlag) {
            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
        }
        ((TextView) tv.findViewById(R.id.player_name)).setText(playerName);
        ((RadioButton)tv.findViewById(R.id.is_human)).setChecked(isHuman);
        ((RadioButton)tv.findViewById(R.id.is_hard_computer)).setChecked(isHardComputer);
        ((RadioButton)tv.findViewById(R.id.is_expert_computer)).setChecked(isExpertComputer);

        return builder.create();
    }

    //This incredible solution to keeping the default dialog open courtesy of http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final boolean isAddingFlag = getArguments().getBoolean(ADDING_ARG,true);
        final int iPlayer = getArguments().getInt(I_PLAYER_ARG, -1);
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button addEditButton = d.getButton(Dialog.BUTTON_NEGATIVE);
            addEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Retrieve the new/changed values
                    String playerName = ((EditText) EnterPlayersDialogFragment.this.getDialog().findViewById(R.id.player_name)).getText().toString();
                    boolean isHuman = ((RadioButton) EnterPlayersDialogFragment.this.getDialog().findViewById(R.id.is_human)).isChecked();
                    boolean isHardComputer = ((RadioButton) EnterPlayersDialogFragment.this.getDialog().findViewById(R.id.is_hard_computer)).isChecked();
                    boolean isExpertComputer = ((RadioButton) EnterPlayersDialogFragment.this.getDialog().findViewById(R.id.is_expert_computer)).isChecked();
                    //Register the FiveKings activity method as the callback
                    ((FiveKings) getActivity()).addEditPlayerClicked(playerName, isHuman, isHardComputer, isExpertComputer, isAddingFlag, iPlayer);
                    dismiss(); //dismiss the dialog on add or edit
                }
            });
            Button deleteButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Register the FiveKings activity method as the callback
                    ((FiveKings) getActivity()).deletePlayerClicked(iPlayer);
                    dismiss();
                }
            });
        }
    }//end onStart()
}
