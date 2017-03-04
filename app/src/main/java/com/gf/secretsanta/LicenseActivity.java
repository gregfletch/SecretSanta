package com.gf.secretsanta;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if(toolbar != null) {
            toolbar.setTitle(R.string.licenses_title);
        }

        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        enableHyperlinks();
    }

    private void enableHyperlinks() {
        TextView icons = (TextView) findViewById(R.id.icons);
        if(icons != null) {
            icons.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView appcompat = (TextView) findViewById(R.id.appcompat);
        if (appcompat != null) {
            appcompat.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView cardview = (TextView) findViewById(R.id.cardview);
        if (cardview != null) {
            cardview.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView design = (TextView) findViewById(R.id.design);
        if (design != null) {
            design.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView recyclerview = (TextView) findViewById(R.id.recyclerview);
        if(recyclerview != null) {
            recyclerview.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

}
