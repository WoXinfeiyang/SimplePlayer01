package com.fxj.simpleplayer01;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private final String TAG=MainActivity.class.getSimpleName();

    private String url01=Environment.getExternalStorageDirectory().getAbsolutePath() + "/video.mp4";
    private String url02="http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8";
    private String url03=Environment.getExternalStorageDirectory().getAbsolutePath() + "/cuc_ieschool.mp4";
    private String url04="http://ips.ifeng.com/video19.ifeng.com/video09/2019/01/09/4966943-102-007-1715.mp4";

    private SurfaceView mSurfaceView;

    private SimplePlayer mPlayer;
    private SurfaceHolder mSurfaceViewHolder;
    private Button mPlayBtn;
    private Button mStopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceViewHolder = mSurfaceView.getHolder();
        mSurfaceViewHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mPlayer=new SimplePlayer(holder.getSurface(),url04);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        mPlayBtn = findViewById(R.id.btn_play);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.play();
            }
        });

        mStopBtn = findViewById(R.id.btn_stop);
        mStopBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mPlayer.stop();
            }
        });
    }
}
