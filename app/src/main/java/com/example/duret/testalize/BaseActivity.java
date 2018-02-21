package com.example.duret.testalize;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class BaseActivity extends AppCompatActivity {

    public static final int RequestPermissionCode = 1;

    protected View addHorizontalRuler(int marginTop) {
        return addHorizontalRuler(marginTop, 2, Color.BLACK);
    }

    protected View addHorizontalRuler(int marginTop, int height) {
        return addHorizontalRuler(marginTop, height, Color.BLACK);
    }

    protected View addHorizontalRuler(int marginTop, int height, int color) {
        View v = new View(this);
        RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        v.setBackgroundColor(color);
        rllp.topMargin = marginTop;
        v.setLayoutParams(rllp);
        return v;
    }

    protected void makeToast(String text) {
        Toast.makeText(BaseActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == RequestPermissionCode) {
            if (grantResults.length > 0) {
                boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;

                if (StoragePermission && RecordPermission) {
                    Toast.makeText(BaseActivity.this, "Permission Granted",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(BaseActivity.this,
                            "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermission() {
        ActivityCompat.requestPermissions(BaseActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }
}
