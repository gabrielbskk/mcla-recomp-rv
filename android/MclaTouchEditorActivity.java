package org.mcla.recomp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Edit the same normalized touch layout used over the SDL surface. */
public final class MclaTouchEditorActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF18232C);
        MclaTouchControls preview = new MclaTouchControls(this, true);
        root.addView(preview, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setGravity(Gravity.CENTER);
        tools.setPadding(12, 4, 12, 8);
        tools.setBackgroundColor(0xE8000000);
        TextView instructions = new TextView(this);
        instructions.setGravity(Gravity.CENTER);
        instructions.setText("Drag each Xbox control to move it. Size applies to all controls.");
        tools.addView(instructions);

        SeekBar size = new SeekBar(this);
        size.setMax(90);
        size.setProgress(preview.getSizePercent() - 60);
        tools.addView(size);
        TextView sizeLabel = new TextView(this);
        sizeLabel.setGravity(Gravity.CENTER);
        sizeLabel.setText("HUD size: " + preview.getSizePercent() + "%");
        tools.addView(sizeLabel);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                preview.setSizePercent(60 + progress);
                sizeLabel.setText("HUD size: " + (60 + progress) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        Button reset = new Button(this);
        reset.setText("Reset layout");
        reset.setOnClickListener(v -> {
            preview.resetLayout();
            size.setProgress(40);
        });
        actions.addView(reset);
        Button hide = new Button(this);
        hide.setText("Hide tools");
        actions.addView(hide);
        Button done = new Button(this);
        done.setText("Done");
        done.setOnClickListener(v -> finish());
        actions.addView(done);
        tools.addView(actions);

        Button showTools = new Button(this);
        showTools.setText("Show tools");
        showTools.setVisibility(View.GONE);
        hide.setOnClickListener(v -> {
            tools.setVisibility(View.GONE);
            showTools.setVisibility(View.VISIBLE);
        });
        showTools.setOnClickListener(v -> {
            showTools.setVisibility(View.GONE);
            tools.setVisibility(View.VISIBLE);
        });
        FrameLayout.LayoutParams bar = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(tools, bar);
        FrameLayout.LayoutParams toggle = new FrameLayout.LayoutParams(-2, -2,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        root.addView(showTools, toggle);
        setContentView(root);
    }
}
