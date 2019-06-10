/*
 * MIT License
 *
 * Copyright (c) 2018 CXXT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * */

package com.huangyz0918.photocollector;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.camerakit.CameraKitView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadFileListener;
import dmax.dialog.SpotsDialog;
import io.github.tonnyl.whatsnew.WhatsNew;
import io.github.tonnyl.whatsnew.item.WhatsNewItem;
import timber.log.Timber;

import static com.huangyz0918.photocollector.PermissionManager.requestNeededPermissions;

/**
 * Photo Collector
 * The application aims to collect the phone model and photos taken by the devices.
 * Can upload the message to the remote server.
 * User can set the server address vis settings in this application.
 *
 * @author huangyz0918
 * @since 10/22/2018
 * <p>
 * The {@link MainActivity} is the main interface of this application.
 */
public class MainActivity extends AppCompatActivity {
    private String photoPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/";

    @BindView(R.id.camera)
    CameraKitView cameraView;

    @BindView(R.id.btn_snap)
    Button btnSnap;

    @BindView(R.id.btn_upload)
    Button btnUpload;

    private Bitmap resultBitmap;
    private Handler handler = new Handler();
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initViews();
        requestNeededPermissions(MainActivity.this);
        initEvents();
    }

    private void initViews() {
        SharedPreferences sharedPreferences = getSharedPreferences("firstRun", 0);
        Boolean firstRun = sharedPreferences.getBoolean("first", true);
        if (firstRun) {
            sharedPreferences.edit().putBoolean("first", false).apply();
            showDialogInfo(MainActivity.this);
        }
        Bmob.initialize(this, "029fb486c5e738c9fe68b89d16739723"); // This key cannot be uploaded to the github.
        dialog = new SpotsDialog.Builder().setContext(this).build();
    }

    private void initEvents() {
        btnSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasSDCard()) {
                    requestNeededPermissions(MainActivity.this);
                    cameraView.captureImage(new CameraKitView.ImageCallback() {
                        @Override
                        public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                            if (bytes != null) {
                                resultBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                Toast.makeText(MainActivity.this,
                                        getString(R.string.success), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_no_sd_card), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInfoUpload();
            }
        });
    }

    private boolean hasSDCard() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * getDeviceId
     *
     * @return the android ID.
     */
    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    /**
     * get the device info and wrap them with the picture.
     * sent them into the specific server address.
     */
    private void getInfoUpload() {
        dialog.show();
        DeviceName.with(this).request(new DeviceName.Callback() {
            @Override
            public void onFinished(DeviceName.DeviceInfo info, Exception error) {
                String manufacturer = info.manufacturer;
                String name = info.marketName;
                String model = info.model;
                String androidId = getDeviceId();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(Calendar.getInstance().getTime());
                String deviceFinalName =
                        manufacturer + "|"
                                + name + "|"
                                + model + "|"
                                + androidId + "|"
                                + System.getProperty("os.version") + "|"
                                + android.os.Build.VERSION.SDK + "|"
                                + timeStamp
                                + ".png";

                saveAndUploadPic(
                        deviceFinalName,
                        manufacturer,
                        android.os.Build.VERSION.SDK,
                        androidId,
                        name,
                        model,
                        System.getProperty("os.version")
                );
            }
        });
    }

    /**
     * The progress is:
     * 1. get the binary data from the camera.
     * 2. wrap the binary data and compress into a .PNG file (lossless compression).
     * 3. save the picture into local machine. (takes a long time, put operations in another thread)
     * 4. upload the local picture to our server, it costs time too.
     * 5. remove the local file, finish the whole progress.
     */
    private void saveAndUploadPic(final String deviceFinalName,
                                  final String manufacturer,
                                  final String version,
                                  final String id,
                                  final String name,
                                  final String model,
                                  final String os) {

        requestNeededPermissions(MainActivity.this);
        if (resultBitmap == null || resultBitmap.isRecycled()) {
            Toast.makeText(MainActivity.this, getString(R.string.error_forget_taking), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    requestNeededPermissions(MainActivity.this);
                    try {
                        FileOutputStream out = new FileOutputStream(photoPath + deviceFinalName);
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        resultBitmap.recycle();
                        final Picture picture = new Picture();
                        final BmobFile bmobFile = new BmobFile(new File(photoPath + deviceFinalName));
                        bmobFile.upload(new UploadFileListener() {
                            @Override
                            public void done(BmobException e) {
                                if (e == null) {
                                    picture.setPicture(bmobFile);
                                    picture.setId(id);
                                    picture.setManufacturer(manufacturer);
                                    picture.setModel(model);
                                    picture.setName(name);
                                    picture.setOs(os);
                                    picture.setVersion(version);
                                    picture.save(new SaveListener<String>() {
                                        @Override
                                        public void done(String s, BmobException e) {
                                            boolean isDeleted = new File(photoPath + deviceFinalName).delete();
                                            if (e == null) {
                                                finishedUploading(getString(R.string.success) + " Are you cute? " + isDeleted);
                                            } else {
                                                finishedUploading(getString(R.string.error_failed) + isDeleted + ":" + e.getMessage());
                                            }
                                        }
                                    });
                                } else {
                                    finishedUploading(getString(R.string.error_failed) + e.getMessage());
                                }
                            }
                        });
                    } catch (IOException e) {
                        Timber.tag(getString(R.string.error_tag_io)).e(e);
                        finishedUploading(getString(R.string.error_uploading));
                    }
                }
            }).start();
        }
    }

    /**
     * finish the uploading and cancel the loading dialog from the UI thread.
     * can throw a message when finished.
     */
    private void finishedUploading(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void showDialogInfo(AppCompatActivity activity) {
        WhatsNew whatsNew = WhatsNew.newInstance(
                new WhatsNewItem(getString(R.string.title_1), getString(R.string.info_1)),
                new WhatsNewItem(getString(R.string.title_2), getString(R.string.info_2)),
                new WhatsNewItem(getString(R.string.title_3), getString(R.string.info_3)),
                new WhatsNewItem(getString(R.string.title_4), getString(R.string.info_4)),
                new WhatsNewItem(getString(R.string.title_5), getString(R.string.info_5)),
                new WhatsNewItem(getString(R.string.title_6), getString(R.string.info_6)),
                new WhatsNewItem(getString(R.string.title_7), getString(R.string.title_author) + "\n"
                        + getString(R.string.info_7))
        );
        whatsNew.setTitleText(getString(R.string.title));
        whatsNew.setButtonText(getString(R.string.go_on));
        whatsNew.setButtonTextColor(Color.parseColor(getString(R.string.color_white)));
        whatsNew.setButtonBackground(Color.parseColor(getString(R.string.color_grey)));
        whatsNew.presentAutomatically(activity);
    }

    /**
     * Menu related.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.btn_settings) {
            startActivity(new Intent(getApplicationContext(), AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestNeededPermissions(MainActivity.this);
        cameraView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestNeededPermissions(MainActivity.this);
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        cameraView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraView.onStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
