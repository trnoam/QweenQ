package com.qween.qweenq;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.GridLayout.LayoutParams;


import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class ChoiceActivity extends AppCompatActivity {

    public Firebase main_ref = null;
    public static int park_id_choice = 0;
    public static String park_name = null;
    public static FirebaseStorage storage;
    public static StorageReference storage_ref;
    public static final long TEN_MEGABYTE = 10 * 1024 * 1024;
    public static final int IMAGE_PADDING = 10;
    public static long ATTRACTION_LAYOUT_HEIGHT = 250;
    public static AttractionsData attractions = null;
    public static Vector<View> views_vector;
    public static Vector<View> costs_vector;
    public static int max_width_choice_part;
    public static Vector<RoundedImageView> images_vector;
    public static SharedPreferences data_choice;
    public static Map<String, Integer> attractions_choices = null;
    public static Map<String, Integer> attractions_maxes = null;
    public static int coins_left = -1;
    public Toast prev_toast = null;
    public Button lets_go_button_choice = null;
    public int max_coins = 0;
    public static int num_attractions_allowed_by_time = 0;
    public String main_code = "";
    public static Set<String> codes_choice_screen;
    public static SharedPreferences.Editor editor_choice;
    public boolean is_calendar_active = false;
    public static ChoiceActivity this_choice_activity= null;
    public DataSnapshot park_full_times = null;
    public DataSnapshot park_sync_times = null;
    public ValueEventListener full_times_changes = null;
    public static Vector<ProgressBar> progressBars = null;
    public static Vector<Boolean> is_image_loaded = null;
    public static Vector<RelativeLayout> progress_bars_layouts = null;
    public static int current_time = -1;
    public static Timer clock_updater = null;
    public static TextView clock = null;
    public static int[] user_impossible_times = null;
    public static int min_time_components = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this_choice_activity = this;
        setContentView(R.layout.activity_choice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.choice_toolbar);
        setSupportActionBar(toolbar);
        CodeActivity.set_attraction_layout_height();
        codes_choice_screen = CodeActivity.codes;
        my_set_title();
        if(savedInstanceState == null){
            my_set_title();
        }
        if(CodeActivity.codes != null && !CodeActivity.codes.isEmpty()) {
            main_code = (String) CodeActivity.codes.toArray()[0];
        }else{
            my_toast("Error!", Toast.LENGTH_LONG);
            ChoiceActivity.this.finish();
        }
        clock = null;
        if(clock_updater != null) {
            clock_updater.cancel();
        }
        clock_updater = null;
        views_vector= new Vector<>();
        costs_vector = new Vector<>();
        max_width_choice_part = -1;


        storage = FirebaseStorage.getInstance();
        storage_ref = storage.getReferenceFromUrl("gs://qweenq-48917.appspot.com/");

        lets_go_button_choice = (Button)findViewById(R.id.lets_go_choice);

        data_choice = PreferenceManager.getDefaultSharedPreferences(this);

        if(data_choice.contains("curr_coins")){
            CodeActivity.curr_coins = Double.parseDouble(data_choice.getString("curr_coins", ""));
        }else{
            CodeActivity.curr_coins = 0;
        }
        max_coins = (int)(CodeActivity.curr_coins + 0.5);

        if(park_id_choice == 0){
            park_id_choice = data_choice.getInt("park_id", 0);
        }

        if (attractions_maxes != null) {
            announce_sync_times(attractions, false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            return;
        }
        images_vector = new Vector<>();
        progressBars = new Vector<>();
        is_image_loaded = new Vector<>();
        progress_bars_layouts = new Vector<>();
        main_ref = new Firebase("https://qweenq-48917.firebaseio.com/");
        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ChoiceActivity.park_name = snapshot.getValue(String.class);
                editor_choice = data_choice.edit();
                editor_choice.putString("park name", snapshot.getValue(String.class));
                editor_choice.apply();
                my_set_title();
            }
            public void onCancelled(FirebaseError firebaseError) {
                Toast.makeText(getApplicationContext(), "Internet error!!!", Toast.LENGTH_SHORT).show();
            }
        });
        Bundle bundle = this.getIntent().getExtras();
        attractions = bundle.getParcelable("parks attractions");
        announce_sync_times(attractions, true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choice, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.set_trip_finish_time) {
            // custom dialog
            final Dialog dialog = new Dialog(ChoiceActivity.this_choice_activity);
            dialog.setContentView(R.layout.set_finish_time_dialog);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // set the custom dialog components - button
            Button ok_set_finish_time= (Button) dialog.findViewById(R.id.ok_set_finish_time);
            ok_set_finish_time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            Display display = this_choice_activity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            //final int height_of_dialog = size.y / 1.3;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.show();
            dialog.getWindow().setAttributes(lp);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public static void load_components(AttractionsData attractions_data, Boolean is_first){
        LinearLayout attractions_layout = (LinearLayout)this_choice_activity.findViewById(R.id.attractions_layout);
        attractions_layout.removeAllViews();
        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = this_choice_activity.obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);
        attractions = attractions_data;
        if(is_first){
            this_choice_activity.init_choices_and_maxes();
        }
        if(this_choice_activity.min_time_components != -1){
            this_choice_activity.update_max_trip_hour();
        }
        this_choice_activity.update_coins_lets_go_button();
        for(final Attraction attraction : attractions_data._attractions_array){
            final View times_choice;
            final int amount_possible = Math.min(this_choice_activity.attractions_maxes.get(attraction._key),
                    this_choice_activity.coins_left / attraction._cost +
                            this_choice_activity.attractions_choices.get(attraction._key));
            switch(amount_possible){
                case 0:
                    times_choice = new TextView(this_choice_activity);
                    ((TextView)times_choice).setText(R.string.full_notice_text);
                    ((TextView)times_choice).setTextColor(ContextCompat.getColor(this_choice_activity, R.color.colorToolBar));
                    ((TextView)times_choice).setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
                    ((TextView)times_choice).setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    times_choice.measure(0, 0);
                    int width = 2 * times_choice.getMeasuredWidth();
                    if(width > max_width_choice_part){
                        max_width_choice_part = width;
                    }
                    break;
                case 1:
                    times_choice = new ToggleButton(this_choice_activity);
                    ((ToggleButton)times_choice).setText(this_choice_activity.getString(R.string.off_button_string));
                    ((ToggleButton)times_choice).setTextOff(this_choice_activity.getString(R.string.off_button_string));
                    ((ToggleButton)times_choice).setTextOn(this_choice_activity.getString(R.string.on_button_string));
                    ((ToggleButton)times_choice).setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
                    ((ToggleButton)times_choice).setTextColor(ContextCompat.getColor(this_choice_activity, R.color.colorToolBar));
                    ((ToggleButton)times_choice).setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    Paint paint = ((ToggleButton)times_choice).getPaint();
                    float length =  2 * Math.max(paint.measureText(this_choice_activity.getString(R.string.on_button_string)),
                            paint.measureText(this_choice_activity.getString(R.string.off_button_string)));
                    if(length > max_width_choice_part){
                        max_width_choice_part = (int)length;
                    }
                    times_choice.setBackgroundColor(ContextCompat.getColor(this_choice_activity, R.color.transparent));
                    ((ToggleButton)times_choice).setChecked(this_choice_activity.attractions_choices.get(attraction._key) == 1);
                    times_choice.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Boolean is_checked = ((ToggleButton)times_choice).isChecked();
                            if(is_checked){
                                ((ToggleButton)times_choice).setText(this_choice_activity.getString(R.string.on_button_string));
                                this_choice_activity.coins_left -= attraction._cost;
                                if(this_choice_activity.coins_left < 0){
                                    this_choice_activity.my_toast("Not enough coins, sorry");
                                    this_choice_activity.coins_left += attraction._cost;
                                    ((ToggleButton)times_choice).setText(this_choice_activity.getString(R.string.off_button_string));
                                }
                                this_choice_activity.update_coins_lets_go_button();
                            }else{
                                ((ToggleButton)times_choice).setText(this_choice_activity.getString(R.string.off_button_string));
                                this_choice_activity.coins_left += attraction._cost;
                                this_choice_activity.update_coins_lets_go_button();
                            }
                            this_choice_activity.choice_algorithm(Integer.parseInt(attraction._key),
                                    is_checked ? 1 : 0);
                            views_vector = new Vector<View>();
                            costs_vector = new Vector<View>();
                            load_components(attractions, false);
                        }
                    });
                    break;
                default:
                    ArrayList<String> choices = new ArrayList<>();
                    for(int j = 0; j <= amount_possible; j++){
                        choices.add(j, Integer.toString(j));
                    }
                    times_choice = new Spinner(this_choice_activity);
                    ((Spinner)times_choice).setDropDownWidth((int)ATTRACTION_LAYOUT_HEIGHT);
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>
                            (this_choice_activity, R.layout.spinner_dropdown_item, choices);
                    spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item); // The drop down view
                    ((Spinner)times_choice).setAdapter(spinnerArrayAdapter);
                    ((Spinner)times_choice).setSelection(this_choice_activity.attractions_choices.get(attraction._key));
                    times_choice.getBackground().setColorFilter(
                            ContextCompat.getColor(this_choice_activity, R.color.colorToolBar), PorterDuff.Mode.SRC_ATOP);
                    ((Spinner)times_choice).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int curr_choice, long id) {
                            /*((TextView) parentView.getChildAt(0)).setTextColor(
                                    ContextCompat.getColor(this_choice_activity, R.color.colorToolBar));
                            ((TextView) parentView.getChildAt(0)).setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);*/
                            int prev_choice = 0;
                            if(this_choice_activity.attractions_choices.containsKey(attraction._key)) {
                                prev_choice = this_choice_activity.attractions_choices.get(attraction._key);
                            }
                            if(prev_choice == curr_choice){
                                return;
                            }
                            int check_coins = this_choice_activity.coins_left;
                            check_coins -= ((curr_choice - prev_choice) * attraction._cost);
                            if(check_coins < 0){
                                this_choice_activity.my_toast("Not enough coins, sorry");
                                ((Spinner) times_choice).setSelection(prev_choice);
                                return;
                            }
                            this_choice_activity.coins_left = check_coins;
                            this_choice_activity.update_coins_lets_go_button();
                            this_choice_activity.choice_algorithm(Integer.parseInt(attraction._key),
                                    curr_choice);
                            views_vector = new Vector<View>();
                            costs_vector = new Vector<View>();
                            load_components(attractions, false);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView){

                        }
                    });
                    ((Spinner)times_choice).setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            }
            views_vector.addElement(times_choice);
            if(attraction._max != 1) {
                times_choice.setLayoutParams(
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
            }
        }
        for(final Attraction attraction : attractions_data._attractions_array){
            TextView cost_text = new TextView(this_choice_activity);
            int num_coins = attraction._cost;
            String amount_coins = Integer.toString(attraction._cost) +
                    ((num_coins == 1) ? (" coin") : (" coins"));
            cost_text.setText(amount_coins);
            cost_text.setTextColor(ContextCompat.getColor(this_choice_activity, R.color.colorToolBar));
            cost_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
            cost_text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            cost_text.measure(0, 0);
            int width = (int)(1.5 * cost_text.getMeasuredWidth());
            if(width > max_width_choice_part) {
                max_width_choice_part = width;
            }
            costs_vector.addElement(cost_text);
        }
        for(View choice : views_vector){
            choice.setLayoutParams(
                    new LinearLayout.LayoutParams(max_width_choice_part, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
        }
        for(View cost : costs_vector){
            cost.setLayoutParams(
                    new LinearLayout.LayoutParams(max_width_choice_part, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
        }
        int i = 0;
        for(final Attraction attraction : attractions_data._attractions_array){
            final LinearLayout curr_layout = new LinearLayout(this_choice_activity);
            curr_layout.setOrientation(LinearLayout.HORIZONTAL);
            curr_layout.setGravity(Gravity.START);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)ATTRACTION_LAYOUT_HEIGHT);
            params.setMargins(0,0,0,0);
            curr_layout.setLayoutParams(params);

            final LinearLayout choice_part = new LinearLayout(this_choice_activity);
            choice_part.setOrientation(LinearLayout.VERTICAL);
            choice_part.setGravity(Gravity.BOTTOM);
            choice_part.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int)ATTRACTION_LAYOUT_HEIGHT));

            if(views_vector.elementAt(i).getParent() != null){
                ((ViewGroup)views_vector.elementAt(i).getParent()).removeView(views_vector.elementAt(i));
            }
            if(costs_vector.elementAt(i).getParent() != null){
                ((ViewGroup)costs_vector.elementAt(i).getParent()).removeView(costs_vector.elementAt(i));
            }
            choice_part.addView(views_vector.elementAt(i));
            choice_part.addView(costs_vector.elementAt(i));

            curr_layout.addView(choice_part);

            if(is_first) {
                final RelativeLayout progress_bar_layout = new RelativeLayout(this_choice_activity);
                LinearLayout.LayoutParams temp_params = new LinearLayout.LayoutParams(
                        (int) ATTRACTION_LAYOUT_HEIGHT, (int) ATTRACTION_LAYOUT_HEIGHT);
                temp_params.gravity = Gravity.CENTER;
                progress_bar_layout.setLayoutParams(temp_params);
                this_choice_activity.is_image_loaded.addElement(false);
                final ProgressBar progressBar = new ProgressBar(this_choice_activity, null, android.R.attr.progressBarStyle);
                RelativeLayout.LayoutParams layout_params = new RelativeLayout.LayoutParams((int) ATTRACTION_LAYOUT_HEIGHT / 3,
                        (int) ATTRACTION_LAYOUT_HEIGHT / 3);
                layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
                progressBar.setLayoutParams(layout_params);
                progressBar.setVisibility(View.VISIBLE);
                progress_bar_layout.addView(progressBar);
                curr_layout.addView(progress_bar_layout);
                progressBars.add(progressBar);
                progress_bars_layouts.add(progress_bar_layout);

                StorageReference attraction_image_ref = storage_ref.child("images/" +  attraction._picture);
                final RoundedImageView attraction_image = new RoundedImageView(this_choice_activity);
                attraction_image.setClickable(true);
                final ImageView attraction_dialog_window_image = new ImageView(this_choice_activity);
                if(data_choice.contains("attraction" + attraction._key + "park" + park_id_choice)){
                    String image_as_string = data_choice.getString("attraction" + attraction._key + "park" + park_id_choice,
                            "");
                    byte[] image = Base64.decode(image_as_string, Base64.DEFAULT);
                    this_choice_activity.is_image_loaded.set(i, true);
                    ((ViewGroup)progressBars.elementAt(i).getParent()).removeView(progressBars.elementAt(i));
                    progressBars.elementAt(i).setVisibility(View.INVISIBLE);
                    ((ViewGroup)progress_bars_layouts.elementAt(i).getParent()).removeView(progress_bars_layouts.elementAt(i));
                    orginize_image(attraction, image, attraction_image, this_choice_activity, attraction_dialog_window_image);
                }
                else {
                    final int finalI = i;
                    attraction_image_ref.getBytes(ChoiceActivity.TEN_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            this_choice_activity.is_image_loaded.set(finalI, true);
                            ((ViewGroup)progressBars.elementAt(finalI).getParent()).removeView(progressBars.elementAt(finalI));
                            progressBars.elementAt(finalI).setVisibility(View.INVISIBLE);
                            ((ViewGroup)progress_bars_layouts.elementAt(finalI).getParent()).removeView(progress_bars_layouts.elementAt(finalI));
                            orginize_image(attraction, bytes, attraction_image,
                                    this_choice_activity, attraction_dialog_window_image);
                            String image_as_string = Base64.encodeToString(bytes, Base64.NO_WRAP);
                            editor_choice = data_choice.edit();
                            editor_choice.putString("attraction" + attraction._key + "park" +
                                    park_id_choice, image_as_string);
                            editor_choice.apply();
                        }
                    }
                    ).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(this_choice_activity.getApplicationContext(), "Internet error!!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                images_vector.addElement(attraction_image);
                curr_layout.addView(attraction_image);
            }else{
                if((ViewGroup)progress_bars_layouts.elementAt(i).getParent() != null) {
                    ((ViewGroup) progress_bars_layouts.elementAt(i).getParent()).removeView(progress_bars_layouts.elementAt(i));
                }
                ((ViewGroup)images_vector.elementAt(i).getParent()).removeView(images_vector.elementAt(i));
                if(!is_image_loaded.elementAt(i)){
                    curr_layout.addView(progress_bars_layouts.elementAt(i));
                }
                curr_layout.addView(images_vector.elementAt(i));
                images_vector.elementAt(i).setClickable(true);
            }

            final LinearLayout text_part = new LinearLayout(this_choice_activity);
            text_part.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            text_part.setOrientation(LinearLayout.VERTICAL);
            text_part.setGravity(Gravity.TOP);
            text_part.setClickable(true);
            text_part.setBackgroundResource(backgroundResource);
            text_part.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    if(v == text_part && (!this_choice_activity.isFinishing())) {
                        // custom dialog
                        final Dialog dialog = new Dialog(ChoiceActivity.this_choice_activity);
                        dialog.setContentView(R.layout.description_view);
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


                        // set the custom dialog components - text, image and button
                        TextView text = (TextView) dialog.findViewById(R.id.description_text);
                        text.setText(attraction._description);

                        TextView title = (TextView)dialog.findViewById(R.id.title_description_view_choice);
                        title.setText(attraction._name);

                        Button ok_description = (Button) dialog.findViewById(R.id.ok_description);
                        ok_description.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        Display display = this_choice_activity.getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        //final int height_of_dialog = size.y / 1.3;

                        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                        lp.copyFrom(dialog.getWindow().getAttributes());
                        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        dialog.show();
                        dialog.getWindow().setAttributes(lp);
                    }
                }
            });

            TextView name_text = new TextView(this_choice_activity);
            name_text.setText((CharSequence) attraction._name);
            name_text.setTextColor(ContextCompat.getColor(this_choice_activity, R.color.black));
            name_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 15);
            name_text.setTypeface(null, Typeface.BOLD);
            name_text.setClickable(false);
            name_text.setEllipsize(TextUtils.TruncateAt.END);
            name_text.setMaxLines(1);
            name_text.setMinLines(1);
            text_part.addView(name_text);

            final TextView description_text = new TextView(this_choice_activity);
            description_text.setText((CharSequence) attraction._description);
            description_text.setTextColor(ContextCompat.getColor(this_choice_activity, R.color.black));
            description_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
            description_text.setClickable(false);
            description_text.setEllipsize(TextUtils.TruncateAt.END);
            description_text.setMaxLines(3);
            description_text.setMinLines(3);
            text_part.addView(description_text);

            curr_layout.addView(text_part);

            attractions_layout.addView(curr_layout);
            i++;
        }
        typedArray.recycle();
    }
    public void my_set_title(){
        if(park_name != null) {
            switch (codes_choice_screen.size()) {
                case 0:
                    setTitle(park_name);
                    break;
                case 1:
                    setTitle(park_name + "(single rider)");
                    break;
                default:
                    setTitle(park_name + "(" + codes_choice_screen.size() + " riders)");
            }
        }else{
            switch (codes_choice_screen.size()) {
                case 0:
                    setTitle("");
                    break;
                case 1:
                    setTitle("single rider");
                    break;
                default:
                    setTitle(codes_choice_screen.size() + " riders");
            }
        }
    }
    public void init_choices_and_maxes(){
        attractions_choices = new HashMap<>();
        attractions_maxes = new HashMap<>();
        if(attractions != null) {
            int i = 1;
            for(Attraction attraction : attractions._attractions_array) {
                attractions_choices.put(Integer.toString(i), 0);
                attractions_maxes.put(Integer.toString(i), Math.min(
                        sum_zeros(park_full_times.child(attraction._key)),
                        attraction._max));
                i++;
            }
        }
    }
    int sum_zeros(DataSnapshot attractions_full_times){
        int sum = 0;
        for(DataSnapshot full_time : attractions_full_times.getChildren()){
            if(full_time.getValue(Integer.class) == 0){
                sum++;
            }
        }
        return sum;
    }
    public void my_toast(String message){
        if(prev_toast != null){
            prev_toast.cancel();
        }
        prev_toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast.show();
    }
    public void my_toast(String message, int length){
        if(prev_toast != null){
            prev_toast.cancel();
        }
        prev_toast = Toast.makeText(getApplicationContext(), message, length);
        prev_toast.show();
    }
    public void update_coins_lets_go_button(){
        int sum = 0;
        for(Attraction attraction: attractions._attractions_array){
            sum += attractions_choices.get(attraction._key) * attraction._cost;
        }
        coins_left = max_coins - sum;
        lets_go_button_choice.setText(getResources().getString(R.string.button_lets_go) + "(" + ((coins_left > 1) ? Integer.toString(coins_left) : "NO") +
                ((coins_left == 1) ? " coin" : " coins") + " left)");
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public void on_lets_go_choice_clicked(final View lets_go_choice_button){
        if(!CodeActivity.isNetworkAvailable(ChoiceActivity.this)){
            my_toast("Please make sure you have an internet connection", Toast.LENGTH_SHORT);
            return;
        }
        if(is_choice_empty()){
            my_toast("Nothing chosen", Toast.LENGTH_SHORT);
            return;
        }
        if(main_ref == null){
            main_ref = new Firebase("https://qweenq-48917.firebaseio.com/");
        }
        editor_choice = data_choice.edit();
        editor_choice.putString("curr main_code", main_code);
        editor_choice.apply();

        Map<String, Object> unanswered_answer_value = new HashMap<>();
        unanswered_answer_value.put("attractions times", 0);
        unanswered_answer_value.put("number of people", codes_choice_screen.size());
        unanswered_answer_value.put("is handled", false);
        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("answers").child(main_code)
                .setValue(unanswered_answer_value, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        Snackbar.make(findViewById(R.id.main_choice_layout), "Please wait, your request is being handled",
                                Snackbar.LENGTH_INDEFINITE).show();
                        lets_go_choice_button.setVisibility(View.GONE);
                    }
                });

        Map<String, Object> request_value = new HashMap<>();
        request_value.put("attractions", attractions_choices);
        request_value.put("number of people", codes_choice_screen.size());
        request_value.put("type", "queue");
        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("requests").child(main_code).setValue(request_value);

        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("answers").child(main_code)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot answer) {
                        if(answer.child("is handled").getValue(Boolean.class) && !is_calendar_active){
                            DataSnapshot attractions_times = answer.child("attractions times");
                            Intent intent = new Intent(ChoiceActivity.this, CalendarActivity.class);
                            intent.putExtra("parks attractions", attractions);
                            startActivity(intent);
                            is_calendar_active = true;
                        }
                    }
                    @Override
                    public void onCancelled(FirebaseError firebase_error) {
                        my_toast("Internet Error!!!");
                    }
                });
    }
    public boolean is_choice_empty(){;
        for(int amount_chosen : attractions_choices.values()){
            if(amount_chosen != 0){
                return false;
            }
        }
        return true;
    }
    public void announce_sync_times(final AttractionsData attractions_data_for_load_components, final Boolean is_first_load_components){
        if(main_ref == null){
            main_ref = new Firebase("https://qweenq-48917.firebaseio.com/");
        }
        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("sync times").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot sync_times) {
                park_sync_times = sync_times;
                park_full_times = sync_times.child("full times");
                if(!sync_times.child("full times").exists()){
                    my_toast("Sorry, park is closed right now", Toast.LENGTH_LONG);
                    ChoiceActivity.this.finish();
                    return;
                }
                load_components(attractions_data_for_load_components, is_first_load_components);
                current_time = sync_times.child("current time").getValue(Integer.class);
                if(clock_updater != null){
                    clock_updater.cancel();
                }
                update_clock();
                clock_updater = new Timer();
                clock_updater.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        current_time++;
                        if(current_time >= 24 * 60 * 60){
                            current_time = 0;
                        }
                        update_clock();
                    }
                }, 1000, 1000);//updates the clock every 1 second
                if(user_impossible_times == null) {
                    user_impossible_times = new int[(int)park_full_times.child("1").getChildrenCount()];
                    for(int i= 0; i < user_impossible_times.length; i++){
                        user_impossible_times[i] = 0;
                    }
                }
                if(full_times_changes == null){
                    full_times_changes = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot full_times) {
                            park_full_times = full_times;
                            if(attractions_maxes == null){
                                init_choices_and_maxes();
                            }
                            Boolean is_choices_possible = is_choices_still_possible();
                            if(!is_choices_possible){
                                my_toast("Choices no longer possible, please choose again", Toast.LENGTH_LONG);
                            }
                            update_min_time_components_number();
                            load_components(attractions_data_for_load_components, !is_choices_possible);
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            my_toast("Internet error!!!");
                        }
                    };
                    main_ref.child("parks").child(Integer.toString(park_id_choice)).child("sync times")
                            .child("full times").addValueEventListener(full_times_changes);
                }

            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Internet Error!!!");
            }
        });
    }
    public boolean is_choices_still_possible(){
        Map<String, Integer> prev_choice = new HashMap<String, Integer>(attractions_choices);
        init_choices_and_maxes();
        for(String key : prev_choice.keySet()){
            if(attractions_maxes.get(key) < prev_choice.get(key)){
                init_choices_and_maxes();
                return false;
            }else{
                choice_algorithm(Integer.parseInt(key), prev_choice.get(key));
            }
        }
        attractions_choices = new HashMap<String, Integer>(prev_choice);
        return true;
    }
    public void display_amount_attractions_to_user(int num_attractions){
        my_toast("Due to park's closing time, the maximum amount of attractions is " + num_attractions, 10 * 3500);
    }
    public static void orginize_image(final Attraction curr_attraction, final byte[] image, final RoundedImageView attraction_image,
                                      final ChoiceActivity this_choice_activity, final ImageView attraction_dialog_window_image){
        Display display = this_choice_activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int imageWidthInPX = size.x;
        final Bitmap unscaled_bm = BitmapFactory.decodeByteArray(image, 0, image.length);
        final Bitmap scaled_bm = Bitmap.createScaledBitmap(unscaled_bm,imageWidthInPX,imageWidthInPX, true);
        final Bitmap small_scaled_bm = Bitmap.createScaledBitmap(unscaled_bm,
                (int)ATTRACTION_LAYOUT_HEIGHT - 2 * IMAGE_PADDING, (int)ATTRACTION_LAYOUT_HEIGHT - 2 * IMAGE_PADDING, true);

        int gray_filter = Color.argb(150,200,200,200);
        final Bitmap gray_bitmap = Bitmap.createBitmap(small_scaled_bm, 0, 0,
                small_scaled_bm.getWidth(), small_scaled_bm.getHeight());
        final Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(gray_filter, 1);
        p.setColorFilter(filter);

        attraction_image.setImageBitmap(small_scaled_bm);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                (int) ATTRACTION_LAYOUT_HEIGHT - 2 * IMAGE_PADDING, (int) ATTRACTION_LAYOUT_HEIGHT - 2 * IMAGE_PADDING);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING);
        attraction_image.setLayoutParams(layoutParams);
        attraction_dialog_window_image.setImageBitmap(scaled_bm);

        attraction_image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        attraction_image.setImageBitmap(gray_bitmap);
                        Canvas canvas = new Canvas(gray_bitmap);
                        canvas.drawBitmap(gray_bitmap, 0, 0, p);
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                        attraction_image.setImageBitmap(small_scaled_bm);
                        break;
                }
                return false;
            }
        });
        attraction_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == attraction_image) {
                    // custom dialog
                    final Dialog dialog = new Dialog(ChoiceActivity.this_choice_activity);
                    dialog.setContentView(R.layout.attraction_image_view);
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


                    // set the custom dialog components - text, image and button
                    ImageView att_image = (ImageView)dialog.findViewById(R.id.attraction_image_dialog);

                    att_image.setImageBitmap(scaled_bm );
                    att_image.setLayoutParams(new LinearLayout.LayoutParams(
                            imageWidthInPX, imageWidthInPX));

                    TextView title = (TextView)dialog.findViewById(R.id.image_title_choice);
                    title.setText(curr_attraction._name);

                    Button ok_image = (Button) dialog.findViewById(R.id.ok_attraction_image_dialog);
                    ok_image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(dialog.getWindow().getAttributes());
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    dialog.show();
                    dialog.getWindow().setAttributes(lp);
                }
            }
        });
    }
    public static int text_to_minutes(String text, int utc_difference){
        int return_value =  ((Integer.parseInt(text.substring(0, 2)) + utc_difference) * 60
                + Integer.parseInt(text.substring(3, 5))) % (24 * 60);
        if(return_value < 0){
            return_value += (24 * 60);
        }
        return return_value;
    }
    public static String get_curr_hour(){
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatGmt.format(new Date());
    }
    void choice_algorithm(int att_id, int next_choice){
        if(next_choice == attractions_choices.get(Integer.toString(att_id))){
            return;
        }
        if(next_choice > attractions_choices.get(Integer.toString(att_id))){
            attractions_choices.put(Integer.toString(att_id), next_choice);
            for(int i = 1; i <= attractions._attractions_array.length; i++){
                int low = 0, high = attractions_maxes.get(Integer.toString(i));
                while(low < high){
                    int mid = (int)(((high + low) / 2.0) + 0.6);
                    if(is_amount_possible(i, mid)){
                        low = mid;
                    }else{
                        high = mid - 1;
                    }
                }
                attractions_maxes.put(Integer.toString(i), low);
            }
        }else{//if(next_choice < attractions_choices.get(Integer.toString(att_id)))
            attractions_choices.put(Integer.toString(att_id), next_choice);
            for(int i = 1; i <= attractions._attractions_array.length; i++){
                int low = attractions_maxes.get(Integer.toString(i));
                int high = Math.min(sum_zeros(park_full_times.child(Integer.toString(i))),
                        attractions._attractions_array[i - 1]._max);
                while(low < high){
                    int mid = (int)(((high + low) / 2.0) + 0.6);
                    if(is_amount_possible(i, mid)){
                        low = mid;
                    }else{
                        high = mid - 1;
                    }
                }
                attractions_maxes.put(Integer.toString(i), low);
            }
        }

        update_min_time_components_number();
    }
    boolean is_time_possible(int number_time_components){
        int num_attractions = 0;
        for(int i = 1; i <= attractions._attractions_array.length; i++){
            num_attractions += attractions_choices.get(Integer.toString(i));
        }
        if(num_attractions == 0){
            return true;
        }
        if(number_time_components == 0){
            return true;
        }
        /*if(num_attractions > (int) park_full_times.child("1").getChildrenCount()){
            return false;
        }*/
        double[][] full_times = new double[num_attractions][number_time_components];
        int current_row = 0;
        for(Attraction attraction: attractions._attractions_array){
            int current_choice = attractions_choices.get(attraction._key);
            for (int i = 0; i < current_choice; i++) {
                for (int j = 0; j < number_time_components; j++) {
                    full_times[current_row][j] = Math.max(park_full_times.child(attraction._key)
                            .child(Integer.toString(j)).getValue(Integer.class), user_impossible_times[j]);
                }
                current_row++;
            }
        }
        HungarianAlgorithm algorithm_object = new HungarianAlgorithm(full_times);
        return Math.abs(get_sum(algorithm_object.execute(), full_times)) < 0.001;//Checks if sum equals 0;
    }
    boolean is_amount_possible(int att_id, int amount){
        int num_attractions = 0;
        for(int i = 1; i <= attractions._attractions_array.length; i++){
            if(i != att_id) {
                num_attractions += attractions_choices.get(Integer.toString(i));
            }else{
                num_attractions += amount;
            }
        }
        if(num_attractions == 0){
            return true;
        }
        if(num_attractions > (int) park_full_times.child("1").getChildrenCount()){
            return false;
        }
        double[][] full_times = new double[num_attractions][(int) park_full_times.child("1").getChildrenCount()];
        int current_row = 0;
        for(Attraction attraction: attractions._attractions_array){
            int current_choice = attractions_choices.get(attraction._key);
            for (int i = 0; i < (attraction._key.equals(Integer.toString(att_id)) ? amount :
                    current_choice); i++) {
                for (int j = 0; j < park_full_times.child("1").getChildrenCount(); j++) {
                    full_times[current_row][j] = Math.max(park_full_times.child(attraction._key)
                    .child(Integer.toString(j)).getValue(Integer.class), user_impossible_times[j]);
                }
                current_row++;
            }
        }
        HungarianAlgorithm algorithm_object = new HungarianAlgorithm(full_times);
        return Math.abs(get_sum(algorithm_object.execute(), full_times)) < 0.001;//Checks if sum equals 0;
    }
    double get_sum(int[] result, double[][] costs){
        double sum = 0;
        for(int row = 0; row < costs.length; row++){
            sum += costs[row][result[row]];
        }
        return sum;
    }
    public void update_clock(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (clock == null){
                    clock = (TextView)findViewById(R.id.clock);
                }
                int h = current_time / (60*60);
                int m = (current_time % (60 * 60)) / 60;
                int s = current_time % 60;
                clock.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
            }
        });
    }
    public void update_max_trip_hour(){
        int minutes = current_time / 60;
        int finish_time_in_minutes = (minutes / 5) * 5 + park_sync_times.child("start waiting time").getValue(Integer.class)
                + min_time_components * park_sync_times.child("curr time between attractions").getValue(Integer.class);
        String trip_end = "";
        if(min_time_components > -1){
            int h = finish_time_in_minutes / (60);
            int m = finish_time_in_minutes % 60;
            trip_end = String.format(Locale.US, "Trip ends at: %02d:%02d", h, m);
        }
        TextView trip_end_text_view = (TextView)findViewById(R.id.finish_trip_time);
        trip_end_text_view.setText(trip_end);
    }
    public void update_min_time_components_number(){
        //Searching for the minimal number of time elements required to fulfill the user's request:
        int sum = 0;
        for (String key : attractions_choices.keySet()){
            sum += attractions_choices.get(key);
        }
        int low = sum, high = (int)park_full_times.child("1").getChildrenCount();
        if ((high < low) || (sum == 0)){
            min_time_components = -1;
            update_max_trip_hour();
            return;
        }
        while(low < high){
            int mid = (low + high) / 2;
            if(is_time_possible(mid)){
                high = mid;
            }else{
                low = mid + 1;
            }
        }
        min_time_components = low;
        update_max_trip_hour();
    }
    /*@Override
    public void onPause(){
        super.onPause();
        this_choice_activity = ChoiceActivity.this;
    }
    @Override
    public void onResume(){
        super.onResume();
        this_choice_activity = ChoiceActivity.this;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        this_choice_activity = ChoiceActivity.this;
    }*/
}
