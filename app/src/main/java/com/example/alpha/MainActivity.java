package com.example.alpha;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    TextView userState;
    EditText eventTitleET, inventorET;

    String eventTitle, inventorName;
    myDate defaultDate;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userState = findViewById(R.id.userState);
        eventTitleET = findViewById(R.id.eventTitleET);
        inventorET = findViewById(R.id.inventorET);

        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        Variable.setEmailVer(settings.getString("email",""));
        userState.setText("You signed-in as: "+  Variable.getEmailVer());

    }


    public void createEvent(View view) {
        eventTitle = eventTitleET.getText().toString();
        inventorName = inventorET.getText().toString();

        if (eventTitle.isEmpty()){
            eventTitleET.requestFocus();
        }
        else if (inventorName.isEmpty()){
            inventorET.requestFocus();
        }
        else{
            defaultDate = new myDate(1,1,1,13,0);
            Event tempEvent = new Event(eventTitle, inventorName, defaultDate);

            sendToGoogleCalender(tempEvent);

            eventTitleET.setText("");
            inventorET.setText("");
            sendToGoogleCalender(tempEvent);
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void sendToGoogleCalender(Event tempEvent) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, eventTitle);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, inventorName);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "Local");
        intent.putExtra(Intent.EXTRA_EMAIL, Variable.emailVer);

        if (intent.resolveActivity(getPackageManager()) != null){
            Intent intent1 = Intent.createChooser(intent, "Open using");
            startActivity(intent1);
        } else {
            Toast.makeText(this, "No supported app", Toast.LENGTH_SHORT).show();
        }
    }

    private void Logout() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Are you sure?");
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        Variable.setEmailVer(settings.getString("email",""));
        adb.setMessage(Variable.getEmailVer().substring(0,Variable.emailVer.indexOf("@"))+" will logged out");

        adb.setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                FirebaseAuth.getInstance().signOut();
                Intent si = new Intent(MainActivity.this, LoginActivity.class);

                SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("email", "");
                editor.putBoolean("stayConnect",false);
                editor.apply();

                startActivity(si);
                finish();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        Intent si;
        if (id == R.id.logOut){
            Logout();
        }
        else if (id == R.id.pdf){
            si = new Intent(this, pdfCreatorActivity.class);
            startActivity(si);
        }
        else if (id == R.id.record){
            si = new Intent(this, RecordActivity.class);
            startActivity(si);
        }
        else if (id == R.id.map){
            si = new Intent(this, LocationActivity.class);
            startActivity(si);
        }

        return super.onOptionsItemSelected(item);
    }

}