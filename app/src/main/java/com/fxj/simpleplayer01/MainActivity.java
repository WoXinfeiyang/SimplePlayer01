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
    private String url05="rtsp://120.205.96.36:554/live/ch16022919053041099962.sdp?playtype=1&boid=001&backupagent=120.205.32.36:554&clienttype=1&time=20161205230814+08&life=172800&ifpricereqsnd=1&vcdnid=001&userid=&mediaid=ch16022919053041099962&ctype=2&TSTVTimeLife=60&contname=&authid=0&terminalflag=1&UserLiveType=0&stbid=&nodelevel=3";
    private String url06="http://140.249.38.225/65720D00C734771CB622731C1/03000808005C386D8B0BB27580F70D61B58349-6C33-42EC-820B-8087C060B456.mp4?ccode=0501&duration=60&expire=18000&psid=7f0236c499037e53548201f764b82516&ups_client_netip=da1e7407&ups_ts=1548316364&ups_userid=38019172&utid=X1ZHFFNFACkCAdoedAMkzm2w&vid=XMzk4OTgxMzQwMA&vkey=Ada6b3eb747b531197fdf3ef16e8ed58c&s=efbfbd705d7fefbfbd1f&sp=&ali_redirect_domain=vali-dns.cp31.ott.cibntv.net&ali_redirect_ex_ftag=96095a128986348839be3ee945d4be562eec7438a5eaa71b&ali_redirect_ex_tmining_ts=1548316366&ali_redirect_ex_tmining_expire=3600&ali_redirect_ex_hot=0";
    private String url07="http://42.81.98.123/677396C0EC039716F81B922CE/03000808015C386D8B0BB27580F70D61B58349-6C33-42EC-820B-8087C060B456.mp4?ccode=0501&duration=240&expire=18000&psid=7f0236c499037e53548201f764b82516&ups_client_netip=da1e7407&ups_ts=1548316364&ups_userid=38019172&utid=X1ZHFFNFACkCAdoedAMkzm2w&vid=XMzk4OTgxMzQwMA&vkey=A9d84c4e46d0c1a975b99574097c6baa6&s=efbfbd705d7fefbfbd1f&sp=&ali_redirect_domain=vali-dns.cp31.ott.cibntv.net&ali_redirect_ex_ftag=6d585e8383a4c7c37a913897c5a8545119d56f5814759517&ali_redirect_ex_tmining_ts=1548316431&ali_redirect_ex_tmining_expire=3600&ali_redirect_ex_hot=11";
    private String url08="http://devimages.apple.com/iphone/samples/bipbop/gear1/fileSequence179.ts";
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
