package org.client.scrcpy;

import android.app.Activity;
import android.os.Bundle;

import org.client.scrcpy.utils.Util;


public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Util.applyStatusBarInset(findViewById(R.id.app_header), 80,
                findViewById(R.id.main_scroll_view));


    }
}