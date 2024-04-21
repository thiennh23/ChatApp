package com.example.chatappnt109;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.chatappnt109.model.UserModel;
import com.example.chatappnt109.utils.AndroidUtil;
import com.example.chatappnt109.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.Timestamp;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class LoginUsernameActivity extends AppCompatActivity {
    EditText usernameInput;
    Button doneBtn;
    ProgressBar progressBar;
    String phoneNumber;
    UserModel userModel;
    ImageView profilePic;

    ActivityResultLauncher<Intent> imagePickerLauncher;
    Uri selectedImageUri;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_username);

        usernameInput = findViewById(R.id.login_username);
        doneBtn = findViewById(R.id.login_done);
        progressBar = findViewById(R.id.login_progress_bar);
        profilePic = findViewById(R.id.profile_image_view);

        phoneNumber = getIntent().getExtras().getString("phone");
        getUserData();

        doneBtn.setOnClickListener((v) -> updateBtnClick());

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if (data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            AndroidUtil.setProfilePic(this, selectedImageUri, profilePic);
                        }
                    }
                }
        );

        profilePic.setOnClickListener(v -> {
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickerLauncher.launch(intent);
                            return null;
                        }
                    });
        });
    }

    void updateBtnClick(){
        String username = usernameInput.getText().toString();
        if (username.isEmpty() || username.length() < 3){
            usernameInput.setError("Username length should be atleast 3 characters");
            return;
        }
        setInProgress(true);

        if (userModel != null){
            userModel.setUsername(username);
        } else {
            userModel = new UserModel(phoneNumber, username, username.toLowerCase(), Timestamp.now(), FirebaseUtil.currentUserId());
        }
        updateToFirestore();
    }

    void updateToFirestore(){
        FirebaseUtil.currentUserDetails().set(userModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        if (selectedImageUri != null) {
                            FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                                    .addOnCompleteListener(t -> {
                                        //
                                    });
                        }
                        Intent intent = new Intent(LoginUsernameActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
    }

    private void getUserData() {
        setInProgress(true);
        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        Uri uri = task.getResult();
                        AndroidUtil.setProfilePic(this, uri, profilePic);
                    }
                });

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            userModel = task.getResult().toObject(UserModel.class);
            if (userModel != null)
                usernameInput.setText(userModel.getUsername());
        });
    }

    void setInProgress(boolean inProgress){
        if (inProgress){
            progressBar.setVisibility(View.VISIBLE);
            doneBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            doneBtn.setVisibility(View.VISIBLE);
        }
    }
}