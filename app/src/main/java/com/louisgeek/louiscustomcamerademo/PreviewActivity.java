package com.louisgeek.louiscustomcamerademo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PreviewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_preview);


        ImageView id_iv_preview_photo = (ImageView) this.findViewById(R.id.id_iv_preview_photo);

        ImageView id_iv_cancel = (ImageView) this.findViewById(R.id.id_iv_cancel);
        ImageView id_iv_ok = (ImageView) this.findViewById(R.id.id_iv_ok);

        id_iv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreviewActivity.this.finish();
            }
        });
        id_iv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        Intent intent = this.getIntent();
        if (intent != null) {
            //byte [] bis=intent.getByteArrayExtra("bitmapByte");

            String filePath = intent.getStringExtra("filePath");
            // Toast.makeText(this, "图片加载filePath:"+filePath, Toast.LENGTH_SHORT).show();
            id_iv_preview_photo.setImageBitmap(getBitmapByUrl(filePath));
        } else {
            Toast.makeText(this, "图片加载错误", Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 根据图片路径获取本地图片的Bitmap
     *
     * @param url
     * @return
     */
    public Bitmap getBitmapByUrl(String url) {
        FileInputStream fis = null;
        Bitmap bitmap = null;
        try {
            fis = new FileInputStream(url);
            bitmap = BitmapFactory.decodeStream(fis);

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bitmap = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                fis = null;
            }
        }

        return bitmap;
    }
}
