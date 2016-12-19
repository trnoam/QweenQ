package com.qween.qweenq;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmployeeChoiceActivity extends AppCompatActivity {


    public static EmployeeChoiceActivity this_employee_choice_activity = null;
    public static DataSnapshot park_data;
    public static final double NAME_PART = 0.7;
    public static final int TEXT_SIZE = 24;
    private Toast prev_toast = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this_employee_choice_activity = EmployeeChoiceActivity.this;
        setContentView(R.layout.activity_employee_choice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.employee_choice_toolbar);
        setSupportActionBar(toolbar);
        park_data = SettingsActivity.park_data_employee;
        setTitle(park_data.child("name").getValue(String.class));
        set_attraction_layout_height();
        TextView instruction_text = (TextView)findViewById(R.id.please_choose_attraction);
        instruction_text.setTextSize(TEXT_SIZE);
        load_ui_employee();



        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choice_employee, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public static void set_attraction_layout_height(){
        ChoiceActivity.ATTRACTION_LAYOUT_HEIGHT = convertDpToPixel(CodeActivity.DP_ATTRACTION_HEIGHT);
    }
    public static int convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
    public void load_ui_employee(){
        LinearLayout main = (LinearLayout)findViewById(R.id.attractions_names_layout);
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = EmployeeChoiceActivity.this.obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);
        for(final DataSnapshot attraction : park_data.child("attractions").getChildren()){
            final LinearLayout curr_layout = new LinearLayout(EmployeeChoiceActivity.this);
            curr_layout.setOrientation(LinearLayout.VERTICAL);
            curr_layout.setGravity(Gravity.START);
            curr_layout.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT,
                    (int)(ChoiceActivity.ATTRACTION_LAYOUT_HEIGHT * NAME_PART)));
            curr_layout.setClickable(true);
            curr_layout.setBackgroundResource(backgroundResource);
            curr_layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

            curr_layout.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    if(v == curr_layout) {
                        Firebase dictionary_ref = new Firebase("https://qweenq-48917.firebaseio.com/passwords dictionary");
                        dictionary_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Intent intent = new Intent(EmployeeChoiceActivity.this, EmployeePasswordActivity.class);
                                intent.putExtra("attraction id", attraction.getKey());
                                intent.putExtra("attraction name", attraction.child("name").getValue(String.class));
                                intent.putExtra("park name", park_data.child("name").getValue(String.class));
                                intent.putStringArrayListExtra("adjectives", get_array_of_values(
                                        dataSnapshot.child("adjectives")));
                                intent.putStringArrayListExtra("nouns", get_array_of_values(
                                        dataSnapshot.child("nouns")));
                                startActivity(intent);
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                my_toast_employee("Internet error!!!");
                            }
                        });
                    }
                }
            });

            TextView name_text = new TextView(EmployeeChoiceActivity.this);
            name_text.setText(attraction.child("name").getValue(String.class));
            name_text.setTextColor(ContextCompat.getColor(EmployeeChoiceActivity.this, R.color.colorToolBar));
            name_text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            name_text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            name_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE);
            name_text.setEllipsize(TextUtils.TruncateAt.END);
            name_text.setMaxLines(1);
            name_text.setMinLines(1);

            curr_layout.addView(name_text);
            main.addView(curr_layout);
        }
        typedArray.recycle();
    }
    public void my_toast_employee(String message){
        if(prev_toast != null){
            prev_toast.cancel();
        }
        prev_toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast.show();
    }
    protected void onPause() {
        super.onPause();
    }
    private ArrayList<String> get_array_of_values(DataSnapshot words_data){
        ArrayList<String> string_array = new ArrayList<>();
        for(DataSnapshot word : words_data.getChildren()){
            string_array.add(word.getValue(String.class));
        }
        return string_array;
    }
}
