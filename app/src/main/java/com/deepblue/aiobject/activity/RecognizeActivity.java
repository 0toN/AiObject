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
        initViews();
        startRecognize();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    private void initViews() {
        time = System.currentTimeMillis();
        Intent intent = getIntent();
        imgPathName = intent.getStringExtra("imgPathName");
        mTxtTabName.setText("识别结果");
        mLayoutScanning.setVisibility(View.VISIBLE);
    }

    private void startRecognize() {
        try {
            classifier = new ImageClassifierQuantizedMobileNet(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        Bitmap bitmap = BitmapUtil.getBitmapByPath(imgPathName, classifier.getImageSizeX(), classifier.getImageSizeY());
        mBitmap = BitmapFactory.decodeFile(imgPathName);
        int degree = BitmapUtil.readPictureDegree(imgPathName);
        if (degree != 0) {
            bitmap = BitmapUtil.rotaingImageView(degree, bitmap);
            mBitmap = BitmapUtil.rotaingImageView(degree, mBitmap);
        }
        if (bitmap != null) {
            mImgPhoto.setImageBitmap(mBitmap);

            int randomNum = (int) (Math.random() * 100);
            if (randomNum <= 35) {
                String result = classifier.classifyFrame(bitmap);

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
                reqFacePlusPlus(Base64Util.bitmapToBase64(bitmap));
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
}
