package com.example.duret.testalize;

import android.os.Bundle;

public class Verification extends BaseActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.verification);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
