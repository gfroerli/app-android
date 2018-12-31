package ch.coredump.watertemp;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

public class Utils {

    /**
     * Join a string list using a delimiter.
     * @param delimiter The delimiter.
     * @param elements List of string elements.
     * @return The joined string.
     */
    public static String join(String delimiter,
                              List<String> elements) {
        StringBuilder builder = new StringBuilder();
        final int size = elements.size();
        for (int i = 0; i < size - 1; i++) {
            builder.append(elements.get(i));
            builder.append(delimiter);
        }
        builder.append(elements.get(size - 1));
        return builder.toString();
    }

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
