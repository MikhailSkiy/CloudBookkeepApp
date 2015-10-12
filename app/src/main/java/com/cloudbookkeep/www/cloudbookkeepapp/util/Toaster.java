package com.cloudbookkeep.www.cloudbookkeepapp.util;

/**
 * Created by Mikhail Valuyskiy on 08.10.2015.
 */

import android.content.Context;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.Toast;

public class Toaster {
    private static final int SHORT_TOAST_DURATION = 2000;

    private Toaster() {}

    public static void makeLongToast(Context context,String text, long durationInMillis) {
        final Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER| Gravity.CENTER_HORIZONTAL, 0, 0);

        new CountDownTimer(Math.max(durationInMillis - SHORT_TOAST_DURATION, 1000), 1000) {
            @Override
            public void onFinish() {
                t.show();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                t.show();
            }
        }.start();
    }
}

