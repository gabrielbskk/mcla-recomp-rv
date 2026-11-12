package org.mcla.recomp;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MclaLauncherActivity extends Activity {
    private static final String TAG = "MCLA Import";
    private static final int PICK_GAME_FOLDER = 1;
    private static final int PICK_TURNIP_ZIP = 2;
    private static final int PICK_SHADER_PACK = 3;
    private static final long MAX_DRIVER_BYTES = 128L * 1024 * 1024;
    private static final long MAX_SHADER_PACK_BYTES = 64L * 1024 * 1024;
    private static final String SHADER_PACK_NAME = "545407F8.shaderpack";
    private static final String[] REQUIRED = {
        "default.xex", "xarchive_audio.rpf", "xarchive_audlo.rpf",
        "xarchive_cache.rpf", "xarchive_music.rpf",
        "attract576_16x9.bik", "attract576_4x3.bik", "attract720.bik",
        "intro576_16x9.bik", "intro576_4x3.bik", "intro720.bik",
        "nxeart", "root_directory_padding.pad"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button pickButton;
    private Button playButton;
    private Button driverButton;
    private Button systemButton;
    private Button shaderButton;
    private TextView driverStatus;
    private TextView shaderStatus;
    private ProgressBar progress;
    private boolean playPending;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MclaLogExporter.exportAsync(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        status = new TextView(this);
        status.setText(hasGameFiles() ? "Game files ready." :
                "Select the folder containing the 13 extracted files from your original MCLA copy. "
                + "The files must be directly inside that folder. Copying may take several minutes.");
        layout.addView(status);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        layout.addView(progress);

        pickButton = new Button(this);
        pickButton.setText("Select game folder");
        pickButton.setVisibility(hasGameFiles() ? View.GONE : View.VISIBLE);
        pickButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_GAME_FOLDER);
        });
        layout.addView(pickButton);

        driverStatus = new TextView(this);
        layout.addView(driverStatus);
        updateDriverStatus();

        driverButton = new Button(this);
        driverButton.setText("Select Turnip (.zip)");
        driverButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_TURNIP_ZIP);
        });
        layout.addView(driverButton);

        systemButton = new Button(this);
        systemButton.setText("Use system driver");
        systemButton.setOnClickListener(view -> {
            File selection = new File(driverFolder(), "selected.txt");
            if (selection.exists() && !selection.delete()) {
                status.setText("Could not switch back to the system driver.");
                return;
            }
            updateDriverStatus();
            status.setText("System driver selected.");
        });
        layout.addView(systemButton);

        shaderStatus = new TextView(this);
        layout.addView(shaderStatus);
        updateShaderPackStatus();

        shaderButton = new Button(this);
        shaderButton.setText("Select shader pack (.shaderpack)");
        shaderButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_SHADER_PACK);
        });
        layout.addView(shaderButton);

        Button touchEditor = new Button(this);
        touchEditor.setText("Customize touch controls");
        touchEditor.setOnClickListener(view ->
                startActivity(new Intent(this, MclaTouchEditorActivity.class)));
        layout.addView(touchEditor);

        playButton = new Button(this);
        playButton.setText("Play");
        playButton.setEnabled(hasGameFiles());
        playButton.setOnClickListener(view -> startGame());
        layout.addView(playButton);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private File driverFolder() {
        return new File(getFilesDir(), "gpu-drivers");
    }

    private void updateDriverStatus() {
        boolean custom = new File(driverFolder(), "selected.txt").isFile();
        driverStatus.setText(custom ? "Selected driver: Turnip (experimental)" :
                "Selected driver: system");
        if (systemButton != null) systemButton.setEnabled(custom);
    }

    private File shaderPackFolder() {
        return new File(getFilesDir(), "shaderpack");
    }

    private File shaderPackFile() {
        return new File(shaderPackFolder(), SHADER_PACK_NAME);
    }

    private void updateShaderPackStatus() {
        shaderStatus.setText(shaderPackFile().isFile()
                ? "Shader pack: installed (optional)"
                : "Shader pack: not installed (optional)");
    }

    private boolean hasGameFiles() {
        File folder = new File(getFilesDir(), "game");
        for (String name : REQUIRED) {
            File file = new File(folder, name);
            if (!file.isFile() || file.length() == 0) return false;
        }
        return true;
    }

    private void startGame() {
        if (playPending) return;
        playPending = true;
        playButton.setEnabled(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            openGame();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (playPending && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            openGame();
        }
    }

    private void openGame() {
        playPending = false;
        startActivity(new Intent(this, MclaActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == PICK_TURNIP_ZIP) {
            Uri uri = data.getData();
            if (uri == null) return;
            driverButton.setEnabled(false);
            playButton.setEnabled(false);
            status.setText("Importing Turnip driver...");
            new Thread(() -> importDriver(uri), "MCLA Turnip import").start();
            return;
        }
        if (requestCode == PICK_SHADER_PACK) {
            Uri uri = data.getData();
            if (uri == null) return;
            shaderButton.setEnabled(false);
            status.setText("Importing shader pack...");
            new Thread(() -> importShaderPack(uri), "MCLA shader pack import").start();
            return;
        }
        if (requestCode != PICK_GAME_FOLDER) return;
        Uri tree = data.getData();
        if (tree == null) return;
        if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            Log.w(TAG, "The folder picker did not report a read grant; trying the URI anyway");
        }
        try {
            getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Some document providers grant access only until this import finishes.
            Log.w(TAG, "Could not persist folder access; trying the current grant", e);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        pickButton.setEnabled(false);
        progress.setIndeterminate(false);
        progress.setMax(REQUIRED.length);
        progress.setProgress(0);
        progress.setVisibility(View.VISIBLE);
        status.setText("Checking game files...");
        new Thread(() -> importGame(tree), "MCLA game import").start();
    }

    private Map<String, Uri> children(Uri tree, String parentId) throws Exception {
        Uri list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        Map<String, Uri> found = new HashMap<>();
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE};
        try (Cursor cursor = getContentResolver().query(list, projection, null, null, null)) {
            if (cursor == null) throw new Exception("Could not read the selected folder.");
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String type = cursor.getString(2);
                if (name == null || name.isEmpty() || name.equals(".") || name.equals("..")
                        || name.contains("/") || name.contains("\\")) continue;
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(type)) continue;
                found.put(name, DocumentsContract.buildDocumentUriUsingTree(tree, id));
            }
        }
        return found;
    }

    private void importGame(Uri tree) {
        File folder = new File(getFilesDir(), "game");
        try {
            String rootId = DocumentsContract.getTreeDocumentId(tree);
            Map<String, Uri> files = children(tree, rootId);
            for (String name : REQUIRED) {
                if (!files.containsKey(name)) throw new Exception("Missing file: " + name);
            }
            if (!folder.isDirectory() && !folder.mkdirs())
                throw new Exception("Could not create the private game folder.");

            byte[] buffer = new byte[1024 * 1024];
            for (int index = 0; index < REQUIRED.length; index++) {
                String name = REQUIRED[index];
                final int fileNumber = index + 1;
                mainHandler.post(() -> status.setText("Copying " + name + " ("
                        + fileNumber + "/" + REQUIRED.length + ")..."));
                File target = new File(folder, name);
                File part = new File(folder, name + ".partial");
                try {
                    try (InputStream input = getContentResolver().openInputStream(files.get(name));
                         FileOutputStream output = new FileOutputStream(part)) {
                        if (input == null) throw new Exception("Could not open " + name);
                        int count;
                        long copied = 0;
                        long nextReport = 64L * 1024 * 1024;
                        while ((count = input.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                            copied += count;
                            if (copied >= nextReport) {
                                final long copiedMiB = copied / (1024 * 1024);
                                mainHandler.post(() -> status.setText("Copying " + name + " ("
                                        + fileNumber + "/" + REQUIRED.length + ", "
                                        + copiedMiB + " MiB)..."));
                                nextReport += 64L * 1024 * 1024;
                            }
                        }
                        output.getFD().sync();
                    }
                    if (!part.renameTo(target))
                        throw new Exception("Could not save " + name);
                } catch (Exception e) {
                    part.delete();
                    throw e;
                }
                mainHandler.post(() -> progress.setProgress(fileNumber));
            }
            if (!hasGameFiles()) throw new Exception("The copy finished with incomplete files.");
            mainHandler.post(() -> {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                pickButton.setVisibility(View.GONE);
                playButton.setEnabled(true);
                status.setText("Game files ready. Tap Play.");
            });
        } catch (SecurityException e) {
            Log.w(TAG, "No access to the selected folder", e);
            showImportFailure("No permission to read that folder. Select it again "
                    + "and grant access in the Android picker.");
        } catch (Exception e) {
            showImportFailure("Import failed: " + e.getMessage());
        }
    }

    private void importDriver(Uri uri) {
        File folder = driverFolder();
        File part = new File(folder, "libvulkan_freedreno.so.partial");
        File target = new File(folder, "libvulkan_freedreno.so");
        try {
            if (!folder.isDirectory() && !folder.mkdirs())
                throw new Exception("Could not create the driver folder.");
            boolean found = false;
            try (InputStream raw = getContentResolver().openInputStream(uri)) {
                if (raw == null) throw new Exception("Could not open the ZIP.");
                try (ZipInputStream zip = new ZipInputStream(raw)) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (entry.isDirectory() || name == null ||
                                !(name.equals("libvulkan_freedreno.so") ||
                                  name.endsWith("/libvulkan_freedreno.so"))) continue;
                        if (found) throw new Exception("The ZIP contains more than one driver.");
                        found = true;
                        long copied = 0;
                        byte[] buffer = new byte[64 * 1024];
                        try (FileOutputStream output = new FileOutputStream(part)) {
                            int count;
                            while ((count = zip.read(buffer)) != -1) {
                                copied += count;
                                if (copied > MAX_DRIVER_BYTES)
                                    throw new Exception("Driver exceeds 128 MiB.");
                                output.write(buffer, 0, count);
                            }
                            output.getFD().sync();
                        }
                    }
                }
            }
            if (!found) throw new Exception("The ZIP does not contain libvulkan_freedreno.so.");
            validateArm64Library(part);
            if (!part.renameTo(target)) throw new Exception("Could not save the driver.");
            File selection = new File(folder, "selected.txt");
            try (FileOutputStream output = new FileOutputStream(selection)) {
                output.write("libvulkan_freedreno.so\n".getBytes("UTF-8"));
                output.getFD().sync();
            }
            mainHandler.post(() -> {
                updateDriverStatus();
                status.setText("Turnip imported. Tap Play to test it.");
            });
        } catch (Exception e) {
            Log.e(TAG, "Turnip import failed", e);
            mainHandler.post(() -> status.setText("Turnip import failed: " + e.getMessage()));
        } finally {
            part.delete();
            mainHandler.post(() -> {
                driverButton.setEnabled(true);
                playButton.setEnabled(hasGameFiles());
            });
        }
    }

    private void importShaderPack(Uri uri) {
        File folder = shaderPackFolder();
        File part = new File(folder, SHADER_PACK_NAME + ".partial");
        File target = shaderPackFile();
        try {
            if (!folder.isDirectory() && !folder.mkdirs())
                throw new Exception("Could not create the shader pack folder.");
            byte[] header = new byte[16];
            int headerRead = 0;
            long copied = 0;
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(part)) {
                if (input == null) throw new Exception("Could not open the shader pack.");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (headerRead < header.length) {
                        int take = Math.min(count, header.length - headerRead);
                        System.arraycopy(buffer, 0, header, headerRead, take);
                        headerRead += take;
                    }
                    copied += count;
                    if (copied > MAX_SHADER_PACK_BYTES)
                        throw new Exception("Shader pack exceeds 64 MiB.");
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
            }
            if (headerRead < header.length || header[0] != 'M' || header[1] != 'C' ||
                    header[2] != 'L' || header[3] != 'A' || header[4] != 'S' ||
                    header[5] != 'P' || header[6] != 'K' || header[7] != '1')
                throw new Exception("Not an MCLA shader pack.");
            int title = (header[12] & 0xff) | ((header[13] & 0xff) << 8)
                    | ((header[14] & 0xff) << 16) | ((header[15] & 0xff) << 24);
            if (title != 0x545407F8)
                throw new Exception("The shader pack is for another game.");
            if (!part.renameTo(target))
                throw new Exception("Could not save the shader pack.");
            mainHandler.post(() -> {
                updateShaderPackStatus();
                status.setText("Shader pack imported. It will be added on the next start.");
            });
        } catch (Exception e) {
            Log.e(TAG, "Shader pack import failed", e);
            mainHandler.post(() -> status.setText("Shader pack import failed: " + e.getMessage()));
        } finally {
            part.delete();
            mainHandler.post(() -> shaderButton.setEnabled(true));
        }
    }

    private static void validateArm64Library(File file) throws Exception {
        byte[] header = new byte[20];
        try (FileInputStream input = new FileInputStream(file)) {
            if (input.read(header) != header.length || header[0] != 0x7f ||
                    header[1] != 'E' || header[2] != 'L' || header[3] != 'F' ||
                    header[4] != 2 || header[5] != 1 ||
                    (header[18] & 0xff) != 183 || header[19] != 0)
                throw new Exception("The driver is not a valid ARM64 ELF library.");
        }
    }

    private void showImportFailure(String message) {
        mainHandler.post(() -> {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progress.setVisibility(View.GONE);
            pickButton.setEnabled(true);
            status.setText(message);
        });
    }
}
