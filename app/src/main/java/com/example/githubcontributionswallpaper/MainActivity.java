package com.example.githubcontributionswallpaper;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText github_username;
    private TextView github_status;
    private ImageView github_graph;
    private Switch github_wallpaper_switch;

    private static final int RECT_DIM = 14;

    private static final int DAYS_IN_WEEK = 7;
    private static final int WEEKS_TO_SHOW = 8;

    private String[][] contribution_depths = new String[DAYS_IN_WEEK][WEEKS_TO_SHOW];

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
        github_status = (TextView) findViewById(R.id.github_status);
        github_graph = (ImageView) findViewById(R.id.github_graph);
        github_wallpaper_switch = (Switch) findViewById(R.id.github_wallpaper_switch);

        github_username.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() == 0) {
                    return;
                }
                String input = s.toString();
                // TODO Jsoup
                // TODO set status
                // TODO set graph
            }
        });

        github_wallpaper_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getGraph();
                if (isChecked) {
                    // TODO set wallpaper
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
                    Document document = Jsoup.connect("https://github.com/thisistrivial").get();
                    Elements points = document.select("rect[x~=-3(1|2|3|4|5|6|7|8)]");
                    int i = 0;
                    for (Element point : points) {
                        contribution_depths[i / 8][i % 8] = point.attr("fill");
                        i++;
                    }
                    generateGraph();
                    // TODO FIX ERROR
                } catch (Exception e) {
                    Log.d("ERROR", e.toString());
                }
            }
        }.start();
    }

    private void generateGraph() {
        Bitmap rect = BitmapFactory.decodeResource(getResources(), R.drawable.github_empty_contribution);
        Bitmap result = Bitmap.createBitmap(WEEKS_TO_SHOW * RECT_DIM,
                DAYS_IN_WEEK * RECT_DIM, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        for (int r = 0; r < contribution_depths.length; r++) {
            for (int c = 0; c < contribution_depths[r].length; c++) {
                String hex = contribution_depths[r][c];
                ColorFilter filter = new PorterDuffColorFilter(Color.parseColor(contribution_depths[r][c]), PorterDuff.Mode.MULTIPLY);
                paint.setColorFilter(filter);
                canvas.drawBitmap(rect, null, new Rect(c * RECT_DIM,
                        r * RECT_DIM,
                        (c + 1) * RECT_DIM,
                        (r + 1) * RECT_DIM), paint);
            }
        }
        github_graph.setImageBitmap(result);
    }
}