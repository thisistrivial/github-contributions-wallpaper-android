package com.example.githubcontributionswallpaper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private EditText github_username;
    private Button github_search;
    private TextView github_status;
    private ImageView github_graph;
    private Switch github_wallpaper_switch;

    private Bitmap display;
    private Bitmap wallpaper;

    private static final int RECT_DIM = 14;

    private static final int DAYS_IN_WEEK = 7;
    private static final int WEEKS_TO_SHOW = 8;

    private String[][] contribution_depths;

    enum GithubColors {
        ONE("#ebedf0"),
        TWO("#9be9a8"),
        THREE("#40c463"),
        FOUR("#30a14e"),
        FIVE("#216e39");

        String colorHex_;

        GithubColors(String colorHex) {
            this.colorHex_ = colorHex;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        github_username = (EditText) findViewById(R.id.github_username);
        github_search = (Button) findViewById(R.id.github_search);
        github_status = (TextView) findViewById(R.id.github_status);
        github_graph = (ImageView) findViewById(R.id.github_graph);
        github_wallpaper_switch = (Switch) findViewById(R.id.github_wallpaper_switch);

        requestPermissions();

        github_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGraph();
            }
        });

        github_wallpaper_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                   setWallpaper();
                } else {
                    // TODO remove wallpaper
                }
            }
        });
    }

    private void getGraph() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Document document = Jsoup.connect("https://github.com/" + github_username.getText().toString()).get();
                    Elements points = document.select("rect[x]");
                    int lastX = Integer.parseInt(points.last().attr("x"));
                    int firstX = lastX + WEEKS_TO_SHOW;
                    int cnt = 0;
                    contribution_depths = new String[WEEKS_TO_SHOW][DAYS_IN_WEEK];
                    for (int i = points.size() - DAYS_IN_WEEK * WEEKS_TO_SHOW; i < points.size(); i++) {
                        Element pointOn = points.get(i);
                        int xOn = Integer.parseInt(pointOn.attr("x"));
                        if (xOn < firstX) {
                            contribution_depths[cnt / 7][cnt % 7] = pointOn.attr("fill");
                            cnt++;
                        }
                    }
                    generateGraph();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            github_status.setText(getResources().getString(R.string.empty));
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            github_status.setText(getResources().getString(R.string.not_found));
                        }
                    });
                    Log.d("ERROR", e.toString());
                }
            }
        }.start();
    }

    private void generateGraph() {
        Bitmap rect = BitmapFactory.decodeResource(getResources(), R.drawable.github_empty_contribution);
        display = Bitmap.createBitmap(WEEKS_TO_SHOW * RECT_DIM,
                DAYS_IN_WEEK * RECT_DIM, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(display);
        Paint paint = new Paint();

        for (int r = 0; r < contribution_depths.length; r++) {
            for (int c = 0; c < contribution_depths[r].length; c++) {
                if (contribution_depths[r][c] != null) {
                    String hex = contribution_depths[r][c];
                    ColorFilter filter = new PorterDuffColorFilter(Color.parseColor(contribution_depths[r][c]), PorterDuff.Mode.MULTIPLY);
                    paint.setColorFilter(filter);
                    canvas.drawBitmap(rect, null, new Rect(r * RECT_DIM,
                            c * RECT_DIM,
                            (r + 1) * RECT_DIM,
                            (c + 1) * RECT_DIM), paint);
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                github_graph.setImageBitmap(display);
            }
        });
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            System.exit(1);
        }
    }

    @SuppressLint("MissingPermission")
    private void setWallpaper() {
        Bitmap currentWallpaper;
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        ParcelFileDescriptor pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
        if (pfd == null) {
            Log.d("ERROR", "1");
            pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
        }
        if (pfd != null) {
            currentWallpaper = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
            try
            {
                pfd.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        } else {
            final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            currentWallpaper = Bitmap.createBitmap(wallpaperDrawable.getIntrinsicWidth(),
                    wallpaperDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(currentWallpaper);
            wallpaperDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            wallpaperDrawable.draw(canvas);
        }
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int graph_l = screenWidth / 5;
        int graph_t = screenHeight / 2;
        int graph_r = screenWidth * 4 / 5;
        int graph_b = screenHeight / 2 + screenWidth * 3 / 5;

        wallpaper = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(wallpaper);
        canvas.drawBitmap(currentWallpaper, null,
                new Rect(0, 0, screenWidth, screenHeight), null);
        if (display != null) {
            canvas.drawBitmap(display, null,
                    new Rect(graph_l, graph_t, graph_r, graph_b), null);
        }
        github_graph.setImageBitmap(wallpaper);
    }
}