package com.qween.qweenq;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CodeActivity extends AppCompatActivity {

    Button lets_go_button;
    Button add_rider_button;
    EditText edit_code_text_field;
    public static CodeActivity this_activity;
    public static String curr_code;
    public static Firebase main_ref;
    SharedPreferences data;
    SharedPreferences.Editor editor;
    public String code_to_add = "";
    public static Set<String> codes = null;
    public Toast prev_toast = null;
    public static double curr_coins = 0;
    public static Map<String, String> codes_coins;
    public static final int PORTRAIT_AND_KEYBOARD_OPEN = 6;
    public static final long DP_ATTRACTION_HEIGHT = 80;
    public static final long BUTTONS_DP = 50;
    public static int park_id_code = 0;
    public static final int TEXT_SIZE = 24;
    public static Dialog current_multi_parks_dialog = null;
    public Set<String> codes_clone = null;
    public AttractionsData parks_attractions = null;
    public int curr_park_id = 0;
    public boolean starting_choice_activity = false;
    public Firebase parks_ref = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_code);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        set_attraction_layout_height();
        starting_choice_activity = false;

        data = PreferenceManager.getDefaultSharedPreferences(this);
        edit_code_text_field = (EditText)findViewById(R.id.edit_code);
        add_buttons_click_listeners();

        edit_code_text_field.requestFocus();

        if(data.contains("curr_codes")) {
            codes = new HashSet<String>();
            Set<String> temp = data.getStringSet("curr_codes", null);
            codes.clear();
            codes.addAll(temp);
            sync_codes();
            if(!codes.isEmpty()) {
                load_codes();
            }
        }else{
            codes = null;
        }

        if(data.contains("curr_coins")){
            curr_coins = Double.parseDouble(data.getString("curr_coins", "DEFAULT"));
        }
        if(data.contains("park_id")){
            park_id_code = data.getInt("park_id", 0);
            if(parks_attractions != null) {
                download_park_attractions(park_id_code, false);
            }
        }

        if(savedInstanceState != null){
            my_toast("Welcome back!");
        }
        codes_coins = new HashMap<>();

        if(codes != null){
            load_codes_coins();//loads the amount of coins every code has...
            load_codes();//Displays the codes
        }

        this_activity = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_code, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(CodeActivity.this_activity, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    protected void onPause() {
        super.onPause();
    }
    protected void onResume(){
        super.onResume();
        add_buttons_click_listeners();
        starting_choice_activity = false;}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        add_buttons_click_listeners();
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        add_buttons_click_listeners();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        parks_attractions = null;
        add_buttons_click_listeners();
    }
    public void my_toast(String message){
        if(prev_toast != null){
            prev_toast.cancel();
        }
        prev_toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast.show();
    }
    public void add_code(String code, final boolean is_choice_activity){
        final Firebase main_ref = new Firebase("https://qweenq-48917.firebaseio.com/codes");
        CodeActivity.main_ref = main_ref;
        code = edit_code_text_field.getText().toString();
        if(!is_legal_firebase_key(code)){
            my_toast("Code contains illegal characters");
            return;
        }
        if(code.equals("")){
            my_toast("Code field empty");
            return;
        }
        if(codes != null){
            if(!codes.isEmpty()){
                if(codes.contains(code)){
                    my_toast("Code already in list");
                    return;
                }
            }
        }
        final String finalCode = code;
        main_ref.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    my_toast("Sorry, code does not exist...");
                    return;
                }
                if(snapshot.hasChild("use")) {
                    if (snapshot.child("use").getValue().equals("not used")) {
                        if ((park_id_code != 0) &&
                                (park_id_code != snapshot.child("park").getValue(Integer.class))) {
                            my_toast("Codes are not in same parks!!!");
                            return;
                        }
                        main_ref.child(finalCode).child("use").setValue("being used", new Firebase.CompletionListener() {
                            @Override
                            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                                my_toast("Code accepted!");
                                park_id_code = snapshot.child("park").getValue(Integer.class);
                                if(parks_attractions != null){
                                    download_park_attractions(park_id_code, is_choice_activity);
                                }

                                editor = data.edit();
                                codes = new HashSet<String>();
                                if (data.contains("curr_codes")) {
                                    Set<String> temp = data.getStringSet("curr_codes", null);
                                    codes.clear();
                                    codes.addAll(temp);
                                }
                                codes.add(finalCode);
                                editor.putStringSet("curr_codes", codes);
                                editor.putInt("park_id", park_id_code);
                                codes_coins.put(finalCode, Integer.toString(snapshot.child("coins").getValue(Integer.class)));

                                if (codes.isEmpty()) {
                                    curr_coins = snapshot.child("coins").getValue(Integer.class);
                                } else {
                                    curr_coins *= (codes.size() - 1);
                                    curr_coins += snapshot.child("coins").getValue(Integer.class);
                                    curr_coins /= (codes.size());
                                }

                                editor.putString("curr_coins", Double.toString(curr_coins));
                                save_codes_coins();

                                main_ref.child(finalCode).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (!dataSnapshot.exists()) {
                                            reset_memory();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(FirebaseError databaseError) {
                                        my_toast("Internet error!!!");
                                    }
                                });
                                if (is_choice_activity) {
                                    download_park_attractions(park_id_code, true);
                                }
                                load_codes();
                            }
                        });

                    } else {
                        if (snapshot.child("use").getValue().equals("being used")) {
                            my_toast("Code being used");
                        } else {
                            my_toast("Code already used");
                        }
                    }
                }else{
                    if(park_id_code == 0){//park is unknown.
                        create_park_choice_dialog(snapshot);
                        return;
                    }else{
                        if(snapshot.hasChild(Integer.toString(park_id_code))){
                            if (snapshot.child(Integer.toString(park_id_code)).child("use").getValue().equals("not used")) {
                                main_ref.child(finalCode).child(Integer.toString(park_id_code)).child("use").
                                        setValue("being used", new Firebase.CompletionListener() {
                                    @Override
                                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                                        my_toast("Code accepted!");
                                        if(current_multi_parks_dialog != null){
                                            current_multi_parks_dialog.dismiss();
                                            current_multi_parks_dialog = null;
                                        }
                                        editor = data.edit();
                                        codes = new HashSet<String>();
                                        if (data.contains("curr_codes")) {
                                            Set<String> temp = data.getStringSet("curr_codes", null);
                                            codes.clear();
                                            codes.addAll(temp);
                                        }
                                        codes.add(finalCode);
                                        editor.putStringSet("curr_codes", codes);
                                        editor.putInt("park_id", park_id_code);
                                        codes_coins.put(finalCode, Integer.toString(snapshot.child(Integer.toString(park_id_code)).
                                                child("coins").getValue(Integer.class)));

                                        if (codes.isEmpty()) {
                                            curr_coins = snapshot.child(Integer.toString(park_id_code))
                                                    .child("coins").getValue(Integer.class);
                                        } else {
                                            curr_coins *= (codes.size() - 1);
                                            curr_coins += snapshot.child(Integer.toString(park_id_code)).
                                                    child("coins").getValue(Integer.class);
                                            curr_coins /= (codes.size());
                                        }

                                        editor.putString("curr_coins", Double.toString(curr_coins));
                                        save_codes_coins();

                                        main_ref.child(finalCode).child(Integer.toString(park_id_code)).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (!dataSnapshot.exists()) {
                                                    reset_memory();
                                                    load_codes();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(FirebaseError databaseError) {
                                                my_toast("Internet error!!!");
                                            }
                                        });
                                        if (is_choice_activity) {
                                            download_park_attractions(park_id_code, true);
                                        }
                                        load_codes();
                                    }
                                });

                            } else {
                                if (snapshot.child(Integer.toString(park_id_code)).child
                                        ("use").getValue().equals("being used")) {
                                    my_toast("Code being used");
                                } else {
                                    my_toast("Code already used");
                                }
                            }
                        }else{
                            my_toast("code does not exist, sorry!");
                            return;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Internet error!!!");
            }
        });
    }
    public void remove_code(final String code){
        final Firebase main_ref = new Firebase("https://qweenq-48917.firebaseio.com/codes");
        main_ref.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    reset_memory();
                    return;
                }
                if(dataSnapshot.hasChild("use")){
                    main_ref.child(code).child("use").setValue("not used", new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if(firebaseError == null){ //if no error
                                remove_code_localy(code);
                            }else{
                                my_toast("Please make sure you have an internet connection");
                            }
                        }
                    });
                    return;
                }
                if(dataSnapshot.hasChild(Integer.toString(park_id_code))){
                    main_ref.child(code).child(Integer.toString(park_id_code)).child("use").setValue("not used", new Firebase.CompletionListener(){
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError == null) { // if no error
                                remove_code_localy(code);
                            } else {
                                my_toast("Please make sure you have an internet connection");
                            }
                        }
                    });
                    return;
                }
                reset_memory();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Please make sure you have an internet connection");
            }
        });
    }
    public void remove_code_localy(String code){
        final Firebase main_ref = new Firebase("https://qweenq-48917.firebaseio.com/codes");
        editor = data.edit();
        codes = new HashSet<String>();
        if(data.contains("curr_codes")) {
            Set<String> temp = data.getStringSet("curr_codes", null);
            codes.clear();
            codes.addAll(temp);
        }
        if(!codes.contains(code)){
            return;
        }
        codes.remove(code);
        editor.putStringSet("curr_codes", codes);
        if(codes.isEmpty()){
            park_id_code = 0;
            parks_attractions = null;
            editor.putInt("park_id", 0);
            curr_coins = 0;
            editor.apply();
        }else{
            if(codes_coins.get(code) != null) {
                curr_coins *= (codes.size() + 1);
                curr_coins -= Double.parseDouble(codes_coins.get(code));
                curr_coins /= (codes.size());
                editor.putString("curr_coins", Double.toString(curr_coins));
                editor.remove(code);
                codes_coins.remove(code);
                save_codes_coins();
            }
        }
        my_toast("Code " + code + " removed");
        load_codes();
    }
    public void save_codes_coins(){
        for(String curr_code : codes_coins.keySet()){
            editor.putString("###" + curr_code, codes_coins.get(curr_code)); //Makes sure this is not a firebase key.
        }
        editor.apply();
    }
    public void load_codes_coins(){
        codes_coins = new HashMap<String, String>();
        for(String curr_code : codes){
            codes_coins.put(curr_code, data.getString("###" + curr_code, "DEFAULT"));//Makes sure this is not a firebase key.
        }
    }
    public void load_codes(){
        switch(codes.size()){
            case 0:
                setTitle("QweenQ");

                break;
            case 1:
                setTitle("QweenQ(singel rider)");
                break;
            default:
                setTitle("QweenQ" + "(" + codes.size() + " riders)");
        }
        edit_code_text_field.setHint("QweenQ code");
        edit_code_text_field.setText("");
        TextView instruction_text = (TextView)findViewById(R.id.instruction_text);
        LinearLayout code_scroll_layout = (LinearLayout)findViewById(R.id.code_scroll_layout);
        code_scroll_layout.removeAllViews();
        if(codes.size() == 0){
            instruction_text.setText("Please enter your QweenQ code:");
        }else {
            instruction_text.setText(R.string.add_another_rider);
        }
        code_scroll_layout.addView(instruction_text);
        code_scroll_layout.addView(edit_code_text_field);
        edit_code_text_field.requestFocus();
        TextView current_codes = new TextView(CodeActivity.this);
        current_codes.setText(R.string.current_codes);
        current_codes.setTextColor(ContextCompat.getColor(CodeActivity.this, R.color.colorToolBar));
        DisplayMetrics metrics;
        metrics = getApplicationContext().getResources().getDisplayMetrics();
        float text_size =instruction_text.getTextSize()/metrics.density;
        current_codes.setTextSize(TypedValue.COMPLEX_UNIT_SP, text_size);
        current_codes.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        code_scroll_layout.addView(current_codes);
        for(final String code : codes){
            LinearLayout curr_layout = new LinearLayout(CodeActivity.this);
            curr_layout.setOrientation(LinearLayout.HORIZONTAL);
            curr_layout.setGravity(Gravity.START);
            curr_layout.setLayoutParams(new LinearLayout.LayoutParams((int) LinearLayout.LayoutParams.MATCH_PARENT, (int)LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView code_text = new TextView(CodeActivity.this);
            code_text.setText(code);
            code_text.setTextColor(ContextCompat.getColor(CodeActivity.this, R.color.colorToolBar));
            code_text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            code_text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            code_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

            Button stop_use_button = new Button(CodeActivity.this);
            stop_use_button.setText(R.string.stop_use_notice);
            stop_use_button.setTextColor(ContextCompat.getColor(CodeActivity.this, R.color.colorToolBar));
            stop_use_button.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            stop_use_button.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            stop_use_button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            stop_use_button.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View view)
                {
                    if(!isNetworkAvailable(CodeActivity.this)) {
                        my_toast("Please make sure you have an internet connection");
                        return;
                    }
                    remove_code(code);
                }
            });

            int[] attrs = new int[] { android.R.attr.selectableItemBackground /* index 0 */};
            TypedArray ta = obtainStyledAttributes(attrs);
            Drawable drawableFromTheme = ta.getDrawable(0 /* index */);
            ta.recycle();
            stop_use_button.setBackground(drawableFromTheme);

            curr_layout.addView(code_text);
            curr_layout.addView(stop_use_button);

            code_scroll_layout.addView(curr_layout);
        }
    }
    public static boolean isNetworkAvailable(final Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }
    private void sync_codes(){
        if(!isNetworkAvailable(CodeActivity.this)){
            my_toast("Please make sure you have an internet connection");
            return;
        }
        final Firebase main_ref = new Firebase("https://qweenq-48917.firebaseio.com/codes");
        for(final String code : codes){
            main_ref.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        reset_memory();//server erased this code.
                        return;
                    }
                    if(snapshot.hasChild("use")) {
                        if (snapshot.child("use").getValue().equals("not used")) {
                            remove_code_localy(code);
                        }
                        return;
                    }
                    if(!snapshot.hasChild(Integer.toString(park_id_code))){
                        reset_memory();
                        return;
                    }
                    if(snapshot.child(Integer.toString(park_id_code)).child("use").getValue(String.class).equals("not used")){
                        remove_code_localy(code);
                        return;
                    }
                }
                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    my_toast("Internet error!!!");
                }
            });
        }
    }
    private boolean is_legal_firebase_key(String key){
        return !(key.contains(".") || key.contains("#") || key.contains("$") || key.contains("[") || key.contains("]"));
    }
    public static void set_attraction_layout_height(){
        ChoiceActivity.ATTRACTION_LAYOUT_HEIGHT = convertDpToPixel(DP_ATTRACTION_HEIGHT);
    }
    public static int convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
    public void create_park_choice_dialog(final DataSnapshot code_parks){
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = this.obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);

        View dialogView = this.getLayoutInflater().inflate(R.layout.multi_park_dialog , null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle( "The code relates to several parks, please choose yours:");
        builder.setView(dialogView);
        LinearLayout main = (LinearLayout)dialogView.findViewById(R.id.multi_park_inner_layout);
        final Dialog dialog = builder.create();
        current_multi_parks_dialog = dialog;
        final Button cancel_dialog = (Button)dialogView.findViewById(R.id.cancel_multi_parks);
        cancel_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == cancel_dialog){
                    dialog.dismiss();
                }
            }
        });
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);
        for(final DataSnapshot code: code_parks.getChildren()) {
            final LinearLayout curr_layout = new LinearLayout(this);
            curr_layout.setOrientation(LinearLayout.VERTICAL);
            curr_layout.setGravity(Gravity.START);
            curr_layout.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT,
                    (int) (convertDpToPixel(BUTTONS_DP))));
            curr_layout.setClickable(true);
            curr_layout.setBackgroundResource(backgroundResource);
            curr_layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

            curr_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v == curr_layout) {
                        park_id_code = Integer.parseInt(code.getKey());
                        if(parks_attractions != null){
                            download_park_attractions(park_id_code, false);
                        }
                        editor = data.edit();
                        editor.putInt("park_id", park_id_code);
                        editor.apply();
                        add_code(code_parks.getKey(), false);
                    }
                }
            });

            TextView name_text = new TextView(this);
            name_text.setText(code.child("park name").getValue(String.class));
            name_text.setTextColor(ContextCompat.getColor(this, R.color.colorToolBar));
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
    public void reset_memory(){
        editor = data.edit();
        editor.clear();
        editor.commit();
        codes = new HashSet<String>();
        curr_coins = 0;
        codes_coins = new HashMap<>();
        park_id_code = 0;
        parks_attractions = null;
        curr_park_id = 0;
        starting_choice_activity = false;
        my_toast("Looks like some of the codes are no longer relevant, sorry...");
        load_codes();
    }

    public void start_choice_activity(){
        if(starting_choice_activity){
            return;
        }
        starting_choice_activity = true;
        codes_clone = new HashSet<String>();
        codes_clone.addAll(codes);
        my_toast("Syncing codes...");
        final Firebase main_ref = new Firebase("https://qweenq-48917.firebaseio.com/codes");
        for(final String code: codes){
            main_ref.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){//Code doesn't exist at all
                        reset_memory();
                        return;
                    }
                    if(dataSnapshot.hasChild("use")){//Code exists and there is only one park for it
                        codes_clone.remove(code);
                        if(codes_clone.isEmpty()) {
                            if(prev_toast != null){
                                prev_toast.cancel();
                                prev_toast = null;
                            }
                            Intent intent = new Intent(CodeActivity.this_activity, ChoiceActivity.class);
                            intent.putExtra("parks attractions", parks_attractions);
                            startActivity(intent);
                        }
                        return;
                    }
                    if(dataSnapshot.hasChild(Integer.toString(park_id_code))){//This code belongs to several parks and exists
                        codes_clone.remove(code);
                        if(codes_clone.isEmpty()) {
                            if(prev_toast != null){
                                prev_toast.cancel();
                                prev_toast = null;
                            }
                            Intent intent = new Intent(CodeActivity.this_activity, ChoiceActivity.class);
                            intent.putExtra("parks attractions", parks_attractions);
                            startActivity(intent);
                        }
                        return;
                    }else{//Code does not exist
                        reset_memory();
                        return;
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    my_toast("Internet error!!!");
                }
            });
        }
    }
    void download_park_attractions(int the_park_id, final boolean is_choice_activity){
        if(parks_attractions != null){
            if(is_choice_activity){
                start_choice_activity();
            }
            return;
        }
        parks_ref = new Firebase("https://qweenq-48917.firebaseio.com/parks");
        parks_ref.child(Integer.toString(the_park_id)).child("attractions").
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                parks_attractions = new AttractionsData(dataSnapshot);
                if(is_choice_activity){
                    start_choice_activity();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Internet error!!!");
            }
        });
    }
    void add_buttons_click_listeners(){
        lets_go_button = (Button)findViewById(R.id.lets_go);
        add_rider_button = (Button)findViewById(R.id.add_rider);
        add_rider_button.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if(!isNetworkAvailable(CodeActivity.this)) {
                            my_toast("Please make sure you have an internet connection");
                            return;
                        }
                        add_code(edit_code_text_field.getText().toString(), false);
                    }
                });
        lets_go_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if(!isNetworkAvailable(CodeActivity.this)) {
                    my_toast("Please make sure you have an internet connection");
                    return;
                }

                if (codes == null) {
                    if(!edit_code_text_field.getText().toString().equals("")){
                        add_code(edit_code_text_field.getText().toString(), true);
                        return;
                    }
                    my_toast("No codes inserted");
                    return;
                }
                if (!edit_code_text_field.getText().toString().equals("")) {
                    if (codes.contains(edit_code_text_field.getText().toString())) {
                        download_park_attractions(park_id_code, true);
                    } else {
                        add_code(edit_code_text_field.getText().toString(), true);
                        return;
                    }
                } else {
                    if (codes.isEmpty()) {
                        my_toast("No codes inserted");
                        return;
                    }
                    download_park_attractions(park_id_code, true);
                }

            }
        });
    }
}
