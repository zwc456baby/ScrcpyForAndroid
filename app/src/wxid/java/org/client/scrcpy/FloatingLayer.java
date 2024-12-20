package org.client.scrcpy;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;


import org.lsposed.lsparanoid.Obfuscate;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 */

@Obfuscate
public class FloatingLayer {

    private static FloatingLayer sFloatingLayer;
    private static final String TAG = "FLOATINGLAYER";

    public static boolean ISSHOW = false;


    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    private Context mContext;
    private boolean isMove = false;
    private View mPopView;

    private AnimationTimerTask mAnimationTask;
    private Timer mAnimationTimer;
    private FloatingLayerListener mListener;
    private final Handler mHander = new Handler(Looper.getMainLooper());
    private int mWidth;
    private int mHeight;
    private float mPrevX;
    private float mPrevY;
    private float touchX;
    private float touchY;
    private int mGetTokenPeriodTime = 500;
    private int mAnimatonPeriodTime = 16;
    private BigDecimal mStartClickTime;

    public static FloatingLayer getInstance(Context context) {
        if (null == sFloatingLayer) {
            synchronized (FloatingLayer.class) {
                if (null == sFloatingLayer) {
                    sFloatingLayer = new FloatingLayer(context);
                }
            }
        }
        return sFloatingLayer;
    }

    private FloatingLayer(Context context) {
        this.mContext = context;
    }

    public void resetContent(Context context){
        this.mContext = context;
    }

    private void initView(int imageResource) {
        initWindowManager();
        initLayoutParams();
        // 填充控件
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopView = layoutInflater.inflate(R.layout.layout_floatinglayer, null);
        ImageView imageView = (ImageView) mPopView.findViewById(R.id.pop_image);
        imageView.setImageResource(imageResource);
        initDrag();
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private void initLayoutParams() {
        mWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        mLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        String packname = this.mContext.getPackageName();
        PackageManager pm = this.mContext.getPackageManager();
        boolean permission = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.SYSTEM_ALERT_WINDOW", packname));
        if (permission) {
            // mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            // LogUtil.d("有全局弹窗系统权限");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;// 系统提示window
            }
        } else {
            // LogUtil.d("没有全局弹窗系统权限");
            // activity 级别的悬浮窗
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        }
        /*
            解决方法
            系统提示window弹窗方式:
              api=26+ -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
              others  -> WindowManager.LayoutParams.TYPE_PHONE;
                         WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
          */
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//        } else {
//            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;// 系统提示window
//        }
        mLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        mLayoutParams.x = 0;
        mLayoutParams.y = mContext.getResources().getDisplayMetrics().heightPixels / 6 * 3;


    }

    private void initDrag() {
        mPopView.setOnTouchListener(new View.OnTouchListener() {
//            boolean click = false;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        touchX = motionEvent.getX();
                        touchY = motionEvent.getY();
                        mPrevX = motionEvent.getRawX();
                        mPrevY = motionEvent.getRawY();
                        mStartClickTime = BigDecimal.valueOf(System.currentTimeMillis());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = motionEvent.getRawX() - mPrevX;
                        float deltaY = motionEvent.getRawY() - mPrevY;
                        mLayoutParams.x += deltaX;
                        mLayoutParams.y += deltaY;
                        mPrevX = motionEvent.getRawX();
                        mPrevY = motionEvent.getRawY();
                        if (mLayoutParams.x < 0) mLayoutParams.x = 0;
                        if (mLayoutParams.x > mWidth - mPopView.getWidth())
                            mLayoutParams.x = mWidth - mPopView.getWidth();
                        if (mLayoutParams.y < 0) mLayoutParams.y = 0;
                        if (mLayoutParams.y > mHeight - mPopView.getHeight() * 2)
                            mLayoutParams.y = mHeight - mPopView.getHeight() * 2;

                        try {
                            mWindowManager.updateViewLayout(mPopView, mLayoutParams);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        if (deltaX > 10 && deltaY > 10) {  //如果移动的距离小于一定的值，则不触发点击事件
//                            click = true;
//                        }
//                        if(deltaX > 10 | deltaY > 10) isMove=true;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if ((motionEvent.getX() - touchX) < 2 && (motionEvent.getX() - touchX) > -2 && motionEvent.getY() - touchY < 2 && motionEvent.getY() - touchY > -2) {
                            if (null != mListener) {
                                mListener.onClick();
                                return true;
                            }
                        }
                        mAnimationTimer = new Timer();
                        mAnimationTask = new AnimationTimerTask();
                        mAnimationTimer.schedule(mAnimationTask, 0, mAnimatonPeriodTime);
                        break;
                }
                return false;
            }
        });
    }

    public void show(Activity activity, int ImageResource) {
        if (!ISSHOW) {
            ISSHOW = true;
            initView(ImageResource);
            mHander.post(() -> {
                try {
//                    mLayoutParams.width = mWidth;
//                    mLayoutParams.height = mHeight;
                    // 添加 view
                    Log.i("Scrcpy", "mPopView add");
                    mWindowManager.addView(mPopView,
                            mLayoutParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        if (null != mListener) {
            mListener.onShow();
        }
    }

    public void close() {
        try {
            if (ISSHOW) {
                mWindowManager.removeViewImmediate(mPopView);
                if (null != mListener) {
                    mListener.onClose();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        } finally {
            ISSHOW = false;
        }
    }

    public void setListener(FloatingLayerListener listener) {
        if (null != sFloatingLayer) {
            this.mListener = listener;
        }
    }

    public interface FloatingLayerListener {
        void onClick();

        void onShow();

        void onClose();
    }

    class AnimationTimerTask extends TimerTask {
        int mStepX;
        int mDestX;

        public AnimationTimerTask() {
            if (mLayoutParams.x > mWidth / 2) {
                mDestX = mWidth - mPopView.getWidth();
                mStepX = (mWidth - mLayoutParams.x) / 10;
            } else {
                mDestX = 0;
                mStepX = -((mLayoutParams.x) / 10);
            }
        }

        @Override
        public void run() {

            if (Math.abs(mDestX - mLayoutParams.x) <= Math.abs(mStepX)) {
                mLayoutParams.x = mDestX;
            } else {
                mLayoutParams.x += mStepX;
            }
            mHander.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mWindowManager.updateViewLayout(mPopView, mLayoutParams);
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                    }
                }
            });
            if (mLayoutParams.x == mDestX) {
                mAnimationTask.cancel();
                mAnimationTimer.cancel();
            }


        }
    }
}
