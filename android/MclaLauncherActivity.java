package org.mcla.recomp;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
    private static final int PICK_DRIVER_ZIP = 2;
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
    private Button audioButton;
    private Spinner turnipDebugSpinner;
    private TextView driverStatus;
    private boolean isMaliBuild;
    private TextView shaderStatus;
    private ProgressBar progress;
    private boolean playPending;

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable surface(int color, int stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(20));
        background.setStroke(dp(1), stroke);
        return background;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(size);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private LinearLayout card(LinearLayout parent, String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(18));
        card.setBackground(surface(0xFF172532, 0xFF2C4352));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(14);
        parent.addView(card, params);
        card.addView(label(title, 19, Color.WHITE, true));
        TextView caption = label(subtitle, 13, 0xFFA8B9C4, false);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(4);
        captionParams.bottomMargin = dp(9);
        card.addView(caption, captionParams);
        return card;
    }

    private void addAction(LinearLayout card, Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? 0xFF06191A : Color.WHITE);
        button.setBackground(surface(primary ? 0xFF70DFCA : 0xFF263A48,
                primary ? 0xFF70DFCA : 0xFF39515F));
        button.setMinHeight(dp(50));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(9);
        card.addView(button, params);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        isMaliBuild = MclaBuildVariant.isMali(this);
        MclaLogExporter.exportAsync(this);
        getWindow().setNavigationBarColor(0xFF0B161F);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(24), dp(18), dp(26));
        layout.setBackgroundColor(0xFF0B161F);

        TextView eyebrow = label("MCLA  /  ANDROID", 12, 0xFF70DFCA, true);
        layout.addView(eyebrow);
        TextView heading = label("Ready to race?", 32, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(6);
        layout.addView(heading, headingParams);
        TextView intro = label("Prepare your game, choose a graphics driver, and set up your controls.",
                14, 0xFFA8B9C4, false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.bottomMargin = dp(20);
        layout.addView(intro, introParams);

        LinearLayout gameCard = card(layout, "01  Game files", "Import the 13 extracted files from your own copy.");

        status = new TextView(this);
        status.setText(hasGameFiles() ? "Game files ready." :
                "Select the folder containing the 13 extracted files from your original MCLA copy. "
                + "The files must be directly inside that folder. Copying may take several minutes.");
        status.setTextColor(0xFFD9E7EB);
        status.setTextSize(14);
        gameCard.addView(status);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        gameCard.addView(progress);

        pickButton = new Button(this);
        pickButton.setText("Select game folder");
        pickButton.setVisibility(hasGameFiles() ? View.GONE : View.VISIBLE);
        pickButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_GAME_FOLDER);
        });
        addAction(gameCard, pickButton, false);

        LinearLayout graphicsCard = card(layout, "02  Graphics", "Select a Vulkan driver for this device.");

        driverStatus = new TextView(this);
        driverStatus.setTextColor(0xFF70DFCA);
        graphicsCard.addView(driverStatus);
        updateDriverStatus();

        driverButton = new Button(this);
        driverButton.setText(isMaliBuild ? "Select PanVK driver (.zip)" : "Select Turnip driver (.zip)");
        driverButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_DRIVER_ZIP);
        });
        addAction(graphicsCard, driverButton, false);

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
        addAction(graphicsCard, systemButton, false);

        if (!isMaliBuild) {
            graphicsCard.addView(label("Turnip debug (applies on next Play; one at a time)",
                    13, 0xFFA8B9C4, false));
            final String[] options = {"syncdraw (recommended)", "No switch", "noubwc",
                    "flushall", "nolrz", "sysmem"};
            turnipDebugSpinner = new Spinner(this);
            turnipDebugSpinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, options));
            String current = getSharedPreferences("driver_debug", MODE_PRIVATE)
                    .getString("tu_debug", "syncdraw");
            int selected = 0;
            for (int i = 2; i < options.length; i++) {
                if (options[i].equals(current)) selected = i;
            }
            if (current.isEmpty()) selected = 1;
            turnipDebugSpinner.setSelection(selected);
            turnipDebugSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent,
                        View view, int position, long id) {
                    String value = position == 0 ? "syncdraw" :
                            position == 1 ? "" : options[position];
                    getSharedPreferences("driver_debug", MODE_PRIVATE).edit()
                            .putString("tu_debug", value).apply();
                    updateDriverStatus();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(-1, -2);
            spinnerParams.topMargin = dp(9);
            graphicsCard.addView(turnipDebugSpinner, spinnerParams);
            updateDriverStatus();
        }

        LinearLayout extrasCard = card(layout, "03  Extras", "Optional graphics assets and touch layout.");

        shaderStatus = new TextView(this);
        shaderStatus.setTextColor(0xFFA8B9C4);
        extrasCard.addView(shaderStatus);
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
        addAction(extrasCard, shaderButton, false);

        Button touchEditor = new Button(this);
        touchEditor.setText("Customize touch controls");
        touchEditor.setOnClickListener(view ->
                startActivity(new Intent(this, MclaTouchEditorActivity.class)));
        addAction(extrasCard, touchEditor, false);

        audioButton = new Button(this);
        audioButton.setOnClickListener(view -> {
            if (MclaBuildVariant.isRedMagicAudioTest(this)) return;
            boolean lowLatency = !audioLowLatency();
            getSharedPreferences("audio", MODE_PRIVATE).edit()
                    .putBoolean("low_latency", lowLatency).apply();
            updateAudioStatus();
        });
        addAction(extrasCard, audioButton, false);
        updateAudioStatus();

        playButton = new Button(this);
        playButton.setText("Play");
        playButton.setEnabled(hasGameFiles());
        playButton.setOnClickListener(view -> startGame());
        addAction(card(layout, "Launch", "Gamepad supported · touch controls experimental"),
                playButton, true);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF0B161F);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private File driverFolder() {
        return new File(getFilesDir(), "gpu-drivers");
    }

    private boolean audioLowLatency() {
        return getSharedPreferences("audio", MODE_PRIVATE)
                .getBoolean("low_latency", false);
    }

    private void updateAudioStatus() {
        if (audioButton == null) return;
        if (MclaBuildVariant.isRedMagicAudioTest(this)) {
            audioButton.setText("Audio test: stereo / normal mixer (fixed for comparison)");
            audioButton.setEnabled(false);
            return;
        }
        boolean lowLatency = audioLowLatency();
        audioButton.setText(lowLatency
                ? "Audio: low-latency (best performance, may be silent)"
                : "Audio: compatibility (normal mixer)");
    }

    private void updateDriverStatus() {
        boolean custom = new File(driverFolder(), "selected.txt").isFile();
        String driverLabel = isMaliBuild ? "PanVK" : "Turnip";
        String suffix = "";
        if (custom && !isMaliBuild) {
            String debug = getSharedPreferences("driver_debug", MODE_PRIVATE)
                    .getString("tu_debug", "syncdraw");
            if (new File(getFilesDir(), "driver.env").isFile()) {
                suffix = " (driver.env override)";
            } else {
                suffix = debug.isEmpty() ? " (no debug switch)" : " (" + debug + ")";
            }
        }
        driverStatus.setText(custom ? "Selected driver: " + driverLabel + suffix :
                "Selected driver: system");
        if (systemButton != null) systemButton.setEnabled(custom);
        if (turnipDebugSpinner != null) turnipDebugSpinner.setEnabled(custom);
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
        if (requestCode == PICK_DRIVER_ZIP) {
            Uri uri = data.getData();
            if (uri == null) return;
            driverButton.setEnabled(false);
            playButton.setEnabled(false);
            status.setText("Importing " + (isMaliBuild ? "PanVK" : "Turnip") + " driver...");
            new Thread(() -> importDriver(uri), "MCLA driver import").start();
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
        String driverName = isMaliBuild ? "libvulkan_panfrost.so" : "libvulkan_freedreno.so";
        File part = new File(folder, driverName + ".partial");
        File target = new File(folder, driverName);
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
                                !(name.equals(driverName) || name.endsWith("/" + driverName) ||
                                  (isMaliBuild && (name.equals("libvulkan_freedreno.so") ||
                                  name.endsWith("/libvulkan_freedreno.so"))))) continue;
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
            if (!found) throw new Exception("The ZIP does not contain " + driverName + ".");
            validateArm64Library(part);
            if (!part.renameTo(target)) throw new Exception("Could not save the driver.");
            File selection = new File(folder, "selected.txt");
            try (FileOutputStream output = new FileOutputStream(selection)) {
                output.write((driverName + "\n").getBytes("UTF-8"));
                output.getFD().sync();
            }
            mainHandler.post(() -> {
                updateDriverStatus();
                status.setText((isMaliBuild ? "PanVK" : "Turnip") + " driver imported. Tap Play to test it.");
            });
        } catch (Exception e) {
            Log.e(TAG, "Driver import failed", e);
            mainHandler.post(() -> status.setText("Driver import failed: " + e.getMessage()));
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
