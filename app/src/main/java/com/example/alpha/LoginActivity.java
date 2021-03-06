package com.example.alpha;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.TextUtils;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    EditText emailET, passwordET;
    CheckBox checkBox;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private static final String TAG = "EmailPassword";

    boolean needToSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        checkBox = findViewById(R.id.checkBox);

        mAuth = FirebaseAuth.getInstance();

        FirebaseAuth.AuthStateListener mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                currentUser = user;
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        boolean toSkip = settings.getBoolean("stayConnect",false);

        currentUser = mAuth.getCurrentUser();

        if (toSkip && (currentUser != null)){
            Variable.setEmailVer(emailET.getText().toString());
            Intent si = new Intent(this, MainActivity.class);
            startActivity(si);
            finish();
        }
    }

    public void login(View view) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        if (TextUtils.isEmpty(email)){
            emailET.setError("The email field can not be empty");
            emailET.requestFocus();
        }
        else if (TextUtils.isEmpty(password)){
            passwordET.setError("The password field can not be empty");
            passwordET.requestFocus();
        }
        else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);

                                Intent si = new Intent(LoginActivity.this, MainActivity.class);
                                Variable.setEmailVer(email);
                                SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("email", Variable.getEmailVer());
                                editor.apply();
                                startActivity(si);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "There is no user found",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        }
    }

    public void createUser(View view) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        if (TextUtils.isEmpty(email)){
            emailET.setError("The email field can not be empty");
            emailET.requestFocus();
        }
        else if (TextUtils.isEmpty(password)){
            passwordET.setError("The password filed can not be empty");
            passwordET.requestFocus();
        }
        else{
            User tempUser = new User(passwordET.getText().toString(),emailET.getText().toString());
            mAuth.createUserWithEmailAndPassword(tempUser.getEmail(), tempUser.getPassword() )
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);

                                Intent si = new Intent(LoginActivity.this, MainActivity.class);
                                Variable.setEmailVer(tempUser.getEmail());
                                SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("email", Variable.getEmailVer());
                                editor.apply();
                                startActivity(si);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "The user was created already",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        }

    }


    private void updateUI(FirebaseUser user) {
        currentUser = user;
    }

    @SuppressLint("ApplySharedPref")
    public void changeConnection(View view) {
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("stayConnect",checkBox.isChecked());
        editor.commit();
    }
}