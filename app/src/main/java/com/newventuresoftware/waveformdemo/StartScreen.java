package com.newventuresoftware.waveformdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.newventuresoftware.waveformdemo.R;

public class StartScreen extends AppCompatActivity {


    /** Called when the user clicks the "TAP TO BEGIN" button */
    public void redirect(View view) {
<<<<<<< HEAD
        Intent intent = new Intent(this, com.newventuresoftware.waveformdemo.MainMenu.class);
=======
        Intent intent = new Intent(this, com.newventuresoftware.waveformdemo.MainActivity.class);
>>>>>>> d976102d5b97b23b36dd4dabee7913e41347ad12
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
<<<<<<< HEAD
        getMenuInflater().inflate(R.menu.menu_start_screen, menu);
=======
        //getMenuInflater().inflate(R.menu.menu_start_screen, menu);        //add menu/menu_start_screen.xml (below) is present

        //still couldn't delete...

//        <menu xmlns:android="http://schemas.android.com/apk/res/android"
//        xmlns:app="http://schemas.android.com/apk/res-auto"
//        xmlns:tools="http://schemas.android.com/tools"
//        tools:context=".StartScreen">
//        <item
//        android:id="@+id/action_settings"
//        android:orderInCategory="100"
//        android:title="@string/action_settings"
//        app:showAsAction="never" />
//        </menu>

>>>>>>> d976102d5b97b23b36dd4dabee7913e41347ad12
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
