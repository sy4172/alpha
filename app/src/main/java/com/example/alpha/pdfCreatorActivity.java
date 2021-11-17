package com.example.alpha;

import static com.example.alpha.FBref.storageRef;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class pdfCreatorActivity extends AppCompatActivity implements View.OnCreateContextMenuListener {

    ListView lv;
    Bitmap bmp, scaledBmp;
    ArrayList<String> pdfNames;
    ArrayList<String> pathList;

    String textToDisplay, fileName;
    File rootPath;
    ArrayAdapter<String> adp;

    FirebaseStorage storage;
    FirebaseDatabase database;

    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_creator);

        lv = findViewById(R.id.lv);

        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setOnCreateContextMenuListener(this);

        textToDisplay = fileName = "";

        bmp = BitmapFactory.decodeResource(getResources(),R.drawable.logo);
        scaledBmp = Bitmap.createScaledBitmap(bmp, 1097, 624, false);
        rootPath = new File(this.getExternalFilesDir("/"), "myPDFS");

        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }

        getAllPDFNames();
        adp = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,pdfNames);
        lv.setAdapter(adp);
    }

    private void getAllPDFNames() {
        pathList = new ArrayList<>();
        pdfNames = new ArrayList<>();

        File[] files = new File(rootPath.getPath()).listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isFile()) {
                pathList.add(file.getPath());
                pdfNames.add(file.getName());
            }
        }
    }

    public void getContent(View view) {
        openAlertDialog(textToDisplay);
    }

    private void openAlertDialog(String presevedText) {
        LinearLayout AdScreen = new LinearLayout(this);
        AdScreen.setOrientation(LinearLayout.VERTICAL);

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        final EditText nameET = new EditText(this);
        nameET.setHint("SAVE AS");
        final EditText contentET = new EditText(this);
        contentET.setHint("TYPE HERE");
        AdScreen.addView(nameET);
        AdScreen.addView(contentET);
        adb.setView(AdScreen);
        adb.setTitle("Enter a text to be displayed");
        adb.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                textToDisplay = "";
                createPDFfile();
            }
        });

        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!presevedText.isEmpty()){
                    textToDisplay = presevedText;
                    contentET.setText(presevedText);
                }
                else{
                    textToDisplay = contentET.getText().toString();
                }
                fileName = nameET.getText().toString();
                createPDFfile();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    private void createPDFfile() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(2480,3508,1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(scaledBmp,0,0,paint);

        Paint titlePaint = new Paint();
        if (!textToDisplay.isEmpty()) {
            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
            titlePaint.setTextSize(500);
            titlePaint.setColor(Color.BLACK);
            canvas.drawText(textToDisplay,1240,1754 , titlePaint);
        }
        else page.getCanvas().drawText(" ",150, 1240, titlePaint);

        pdfDocument.finishPage(page);
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }

        File dataFile;
        if (!fileName.isEmpty()){
            dataFile = new File(rootPath, fileName+".pdf");
        } else{
            Toast.makeText(this, "file name cannot be empty", Toast.LENGTH_SHORT).show();
            openAlertDialog(textToDisplay);
            dataFile = new File(rootPath, fileName+".pdf");
        }

        try {
            pdfDocument.writeTo(new FileOutputStream(dataFile));
            Toast.makeText(this, "saved in "+dataFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            getAllPDFNames();
            adp = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,pdfNames);
            lv.setAdapter(adp);

        } catch (IOException e){
            e.printStackTrace();
        }

        pdfDocument.close();
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
            uploadFile(pdfNames.get(adpInfo.position));
        }
        else if (option.equals("Open")){
            openFile(pdfNames.get(adpInfo.position));
        }

        return super.onContextItemSelected(item);
    }

    private void openFile(String fileName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File dataFile = new File(pathList.get(pdfNames.indexOf(fileName)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri contentUri = FileProvider.getUriForFile(this, "com.example.alpha.provider",dataFile);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/pdf");

        }else{
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(dataFile), "application/vnd.android.package-archive");

        }


        startActivity(intent);
    }


    private void uploadFile(String fileName) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading file");
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);


        Uri file = Uri.fromFile(new File(pathList.get(pdfNames.indexOf(fileName))));
        StorageReference riversRef = storageRef.child("files/"+file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(pdfCreatorActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.setCancelable(true);
                progressDialog.show();
                Toast.makeText(pdfCreatorActivity.this, fileName+" successfully uploaded", Toast.LENGTH_SHORT).show();
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
                Intent si = new Intent(pdfCreatorActivity.this, LoginActivity.class);

                SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("email", "");
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
        else if (id == R.id.calander){
            si = new Intent(this, MainActivity.class);
            startActivity(si);
        }
        else if (id == R.id.record){
            si = new Intent(this, RecordActivity.class);
            startActivity(si);
        }
//        else if (id == R.id.map){
//            si = new Intent(this, )
//        }


        return super.onOptionsItemSelected(item);
    }
}