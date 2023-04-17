package com.example.chitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.chitchat.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    //binding---------------------------------------------------------------------------------------*****************************
    private ActivityMainBinding binding;
    //fail olursa tekrar gönderecek
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    //
    private  PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private String mVerificationId;

    private static  final String TAG = "MAIN_TAG";

    private FirebaseAuth firebaseAuth;

    private ProgressDialog pd;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CountryCodePicker ccp;
        EditText editTextCarrierNumber;
        ccp = (CountryCodePicker) findViewById(R.id.ccp);
        editTextCarrierNumber = (EditText) findViewById(R.id.editText_carrierNumber);
        ccp.registerCarrierNumberEditText(editTextCarrierNumber);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        binding.Signin.setVisibility(View.VISIBLE);
        binding.verificationLayout.setVisibility(View.GONE);




        pd = new ProgressDialog(MainActivity.this);
        pd.setTitle("Please wait...");
        pd.setCanceledOnTouchOutside(false);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                        pd.dismiss();
                Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verifcationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verifcationId, forceResendingToken);
                mVerificationId = verifcationId;
                Log.d(TAG,"onCodeSent: "+verifcationId);
                forceResendingToken = token;
                pd.dismiss();
                binding.Signin.setVisibility(View.GONE);
                binding.verificationLayout.setVisibility(View.VISIBLE);


                binding.text4.setText("Please type the verification code we sent \nto"+binding.ccp.getFullNumberWithPlus());


                Toast.makeText(MainActivity.this, "Verification Code Sent !", Toast.LENGTH_SHORT).show();

            }
        };


        binding.SendVerificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = binding.verifytextbar.getText().toString().trim();
                if (TextUtils.isEmpty(code)){
                    Toast.makeText(MainActivity.this, "Please enter verification number...", Toast.LENGTH_SHORT).show();

                }
                else {
                    verifyPhoneNumberWithCode(mVerificationId,code);

                }


            }
        });



        binding.resendCodeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = ccp.getFullNumberWithPlus();
                String number = ccp.getFullNumber();
                if (TextUtils.isEmpty(number)){
                    Toast.makeText(MainActivity.this, "Please enter verification number we sent...", Toast.LENGTH_SHORT).show();
                }
                else {
                    resendVerificationCode(phone, forceResendingToken);
                }


            }
        });




        binding.signinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = ccp.getFullNumberWithPlus();
                String alan = binding.editTextCarrierNumber.getText().toString().trim();
                if (TextUtils.isEmpty(alan)){
                    Toast.makeText(MainActivity.this, "Please enter your number...", Toast.LENGTH_SHORT).show();
                }
                else {
                    startPhoneNumberVerification(phone);
                }


            }
        });










        //animasyon background------------------------------------------------------------**************************************
        ConstraintLayout constraintLayout = findViewById(R.id.mainlayout);

        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);
        animationDrawable.start();
        //------------------------------------**--**************************************************************



        }

    private void verifyPhoneNumberWithCode(String verifcationId, String code) {
        pd.setMessage("Verifying Code");
        pd.show();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verifcationId,code);
        signInWithPhoneAuthCredential(credential);



    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        pd.setMessage("Logged In");
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        pd.dismiss();


                        if(FirebaseAuth.getInstance().getCurrentUser() !=null)
                        {

                                Intent intent=new Intent(MainActivity.this,setProfile.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                        }




                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                            pd.dismiss();
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendVerificationCode(String phone , PhoneAuthProvider.ForceResendingToken token) {

        pd.setMessage("Resending codes...");
        pd.show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(token)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);



    }

    private void startPhoneNumberVerification(String phone) {
        pd.setMessage("Verifying Phone Number.");
        pd.show();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);




    }




    


}