package ch.coredump.watertemp;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class Utils {
    /**
     * Show an error message in a simple dialog.
     * @param activity The activity context.
     * @param message The message to show.
     */
    public static void showError(Activity activity, String message) {
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

}
