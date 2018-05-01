package com.deepblue.aiobject.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.deepblue.aiobject.R;
import com.deepblue.aiobject.util.TransparentStatusBarUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecognizeActivity extends Activity {
    @BindView(R.id.tv_tab_name)
    TextView mTxtTabName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TransparentStatusBarUtil.setTransparent(this);
        }
        setContentView(R.layout.activity_recognize);
        ButterKnife.bind(this);
        initViews();
    }

    private void initViews() {
        mTxtTabName.setText("识别结果");
    }
}
