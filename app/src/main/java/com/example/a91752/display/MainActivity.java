package com.example.a91752.display;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private PhotoView photoView;
    private TextView countTextView;
    private TextView responseCodeT;
    private int count = 1;
    private Context context;
    private Bitmap bitmap;
    private String nginxip = "192.168.31.175";
    private Handler handler;
    private Switch flag;

    private String getMapURL;
    private String setXYThetaURL;
    private Timer timer = new Timer();
    //初始化定时任务
    private TimerTask task = new getImgTask();
    private String x;
    private String y;
    private String theta;
    private String interval = "2000";
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        //初始化控件
        init();

        handler = new getPictureHandler();
        timer.schedule(task, Long.parseLong(interval), Long.parseLong(interval));
    }

    //双击图片一次，图片被放大scale=4f，双击第二次，图片放大scale=12f，双击第三次还原
    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        photoView = (PhotoView) findViewById(R.id.img);
        //设置放大的中等规模和最大规模
        photoView.setMaximumScale(12f);
        photoView.setMediumScale(4f);
        countTextView = (TextView) findViewById(R.id.countT);
        responseCodeT = (TextView) findViewById(R.id.code);
    }

    //创建菜单及其子项目
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //控制toolbar右上角的menu弹出dialog
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showInputDialog();
        } else if (id == R.id.action_setIp) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View inflate = factory.inflate(R.layout.onedialog, null);

            final EditText editTextIp = (EditText) inflate.findViewById(R.id.editTextOneDialog);

            editTextIp.requestFocus();
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

            inputDialog.setTitle("输入请求的ip地址").setView(inflate);
            editTextIp.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            editTextIp.setText(nginxip);
            inputDialog.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    nginxip = editTextIp.getText().toString();
                    Toast.makeText(getApplicationContext(), "you set the ip is " + nginxip,
                            Toast.LENGTH_LONG).show();
                }
            }).show();
        } else if (id == R.id.action_setInterval) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View inflate = factory.inflate(R.layout.onedialog, null);

            final EditText editTextIp = (EditText) inflate.findViewById(R.id.editTextOneDialog);

            editTextIp.requestFocus();
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

            inputDialog.setTitle("输入请求的间隔时间，单位ms").setView(inflate);
            editTextIp.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            inputDialog.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    interval = editTextIp.getText().toString();
                    Toast.makeText(getApplicationContext(), "you set the interval is " + interval + " ms",
                            Toast.LENGTH_LONG).show();
                    task.cancel();
                    timer.cancel();
                    timer = new Timer();
                    task = new getImgTask();
                    timer.scheduleAtFixedRate(task, Long.parseLong(interval), Long.parseLong(interval));
                }
            }).show();
        } else if (id == R.id.about) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View inflate = factory.inflate(R.layout.about_textview, null);
            final TextView Tview = (TextView) inflate.findViewById(R.id.aboutT);
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
            inputDialog.setTitle("当前请求的URL以及请求间隔").setView(inflate);
            Log.d("TAG", interval + " " + getMapURL + " " + setXYThetaURL);
            Tview.setText("Interval : " + interval + "ms" + "\n" + "MapURL : " + getMapURL + "\n" + "SetXYThetaURL : " + setXYThetaURL);
            inputDialog.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    //清理一些资源，特别注意的是isRunning来控制最后一个handleMessage
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        //之前定义的message.what
        Log.d("TAG", "activity destroy");

        task.cancel();
        timer.cancel();

        // Glide.with(getApplicationContext()).pauseRequests();
    }

    //定时器的定时任务
    class getImgTask extends TimerTask {
        @Override
        public void run() {
            Log.d("updateThreadName", Thread.currentThread().getName());
            Log.d("updateThreadId", String.valueOf(Thread.currentThread().getId()));
            getMapURL = "http://" + nginxip + ":8866/map";
            // getMapURL = "http://192.168.31.90:8080/image/map.jpeg";
            Bitmap b = null;
            //b = getHttpBitmap(getMapURL);
            try {
                //get为同步方式获取，fetch为异步
                b = Picasso.with(context).load(getMapURL)
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = b;
            handler.sendMessage(msg);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("TAG", "url = " + getMapURL);
//                    Glide.with(getApplicationContext()).load(getMapURL).asBitmap().into(new SimpleTarget<Bitmap>() {
//                        @Override
//                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//                            bitmap = resource;
//                        }
//                    });
                    if (isRunning) {
//                        float scale = photoView.getScale();
//                        int[] offset = new int[2];
//                        float[] values = new float[9];
//                        Matrix matrix = new Matrix();
//                        photoView.getSuppMatrix(matrix);
//
//                        matrix.getValues(values);
//                        // x方向上的偏移量(单位px)
//                        offset[0] = (int) values[2];
//                        // y方向上的偏移量(单位px)
//                        offset[1] = (int) values[5];
//                        Log.d("TAG","offset[0]"+offset[0]);
//                        Log.d("TAG","offset[1]"+offset[1]);
//                        Log.d("scale", String.valueOf(scale));
//                        Glide.with(getApplicationContext()).load(getMapURL).asBitmap().into(new SimpleTarget<Bitmap>() {
//                            @Override
//                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//                                bitmap = resource;
//                            }
//                        });

                        //Picasso.with(context).load(getMapURL).memoryPolicy(MemoryPolicy.NO_CACHE).into(photoView);
                        //  Log.d("TAG","setDrawable success!");
//                        Log.d("TAG","bitmap.width"+bitmap.getWidth());
                        //photoView.setImageBitmap(bitmap);
                        //photoView.setScale(scale);
                        //  photoView.setSuppMatrix(matrix);

                    }
                    countTextView.setText(String.valueOf(count));
                    count++;
                }
            });
        }
    }

    //处理附带图片的消息的handler
    class getPictureHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Bitmap b = (Bitmap) msg.obj;
            if (isRunning) {
                float scale = photoView.getScale();
                int[] offset = new int[2];
                float[] values = new float[9];
                Matrix matrix = new Matrix();
                photoView.getSuppMatrix(matrix);

                matrix.getValues(values);
                // x方向上的偏移量(单位px)
                offset[0] = (int) values[2];
                // y方向上的偏移量(单位px)
                offset[1] = (int) values[5];
                Log.d("TAG", "offset[0]" + offset[0]);
                Log.d("TAG", "offset[1]" + offset[1]);

                Log.d("scale", String.valueOf(scale));

                photoView.getSuppMatrix(matrix);

                photoView.setImageBitmap(b);

                photoView.setScale(scale);

                photoView.setSuppMatrix(matrix);
            }
            super.handleMessage(msg);
        }
    }

    //弹出dialog对话框，含有三个edittext
    private void showInputDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View inflate = factory.inflate(R.layout.dialog, null);

        final EditText editTextx = (EditText) inflate.findViewById(R.id.editTestX);
        editTextx.setHint("input your x");
        final EditText editTexty = (EditText) inflate.findViewById(R.id.editTextY);
        editTexty.setHint("input your y");
        final EditText editTextTheta = (EditText) inflate.findViewById(R.id.editTextTheta);
        editTextTheta.setHint("input your theta");
        //请求焦点并且强制弹出软键盘
        editTextx.setFocusable(true);
        editTextx.setFocusableInTouchMode(true);
        editTextx.requestFocus();

        final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

        editTextx.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        editTexty.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        editTextTheta.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        inputDialog.setTitle("输入你需要的x，y和theta").setView(inflate);

        inputDialog.setPositiveButton("confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                x = editTextx.getText().toString();
                y = editTexty.getText().toString();
                theta = editTextTheta.getText().toString();
                Log.d("x,y,theta", x + " " + y + " " + theta);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        setXYTheta(x, y, theta);
                    }
                }).start();
            }
        }).show();
    }

    //使用glide库加载网络图片
    public Bitmap getHttpBitmap(String url) {
//        Glide.with(getApplicationContext()).load(url).asBitmap().into(new SimpleTarget<Bitmap>() {
//            @Override
//            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//                bitmap = resource;
//            }
//        });
        URL myFileUrl = null;
        Bitmap bitmap = null;
        Log.d("TAG", "url = " + url);
        try {
            myFileUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) myFileUrl
                    .openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.connect();
            Log.d("TAG", "response code = " + conn.getResponseCode());
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("bitmap", String.valueOf(bitmap.getWidth()));
        return bitmap;
    }

    //请求pgm格式图片的方法
    public static Map<String, Object> getPgmImage(String url) {
        int responseCode = -10;
        Map<String, Object> mapimg = new HashMap<>();
        Bitmap img = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        DataInputStream in = null;
        try {
            //URL picurl = new URL(url+"/map");
            if (!url.contains("http")) {
                url = "http://" + url;
            }
            URL picurl = new URL(url);
            // 获得连接

            conn = (HttpURLConnection) picurl.openConnection();

            //conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setConnectTimeout(6000);//设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);//不缓存
            Log.d("method", conn.getRequestMethod());
            Log.d("request", conn.getRequestProperties().toString());
            Log.d("url", conn.getURL().toString());
            conn.connect();

            // responseCode = conn.getResponseCode();
            Log.d("TAG", String.valueOf(responseCode));
            is = conn.getInputStream();//获得图片的数据流

            /***********************/
            in = new DataInputStream(is);
            Log.d("TAG", in.toString());
            char ch0 = (char) in.readByte();
            char ch1 = (char) in.readByte();
            if (ch0 != 'P') {
                Log.i("-------------", "Not a pgm image!" + " [0]=" + ch0 + ", [1]=" + ch1);
                //System.exit(0);
            }
            if (ch1 != '2' && ch1 != '5') {
                Log.i("-------------", "Not a pgm image!" + " [0]=" + ch0 + ", [1]=" + ch1);
                //System.exit(0);
            }
            in.readByte();                  //读空格
            char c = (char) in.readByte();

            if (c == '#')                    //读注释行
            {
                do {
                    c = (char) in.readByte();
                } while ((c != '\n') && (c != '\r'));
                c = (char) in.readByte();
            }

            //读出宽度
            if (c < '0' || c > '9') {
                Log.d("TAG", "Error!");
                //System.exit(1);
            }

            int k = 0;
            do {
                k = k * 10 + c - '0';
                c = (char) in.readByte();
            } while (c >= '0' && c <= '9');
            int width = k;

            //读出高度
            c = (char) in.readByte();
            if (c < '0' || c > '9') {
                System.out.print("Errow!");
                //System.exit(1);
            }

            k = 0;
            do {
                k = k * 10 + c - '0';
                c = (char) in.readByte();
            } while (c >= '0' && c <= '9');
            int height = k;

            //读出灰度最大值
            c = (char) in.readByte();
            if (c < '0' || c > '9') {
                Log.i("Errow!", "灰度读取错误");
                //System.exit(1);
            }

            k = 0;
            do {
                k = k * 10 + c - '0';
                c = (char) in.readByte();
            } while (c >= '0' && c <= '9');
            int maxpix = k;

            int[] pixels = new int[width * height];
            Log.d("TAG", "width = " + width);
            Log.d("TAG", "height = " + height);
            for (int i = 0; i < width * width; i++) {
                int b = 0;
                try {
                    b = in.readByte();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                //Log.i("----",""+b);
                if (b < 0) b = b + 256;
                pixels[i] = (255 << 24) | (b << 16) | (b << 8) | b;
            }
            double scaleRate = 1;
            Log.d("pattern", width + " " + height);
            mapimg.put("W", width);
            mapimg.put("H", height);
            img = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
            img.setPixels(pixels, 0, (int) (width), 0, 0, (int) (width * scaleRate), (int) (height * scaleRate));
            /********************************/
            Log.d("TAG", String.valueOf(img.getByteCount()));
            mapimg.put("img", img);
            mapimg.put("responseCode", responseCode);
            is.close();
            in.close();
            conn.disconnect();
        } catch (Exception e) {
            try {
                is.close();
                in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            e.printStackTrace();
        }
        Log.d("TAG", String.valueOf(responseCode));
        return mapimg;
    }

    //设置x,y,theta的get请求
    public void setXYTheta(String x, String y, String theta) {
        setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=" + x + "&y=" + y + "&theta=" + theta;
        Log.d("geturl", setXYThetaURL);
        try {
            URL GetUrl = new URL(setXYThetaURL);
            HttpURLConnection connection = (HttpURLConnection) GetUrl.openConnection();
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(5000);
            connection.connect();
            Log.d("get response code", String.valueOf(connection.getResponseCode()));
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
