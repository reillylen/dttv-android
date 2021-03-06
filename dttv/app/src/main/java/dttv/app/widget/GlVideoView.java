package dttv.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;


@SuppressLint("ClickableViewAccessibility")
public class GlVideoView extends GLSurfaceView {

    private float mPosX;
    private float mPosY;
    private float mCurrentPosX;
    private float mCurrentPosY;

    private OnTouchMoveListener moveListener;
    //Paint paint;

    public GlVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return super.onTrackballEvent(event);
    }

    public void setTouchMoveListener(OnTouchMoveListener onTouchMoveListener) {
        this.moveListener = onTouchMoveListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moveListener.onTouch(event);
                mPosX = event.getX();
                mPosY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentPosX = event.getX();
                mCurrentPosY = event.getY();
                if (mCurrentPosX - mPosX > 10 && Math.abs(mCurrentPosY - mPosY) < 100) {
                    //move right
                } else if (mCurrentPosX - mPosX < 10 && Math.abs(mCurrentPosY - mPosY) < 100) {
                    //move left
                } else if (mCurrentPosY - mPosY > 10 && Math.abs(mCurrentPosX - mPosX) < 100) {
                    //move down
                    moveListener.onTouchMoveDown(mCurrentPosX);
                } else if (mCurrentPosY - mPosY < 10 && Math.abs(mCurrentPosX - mPosX) < 100) {
                    //move up
                    moveListener.onTouchMoveUp(mCurrentPosX);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
