package org.mcla.recomp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

final class MclaLogExporter {
    private static final String TAG = "MCLA Logs";
    private static final String RELATIVE_PATH = Environment.DIRECTORY_DOCUMENTS + "/MCLA/logs/";
    private static final Object LOCK = new Object();

    private MclaLogExporter() {}

    static void exportAsync(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> export(app), "MCLA log export").start();
    }

    private static void export(Context context) {
        if (Build.VERSION.SDK_INT < 29) {
            Log.w(TAG, "Automatic Documents export requires Android 10 or newer");
            return;
        }
        synchronized (LOCK) {
            File source = new File(context.getFilesDir(), "logs");
            File[] logs = source.listFiles((dir, name) -> name.startsWith("mcla_") &&
                    name.endsWith(".log"));
            if (logs == null) return;
            Arrays.sort(logs, (left, right) -> left.getName().compareTo(right.getName()));
            SharedPreferences state = context.getSharedPreferences("exported-logs", Context.MODE_PRIVATE);
            for (File log : logs) {
                if (!log.isFile()) continue;
                String name = log.getName();
                String stamp = log.length() + ":" + log.lastModified();
                if (stamp.equals(state.getString(name + ".stamp", "")) &&
                        "txt".equals(state.getString(name + ".format", ""))) continue;
                try {
                    exportOne(context.getContentResolver(), state, log);
                    state.edit().putString(name + ".stamp", stamp)
                            .putString(name + ".format", "txt").apply();
                } catch (Exception e) {
                    Log.e(TAG, "Could not export " + name + " to " + RELATIVE_PATH, e);
                }
            }
        }
    }

    private static void exportOne(ContentResolver resolver, SharedPreferences state, File log)
            throws Exception {
        String name = log.getName();
        String exportName = name.substring(0, name.length() - 4) + ".txt";
        String saved = state.getString(name + ".uri", null);
        Uri target = saved == null ? null : Uri.parse(saved);
        boolean fresh = target == null;
        if (fresh) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, exportName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            target = resolver.insert(MediaStore.Files.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
            if (target == null) throw new Exception("MediaStore refused " + RELATIVE_PATH + name);
        } else {
            ContentValues rename = new ContentValues();
            rename.put(MediaStore.MediaColumns.DISPLAY_NAME, exportName);
            if (resolver.update(target, rename, null, null) != 1) {
                state.edit().remove(name + ".uri").apply();
                throw new Exception("Could not rename exported log " + name);
            }
        }
        try (InputStream input = new FileInputStream(log);
             OutputStream output = resolver.openOutputStream(target, "wt")) {
            if (output == null) throw new Exception("Cannot open Documents output");
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } catch (Exception e) {
            if (fresh) resolver.delete(target, null, null);
            else state.edit().remove(name + ".uri").apply();
            throw e;
        }
        if (fresh) {
            ContentValues published = new ContentValues();
            published.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(target, published, null, null) != 1)
                throw new Exception("Could not publish exported log " + name);
            state.edit().putString(name + ".uri", target.toString()).apply();
        }
        Log.i(TAG, "Exported " + name + " to " + RELATIVE_PATH);
    }
}
