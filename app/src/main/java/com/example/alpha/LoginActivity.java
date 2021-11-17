package com.example.alpha;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    CheckBox checkBN;

    FirebaseAuth mAuth;
    FirebaseUser user;

    boolean needToSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        checkBN = findViewById(R.id.checkBN);

        mAuth = FirebaseAuth.getInstance();

        // check the current user that was signed up lately
        FirebaseAuth.AuthStateListener mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                needToSignUp = user == null;
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        boolean toSkip = settings.getBoolean("stayConnect",false);

        if (toSkip && (user != null)){
            Variable.setEmailVer(emailET.getText().toString());
            Intent si = new Intent(this, MainActivity.class);
            startActivity(si);
            finish();
        }
    }

    public void login(View view) {
        if (needToSignUp) signUp();
        else signIn();
    }
    private void signUp() {
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
            mAuth.createUserWithEmailAndPassword(tempUser.getEmail(), tempUser.getPassword()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(LoginActivity.this, "Sign up is done successfully", Toast.LENGTH_SHORT).show();
                        Intent si = new Intent(LoginActivity.this, MainActivity.class);
                        Variable.setEmailVer(tempUser.getEmail());
                        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("email", Variable.getEmailVer());
                        editor.apply();
                        startActivity(si);
                    }
                    else {
                        Toast.makeText(LoginActivity.this, "Sign Up error: "+ Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    private void signIn() {
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
        else{
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(LoginActivity.this, "Sign-In is done successfully", Toast.LENGTH_SHORT).show();
                        Intent si = new Intent(LoginActivity.this, MainActivity.class);
                        Variable.setEmailVer(email);
                        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("email", Variable.getEmailVer());
                        editor.apply();
                        startActivity(si);
                    }
                    else{
                        Toast.makeText(LoginActivity.this, "Sign In error: "+ Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }


    public void changeConnection(View view) {
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("stayConnect",checkBN.isChecked());
        editor.apply();
    }

}