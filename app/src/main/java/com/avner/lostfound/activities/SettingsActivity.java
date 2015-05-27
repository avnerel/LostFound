package com.avner.lostfound.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.avner.lostfound.Constants;
import com.avner.lostfound.LostFoundApplication;
import com.avner.lostfound.R;
import com.avner.lostfound.utils.ImageUtils;
import com.facebook.Profile;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener{

    private static final String INFINITY = "\u221E";
    Spinner messageHistory;

    ImageButton userPhotoImageButton;
    private EditText userDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        messageHistory = (Spinner)findViewById(R.id.sp_history_length);
        String[] items = new String[]{"30", "50", "100", INFINITY};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.message_history_spinner_item, items);
        messageHistory.setAdapter(adapter);
        messageHistory.setOnItemSelectedListener(this);

        userDisplayName = (EditText) findViewById(R.id.et_userName);
        LostFoundApplication app = (LostFoundApplication) getApplication();
        userDisplayName.setText(app.getUserDisplayName());

        Button b_updateUserName = (Button) findViewById(R.id.b_updateUserName);
        b_updateUserName.setOnClickListener(this);

        userPhotoImageButton = (ImageButton) findViewById(R.id.ib_user_image);

        Button b_logout = (Button) findViewById(R.id.b_log_out);
        b_logout.setOnClickListener(this);

        Button b_changePassword = (Button) findViewById(R.id.b_change_password);
        // can't change password if login was through facebook.
        if(Profile.getCurrentProfile() != null){
            b_changePassword.setVisibility(Button.INVISIBLE);
        }
        b_changePassword.setOnClickListener(this);

        Button b_deleteUser = (Button) findViewById(R.id.b_delete_user);
        b_deleteUser.setOnClickListener(this);

        File file = new File(Constants.USER_IMAGE_FILE_PATH);
        if(file.exists()){
            userPhotoImageButton.setImageBitmap(BitmapFactory.decodeFile(Constants.USER_IMAGE_FILE_PATH));
        }
        userPhotoImageButton.setOnClickListener(this);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == Constants.REQUEST_CODE_CAMERA) {
            Bitmap image_from_camera = (Bitmap) data.getExtras().get("data");
            ImageUtils.saveImageToFile(image_from_camera, Constants.USER_IMAGE_FILE_NAME);
            userPhotoImageButton.setImageBitmap(image_from_camera);

        } else if (requestCode == Constants.REQUEST_CODE_SELECT_FILE) {

            try{
                Bitmap imageFromGallery = ImageUtils.decodeUri(getContentResolver(),data.getData());
                ImageUtils.saveImageToFile(imageFromGallery, Constants.USER_IMAGE_FILE_NAME);
                userPhotoImageButton.setImageBitmap(imageFromGallery);

            } catch (FileNotFoundException e) {
                Log.e(Constants.LOST_FOUND_TAG, "user image file from gallery not found. WTF???");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.ib_user_image:
                ImageUtils.selectItemImage(this);
                break;
            case R.id.b_log_out:
                // return to main activity.
                setResult(Constants.RESULT_CODE_LOGOUT, null);
                finish();
                break;
            case R.id.b_change_password:
                getNewPassword();
                break;
            case R.id.b_delete_user:
                deleteUser();
                break;
            case R.id.b_updateUserName:
                updateUserName();
                break;
        }
    }

    private void updateUserName() {
        String name = userDisplayName.getText().toString();
        ParseUser currentUser = ParseUser.getCurrentUser();
        currentUser.put(Constants.ParseUser.USER_DISPLAY_NAME, name);
        currentUser.saveInBackground();
        Toast.makeText(this,"Display name has been updated.", Toast.LENGTH_SHORT).show();
    }

    private void deleteUser() {
    }

    private void getNewPassword() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View layout = inflater.inflate(R.layout.dialog_change_pass, (ViewGroup) findViewById(R.id.root));
        final EditText password1 = (EditText) layout.findViewById(R.id.EditText_Pwd1);
        final EditText password2 = (EditText) layout.findViewById(R.id.EditText_Pwd2);
        final TextView error = (TextView) layout.findViewById(R.id.TextView_PwdProblem);

        password2.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String strPass1 = password1.getText().toString();
                String strPass2 = password2.getText().toString();
                if(strPass1.length() < Constants.MIN_PASSWORD_LENGTH || strPass1.length() > Constants.MAX_PASSWORD_LENGTH){

                    error.setText(R.string.password_too_short);
                    error.setTextColor(Color.RED);

                }
                else if (strPass1.equals(strPass2)) {
                    error.setText(R.string.passwords_match);
                    error.setTextColor(Color.BLACK);

                } else {
                    error.setText(R.string.passwords_not_equal);
                    error.setTextColor(Color.RED);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");
        builder.setView(layout);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        final Activity thisActivity = this;
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String strPassword1 = password1.getText().toString();
                String strPassword2 = password2.getText().toString();
                if (strPassword1.equals(strPassword2) && strPassword1.length()> Constants.MIN_PASSWORD_LENGTH
                                                      && strPassword1.length()< Constants.MAX_PASSWORD_LENGTH) {
                    dialog.dismiss();
                    ParseUser.getCurrentUser().setPassword(strPassword1);
                    ParseUser.getCurrentUser().saveInBackground();
                    showPasswordsUpdatedDialog();
                }else{
                    Toast.makeText(thisActivity,
                            "Invalid password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        AlertDialog passwordDialog = builder.create();
        passwordDialog.show();


    }

    private void showPasswordsUpdatedDialog() {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.password_changed)
                .setMessage(R.string.password_has_been_changed)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

}
