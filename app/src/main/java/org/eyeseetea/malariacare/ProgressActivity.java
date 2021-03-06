/*
 * Copyright (c) 2015.
 *
 * This file is part of QIS Surveillance App.
 *
 *  QIS Surveillance App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QIS Surveillance App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QIS Surveillance App.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.eyeseetea.malariacare.database.iomodules.dhis.exporter.PushController;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.PullController;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.SyncProgressStatus;
import org.eyeseetea.malariacare.database.model.Survey;
import org.eyeseetea.malariacare.database.utils.Session;
import org.hisp.dhis.android.sdk.controllers.DhisService;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;

import java.util.ArrayList;
import java.util.List;

public class ProgressActivity extends Activity {

    private static final String TAG=".ProgressActivity";

    /**
     * Intent param that tells what to do (push, pull or push before pull)
     */
    public static final String TYPE_OF_ACTION="TYPE_OF_ACTION";

    /**
     * Intent param that tells what do before push
     */
    public static final String AFTER_ACTION="AFTER_ACTION";
    /**
     * To pull data from server
     */
    public static final int ACTION_PULL=0;

    /**
     * To push a single survey to server
     */
    public static final int ACTION_PUSH=1;

    /**
     * To dont show the survey pushed feedback
     */
    public static final int DONT_SHOW_FEEDBACK = 1;

    /**
     * To show the survey pushed feedback
     */
    public static final int SHOW_FEEDBACK = 2;
    /**
     * To push every unsent data to server before pulling metadata
     */
    public static final int ACTION_PUSH_BEFORE_PULL=2;

    /**
     * Num of expected steps while pulling
     */
    private static final int MAX_PULL_STEPS=7;

    /**
     * Num of expected steps while pushing
     */
    private static final int MAX_PUSH_STEPS=4;
    /**
     * Used for control new steps
     */
    public static Boolean PULL_IS_ACTIVE =false;

    /**
     * Used for control autopull from login
     */
    public static Boolean PULL_CANCEL =false;

    ProgressBar progressBar;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        PULL_CANCEL = false;
        PULL_IS_ACTIVE = true;
        prepareUI();
    }

    private void cancellPull() {
        if(PULL_IS_ACTIVE) {
            PULL_CANCEL = true;
            PULL_IS_ACTIVE = false;
            step(getBaseContext().getResources().getString(R.string.cancellingPull));
            if(PullController.getInstance().finishPullJob())
                finishAndGo(LoginActivity.class);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
        Dhis2Application.bus.register(this);
        }catch(Exception e){
            e.printStackTrace();
            Dhis2Application.bus.unregister(this);
            Dhis2Application.bus.register(this);
        }
        launchPull();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBus();
        //TODO this is not expected in pictureapp
        if(PULL_CANCEL==true) {
            finishAndGo(LoginActivity.class);
        }
    }

    private void unregisterBus(){
        try {
            Dhis2Application.bus.unregister(this);
        }catch(Exception e){
            Log.e(TAG,e.getMessage());
        }
    }

    private void prepareUI(){
        progressBar=(ProgressBar)findViewById(R.id.pull_progress);
        progressBar.setMax(MAX_PULL_STEPS);
        textView=(TextView)findViewById(R.id.pull_text);
        final Button button = (Button) findViewById(R.id.cancelPullButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cancellPull();
            }
        });
    }

    @Subscribe
    public void onProgressChange(final SyncProgressStatus syncProgressStatus) {
        if(syncProgressStatus ==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (syncProgressStatus.hasError()) {
                    showException(syncProgressStatus.getException().getMessage());
                    return;
                }

                //Step
                if (syncProgressStatus.hasProgress()) {
                    step(syncProgressStatus.getMessage());
                    return;
                }

                //Finish
                if (syncProgressStatus.isFinish()) {
                    showAndMoveOn();
                }
            }
        });
    }

    /**
     * Shows a dialog with the given message y move to login after showing error
     * @param msg
     */
    private void showException(String msg){
        String title=getDialogTitle();

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //A crash during a pull requires to start from scratch -> logout
                        Log.d(TAG, "Logging out from sdk...");
                        DhisService.logOutUser(ProgressActivity.this);
                    }
                })
                .create()
                .show();
    }

    /**
     * Prints the step in the progress bar
     * @param msg
     */
    private void step(final String msg) {
        final int currentProgress = progressBar.getProgress();
        progressBar.setProgress(currentProgress + 1);
        textView.setText(msg);
    }

    /**
     * Shows a dialog to tell that pull is done and then moves into the dashboard.
     *
     */
    private void showAndMoveOn() {
        final boolean isAPull = isAPull();

        //Annotate pull is done
        if(isAPull) {
            //If is not active, we need restart the process
            if(!PULL_IS_ACTIVE) {
                try{
                    Dhis2Application.bus.unregister(this);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                finishAndGo(LoginActivity.class);
                return;
            }
        }

        //Show final step -> done
        step(getString(R.string.progress_pull_done));

        String title=getDialogTitle();

        final int msg=getDoneMessage();

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        //Pull -> Settings
                        if (isAPull()) {
                            //Move back to setting with extras
                            Intent intent = new Intent(ProgressActivity.this,SettingsActivity.class);
                            intent.putExtra(SettingsActivity.SETTINGS_CHANGING_SERVER, true);
                            intent.putExtra(SettingsActivity.LOGIN_BEFORE_CHANGE_DONE, true);
                            finish();
                            startActivity(intent);
                            return;
                        }

                        //Push before pull -> Dashboard
                        if (!hasAPullAfterPush()){
                            finishAndGo(DashboardActivity.class);
                            return;
                        }

                        launchPull();

                        return;

                    }
                }).create().show();

    }

    /**
     * Once an action is over there is a message that changes depending on the kind of action:
     *  -Pull: Pull ok, let's move to dashboard
     *  -Push (single): Push ok, let's move to dashboard
     *  -Push (before pull): Push ok, let's start with the pull
     *
     * @return
     */
    private int getDoneMessage(){
        boolean isAPull = isAPull();

        //Pull
        if(isAPull){
            return R.string.dialog_pull_success;
        }

        //Push before pull
        if(hasAPullAfterPush()){
            return R.string.dialog_push_before_pull_success;
        }

        //Push (single)
        return R.string.dialog_push_success;
    }

    /**
     * Tells if a push is required
     * @return
     */
    private boolean isAPull() {
        //Check intent params
        Intent intent=getIntent();
        //Not a pull -> is a Push
        return (intent!=null && intent.getIntExtra(TYPE_OF_ACTION,ACTION_PULL)==ACTION_PULL);
    }


    /**
     * Tells is the intent requires a Pull after the push is done
     * @return
     */
    private boolean hasAPullAfterPush(){
        Intent intent=getIntent();
        return (intent!=null && intent.getIntExtra(TYPE_OF_ACTION,ACTION_PULL)==ACTION_PUSH_BEFORE_PULL);
    }

    private String getDialogTitle(){
        int stringId=R.string.dialog_title_pull_response;
        return getString(stringId);
    }

    private void launchPull(){
        progressBar.setProgress(0);
        progressBar.setMax(MAX_PULL_STEPS);
        PullController.getInstance().pull(this);
    }

    @Subscribe
    public void onLogoutFinished(UiEvent uiEvent){
        //No event or not a logout event -> done
        if(uiEvent==null || !uiEvent.getEventType().equals(UiEvent.UiEventType.USER_LOG_OUT)){
            return;
        }
        Log.d(TAG,"Logging out from sdk...OK");
        Session.logout();
        //Go to login
        finishAndGo(LoginActivity.class);
    }

    /**
     * Finish current activity and launches an activity with the given class
     * @param targetActivityClass Given target activity class
     */
    public void finishAndGo(Class targetActivityClass){
        Intent targetActivityIntent = new Intent(this,targetActivityClass);
        finish();
        startActivity(targetActivityIntent);
    }

}
