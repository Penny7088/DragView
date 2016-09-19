package com.penny.root.dragdemo;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.penny.root.dragdemo.ui.Data;
import com.penny.root.dragdemo.ui.DragLayout;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ListView mLeft;
    private ListView mMain;
    private View iv_header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_header = findViewById(R.id.iv_header);

        mLeft = (ListView) findViewById(R.id.lv_left);
        mLeft.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Data.sCheeseStrings) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.BLACK);
                return view;
            }
        });

        mMain = (ListView) findViewById(R.id.lv_main);
        mMain.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Data.NAMES));

        final DragLayout dl = (DragLayout) findViewById(R.id.dl);

        dl.setOnStatusUpdateListener(new DragLayout.OnStatusUpdateListener() {
            @Override
            public void onOpen() {
                System.out.println(">>打开了!!!!!!!");
                Random random = new Random();
                mLeft.smoothScrollToPosition(random.nextInt(50));
            }

            @Override
            public void onDraging(float percent) {
                // percent 0.0 -> 1.0
                // alpha 1.0 -> 0.0
                ViewHelper.setAlpha(iv_header, 1 - percent);
                System.out.println(">>拖拽中--------------");
            }

            @Override
            public void onClose() {
                System.out.println(">>关闭了!!!!!!!");
//				iv_header.setTranslationX(translationX)

                ObjectAnimator animator = ObjectAnimator.ofFloat(iv_header, "translationX", 15f);
                animator.setInterpolator(new CycleInterpolator(4));
                animator.setDuration(500);
                animator.start();
            }
        });

        iv_header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dl.open(true);
            }
        });

    }
}