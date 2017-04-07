package com.demo.bluetoothdemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by THF on 17/4/7.
 */

public class ProgressDialogUtil {


    private static final String TAG = "ProgressDialogUtil";
    private static final int TIME_OUT = 20000;

    private static final HashMap<String, Dialog> map = new HashMap<String, Dialog>();

    /**
     * 加载中的
     * @param context
     */
    public synchronized static void showLoading(Activity context) {
        if (context != null && !context.isFinishing()) {
            showWaiting(context, R.layout.load_wait_dialog,false,"");
        }
    }
    public synchronized static void showLoading(Activity context,String msg) {
        if (context != null && !context.isFinishing()) {
            showWaiting(context, R.layout.load_wait_dialog,false,false,msg);
        }
    }

    /**
     * 显示返回结果成功的
     * @param context
     * @param isSuccess
     */
    public synchronized static void showLoading(Activity context,boolean isSuccess) {
        if (context != null && !context.isFinishing()) {
            showWaiting(context, R.layout.load_wait_dialog,isSuccess,"");
        }

    }

    public synchronized static void showLoading(Activity context, boolean isshow,boolean isSuccess) {
        if (context != null && !context.isFinishing()) {
            showWaiting(context, R.layout.load_wait_dialog, isshow,isSuccess,"");
        }
    }

    public synchronized static void showLoading(Fragment context) {
        showWaiting(context, R.layout.load_wait_dialog);
    }


    public synchronized static boolean isActivity(Activity context) {
        return map.containsKey(getUid(context));
    }

    public synchronized static boolean isActivity(Fragment context) {
        return map.containsKey(getUid(context));
    }

    public synchronized static void showWaiting(final Activity context,
                                                int resid,boolean isSuccess,String msg) {
        showWaiting(context, resid, false,isSuccess,msg);
    }

    public synchronized static void showWaiting(Activity context, int resid,
                                                boolean isshow,boolean isSuccess,String msg) {
        if (!isActivity(context)) {
            // dismiss(context);
            Activity ctx = getSafeContext(context);
            if (ctx != null) {
                View view = LayoutInflater.from(context).inflate(resid,null);
                ImageView imageView = (ImageView) view.findViewById(R.id.success);
                ProgressBar bar = (ProgressBar) view.findViewById(R.id.progress);
                TextView load_text = (TextView) view.findViewById(R.id.load_text);
                if (isSuccess){
                    imageView.setVisibility(View.VISIBLE);
                    bar.setVisibility(View.GONE);
                    load_text.setText("操作成功");
                }else {
                    imageView.setVisibility(View.GONE);
                    bar.setVisibility(View.VISIBLE);
                    if(!msg.equals("")){
                        load_text.setText(msg);
                    }else {
                        load_text.setText("加载中");
                    }
                }
                Dialog progressDialog = new Dialog(ctx, R.style.Dialog);
                progressDialog.setContentView(view);
                progressDialog.setCancelable(isshow);
                progressDialog.show();

                map.put(getUid(context), progressDialog);
                startTimer(context);
            }
        }
    }

    public synchronized static void showWaiting(Fragment context, int resid) {
        if (!isActivity(context)) {

            dismiss(context);
            Activity ctx = getSafeContext(context);
            if (ctx != null) {
                if (!ctx.isFinishing()) {
                    Dialog progressDialog = new Dialog(ctx, R.style.Dialog);
                    progressDialog.setContentView(resid);
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    map.put(getUid(context), progressDialog);
                    startTimer(context);
                }
            }
        }
    }

    public synchronized static void showLoading(Activity context,
                                                final DialogInterface.OnCancelListener cancelListener) {
        showWaiting(context, R.layout.load_wait_dialog, cancelListener);
    }

    public synchronized static void showWaiting(final Activity context,
                                                int resid, final DialogInterface.OnCancelListener cancelListener) {
        if (!isActivity(context)) {
            try {
                // dismiss(context);
                Activity ctx = getSafeContext(context);
                final Dialog progressDialog = new Dialog(ctx, R.style.Dialog);
                DialogInterface.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dismiss(context);
                        cancelListener.onCancel(dialog);
                    }
                };
                progressDialog.setOnCancelListener(cancel);
                progressDialog.setContentView(resid);
                progressDialog.setCancelable(true);
                progressDialog.show();

                map.put(getUid(context), progressDialog);
                startTimer(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // hide
    public synchronized static void dismiss(Activity context) {
        dismiss(getUid(context));
    }

    // hide
    public synchronized static void dismiss(Fragment context) {
        dismiss(getUid(context));
    }

    private synchronized static void dismiss(String uid) {
        Dialog progressDialog = map.remove(uid);
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    public synchronized static void dismissAll() {
        for (Dialog progressDialog : map.values()) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                }
            }
        }
        map.clear();
    }

    private static String getUid(Activity context) {
        String uid = "";
        if (context != null) {
            uid = "" + context.hashCode();
        }
        return "" + uid;
    }

    private static String getUid(Fragment context) {
        String uid = "";
        if (context != null) {
            uid = "" + context.getClass().getName();
        }
        return "" + uid;
    }

    public static Activity getSafeContext(Activity context) {
        Activity ctx = context;
        if (ctx != null && ctx.getParent() != null) {
            ctx = ctx.getParent();
        }
        return context;
    }

    public static Activity getSafeContext(Fragment context) {
        Activity ctx = context.getActivity();
        if (ctx != null && ctx.getParent() != null) {
            ctx = ctx.getParent();
        }
        return ctx;
    }

    private static Handler closeHandler = new Handler() {
        public void handleMessage(Message msg) {
            String uid = (String) msg.obj;
            if (uid != null) {
                dismiss(uid);
            }
        }
    };

    private static void startTimer(Activity context) {
        Message msg = closeHandler.obtainMessage();
        msg.obj = getUid(context);
        closeHandler.sendMessageDelayed(msg, TIME_OUT);
    }

    private static void startTimer(Fragment context) {
        Message msg = closeHandler.obtainMessage();
        msg.obj = getUid(context);
        closeHandler.sendMessageDelayed(msg, TIME_OUT);
    }
}
