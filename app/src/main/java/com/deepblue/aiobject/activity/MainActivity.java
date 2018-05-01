package com.deepblue.aiobject.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.deepblue.aiobject.R;
import com.deepblue.aiobject.util.BitmapUtil;
import com.deepblue.aiobject.util.ToastUtil;
import com.deepblue.aiobject.util.TransparentStatusBarUtil;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int CODE_TAKE_PHOTO = 1;
    private static final int CODE_SELECT_PHOTO = 2;
    private static final int CODE_PERMISSION_CAMERA = 3;
    private static final int CODE_PERMISSION_WRITE = 4;
    @BindView(R.id.imageView)
    ImageView mImgPhoto;
    @BindView(R.id.tvResult)
    TextView mTxtResult;
    @BindView(R.id.tv_tab_name)
    TextView mTxtTabName;
    private String imgPathName;
    private ImageClassifier classifier;

    private long mExitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TransparentStatusBarUtil.setTransparent(this);
        }
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initViews();
    }

    @Override
    public void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            ToastUtil.showShort(this, "再按一次退出应用");
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CODE_PERMISSION_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 授权
                    capturePhoto();
                } else {
                    // 未授权
                    ;
                }
                break;
            case CODE_PERMISSION_WRITE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectPhoto();
                } else {
                    ;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = null;
        switch (requestCode) {
            case CODE_TAKE_PHOTO:
                if (data == null && resultCode == RESULT_OK) {
                    ;
                }
                break;
            case CODE_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        imgPathName = handleImage(data);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
        try {
            classifier = new ImageClassifierQuantizedMobileNet(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        bitmap = BitmapUtil.getBitmapByPath(imgPathName, classifier.getImageSizeX(), classifier.getImageSizeY());
        if (bitmap != null) {
            mImgPhoto.setImageBitmap(bitmap);
            String result = classifier.classifyFrame(bitmap);
            mTxtResult.setText(result);
            classifier.close();
        }
    }

    private void selectPhoto() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CODE_SELECT_PHOTO);
    }

    private void initViews() {
        mTxtTabName.setText("识物");
    }

    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoUri = getMediaFileUri(Build.VERSION.SDK_INT);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CODE_TAKE_PHOTO);
    }

    public Uri getMediaFileUri(int sdkVersion) {
        File cacheDir = getExternalCacheDir();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        imgPathName = cacheDir + File.separator + timeStamp + ".jpg";
        if (sdkVersion >= 24) {
            return FileProvider.getUriForFile(this, "com.deepblue.aiobject.fileprovider", new File(imgPathName));

        } else {
            return Uri.fromFile(new File(imgPathName));
        }
    }

    private String handleImage(Intent data) {
        String imgPathName = null;
        if (Build.VERSION.SDK_INT >= 19) {
            Uri uri = data.getData();
            if (DocumentsContract.isDocumentUri(this, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    imgPathName = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    imgPathName = getImagePath(contentUri, null);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                imgPathName = getImagePath(uri, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                imgPathName = uri.getPath();
            }
        } else {
            Uri uri = data.getData();
            imgPathName = getImagePath(uri, null);
        }
        return imgPathName;
    }

    private String getImagePath(Uri uri, String selection) {
        String Path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return Path;
    }

    public void onCaptureClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_PERMISSION_CAMERA);
        } else {
            capturePhoto();
        }
    }

    public void onSelectPhotoClick(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_WRITE);
        } else {
            selectPhoto();
        }
    }

}
