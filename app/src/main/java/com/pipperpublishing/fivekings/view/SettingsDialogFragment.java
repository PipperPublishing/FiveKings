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
import android.widget.Checkable;

import com.pipperpublishing.fivekings.R;

/**
 * 4/3/2015 Created to show settings
 *          Currently just showComputerCards (on every hand) and animateDealing
 * 8/25/2015    Reuse the preference names now being saved in FiveKings
 * 8/26/2015    Add showHelp
 * 11/8/2015    Add highlightWildcards option
 */
public class SettingsDialogFragment extends DialogFragment {

    //use newInstance to pass arguments to the Bundle which the dialog can access
    // apparently this is preferred to custom member fields and setters
    static SettingsDialogFragment newInstance(final boolean oldShowComputerCards, final boolean oldAnimateDealing, boolean oldShowHelp, boolean oldHighlightWildcards) {
        SettingsDialogFragment ePDF = new SettingsDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(FiveKings.SHOW_COMPUTER_HANDS_SETTING, oldShowComputerCards);
        args.putBoolean(FiveKings.ANIMATE_DEALING_SETTING, oldAnimateDealing);
        args.putBoolean(FiveKings.NOVICE_MODE, oldShowHelp);
        args.putBoolean(FiveKings.HIGHLIGHT_WILDCARDS, oldHighlightWildcards);
        ePDF.setArguments(args);
        return ePDF;
    }

    @Override
    public Dialog onCreateDialog(final Bundle args) {
        //use getArguments because they were passed to the fragment
        final boolean oldShowComputerCards=getArguments().getBoolean(FiveKings.SHOW_COMPUTER_HANDS_SETTING, false);
        final boolean oldAnimateDealing = getArguments().getBoolean(FiveKings.ANIMATE_DEALING_SETTING, true);
        final boolean oldShowHelp = getArguments().getBoolean(FiveKings.NOVICE_MODE, true);
        final boolean oldHighlightWildcards = getArguments().getBoolean(FiveKings.HIGHLIGHT_WILDCARDS, true);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View tv = inflater.inflate(R.layout.five_kings_settings, null);
        builder.setView(tv)
                .setTitle(getText(R.string.action_settings))
                // Add action buttons
                .setNegativeButton(getText(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //do nothing here because otherwise the button won't get instantiated
                    }
                })
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SettingsDialogFragment.this.getDialog().cancel();
                    }
                });
        //show the current settings
        ((Checkable) tv.findViewById(R.id.show_computer_cards)).setChecked(oldShowComputerCards);
        ((Checkable)tv.findViewById(R.id.animate_dealing)).setChecked(oldAnimateDealing);
        ((Checkable)tv.findViewById(R.id.show_intro)).setChecked(oldShowHelp);
        ((Checkable)tv.findViewById(R.id.highlight_wildcards)).setChecked(oldHighlightWildcards);
        return builder.create();
    }

    //This incredible solution to keeping the default dialog open courtesy of http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button saveButton = d.getButton(Dialog.BUTTON_NEGATIVE);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Retrieve the new/changed values
                    boolean showComputerCards = ((Checkable) SettingsDialogFragment.this.getDialog().findViewById(R.id.show_computer_cards)).isChecked();
                    boolean animateDealing = ((Checkable) SettingsDialogFragment.this.getDialog().findViewById(R.id.animate_dealing)).isChecked();
                    boolean showHelp = ((Checkable) SettingsDialogFragment.this.getDialog().findViewById(R.id.show_intro)).isChecked();
                    boolean highlightWildcards = ((Checkable) SettingsDialogFragment.this.getDialog().findViewById(R.id.highlight_wildcards)).isChecked();
                    //Register the FiveKings activity method as the callback
                    ((FiveKings) getActivity()).setSettings(showComputerCards, animateDealing,showHelp,highlightWildcards );
                    dismiss(); //dismiss the dialog on [Save]
                }
            });
        }
    }//end onStart()
}
