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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.zzhoujay.richtext.RichText;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This is the "About" page, which used for display the
 * essential messages and the description.
 *
 * @author huangyz0918
 * @since 11/04/2018
 */
public class AboutActivity extends AppCompatActivity {

    @BindView(R.id.textView)
    TextView textView;

    private String info = "### Photo Collector" +
            "\n" +
            "Photo Collector is a small android application aims to collect the photos and build a data base " +
            "according to different android devices.\n" +
            "\n" +
            "We **will not collect any other private information** related to you. You can checkout " +
            "our permissions which are very safe, all the pictures you collected will be uploaded to our " +
            " _bmob_ server if you click the _upload_ button in the camera page.\n " +
            "\n" +
            "I can see you as a volunteer to build a better and more " +
            "standard mobile raw pictures database, which is totally open source and free for downloading. Everybody can join us through this " +
            "application. we need your help and really appreciate your work.\n" +
            "\n" +
            "If you found any downsides about this application, please file an issue in our [Github page](https://github.com/CXXT-Projects/PhotoCollector/issues), thanks.\n" +
            "\n" +
            "All the pictures you took will be saved in the root folder inside your internal storage. You can delete them after uploading." +
            " The picture can only be uploaded right after your created it, so don't quit the application after snapping without uploading, or you cannot upload the picture any more." +
            "\n" +
            "#### The kinds of information we collect" +
            "\n" +
            "- The manufacturer." +
            "\n" +
            "- The android SDK version." +
            "\n" +
            "- The android ROM version." +
            "\n" +
            "- The name of your device." +
            "\n" +
            "- The model of your device." +
            "\n" +
            "- The android Id of your device." +
            "\n" +
            "#### About the author" +
            "\n" +
            "[@huangyz0918](https://github.com/huangyz0918)\n " +
            "\n" +
            "who comes from the Chongxin college of Shandong University, China." +
            "\n" +
            "#### Open source libraries" +
            "\n" +
            "- [butter knife](http://jakewharton.github.io/butterknife/)" +
            "\n" +
            "- [cameraKit](https://github.com/CameraKit/camerakit-android)" +
            "\n" +
            "- [spots dialog](https://github.com/d-max/spots-dialog)" +
            "\n" +
            "- [timber](https://github.com/JakeWharton/timber)" +
            "\n" +
            "- bmob-sdk" +
            "\n" +
            "- [richtext](https://github.com/zzhoujay/RichText)" +
            "\n" +
            "\n" +
            "#### Open Source License\n" +
            "\n" +
            "> MIT License\n" +
            "\n" +
            "> Copyright (c) 2018 崇新学堂\n" +
            "\n" +
            " ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initViews();
    }

    private void initViews() {
        ButterKnife.bind(this);
        RichText.fromMarkdown(info).into(textView);
    }
}
