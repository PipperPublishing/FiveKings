package com.pipperpublishing.fivekings.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.util.Linkify;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pipperpublishing.fivekings.R;

/**
 * Created by Jeffrey on 4/5/2015.
 * From http://tekeye.biz/2012/about-box-in-android-app
 */
class IntroBox {

    static void show(Activity callingActivity) {
        //Use a Spannable to allow for links highlighting
        //Generate views to pass to AlertDialog.Builder and to set the text
        View help;
        TextView tvHelp;
        try {
            //Inflate the custom view
            LayoutInflater inflater = callingActivity.getLayoutInflater();
            help = inflater.inflate(R.layout.helpbox, (ViewGroup) callingActivity.findViewById(R.id.helpView));
            tvHelp = (TextView) help.findViewById(R.id.helpText);
        } catch(InflateException e) {
            //Inflater can throw exception, unlikely but default to TextView if it occurs
            help = tvHelp = new TextView(callingActivity);
        }
        // Now Linkify the text
        Linkify.addLinks(tvHelp, Linkify.ALL);
        //Build and show the dialog
        new AlertDialog.Builder(callingActivity)
                .setTitle("Help")
                .setCancelable(true)
                .setIcon(R.drawable.ic_launcher)
                .setPositiveButton("OK", null)
                .setView(help)
                .show();    //Builder method returns allow for method chaining
    }
}