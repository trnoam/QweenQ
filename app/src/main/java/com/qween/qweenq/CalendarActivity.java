package com.qween.qweenq;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.support.design.widget.FloatingActionButton;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class CalendarActivity extends AppCompatActivity {

    public static Firebase main_ref;
    public static long ATTRACTION_LAYOUT_HEIGHT = 0;
    public static Vector<View> calendar_views_vector;
    public static StorageReference storage_ref;
    public static FirebaseStorage storage;
    public static SharedPreferences data_calendar;
    public static SharedPreferences.Editor editor_calendar;
    public static int park_id_calendar = 0;
    public static Vector<RoundedImageView> images_calendar_vector;
    public static DataSnapshot attractions_information = null;
    public static DataSnapshot times_of_attractions = null;
    public static DataSnapshot syncing_times = null;
    public Toast prev_toast = null;
    public Semaphore sem;
    public String main_code = "";
    public static Map<Integer, DataSnapshot> attractions_passwords;
    public String park_name = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.calendar_toolbar);
        setSupportActionBar(toolbar);

        display_dialog_rechoose();

        calendar_views_vector = new Vector<>();
        attractions_passwords = new HashMap<>();
        set_attraction_layout_height_calendar();

        main_ref = new Firebase("https://qweenq-48917.firebaseio.com/");
        storage = FirebaseStorage.getInstance();
        storage_ref = storage.getReferenceFromUrl("gs://qweenq-48917.appspot.com/");
        data_calendar = PreferenceManager.getDefaultSharedPreferences(this);
        park_id_calendar = data_calendar.getInt("park_id", 0);
        main_code = data_calendar.getString("curr main_code", "");

        //Need to check if the park's name is in the default SharedPreferences
        main_ref.child("parks").child(Integer.toString(park_id_calendar)).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                park_name = dataSnapshot.getValue(String.class);
                int amount_people = data_calendar.getStringSet("curr_codes", null).size();
                setTitle(park_name + "(" + ((amount_people <= 1) ? "Single rider" : Integer.toString(amount_people)) + ")");
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Internet Error!!!");
            }
        });
        sem = new Semaphore(1);
        //TODO: Must be changed
        /*if(ChoiceActivity.attractions != null){
            attractions_information = ChoiceActivity.attractions;
        }
        else{*/
            images_calendar_vector = new Vector<>();
            main_ref.child("parks").child(Integer.toString(park_id_calendar)).child("attractions").
                    addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    attractions_information = snapshot;
                    try {
                        sem.acquire();
                    } catch (InterruptedException e) {

                    }
                    if(attractions_information != null && times_of_attractions != null && syncing_times != null){
                        load_components(attractions_information, true, CalendarActivity.this,
                                times_of_attractions, syncing_times);
                        keep_passwords_synced(CalendarActivity.this);
                    }
                    sem.release();
                }
                public void onCancelled(FirebaseError firebaseError) {
                    my_toast("Internet error!!!", Toast.LENGTH_SHORT);
                }
            });
        //}
        main_ref.child("parks").child(Integer.toString(park_id_calendar)).child("answers").child(main_code).
                child("attractions times").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        times_of_attractions = snapshot;
                        try {
                            sem.acquire();
                        } catch (InterruptedException e) {

                        }
                        if(attractions_information != null && times_of_attractions != null && syncing_times != null){
                            load_components(attractions_information, true, CalendarActivity.this,
                                    times_of_attractions, syncing_times);
                            keep_passwords_synced(CalendarActivity.this);
                        }
                        sem.release();
                    }
                    public void onCancelled(FirebaseError firebaseError) {
                        my_toast("Internet error!!!", Toast.LENGTH_SHORT);
                    }
                });
        main_ref.child("parks").child(Integer.toString(park_id_calendar)).child("sync times").
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                syncing_times = snapshot;
                try {
                    sem.acquire();
                } catch (InterruptedException e) {

                }
                if(attractions_information != null && times_of_attractions != null && syncing_times != null){
                    load_components(attractions_information, true, CalendarActivity.this,
                            times_of_attractions, syncing_times);
                    keep_passwords_synced(CalendarActivity.this);
                }
                sem.release();
            }
            public void onCancelled(FirebaseError firebaseError) {
                my_toast("Internet error!!!", Toast.LENGTH_SHORT);
            }
        });
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;
            case android.R.id.home:
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onBackPressed()
    {
        return;
    }
    public static void set_attraction_layout_height_calendar(){
        CalendarActivity.ATTRACTION_LAYOUT_HEIGHT = convertDpToPixel(CodeActivity.DP_ATTRACTION_HEIGHT);
    }
    public static int convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
    public void display_dialog_rechoose(){
        final Dialog dialog = new Dialog(CalendarActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rechoose_window);

        Button ok_description = (Button) dialog.findViewById(R.id.ok_rechoose);
        ok_description.setOnClickListener(new View.OnClickListener() {
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
    public static void load_components(final DataSnapshot attractions_data, Boolean is_first,
                                       final CalendarActivity this_calendar_activity, DataSnapshot attractions_times,
                                       final DataSnapshot sync_times){
        int max_width_calendar_part = -1;

        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray typedArray = this_calendar_activity.obtainStyledAttributes(attrs);
        int backgroundResource = typedArray.getResourceId(0, 0);

        LinearLayout attractions_layout = (LinearLayout)this_calendar_activity.findViewById(R.id.attractions_times_layout);
        attractions_layout.removeAllViews();
        int curr_hour = ChoiceActivity.text_to_minutes(ChoiceActivity.get_curr_hour(),
                sync_times.child("UTC difference").getValue(Integer.class));
        update_attractions_passwords();
        for(final DataSnapshot attraction : attractions_times.getChildren()){
            //START:
            final TextView start_time_text = new TextView(this_calendar_activity);
            int attraction_hour = ChoiceActivity.text_to_minutes(attraction.getKey(), 0);
            int dt = attraction_hour - curr_hour;
            if(dt > 0) {
                start_time_text.setText("START in " + ((dt / 60 > 0) ? Integer.toString(dt / 60) : "") + "h "
                        + Integer.toString(dt % 60) + "m");
                start_time_text.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.colorToolBar));
            }else{
                if(Math.abs(dt) < sync_times.child("time range").getValue(Integer.class)){
                    start_time_text.setText("STARTED");
                    start_time_text.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.colorToolBar));
                }else{
                    if(Math.abs(dt) == sync_times.child("time range").getValue(Integer.class)){
                        start_time_text.setText("ENDING");
                    }
                    else {
                        start_time_text.setText("FINISHED");
                        start_time_text.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.gray));
                    }
                }
            }
            start_time_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
            start_time_text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            start_time_text.measure(0, 0);
            int width = (int)(1.5 * start_time_text.getMeasuredWidth());
            if(width > max_width_calendar_part){
                max_width_calendar_part = width;
            }
            calendar_views_vector.addElement(start_time_text);
            start_time_text.setLayoutParams(
                    new LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
            //END:
            final TextView end_time_text = new TextView(this_calendar_activity);
            attraction_hour += sync_times.child("time range").getValue(Integer.class);
            dt = attraction_hour - curr_hour;
            if(dt > 0) {
                end_time_text.setText("END in " + ((dt / 60 > 0) ? Integer.toString(dt / 60) : "") + "h "
                        + Integer.toString(dt % 60) + "m");
                end_time_text.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.colorToolBar));
            }else{
                end_time_text.setText("");
            }
            end_time_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
            end_time_text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            end_time_text.measure(0, 0);
            width = (int)(1.5 * end_time_text.getMeasuredWidth());
            if(width > max_width_calendar_part){
                max_width_calendar_part = width;
            }
            calendar_views_vector.addElement(end_time_text);
            end_time_text.setLayoutParams(
                    new LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
        }
        for(View text_view : calendar_views_vector){
            text_view.setLayoutParams(
                    new LinearLayout.LayoutParams(max_width_calendar_part, (int) ATTRACTION_LAYOUT_HEIGHT / 2));
        }
        int i = 0;
        for(final DataSnapshot attraction : attractions_times.getChildren()){
            LinearLayout curr_layout = new LinearLayout(this_calendar_activity);
            curr_layout.setOrientation(LinearLayout.HORIZONTAL);
            curr_layout.setGravity(Gravity.START);
            curr_layout.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT, (int)ATTRACTION_LAYOUT_HEIGHT));

            final LinearLayout times_part = new LinearLayout(this_calendar_activity);
            times_part.setOrientation(LinearLayout.VERTICAL);
            times_part.setGravity(Gravity.BOTTOM);
            times_part.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT, (int)ATTRACTION_LAYOUT_HEIGHT));

            if(calendar_views_vector.elementAt(2 * i).getParent() != null){
                ((ViewGroup)calendar_views_vector.elementAt(2 * i).getParent()).
                        removeView(calendar_views_vector.elementAt(2 * i));
            }
            if(calendar_views_vector.elementAt(2 * i + 1).getParent() != null){
                ((ViewGroup)calendar_views_vector.elementAt(2 * i + 1).getParent()).
                        removeView(calendar_views_vector.elementAt(2 * i + 1));
            }
            times_part.addView(calendar_views_vector.elementAt(2 * i));//START
            times_part.addView(calendar_views_vector.elementAt(2 * i + 1));//END

            curr_layout.addView(times_part);
            if(is_first) {
                StorageReference attraction_image_ref = storage_ref.child("images/" +
                        attractions_data.child(attraction.getValue(String.class)).child("picture").getValue(String.class));
                final RoundedImageView attraction_image = new RoundedImageView(this_calendar_activity);
                attraction_image.setClickable(true);
                final ImageView attraction_dialog_window_image = new ImageView(this_calendar_activity);

                if(data_calendar.contains("attraction" + attraction.getValue(Integer.class).toString() + "park" + park_id_calendar)){
                    String image_as_string = data_calendar.getString("attraction" +
                            attraction.getValue(Integer.class).toString() + "park" + park_id_calendar, "");
                    byte[] image = Base64.decode(image_as_string, Base64.DEFAULT);
                    orginize_image(attraction, image, attraction_image, this_calendar_activity, attraction_dialog_window_image);
                }
                else {
                    attraction_image_ref.getBytes(ChoiceActivity.TEN_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            String image_as_string = Base64.encodeToString(bytes, Base64.NO_WRAP);
                            editor_calendar = data_calendar.edit();
                            editor_calendar.putString("attraction" + attraction.getValue() + "park" +
                                    park_id_calendar, image_as_string);
                            editor_calendar.apply();
                            orginize_image(attraction, bytes, attraction_image,
                                    this_calendar_activity, attraction_dialog_window_image);
                        }
                    }
                    ).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(this_calendar_activity.getApplicationContext(),
                                    "Internet error!!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                //images_calendar_vector.addElement(attraction_image);
                curr_layout.addView(attraction_image);
            }/*else{
                ((ViewGroup)images_calendar_vector.elementAt(i).getParent()).removeView(images_calendar_vector.elementAt(i));
                curr_layout.addView(images_calendar_vector.elementAt(i));
                images_calendar_vector.elementAt(i).setClickable(true);
            }*/

            final LinearLayout name_lets_go_part = new LinearLayout(this_calendar_activity);
            name_lets_go_part .setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT,
                    GridLayout.LayoutParams.MATCH_PARENT));
            name_lets_go_part.setOrientation(LinearLayout.VERTICAL);
            name_lets_go_part.setGravity(Gravity.TOP);
            name_lets_go_part.setClickable(false);

            final TextView name_text = new TextView(this_calendar_activity);
            name_text.setText(attractions_data.child(attraction.getValue(Integer.class).toString())
                    .child("name").getValue(String.class));
            name_text.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.black));
            name_text.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 15);
            name_text.setTypeface(null, Typeface.BOLD);
            name_text.setClickable(true);
            name_text.setEllipsize(TextUtils.TruncateAt.END);
            name_text.setBackgroundResource(backgroundResource);
            name_text.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    if(v == name_text && (!this_calendar_activity.isFinishing())) {
                        // custom dialog
                        final Dialog dialog = new Dialog(this_calendar_activity);
                        dialog.setContentView(R.layout.description_view);
                        dialog.setTitle(attractions_data.child(attraction.getValue(Integer.class).toString())
                                .child("name").getValue(String.class));


                        // set the custom dialog components - text, image and button
                        TextView text = (TextView) dialog.findViewById(R.id.description_text);
                        text.setText(attractions_data.child(attraction.getValue(Integer.class).toString())
                                .child("description").getValue(String.class));

                        Button ok_description = (Button) dialog.findViewById(R.id.ok_description);
                        ok_description.setOnClickListener(new View.OnClickListener() {
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
            name_lets_go_part.addView(name_text);

            final Button get_in_button = new Button(this_calendar_activity);
            get_in_button.setText("GET IN!");
            get_in_button.setTextColor(ContextCompat.getColor(this_calendar_activity, R.color.colorPrimary));
            get_in_button.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 15);
            get_in_button.setClickable(true);

            get_in_button.setBackgroundResource(backgroundResource);

            get_in_button.setTextSize(ATTRACTION_LAYOUT_HEIGHT / 20);
            get_in_button.setClickable(true);

            get_in_button.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    if(v != get_in_button || this_calendar_activity.park_name.equals("")){
                        this_calendar_activity.my_toast("please wait...");
                        return;
                    }
                    Intent intent = new Intent(this_calendar_activity, PasswordActivity.class);
                    intent.putExtra("attraction id", Integer.toString(attraction.getValue(Integer.class)));
                    intent.putExtra("attraction name", attractions_data.child(attraction.getValue(Integer.class).toString())
                            .child("name").getValue(String.class));
                    intent.putExtra("amount people", data_calendar.getStringSet("curr_codes", null).size());
                    intent.putExtra("picture path", attractions_data.child(attraction.getValue(String.class))
                            .child("picture").getValue(String.class));
                    intent.putExtra("park id", Integer.toString(park_id_calendar));
                    intent.putExtra("park name", this_calendar_activity.park_name);
                    this_calendar_activity.startActivity(intent);
                }
            });
            name_lets_go_part.addView(get_in_button);

            curr_layout.addView(name_lets_go_part);

            attractions_layout.addView(curr_layout);
            i++;
        }
        typedArray.recycle();
    }
    public static void orginize_image(final DataSnapshot curr_attraction, byte[] image, final RoundedImageView attraction_image,
                                      final CalendarActivity this_calendar_activity, final ImageView attraction_dialog_window_image){
        Display display = this_calendar_activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int imageWidthInPX = size.x;
        final Bitmap unscaled_bm = BitmapFactory.decodeByteArray(image, 0, image.length);
        final Bitmap scaled_bm = Bitmap.createScaledBitmap(unscaled_bm,imageWidthInPX,imageWidthInPX, true);
        DisplayMetrics dm = new DisplayMetrics();
        this_calendar_activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

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
                if(v == attraction_image && (!this_calendar_activity.isFinishing())) {
                    if(attraction_dialog_window_image.getParent() != null){
                        ((ViewGroup)attraction_dialog_window_image.getParent()).
                                removeView(attraction_dialog_window_image);
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(this_calendar_activity);
                    final AlertDialog dialog;
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    //LayoutInflater inflater = this_choice_activity.getLayoutInflater();
                    LinearLayout see_image_layout= new LinearLayout(this_calendar_activity);
                    see_image_layout.setOrientation(LinearLayout.VERTICAL);
                    see_image_layout.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT,
                            GridLayout.LayoutParams.WRAP_CONTENT));
                    see_image_layout.addView(attraction_dialog_window_image);
                    dialog.setView(see_image_layout);
                    //dialog.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
                    dialog.setTitle(curr_attraction.child("name").getValue(String.class));

                    dialog.show();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface d) {

                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Math.round(imageWidthInPX),
                                    Math.round(imageWidthInPX));
                            attraction_dialog_window_image.setLayoutParams(layoutParams);


                        }
                    });
                }
            }
        });
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
    public static void update_attractions_passwords(){
        for(DataSnapshot curr_attraction : times_of_attractions.getChildren()){
            DataSnapshot temp = attractions_information.child(Integer.toString(curr_attraction.getValue(Integer.class)));
            attractions_passwords.put(curr_attraction.getValue(Integer.class), attractions_information
                    .child(Integer.toString(curr_attraction.getValue(Integer.class))).child("password"));
        }
    }
    public void onResume(){
        super.onResume();
        // put your code here...

    }
    public void onPause(){
        super.onPause();
        // put your code here...

    }
    public static void keep_passwords_synced(final CalendarActivity this_calendar_activity){
        for(final int curr_attraction_id : attractions_passwords.keySet()){
            main_ref.child("parks").child(Integer.toString(park_id_calendar)).child("attractions")
                    .child(Integer.toString(curr_attraction_id)).child("password")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot password) {
                            attractions_passwords.put(curr_attraction_id, password);
                        }
                        @Override
                        public void onCancelled(FirebaseError firebase_error) {
                            this_calendar_activity.my_toast("Internet Error!!!");
                        }
                    });
        }
    }
}
