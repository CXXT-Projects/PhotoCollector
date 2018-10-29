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
 *
 * */

package com.huangyz0918.photocollector;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;

import static com.huangyz0918.photocollector.PermissionManager.requestCameraPermissions;

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
        requestCameraPermissions(MainActivity.this);
        initViews();
        initEvents();
    }

    private void initViews() {
        dialog = new SpotsDialog.Builder().setContext(this).build();
    }

    private void initEvents() {
        // button to take a snap and save the picture into local device.
        btnSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCameraPermissions(MainActivity.this);
                if (hasSDCard()) {
                    cameraView.captureImage(new CameraKitView.ImageCallback() {
                        @Override
                        public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                            if (bytes != null) {
                                resultBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "No SD card in your device, " +
                            "we cannot save the picture to local!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // button to save the picture and wrap them and upload to the server.
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
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                String deviceFinalName = manufacturer + "-"
                        + name + "-"
                        + model + "-"
                        + androidId + "-"
                        + timeStamp
                        + ".png";

                saveAndUploadPic(deviceFinalName);
            }
        });
    }

    private void saveAndUploadPic(final String deviceFinalName) {
        if (resultBitmap == null || resultBitmap.isRecycled()) {
            Toast.makeText(MainActivity.this, "Have you forgotten taking a pic? :(", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileOutputStream out = new FileOutputStream(photoPath + deviceFinalName);
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        resultBitmap.recycle();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.btn_settings) {
            Toast.makeText(this, "Settings opened!", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

}
