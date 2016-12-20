package com.qween.qweenq;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Vector;

public class ChoiceActivity extends AppCompatActivity {

    public Firebase main_ref = null;
    public static int park_id_choice = 0;
    public static String park_name = null;
    public static FirebaseStorage storage;
    public static StorageReference storage_ref;
    public static final long TEN_MEGABYTE = 10 * 1024 * 1024;
    public static long ATTRACTION_LAYOUT_HEIGHT = 250;
    public static AttractionsData attractions = null;
    public static Vector<View> views_vector;
    public static Vector<View> costs_vector;
    public static int max_width_choice_part;
    public static final int LANDSCAPE_WEIGHT = 5;
    public static final int PORTRAIT_WEIGHT = 10;
    public static final int PORTRAIT_DEFAULT = 8;
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
    public DataSnapshot park_sync_times = null;

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
        this_choice_activity.update_coins_lets_go_button();
        for(final Attraction attraction : attractions_data._attractions_array){
            final View times_choice;
            final int amount_possible = Math.max(Math.min(this_choice_activity.attractions_maxes.get(attraction._key),
                    this_choice_activity.coins_left / attraction._cost),
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
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>
                            (this_choice_activity, android.R.layout.simple_spinner_dropdown_item, choices);
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
                    ((Spinner)times_choice).setAdapter(spinnerArrayAdapter);
                    ((Spinner)times_choice).setSelection(this_choice_activity.attractions_choices.get(attraction._key));
                    times_choice.getBackground().setColorFilter(
                            ContextCompat.getColor(this_choice_activity, R.color.colorToolBar), PorterDuff.Mode.SRC_ATOP);
                    ((Spinner)times_choice).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int curr_choice, long id) {
                            ((TextView) parentView.getChildAt(0)).setTextColor(
                                    ContextCompat.getColor(this_choice_activity, R.color.colorToolBar));
                            ((TextView) parentView.getChildAt(0)).setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
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
            LinearLayout curr_layout = new LinearLayout(this_choice_activity);
            curr_layout.setOrientation(LinearLayout.HORIZONTAL);
            curr_layout.setGravity(Gravity.START);
            curr_layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)ATTRACTION_LAYOUT_HEIGHT));

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
                StorageReference attraction_image_ref = storage_ref.child("images/" +  attraction._picture);
                final RoundedImageView attraction_image = new RoundedImageView(this_choice_activity);
                attraction_image.setClickable(true);
                final ImageView attraction_dialog_window_image = new ImageView(this_choice_activity);
                if(data_choice.contains("attraction" + attraction._key + "park" + park_id_choice)){
                    String image_as_string = data_choice.getString("attraction" + attraction._key + "park" + park_id_choice,
                            "");
                    byte[] image = Base64.decode(image_as_string, Base64.DEFAULT);
                    orginize_image(attraction, image, attraction_image, this_choice_activity, attraction_dialog_window_image);
                }
                else {
                    attraction_image_ref.getBytes(ChoiceActivity.TEN_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            String image_as_string = Base64.encodeToString(bytes, Base64.NO_WRAP);
                            editor_choice = data_choice.edit();
                            editor_choice.putString("attraction" + attraction._key + "park" +
                                    park_id_choice, image_as_string);
                            editor_choice.apply();
                            orginize_image(attraction, bytes, attraction_image,
                                    this_choice_activity, attraction_dialog_window_image);
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
                ((ViewGroup)images_vector.elementAt(i).getParent()).removeView(images_vector.elementAt(i));
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
                        dialog.setTitle(attraction._name);


                        // set the custom dialog components - text, image and button
                        TextView text = (TextView) dialog.findViewById(R.id.description_text);
                        text.setText(attraction._description);

                        Button ok_description = (Button) dialog.findViewById(R.id.ok_description);
                        ok_description.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        /*dialogButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });*/

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
                        sum_zeros(park_sync_times.child("full times").child(attraction._key)),
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
        lets_go_button_choice.setText(getResources().getString(R.string.button_lets_go) + "(" +
                Integer.toString(coins_left) + ((coins_left == 1) ? " coin" : " coins") + " left)");
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public void on_lets_go_choice_clicked(View lets_go_choice_button){
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
                .setValue(unanswered_answer_value);

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
                            startActivity(intent);
                            is_calendar_active = true;
                        }
                    }
                    @Override
                    public void onCancelled(FirebaseError firebase_error) {
                        my_toast("Internet Error!!!");
                    }
                });

        Snackbar.make(findViewById(R.id.main_choice_layout), "Please wait, your request is being handled",
                Snackbar.LENGTH_INDEFINITE).show();
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
        main_ref.child("parks").child(Integer.toString(park_id_choice)).child("sync times").
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot sync_times) {
                        park_sync_times = sync_times;
                        load_components(attractions_data_for_load_components, is_first_load_components);
                        List<Attraction> sorted_attractions = new ArrayList<>();
                        for(Attraction attraction : attractions._attractions_array){
                            sorted_attractions.add(attraction);
                        }
                        Collections.sort(sorted_attractions, new Comparator<Attraction>(){
                            public int compare(Attraction attraction1, Attraction attraction2) {
                                return attraction1._cost - attraction2._cost;
                            }
                        });
                        int coins_counter = 0;
                        int attractions_counter= 0;
                        for(Attraction attraction : sorted_attractions){
                            if(!attraction._is_full) {
                                if ((coins_counter + (attraction._cost
                                        * attraction._max)) > max_coins) {
                                    attractions_counter += (max_coins - coins_counter)
                                            / attraction._cost;
                                    break;
                                } else {
                                    attractions_counter += attraction._max;
                                    coins_counter += attraction._max
                                            * attraction._cost;
                                }
                            }
                        }
                        //attractions_counter now holds the maximum amount of attraction the user can pick using the coins.
                        int average_attraction_time = sync_times.child("curr time between attractions").getValue(Integer.class)
                                + sync_times.child("time range").getValue(Integer.class);
                        String curr_hour = get_curr_hour();
                        int curr_time_minutes = text_to_minutes(curr_hour,
                                sync_times.child("UTC difference").getValue(Integer.class));
                        String closing_time = sync_times.child("closing time").getValue(String.class);
                        int closing_time_minutes = text_to_minutes(closing_time, 0);
                        num_attractions_allowed_by_time = (closing_time_minutes - curr_time_minutes) / average_attraction_time;
                        /*if(closing_time_minutes <= curr_time_minutes){
                            my_toast("Sorry, park is closed right now", Toast.LENGTH_LONG);
                            ChoiceActivity.this.finish();
                            return;
                        }*/
                        //TODO
                        if(num_attractions_allowed_by_time < attractions_counter){
                            display_amount_attractions_to_user(num_attractions_allowed_by_time);
                        }
                    }
                    public void onCancelled(FirebaseError firebaseError) {
                        my_toast("Internet Error!!!");
                    }
                });
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
        DisplayMetrics dm = new DisplayMetrics();
        this_choice_activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

        attraction_image.setImageBitmap(scaled_bm );
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                (int) ATTRACTION_LAYOUT_HEIGHT, (int) ATTRACTION_LAYOUT_HEIGHT);
        layoutParams.gravity = Gravity.CENTER;
        attraction_image.setLayoutParams(layoutParams);
        attraction_dialog_window_image.setImageBitmap(scaled_bm);
                        /*int height = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350,
                                getResources().getDisplayMetrics()) + 0.5);//stack overflow dialog image*/
                        /*attraction_dialog_window_image.setLayoutParams(
                                new LinearLayout.LayoutParams((int)LinearLayout.LayoutParams.MATCH_PARENT,
                                        (int)LinearLayout.LayoutParams.MATCH_PARENT));*/
        attraction_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == attraction_image) {
                    // custom dialog
                    final Dialog dialog = new Dialog(ChoiceActivity.this_choice_activity);
                    dialog.setContentView(R.layout.attraction_image_view);
                    dialog.setTitle(curr_attraction._name);


                    // set the custom dialog components - text, image and button
                    ImageView att_image = (ImageView)dialog.findViewById(R.id.attraction_image_dialog);
                    Display display = this_choice_activity.getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    final int imageWidthInPX = size.x;
                    final Bitmap unscaled_bm = BitmapFactory.decodeByteArray(image, 0, image.length);
                    final Bitmap scaled_bm = Bitmap.createScaledBitmap(unscaled_bm,imageWidthInPX,imageWidthInPX, true);
                    DisplayMetrics dm = new DisplayMetrics();
                    this_choice_activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

                    att_image.setImageBitmap(scaled_bm );
                    att_image.setLayoutParams(new LinearLayout.LayoutParams(
                            imageWidthInPX, imageWidthInPX));

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

    /**
     *
     * @param text has to be in pattern hh:mm
     * @param utc_difference the timezone's utc difference
     * @return the time in minutes as an integer.
     */
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
                int low = attractions_maxes.get(Integer.toString(i)), high = Math.min(
                        sum_zeros(park_sync_times.child("full times").child(Integer.toString(i))),
                        attractions._attractions_array[i - 1]._max
                );
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
        if(num_attractions > (int) park_sync_times.
                child("full times").child("1").getChildrenCount()){
            return false;
        }
        double[][] full_times = new double[num_attractions][(int) park_sync_times.
                child("full times").child("1").getChildrenCount()];
        int current_row = 0;
        for(Attraction attraction: attractions._attractions_array){
            int current_choice = attractions_choices.get(attraction._key);
            for (int i = 0; i < (attraction._key.equals(Integer.toString(att_id)) ? amount :
                    current_choice); i++) {
                for (int j = 0; j < park_sync_times.child("full times").child("1").getChildrenCount(); j++) {
                    full_times[current_row][j] = park_sync_times.child("full times").child(attraction._key)
                    .child(Integer.toString(j)).getValue(Integer.class);
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
