package com.qween.qweenq;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.List;
import java.util.concurrent.Semaphore;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private AppCompatDelegate mDelegate;
    private static Firebase employee_codes_ref;
    private static Toast prev_toast_settings = null;
    public static SettingsActivity this_settings_activity = null;
    public SharedPreferences settings = null;
    private boolean is_next_activity = true;
    public static DataSnapshot park_data_employee;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this_settings_activity = SettingsActivity.this;
        /*getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);*/
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        addPreferencesFromResource(R.xml.preferences);
        employee_codes_ref = new Firebase("https://qweenq-48917.firebaseio.com/employees codes");
        settings = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        is_next_activity = true;

        ActionBar action_bar = getSupportActionBar();
        if(action_bar != null) {
            action_bar.setDisplayHomeAsUpEnabled(true);
            action_bar.setHomeButtonEnabled(true);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Preference pref = findPreference(key);
        if(pref instanceof EditTextPreference) {
            EditTextPreference edit_text_pref = (EditTextPreference) pref;
            if (key.equals("im_an_employee")) {
                employee_codes_ref.child((String) edit_text_pref.getText()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            SharedPreferences data_settings = PreferenceManager.getDefaultSharedPreferences(this_settings_activity);
                            SharedPreferences.Editor editor_settings = data_settings.edit();
                            editor_settings.putInt("park_id employee", dataSnapshot.getValue(Integer.class));
                            if (pref instanceof ListPreference) {
                                ListPreference listPref = (ListPreference) pref;
                                pref.setSummary(listPref.getEntry());
                            }
                            if(is_next_activity){
                                Firebase parks = new Firebase("https://qweenq-48917.firebaseio.com/parks");
                                parks.child(Integer.toString(dataSnapshot.getValue(Integer.class)))
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    park_data_employee = dataSnapshot;
                                                    Intent intent = new Intent(this_settings_activity, EmployeeChoiceActivity.class);
                                                    startActivity(intent);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(FirebaseError databaseError) {
                                                my_toast_settings("Internet error!!!");
                                            }
                                        });
                                is_next_activity = false;
                            }else{
                                is_next_activity = true;
                            }
                        } else {
                            pref.setSummary("(park's password)");
                            my_toast_settings("Sorry, park's password does not exist");
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError databaseError) {
                        my_toast_settings("Internet error!!!");
                    }
                });
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("im_an_employee", "(park's password)");
                editor.commit();
            }
        }
    }

    /*@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }*/

    /*@Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }*/

    /*@Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }*/

    /*public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }*/

    /*private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    public void my_toast_settings(String message){
        if(prev_toast_settings != null){
            prev_toast_settings.cancel();
        }
        prev_toast_settings = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast_settings.show();
    }
    /*public void onEmployeeClicked(View v){
        my_toast_settings("employee clicked");
        EditTextPreference pref;
    }*/
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }
}