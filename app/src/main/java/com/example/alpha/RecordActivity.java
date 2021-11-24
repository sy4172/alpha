package com.example.alpha;

import static com.example.alpha.FBref.storageRef;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class RecordActivity extends AppCompatActivity implements View.OnCreateContextMenuListener {

    private static final int PERMISSION_CODE = 12;
    ListView lv;
    ImageButton recordBN;

    boolean isRecording, isPlaying;

    String fileName;
    final String currentTime = String.valueOf(System.currentTimeMillis());

    File rootPath, fileToPlay;

    ArrayAdapter<String> adp;
    ArrayList<String> recordNames;
    ArrayList<String> pathList;


    private String recordPermisson = Manifest.permission.RECORD_AUDIO;

    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    int fileDuration;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        lv = findViewById(R.id.lv);
        recordBN = findViewById(R.id.recordBN);


        rootPath = new File(this.getExternalFilesDir("/"), "myRECORDS");
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setOnCreateContextMenuListener(this);

        getAllRecordNames();

        adp = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, recordNames);
        lv.setAdapter(adp);

        isRecording = false;
    }

    private void getAllRecordNames() {
        pathList = new ArrayList<>();
        recordNames = new ArrayList<>();

        File[] files = new File(rootPath.getPath()).listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isFile()) {
                pathList.add(file.getPath());
                recordNames.add(file.getName());
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        v.setOnCreateContextMenuListener(this);
        menu.add("Upload");
        menu.add("Open");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo adpInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String option = item.getTitle().toString();

        if(option.equals("Upload")){
            uploadFile(recordNames.get(adpInfo.position));
        }
        else if (option.equals("Open")){
            playFile(recordNames.get(adpInfo.position));
        }

        return super.onContextItemSelected(item);
    }

    private void playFile(String fileName) {
        // creating an alertDialog
        if (isPlaying){
            stopAudio();
            isPlaying = false;
            playAudio(fileName);
            isPlaying = true;
        } else{
            isPlaying = true;
            playAudio(fileName);
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Playing "+fileName+"..");
        final SeekBar seekbar = new SeekBar(this);
        seekbar.setMax(fileDuration/1000);
        adb.setView(seekbar);
        Handler mHandler = new Handler();
        RecordActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(mediaPlayer != null){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekbar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, 1000);
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer != null && fromUser){
                    mediaPlayer.seekTo(progress * 1000);
                }
            }
        });

        adb.setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                stopAudio();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    private void stopAudio() {
        mediaPlayer.stop();
        isPlaying = false;
    }

    private void playAudio(String fileName) {

        mediaPlayer = new MediaPlayer();

        try {
            fileToPlay = new File(pathList.get(recordNames.indexOf(fileName)));
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
            mediaPlayer.prepare();
            fileDuration = mediaPlayer.getDuration();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileToPlay = null;
        isPlaying = true;
    }

    private void uploadFile(String fileName) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading file");
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);


        Uri file = Uri.fromFile(new File(pathList.get(recordNames.indexOf(fileName))));
        StorageReference riversRef = storageRef.child("records/"+file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(RecordActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.setCancelable(true);
                progressDialog.show();
                Toast.makeText(RecordActivity.this, fileName+" successfully uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                int currentProgress = (int) (100*(snapshot.getBytesTransferred()/snapshot.getTotalByteCount()));
                progressDialog.setProgress(currentProgress);
                progressDialog.show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecord(View view) {
        if (isRecording){
            // stop recording
            stopRecording();

            recordBN.setBackgroundColor(Color.GRAY);
            isRecording = false;

            getAllRecordNames();

            adp = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, recordNames);
            lv.setAdapter(adp);
        }
        else{
            // Start recording
            if (checkPermissions()){
                startRecording();

                recordBN.setBackgroundColor(Color.GREEN);
                isRecording = true;
            }
        }
    }

    private void startRecording() {
        Toast.makeText(this, "Recording..", Toast.LENGTH_SHORT).show();
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }
        fileName = currentTime;
        File dataFile = new File(rootPath, fileName+".3gp");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaRecorder.setOutputFile(dataFile);
        }

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaRecorder.start();
    }

    private void stopRecording() {
        Toast.makeText(this, "Stopped..", Toast.LENGTH_SHORT).show();
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), recordPermisson) == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            ActivityCompat.requestPermissions(this,new String[]{recordPermisson}, PERMISSION_CODE);
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
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
                Intent si = new Intent(RecordActivity.this, LoginActivity.class);

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
        else if (id == R.id.map){
            si = new Intent(this, LocationActivity.class);
            startActivity(si);
        }
        else if (id == R.id.calander){
            si = new Intent(this, MainActivity.class);
            startActivity(si);
        }


        return super.onOptionsItemSelected(item);
    }
}