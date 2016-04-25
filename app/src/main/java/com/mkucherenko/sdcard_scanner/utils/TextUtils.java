package com.mkucherenko.sdcard_scanner.utils;

import android.content.Context;

import com.mkucherenko.sdcard_scanner.R;

/**
 * Created by Zim on 4/24/2016.
 */
public class TextUtils {

    private static long KILOBYTE = 1024;
    private static long MEGABYTE = 1024 * KILOBYTE;
    private static long GIGABYTE = 1024 * MEGABYTE;

    public static String formatFileSize(Context context, double fileSize){
        String result = null;
        double resultValue = fileSize;
        int resultFormat = -1;
        if (fileSize < KILOBYTE){
            resultValue = fileSize;
            result = context.getString(R.string.format_byte, resultValue);
        }else if(fileSize >= KILOBYTE && fileSize < MEGABYTE){
            resultValue = fileSize / KILOBYTE;
            result = context.getString(R.string.format_kilobyte, resultValue);
        }else if(fileSize >= MEGABYTE && fileSize < GIGABYTE){
            resultValue = fileSize / MEGABYTE;
            result = context.getString(R.string.format_megabyte, resultValue);
        }else {
            resultValue = fileSize / GIGABYTE;
            result = context.getString(R.string.format_gigabyte, resultValue);
        }
        return result;
    }
}
