package com.example.jeffrey.fivekings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;

/**
 * 4/3/2015 Created to show settings
 *          Currently just showComputerCards (on every hand) and animateDealing
 */
public class SettingsDialogFragment extends DialogFragment {
    private static final String showComputerCardsArg = "SHOW_COMPUTER_CARDS";
    private static final String animateDealingArg = "ANIMATE_DEALING";


    //use newInstance to pass arguments to the Bundle which the dialog can access
    // apparently this is preferred to custom member fields and setters
    static SettingsDialogFragment newInstance(final boolean oldShowComputerCards, final boolean oldAnimateDealing) {
        SettingsDialogFragment ePDF = new SettingsDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(showComputerCardsArg, oldShowComputerCards);
        args.putBoolean(animateDealingArg, oldAnimateDealing);
        ePDF.setArguments(args);
        return ePDF;
    }

    @Override
    public Dialog onCreateDialog(final Bundle args) {
        //use getArguments because they were passed to the fragment
        final boolean oldShowComputerCards=getArguments().getBoolean(showComputerCardsArg, false);
        final boolean oldAnimateDealing = getArguments().getBoolean(animateDealingArg, false);
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
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SettingsDialogFragment.this.getDialog().cancel();
                    }
                });
        //show the current settings
        ((Checkable) tv.findViewById(R.id.show_computer_cards)).setChecked(oldShowComputerCards);
        ((Checkable)tv.findViewById(R.id.animate_dealing)).setChecked(oldAnimateDealing);

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
                    //Register the FiveKings activity method as the callback
                    ((FiveKings) getActivity()).setSettings(showComputerCards, animateDealing);
                    dismiss(); //dismiss the dialog on [Save]
                }
            });
        }
    }//end onStart()
}