package com.deepblue.aiobject.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.IOException;

/**
 * @author Created by XWM on 2018-4-30.
 */
public class BitmapUtil {

    /**
     * 读取图片旋转的角度
     *
     * @param filename
     * @return
     */
    public static int readPictureDegree(String filename) {
        int rotate = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(filename);
            int result = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (result) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (resizedBitmap != bitmap && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return resizedBitmap;
    }

    public static Bitmap setBitmapSize(Bitmap bitmap, int newWidth, int newHeight) {
        // 获得图片的宽高.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算缩放比例.
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数.
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片.
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newBitmap;
    }

    public static Bitmap getBitmapByPath(String pathName, int width, int height) {
        Bitmap bitmap = BitmapFactory.decodeFile(pathName);
        bitmap = setBitmapSize(bitmap, width, height);
        int degree = readPictureDegree(pathName);
        if (degree != 0) {
            bitmap = BitmapUtil.rotaingImageView(degree, bitmap);
        }
        return bitmap;
    }
}
