package dttv.app;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import dttv.app.DtPlayer.OnCompletionListener;
import dttv.app.DtPlayer.OnPreparedListener;
import dttv.app.utils.Constant;
import dttv.app.utils.Log;
import dttv.app.utils.TimesUtil;
import dttv.app.widget.GlVideoView;
/**
 * VideoPlayer Activity
 * 
 * @author shihx1
 *	
 */
public class VideoPlayerActivity extends Activity implements OnClickListener{
	private String TAG = "VideoPlayerActivity";
	private DtPlayer dtPlayer;
	private String mPath;
	
	private View mBarView;
	private RelativeLayout playerBarLay;
	private TextView currentTimeTxt,totalTimeTxt;
	private ImageButton preBtn,nextBtn,pauseBtn;
	private SeekBar playerProgressBar;
	private GlVideoView glSurfaceView;
	private int seek_flag = 0;
	
	private static final int PLAYER_IDLE = 0x0;
	private static final int PLAYER_INITING = 0x1;
	private static final int PLAYER_PREPARED = 0x2;
	private static final int PLAYER_RUNNING = 0x3;
	private static final int PLAYER_PAUSED = 0x4;
	private static final int PLAYER_BUFFERING = 0x5;
	private static final int PLAYER_SEEKING = 0x6;
	
	private static final int PLAYER_STOP = 0x100;
	private static final int PLAYER_EXIT = 0x101;
	private int mState = PLAYER_IDLE;
	
	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.video_play);
		mState = PLAYER_IDLE;		
		dtPlayer = new DtPlayer(this);
		if(OpenglES2Support() == 0)
		{
			Toast.makeText(this, "opengl es2.0 not supported", 1).show();
			return;
		}
		else
		{
			//opengl
			glSurfaceView = (GlVideoView)findViewById(R.id.glvideo_view);
			glSurfaceView.setRenderer(new GLSurfaceViewRender());
	        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	        //glSurfaceView.setOnTouchListener((OnTouchListener) this);
		}

		initExtraData();
		initView();
		initListener();
	}
	
	private int OpenglES2Support()
	{
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
 
	    boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000 || isProbablyEmulator();	 
	    if(supportsEs2)
	    	return 1;
	    else
	    	return 0;
	}
	
	private boolean isProbablyEmulator() {
	    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
	            && (Build.FINGERPRINT.startsWith("generic")
	                    || Build.FINGERPRINT.startsWith("unknown")
	                    || Build.MODEL.contains("google_sdk")
	                    || Build.MODEL.contains("Emulator")
	                    || Build.MODEL.contains("Android SDK built for x86"));
	}
	
	@SuppressLint("ShowToast")
	private void initExtraData(){
		Intent intent = getIntent();
		mPath = intent.getStringExtra(Constant.FILE_MSG);
		Toast.makeText(this, "mPath is:"+mPath, 1).show();
		try {
			mState = PLAYER_INITING;
			if(dtPlayer.setDataSource(mPath) == -1)
			{
				mState = PLAYER_IDLE;
				return;
			}
			//here to set video size
			int width = dtPlayer.getVideoWidth();
			int height = dtPlayer.getVideoHeight();
			Log.d(TAG,"--width:"+width+"  height:"+height);
			if(width > 0 && height > 0 && width <= 1920 && height <= 1080)
			{
				ViewGroup.LayoutParams layoutParams=glSurfaceView.getLayoutParams();
				layoutParams.width=width;
				layoutParams.height=height;
				glSurfaceView.setLayoutParams(layoutParams);
				
				dtPlayer.setVideoSize(width,height);
			}
			dtPlayer.prepare();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initView(){
		mBarView = (View)findViewById(R.id.audio_player_bar_lay);
		playerBarLay = (RelativeLayout)mBarView.findViewById(R.id.dt_play_bar_lay);
		currentTimeTxt = (TextView)mBarView.findViewById(R.id.dt_play_current_time);
		totalTimeTxt = (TextView)mBarView.findViewById(R.id.dt_play_total_time);
		preBtn = (ImageButton)mBarView.findViewById(R.id.dt_play_prev_btn);
		pauseBtn = (ImageButton)mBarView.findViewById(R.id.dt_play_pause_btn);
		nextBtn = (ImageButton)mBarView.findViewById(R.id.dt_play_next_btn);
		playerProgressBar = (SeekBar)mBarView.findViewById(R.id.dt_play_progress_seekbar);
	}
	
	private void initListener(){
		dtPlayer.setOnPreparedListener(new PrePareListener());
		dtPlayer.setOnCompletionListener(new OnCompleteListener());
		playerProgressBar.setOnSeekBarChangeListener(new OnSeekChangeListener());
		preBtn.setOnClickListener(this);
		nextBtn.setOnClickListener(this);
		pauseBtn.setOnClickListener(this);
	}
	
	class PrePareListener implements OnPreparedListener{
		@Override
		public void onPrepared(DtPlayer mp) {
			// TODO Auto-generated method stub
			Log.i(Constant.LOGTAG, "enter onPrepared");
			mState = PLAYER_PREPARED;
			dtPlayer.start();
			mState = PLAYER_RUNNING;
			int duration = mp.getDuration();
            if(duration>0){
            	totalTimeTxt.setText(TimesUtil.getTime(duration));
            	playerProgressBar.setMax(duration);
            }
            startTimerTask();
		}
	}
	
	class OnCompleteListener implements OnCompletionListener{
		@Override
		public void onCompletion(DtPlayer mp) {
			// TODO Auto-generated method stub
			mState = PLAYER_EXIT;
		}
	}
	
	class OnSeekChangeListener implements OnSeekBarChangeListener{

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			if(seek_flag == 1)
			{
				//int currentTime = seekBar.getProgress();
				//dtPlayer.seekTo(currentTime);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			seek_flag = 1;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			mState = PLAYER_SEEKING;
			int currentTime = seekBar.getProgress();
			dtPlayer.seekTo(currentTime);
			dtPlayer.start();
			seek_flag = 0;
		}
		
	}
	
	
	Handler doActionHandler = new Handler(Looper.getMainLooper()){
		public void handleMessage(android.os.Message msg) {
			int msgId = msg.what;
			switch(msgId){
			case Constant.REFRESH_TIME_MSG:
				int currentTime = dtPlayer.getCurrentPosition();
				int duration = dtPlayer.getDuration();
				if(currentTime < 0)
					currentTime = 0;
				if(currentTime > duration)
					currentTime = duration;
				currentTimeTxt.setText(TimesUtil.getTime(currentTime));
				playerProgressBar.setProgress(currentTime);
				break;
			case Constant.BEGIN_MEDIA_MSG:
				
	            //startTimerTask();
				break;
			case Constant.HIDE_OPREATE_BAR_MSG:
				playerBarLay.setVisibility(View.GONE);
				break;
			}
		};
	};
	
	private Timer mTimer;
	private void startTimerTask(){
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				doActionHandler.sendEmptyMessage(Constant.REFRESH_TIME_MSG);
			}
		}, Constant.REFRESH_TIME, Constant.REFRESH_TIME);
	}
	
	private void releaseTimerAndHandler(){
		//isEnableTime = false;
		if(mTimer!=null)
			mTimer.cancel();
		doActionHandler.removeCallbacksAndMessages(null);
	}

    @Override
    public void onPause()
    {
    	releaseTimerAndHandler();
    	glSurfaceView.onPause();
    	dtPlayer.pause();
        super.onPause();
        if(mState == PLAYER_PAUSED)
        	mState = PLAYER_RUNNING;
        if(mState == PLAYER_RUNNING)
        	mState = PLAYER_PAUSED;
        Log.d(TAG,"--PAUSE--");
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        //glSurfaceView.onResume();
    }
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		mState = PLAYER_STOP;
		dtPlayer.release();
		super.onStop();
	}
	
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.dt_play_next_btn:
			break;
		case R.id.dt_play_prev_btn:
			break;
		case R.id.dt_play_pause_btn:
			handlePausePlay();
			break;
		}
	}
	
	private void handlePausePlay(){
		try {
			if(dtPlayer.isPlaying()){
				dtPlayer.pause();
				pauseBtn.setBackgroundResource(R.drawable.btn_mu_pause);
			}else{
				dtPlayer.start();
				pauseBtn.setBackgroundResource(R.drawable.btn_mu_play);
			}
		} catch (IllegalStateException e) {
			// TODO: handle exception
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		//dtPlayer.release();
		super.onDestroy();
	}
		
	//---------------------------OPENGL------------------------------//
	
	class GLSurfaceViewRender implements GLSurfaceView.Renderer {  
  
		@Override
		public void onSurfaceCreated(GL10 gl,
				javax.microedition.khronos.egl.EGLConfig config) {
			// TODO Auto-generated method stub
			Log.i(TAG, "gl create enter");			
			//gl.glClearColor(0.0f, 0f, 1f, 0.5f); // display blue at first
			//gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			dtPlayer.onSurfaceCreated();
			
		}  
		
        @Override  
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //other case
        	Log.i(TAG, "gl surface change enter, width:"+width+" height:"+height);
        	dtPlayer.onSurfaceChanged(width,height);
        }
  
        @Override  
        public void onDrawFrame(GL10 gl) {
            //Log.i(TAG, "onDrawFrame");  
            // 清除屏幕和深度缓存(如果不调用该代码, 将不显示glClearColor设置的颜色)  
            // 同样如果将该代码放到 onSurfaceCreated 中屏幕会一直闪动  
            //gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        	//Log.i(TAG, "draw enter");
        	dtPlayer.onDrawFrame();
        }
		
  
    }  
	
	//-----------------------------OPENGL----------------------------//
	
}