package com.sensorsdata.analytics.android.sdk.exposure;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;

import java.lang.ref.WeakReference;

public class ExposedTransform implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks {

    private final String TAG = "SA.ExposedTransform";
    private final AppPageChange mAppPageChange;
    private final SAExposedProcess.CallBack mCallBack;
    private WeakReference<Activity> mActivityWeakReference;
    private volatile boolean isMonitor = false;
    private volatile int windowCount = -1;
    private View[] views;

    @Override
    public void onNewIntent(Intent intent) {

    }

    public synchronized void observerWindow(Activity activity) {
        int originWindowCount = windowCount;
        processViews();
        SALog.i(TAG, "originWindowCount:" + originWindowCount + ",windowCount:" + windowCount);
        //窗口增加
        if (originWindowCount != windowCount) {
            //移除以前的页面监听
            viewsRemoveTreeObserver(activity);
            //重新进行页面监听
            onActivityResumed(activity);
            return;
        }
        //正常情况未监听则进行监听,避免页面未改变导致的未监听
        if (!isMonitor) {
            onActivityResumed(activity);
        }
    }

    private void processViews() {
        WindowHelper.init();
        views = WindowHelper.getSortedWindowViews();
        if (views != null && views.length > 0) {
            windowCount = views.length;
        } else {
            windowCount = 0;
        }
    }

    interface LayoutCallBack {
        void viewLayoutChange();
    }

    public ExposedTransform(final SAExposedProcess.CallBack callBack) {
        this.mCallBack = callBack;
        LayoutCallBack layoutCallBack = new LayoutCallBack() {
            @Override
            public void viewLayoutChange() {
                if (mActivityWeakReference != null) {
                    Activity activity = mActivityWeakReference.get();
                    if (activity != null) {
                        callBack.viewLayoutChange(activity);
                    }
                }
            }
        };
        mAppPageChange = new AppPageChange(layoutCallBack);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        //避免在 onCreate 中操作可见性这里 activityWeakReference 为空无法监控到
        mActivityWeakReference = new WeakReference<>(activity);
        SALog.i(TAG, "onActivityResumed:" + activity);
        synchronized (this) {
            viewsAddTreeObserver(activity);
            mCallBack.onActivityResumed(activity);
        }
    }


    private void viewTreeObserver(View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(mAppPageChange);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowFocusChangeListener(mAppPageChange);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.addOnDrawListener(mAppPageChange);
        }
        viewTreeObserver.addOnScrollChangedListener(mAppPageChange);
        viewTreeObserver.addOnGlobalFocusChangeListener(mAppPageChange);
    }

    private void viewRemoveTreeObserver(View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.removeGlobalOnLayoutListener(mAppPageChange);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.removeOnWindowFocusChangeListener(mAppPageChange);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnDrawListener(mAppPageChange);
        }
        viewTreeObserver.removeOnScrollChangedListener(mAppPageChange);
        viewTreeObserver.removeOnGlobalFocusChangeListener(mAppPageChange);
    }

    private void viewsAddTreeObserver(Activity activity) {
        SALog.i(TAG, "viewsAddTreeObserver:" + isMonitor);
        if (!isMonitor) {
            if (mCallBack.getExposureViewSize(activity) <= 0) {
                return;
            }
            processViews();
            boolean flag = true;
            View decorView = activity.getWindow().getDecorView();
            if (views != null && views.length > 0) {
                for (View view : views) {
                    if (decorView == view) {
                        //由于 onResume 的时候获取到的窗口数量不是最新的，因此需要加这个逻辑
                        flag = false;
                    }
                    viewTreeObserver(view);
                }
                if (flag) {
                    viewTreeObserver(decorView);
                }
            } else {
                viewTreeObserver(activity.getWindow().getDecorView());
            }
            isMonitor = true;
        }
    }

    private void viewsRemoveTreeObserver(Activity activity) {
        SALog.i(TAG, "viewsRemoveTreeObserver:" + isMonitor);
        if (isMonitor) {
            isMonitor = false;
            if (views != null && views.length > 0) {
                for (View view : views) {
                    viewRemoveTreeObserver(view);
                }
            } else {
                viewRemoveTreeObserver(activity.getWindow().getDecorView());
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        SALog.i(TAG, "onActivityPaused");
        synchronized (this) {
            viewsRemoveTreeObserver(activity);
            mCallBack.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
