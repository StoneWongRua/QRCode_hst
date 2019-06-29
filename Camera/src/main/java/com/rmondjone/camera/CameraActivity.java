package com.rmondjone.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.newland.springdialog.AnimSpring;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import QRCode.CodeUtils;

/**
 * @author stonewong
 * @date 2019/6/29 0028 16:23
 * 注释:Android自定义相机
 */
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String KEY_IMAGE_PATH = "imagePath";
    public QRCodeActivity qrCodeActivity;
    /**
     * 相机预览
     */
    private FrameLayout mPreviewLayout;
    /**
     * 拍摄按钮视图
     */
    private RelativeLayout mPhotoLayout;
    /**
     * 确定按钮视图
     */
    private RelativeLayout mConfirmLayout;
    /**
     * 闪光灯
     */
    private ImageView mFlashButton;
    /**
     * 拍照按钮
     */
    private ImageView mPhotoButton;
    /**
     * 取消保存按钮
     */
    private ImageView mCancleSaveButton;
    /**
     * 保存按钮
     */
    private ImageView mSaveButton;
    /**
     * 聚焦视图
     */
    private OverCameraView mOverCameraView;
    /**
     * 相机类
     */
    private Camera mCamera;
    /**
     * Handle
     */
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    /**
     * 取消按钮
     */
    private Button mCancleButton;
    /**
     * 是否开启闪光灯
     */
    private boolean isFlashing;
    /**
     * 图片流暂存
     */
    private byte[] imageData;
    /**
     * 拍照标记
     */
    private boolean isTakePhoto;
    /**
     * 是否正在聚焦
     */
    private boolean isFoucing;
    /**
     * 蒙版类型
     */
    private MongolianLayerType mMongolianLayerType;
    /**
     * 蒙版图片
     */
    private ImageView mMaskImage;
    /**
     * 护照出入境蒙版
     */
    private ImageView mPassportEntryAndExitImage;
    /**
     * 提示文案容器
     */
    private RelativeLayout rlCameraTip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camre_layout);
        fitComprehensiveScreen();
        mMongolianLayerType = (MongolianLayerType) getIntent().getSerializableExtra("MongolianLayerType");
        PermissionUtils.applicationPermissions(this, new PermissionUtils.PermissionListener() {
            @Override
            public void onSuccess(Context context) {
                initView();
                setOnclickListener();
            }

            @Override
            public void onFailed(Context context) {
                if (AndPermission.hasAlwaysDeniedPermission(context, Permission.Group.CAMERA)
                        && AndPermission.hasAlwaysDeniedPermission(context, Permission.Group.STORAGE)) {
                    AndPermission.with(context).runtime().setting().start();
                }
                Toast.makeText(context, context.getString(R.string.permission_camra_storage), Toast.LENGTH_SHORT);
                finish();
            }
        }, Permission.Group.STORAGE, Permission.Group.CAMERA);
    }


    /**
     * 作者：stonewong
     * 时间：2019/6/29 0009 8:54
     * 注释：适配全面屏
     */
    private void fitComprehensiveScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
            getWindow().addFlags(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * 启动拍照界面
     *
     * @param activity
     * @param requestCode
     * @param type
     */
    public static void startMe(Activity activity, int requestCode, MongolianLayerType type) {
        Intent intent = new Intent(activity, CameraActivity.class);
        intent.putExtra("MongolianLayerType", type);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 注释：获取蒙版图片
     * 时间：2019/6/29 0004 17:19
     * 作者：stonewong
     *
     * @return
     */
    private int getMaskImage() {
        if (mMongolianLayerType == MongolianLayerType.BANK_CARD) {
            return R.mipmap.bank_card;
        } else if (mMongolianLayerType == MongolianLayerType.HK_MACAO_TAIWAN_PASSES_POSITIVE) {
            return R.mipmap.hk_macao_taiwan_passes_positive;
        } else if (mMongolianLayerType == MongolianLayerType.HK_MACAO_TAIWAN_PASSES_NEGATIVE) {
            return R.mipmap.hk_macao_taiwan_passes_negative;
        } else if (mMongolianLayerType == MongolianLayerType.IDCARD_POSITIVE) {
            return R.mipmap.idcard_positive;
        } else if (mMongolianLayerType == MongolianLayerType.IDCARD_NEGATIVE) {
            return R.mipmap.idcard_negative;
        } else if (mMongolianLayerType == MongolianLayerType.PASSPORT_PERSON_INFO) {
            return R.mipmap.passport_person_info;
        }
        return 0;
    }

    /**
     * 注释：设置监听事件
     * 时间：2019/6/29 0001 11:13
     * 作者：stonewong
     */
    private void setOnclickListener() {
        mCancleButton.setOnClickListener(this);
        mCancleSaveButton.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mPhotoButton.setOnClickListener(this);
        mSaveButton.setOnClickListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isFoucing) {
                float x = event.getX();
                float y = event.getY();
                isFoucing = true;
                if (mCamera != null && !isTakePhoto) {
                    mOverCameraView.setTouchFoucusRect(mCamera, autoFocusCallback, x, y);
                }
                mRunnable = () -> {
                    Toast.makeText(CameraActivity.this, "自动聚焦超时,请调整合适的位置拍摄！", Toast.LENGTH_SHORT);
                    isFoucing = false;
                    mOverCameraView.setFoucuing(false);
                    mOverCameraView.disDrawTouchFocusRect();
                };
                //设置聚焦超时
                mHandler.postDelayed(mRunnable, 3000);
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 注释：自动对焦回调
     * 时间：2019/6/29 0001 10:02
     * 作者：stonewong
     */
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            isFoucing = false;
            mOverCameraView.setFoucuing(false);
            mOverCameraView.disDrawTouchFocusRect();
            //停止聚焦超时回调
            mHandler.removeCallbacks(mRunnable);
        }
    };

    /**
     * 注释：拍照并保存图片到相册
     * 时间：2019/6/29 0001 15:37
     * 作者：stonewong
     */
    private void takePhoto() {
        isTakePhoto = true;
        //调用相机拍照
        mCamera.takePicture(null, null, null, (data, camera1) -> {
            //视图动画
            mPhotoLayout.setVisibility(View.GONE);
            mConfirmLayout.setVisibility(View.VISIBLE);
            AnimSpring.getInstance(mConfirmLayout).startRotateAnim(120, 360);
            imageData = data;
            //停止预览
            mCamera.stopPreview();
        });
    }

    /**
     * 注释：切换闪光灯
     * 时间：2019/6/29 0001 15:40
     * 作者：stonewong
     */
    private void switchFlash() {
        isFlashing = !isFlashing;
        mFlashButton.setImageResource(isFlashing ? R.mipmap.flash_open : R.mipmap.flash_close);
        AnimSpring.getInstance(mFlashButton).startRotateAnim(120, 360);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(isFlashing ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Toast.makeText(this, "该设备不支持闪光灯", Toast.LENGTH_SHORT);
        }
    }

    /**
     * 注释：取消保存
     * 时间：2019/6/29 0001 16:31
     * 作者：stonewong
     */
    private void cancleSavePhoto() {
        mPhotoLayout.setVisibility(View.VISIBLE);
        mConfirmLayout.setVisibility(View.GONE);
        AnimSpring.getInstance(mPhotoLayout).startRotateAnim(120, 360);
        //开始预览
        mCamera.startPreview();
        imageData = null;
        isTakePhoto = false;
    }

    /**
     * 解析拍出照片的路径
     *
     * @param data
     * @return
     */
    public static String parseResult(Intent data) {
        return data.getStringExtra(KEY_IMAGE_PATH);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cancle_button) {
            finish();
        } else if (id == R.id.take_photo_button) {
            if (!isTakePhoto) {
                takePhoto();
            }
        } else if (id == R.id.flash_button) {
            switchFlash();
        } else if (id == R.id.save_button) {
            savePhoto();
            //qrCode();
        } else if (id == R.id.cancle_save_button) {
            cancleSavePhoto();
        }
    }


    /**
     * 注释：蒙版类型
     * 时间：2019/6/29 0028 16:26
     * 作者：stonewong
     */
    public enum MongolianLayerType {
        /**
         * 护照个人信息
         */
        PASSPORT_PERSON_INFO,
        /**
         * 护照出入境
         */
        PASSPORT_ENTRY_AND_EXIT,
        /**
         * 身份证正面
         */
        IDCARD_POSITIVE,
        /**
         * 身份证反面
         */
        IDCARD_NEGATIVE,
        /**
         * 港澳通行证正面
         */
        HK_MACAO_TAIWAN_PASSES_POSITIVE,
        /**
         * 港澳通行证反面
         */
        HK_MACAO_TAIWAN_PASSES_NEGATIVE,
        /**
         * 银行卡
         */
        BANK_CARD
    }

    /**
     * 注释：初始化视图
     * 时间：2019/3/1 0001 11:12
     * 作者：stonewong
     */
    private void initView() {
        mCancleButton = findViewById(R.id.cancle_button);
        mPreviewLayout = findViewById(R.id.camera_preview_layout);
        mPhotoLayout = findViewById(R.id.ll_photo_layout);
        mConfirmLayout = findViewById(R.id.ll_confirm_layout);
        mPhotoButton = findViewById(R.id.take_photo_button);
        mCancleSaveButton = findViewById(R.id.cancle_save_button);
        mSaveButton = findViewById(R.id.save_button);
        mFlashButton = findViewById(R.id.flash_button);
        mMaskImage = findViewById(R.id.mask_img);
        rlCameraTip = findViewById(R.id.camera_tip);
        mPassportEntryAndExitImage = findViewById(R.id.passport_entry_and_exit_img);

        mCamera = Camera.open();
        CameraPreview preview = new CameraPreview(this, mCamera);
        mOverCameraView = new OverCameraView(this);
        mPreviewLayout.addView(preview);
        mPreviewLayout.addView(mOverCameraView);
        if (mMongolianLayerType == null) {
            mMaskImage.setVisibility(View.GONE);
            rlCameraTip.setVisibility(View.GONE);
            return;
        }
        //设置蒙版,护照出入境蒙版特殊处理
        if (mMongolianLayerType != MongolianLayerType.PASSPORT_ENTRY_AND_EXIT) {
            Glide.with(this).load(getMaskImage()).into(mMaskImage);
        } else {
            mMaskImage.setVisibility(View.GONE);
            mPassportEntryAndExitImage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 注释：保存图片
     * 时间：2019/6/29 0001 16:32
     * 作者：stonewong
     */
    private void savePhoto() {
        FileOutputStream fos = null;
        String cameraPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator + "Camera";
        //相册文件夹
        File cameraFolder = new File(cameraPath);
        if (!cameraFolder.exists()) {
            cameraFolder.mkdirs();
        }
        //保存的图片文件
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String imagePath = cameraFolder.getAbsolutePath() + File.separator + "IMG_" + simpleDateFormat.format(new Date()) + ".jpg";
        Toast.makeText(CameraActivity.this, "开始识别" + imagePath, Toast.LENGTH_SHORT).show(); //edit
        //com.google.zxing.Result result = qrCodeActivity.scanningImage(imagePath);//edit
        //Toast.makeText(CameraActivity.this, "识别结果" + result.getText(), Toast.LENGTH_SHORT).show();
        Toast.makeText(CameraActivity.this, "保存成功 at:" + imagePath, Toast.LENGTH_SHORT).show(); //edit
        File imageFile = new File(imagePath);


        try {
            fos = new FileOutputStream(imageFile);
            fos.write(imageData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Intent intent = new Intent();
                    intent.putExtra(KEY_IMAGE_PATH, imagePath);
                    setResult(RESULT_OK, intent);
                } catch (IOException e) {
                    setResult(RESULT_FIRST_USER);
                    e.printStackTrace();
                }
            }
            qrCode(imagePath);
            finish();

        }
    }

    private void qrCode(String filePath){
        Toast.makeText(CameraActivity.this, "新函数识别测试:" , Toast.LENGTH_SHORT).show(); //edit
        try {
            CodeUtils.analyzeBitmap(filePath, new CodeUtils.AnalyzeCallback() {
                @Override
                public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
                    Toast.makeText(CameraActivity.this, "解析结果:" + result, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onAnalyzeFailed() {
                    Toast.makeText(CameraActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        finish();

        //return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BitmapFactory(ImageIO.read(new FileInputStream(filePath))))),decodeMap).getText();
    }


}