package com.qween.qweenq;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class EmployeePasswordActivity extends AppCompatActivity {


    public Toast prev_toast_employee_password = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.employee_password_toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String attraction_id = intent.getStringExtra("attraction id");
        String attraction_name = intent.getStringExtra("attraction name");
        String park_name = intent.getStringExtra("park name");
        ArrayList<String> adjectives = intent.getStringArrayListExtra("adjectives");
        ArrayList<String> nouns = intent.getStringArrayListExtra("nouns");
        setTitle(park_name + ", " + attraction_name);

        Spinner adjective = (Spinner) findViewById(R.id.adjective_employee);
        ArrayAdapter<String> adapter_adjective = new ArrayAdapter<String>(this,
                R.layout.spinner_item, adjectives);
        adapter_adjective.setDropDownViewResource(R.layout.spinner_dropdown_item);
        adjective.setAdapter(adapter_adjective);
        adjective.getBackground().setColorFilter(
                ContextCompat.getColor(EmployeePasswordActivity.this, R.color.colorToolBar), PorterDuff.Mode.SRC_ATOP);

        Spinner noun = (Spinner) findViewById(R.id.noun_employee);
        ArrayAdapter<String> adapter_noun = new ArrayAdapter<String>(this,
                R.layout.spinner_item, nouns);
        adapter_noun.setDropDownViewResource(R.layout.spinner_dropdown_item);
        noun.setAdapter(adapter_noun);
        noun.getBackground().setColorFilter(
                ContextCompat.getColor(EmployeePasswordActivity.this, R.color.colorToolBar), PorterDuff.Mode.SRC_ATOP);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    protected void onPause() {
        super.onPause();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_password_employee, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public void on_random_password_clicked(View v){
        my_toast_employee_password("random password clicked");
    }
    public void my_toast_employee_password(String message){
        if(prev_toast_employee_password != null){
            prev_toast_employee_password.cancel();
        }
        prev_toast_employee_password = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        prev_toast_employee_password.show();
    }
}
