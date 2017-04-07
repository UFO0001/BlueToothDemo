package com.demo.bluetoothdemo;

import android.content.Context;
import android.content.SharedPreferences;
import com.demo.bluetoothdemo.MyApplication;
/**
 * SharedPreferences的一个工具类，调用setParam就能保存String, Integer, Boolean, Float, Long类型的参数
 * 同样调用getParam就能获取到保存在手机里面的数据
 */

public class SharedPreferencesUtils {

    /**
     * 保存在手机里面的文件名
     */
    private static final String BULETOOTH = "buletoooth";

    public static void setDevice(int flag,String name,String address){
        SharedPreferences sp = MyApplication.getInstance().getSharedPreferences(BULETOOTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if(flag == 1) {
            editor.putBoolean("flag", true);
        }else if(flag == 0){
            editor.putBoolean("flag", false);
        }
        if(!name.equals("")){
            editor.putString("name",name);
        }
        if(!address.equals("")){
            editor.putString("address",address);
        }
        editor.commit();
    }

    public static Object getDevice(String key, Object defaultObject) {
        String type = defaultObject.getClass().getSimpleName();
        SharedPreferences sp = MyApplication.getInstance().getSharedPreferences(BULETOOTH, Context.MODE_PRIVATE);

        if ("String".equals(type)) {
            return sp.getString(key, (String) defaultObject);
        } else if ("Integer".equals(type)) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if ("Boolean".equals(type)) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if ("Float".equals(type)) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if ("Long".equals(type)) {
            return sp.getLong(key, (Long) defaultObject);
        }

        return null;
    }
}

