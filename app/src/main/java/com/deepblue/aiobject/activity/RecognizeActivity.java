package com.deepblue.aiobject.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.deepblue.aiobject.R;
import com.deepblue.aiobject.callback.JsonCallback;
import com.deepblue.aiobject.model.RecognizeResult;
import com.deepblue.aiobject.util.Base64Util;
import com.deepblue.aiobject.util.BitmapUtil;
import com.deepblue.aiobject.util.Config;
import com.deepblue.aiobject.util.ToastUtil;
import com.deepblue.aiobject.util.TransparentStatusBarUtil;
import com.deepblue.aiobject.util.Urls;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.model.Response;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecognizeActivity extends Activity {
    private static final String TAG = "RecognizeActivity";

    private static final int MSG_FPP_RECOGNIZE = 1;
    private static final int MSG_TFLITE_RECOGNIZE = 2;

    @BindView(R.id.tv_tab_name)
    TextView mTxtTabName;
    @BindView(R.id.scanningLayout)
    RelativeLayout mLayoutScanning;
    @BindView(R.id.ivPhoto)
    ImageView mImgPhoto;
    @BindView(R.id.tvResult)
    TextView mTxtResult;
    @BindView(R.id.tvConfidence)
    TextView mTxtConfidence;
    @BindView(R.id.ivBlur)
    ImageView mImgBlur;

    private Bitmap mBitmap;
    private String imgPathName;
    private ImageClassifier classifier;
    private RecognizeResult recognizeResult;
    private boolean recognizeSuccess = false;

    private long time = 0;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FPP_RECOGNIZE:
                    recognizeResult = (RecognizeResult) msg.obj;
                    if (recognizeResult != null) {
                        recognizeSuccess = true;
                        Log.d(TAG, "result = " + recognizeResult.toString());

                        List<RecognizeResult.Object> objects = recognizeResult.getObjects();
                        if (objects != null && !objects.isEmpty()) {
                            RecognizeResult.Object object = objects.get(0);
                            mTxtResult.setText(object.getValue());
                            mTxtConfidence.setText(object.getConfidence() + "%");
                        }
                    } else {
                        recognizeSuccess = false;
                    }
                    break;
                case MSG_TFLITE_RECOGNIZE:
                    String result = (String) msg.obj;
                    Log.d(TAG, "result = " + result);

                    String[] thereResults = result.split(",");
                    if (thereResults != null && thereResults.length >= 1) {
                        String[] firsrResult = thereResults[0].split(":");
                        if (Float.valueOf(firsrResult[1]) < 0.2) {
                            recognizeSuccess = false;
                            return;
                        } else {
                            recognizeSuccess = true;
                            String confidence = Float.valueOf(firsrResult[1]) * 100 + "%";
                            mTxtResult.setText(firsrResult[0]);
                            mTxtConfidence.setText(confidence);
                        }
                    } else {
                        recognizeSuccess = false;
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
            if (recognizeSuccess) {
                mLayoutScanning.setVisibility(View.GONE);
            } else {
                ToastUtil.showLong(getApplicationContext(), "识别失败！");
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TransparentStatusBarUtil.setTransparent(this);
        }
        setContentView(R.layout.activity_recognize);
        ButterKnife.bind(this);
        init();
        startRecognize();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    private void init() {
        time = System.currentTimeMillis();
        Intent intent = getIntent();
        imgPathName = intent.getStringExtra("imgPathName");
        mTxtTabName.setText("识别结果");
    }

    private void startRecognize() {
        try {
            classifier = new ImageClassifierQuantizedMobileNet(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        Bitmap tfliteBitmap = BitmapUtil.getBitmapByPath(imgPathName, classifier.getImageSizeX(), classifier.getImageSizeY());
        Bitmap blurBitmap = BitmapFactory.decodeFile(imgPathName);
        mBitmap = BitmapFactory.decodeFile(imgPathName);
        int degree = BitmapUtil.readPictureDegree(imgPathName);
        if (degree != 0) {
            tfliteBitmap = BitmapUtil.rotaingImageView(degree, tfliteBitmap);
            blurBitmap = BitmapUtil.rotaingImageView(degree, blurBitmap);
            mBitmap = BitmapUtil.rotaingImageView(degree, mBitmap);
        }
        if (mBitmap != null) {
            mImgPhoto.setImageBitmap(mBitmap);
            blurBitmap = blur(blurBitmap, 25F);
            mImgBlur.setImageBitmap(blurBitmap);

            int randomNum = (int) (Math.random() * 100);
            if (randomNum <= 35) {
                String result = classifier.classifyFrame(tfliteBitmap);

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_TFLITE_RECOGNIZE;
                msg.obj = result;
                long gapTime = System.currentTimeMillis() - time;
                if (gapTime < 2000) {
                    mHandler.sendMessageDelayed(msg, 2000 - gapTime);
                } else {
                    mHandler.sendMessage(msg);
                }
            } else {
                reqFacePlusPlus(Base64Util.bitmapToBase64(tfliteBitmap));
            }
        }
    }

    private void reqFacePlusPlus(String imgBase64) {
        HttpParams params = new HttpParams();
        params.put("api_key", Config.API_KEY);
        params.put("api_secret", Config.API_SECRET);
        params.put("image_base64", imgBase64);
        OkGo.<RecognizeResult>post(Urls.DETECT_SCENE_OBJECT).params(params).execute(new JsonCallback<RecognizeResult>(RecognizeResult.class) {
            @Override
            public void onSuccess(Response<RecognizeResult> response) {
                Message msg = mHandler.obtainMessage();
                msg.what = MSG_FPP_RECOGNIZE;
                msg.obj = response.body();
                long gapTime = System.currentTimeMillis() - time;
                if (gapTime < 2000) {
                    mHandler.sendMessageDelayed(msg, 2000 - gapTime);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        });
    }

    private Bitmap blur(Bitmap bitmap, float radius) {
        // 构建一个RenderScript对象
        RenderScript rs = RenderScript.create(this);
        // 创建高斯模糊脚本
        ScriptIntrinsicBlur gaussianBlue = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // 创建用于输入的脚本类型
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        // 创建用于输出的脚本类型
        Allocation allOut = Allocation.createFromBitmap(rs, bitmap);
        // 设置模糊半径，范围0f<radius<=25f
        gaussianBlue.setRadius(radius);
        // 设置输入脚本类型
        gaussianBlue.setInput(allIn);
        // 执行高斯模糊算法，并将结果填入输出脚本类型中
        gaussianBlue.forEach(allOut);
        // 将输出内存编码为Bitmap，图片大小必须注意
        allOut.copyTo(bitmap);
        // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
        rs.destroy();
        return bitmap;
    }
}
