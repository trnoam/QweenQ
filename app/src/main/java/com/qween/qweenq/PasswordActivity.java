package com.qween.qweenq;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PasswordActivity extends AppCompatActivity {


    public static FirebaseStorage storage_password = null;
    public static StorageReference storage_ref_password = null;
    public static Firebase main_ref = null;
    public static SharedPreferences data_password = null;
    public static SharedPreferences.Editor editor_password = null;
    public static Toast prev_toast_password = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.password_toolbar);
        setSupportActionBar(toolbar);

        storage_password = FirebaseStorage.getInstance();
        storage_ref_password = storage_password.getReferenceFromUrl("gs://qweenq-48917.appspot.com/");
        main_ref = new Firebase("https://qweenq-48917.firebaseio.com/");
        data_password = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        final String attraction_id = intent.getStringExtra("attraction id");
        String attraction_name = intent.getStringExtra("attraction name");
        String picture_path = intent.getStringExtra("picture path");
        final String park_id = intent.getStringExtra("park id");
        int amount_people = intent.getIntExtra("amount people", 0);
        main_ref.child("parks").child(park_id).child("attractions").child(attraction_id).child("password").addListenerForSingleValueEvent(
                new ValueEventListener(){
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        TextView password_text = (TextView)findViewById(R.id.password_text);
                        password_text.setText(dataSnapshot.getValue(String.class));
                    }

                    @Override
                    public void onCancelled(FirebaseError databaseError) {
                        my_toast_password("Internet error!!!");
                    }
                }
        );
        TextView amount_people_attraction_name = (TextView)findViewById(R.id.amount_people_attraction_name);
        String second_text = (amount_people <= 1) ? "Single rider" : Integer.toString(amount_people);
        second_text += ", ";
        second_text += attraction_name;
        amount_people_attraction_name.setText(second_text);
        setTitle(intent.getStringExtra("park name"));

        StorageReference attraction_image_ref = storage_ref_password.child("images/" +  picture_path);
        final ImageView attraction_image = (ImageView)findViewById(R.id.attraction_image);
        attraction_image.setClickable(true);
        if(data_password.contains("attraction" + attraction_id + "park" + park_id)){
            String image_as_string = data_password.getString("attraction" + attraction_id + "park" + park_id, "");
            byte[] image = Base64.decode(image_as_string, Base64.DEFAULT);
            orginize_image_password(image, attraction_image);
        }
        else {
            attraction_image_ref.getBytes(ChoiceActivity.TEN_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    String image_as_string = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    editor_password = data_password.edit();
                    editor_password.putString("attraction" + attraction_id + "park" +
                            park_id, image_as_string);
                    editor_password.apply();
                    orginize_image_password(bytes, attraction_image);
                }
            }
            ).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    my_toast_password("Internet error!!!");
                }
            });
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_password, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    private void orginize_image_password(byte[] image, ImageView main_image){
        Display display = PasswordActivity.this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int imageWidthInPX = size.x;
        final Bitmap unscaled_bm = BitmapFactory.decodeByteArray(image, 0, image.length);
        final Bitmap scaled_bm = Bitmap.createScaledBitmap(unscaled_bm,imageWidthInPX,imageWidthInPX, true);
        DisplayMetrics dm = new DisplayMetrics();
        PasswordActivity.this.getWindowManager().getDefaultDisplay().getMetrics(dm);

        main_image.setImageBitmap(scaled_bm );
        main_image.setLayoutParams(new LinearLayout.LayoutParams(
                imageWidthInPX, imageWidthInPX));
    }
    public void my_toast_password(String message){
        if(prev_toast_password != null){
            prev_toast_password.cancel();
        }
        prev_toast_password = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast_password.show();
    }
}
