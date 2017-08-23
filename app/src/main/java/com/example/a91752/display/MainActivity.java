package com.example.a91752.display;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
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
    private String nginxip = "192.168.31.174";
    private Handler handler;

    private String getMapURL;
    private String setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=?&y=?&theta=?";
    private Timer timer = new Timer();
    //初始化定时任务
    private TimerTask task = new getImgTask();
    private String x;
    private String y;
    private String theta;
    private String interval = "2000";
    private boolean isRunning = true;
    private String TAG = "MainActivity";
    private int change = 1;


    ServerSocket ss = null;
    Socket socket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeakCanary.install(getApplication());
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        //初始化控件
        init();

        //启动定时任务
        //timer.schedule(task, Long.parseLong(interval), Long.parseLong(interval));


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
        handler = new getPictureHandler();


        try {
            ss = new ServerSocket(10101);

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"new serverSocket!!!!!!!!!");

            new SocketThread(socket).start();
        }

    class SocketThread extends Thread{
        private Socket socket_;
        public SocketThread(Socket socket) {
            this.socket_ = socket;
        }
        @Override
        public void run() {

            Bitmap rebitmap = null;
            try {

                while (true) {

                    socket_ = ss.accept();
                    socket_.setKeepAlive(true);
                    socket_.setTcpNoDelay(true);

                    long startTime = System.currentTimeMillis();
                    Log.d(TAG,"begin time = "+startTime);
                    Log.d(TAG,"local port = "+socket_.getLocalPort());
                    Log.d(TAG, "port = "+socket_.getPort());
                    String ip = socket_.getInetAddress().getHostAddress();
                    Log.d(TAG,"client ip = "+ip);
                    InputStream in = socket_.getInputStream();

                    rebitmap = BitmapFactory.decodeStream(in);
                    if (null != rebitmap){
                        Message msg = handler.obtainMessage();
                        msg.what = 0;
                        msg.obj = rebitmap;
                        handler.sendMessage(msg);
                        long endTime = System.currentTimeMillis();
                        Log.d(TAG,"end time = "+endTime);
                        Log.d(TAG,"opt time = "+(endTime-startTime));
                    }else {
                        Log.d(TAG, "bitmap is null or bitmap is error format");
                    }
//                    ss.close();
//                    socket.close();
                    Log.d(TAG,"socket is closed? = "+socket_.isClosed());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            countTextView.setText(String.valueOf(count));
                            count++;
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
            editTextIp.setText(interval);
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
            Log.d(TAG, interval + " " + getMapURL + " " + setXYThetaURL);
            Tview.setText("Interval : " + interval + "ms" + "\n" + "MapURL : " + getMapURL + "\n" + "SetXYThetaURL : " + setXYThetaURL+"\n"+"local ip :"+Tool.getIP(context));
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
//        task.cancel();
//        timer.cancel();
        try {
            ss.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Picasso.with(context).cancelRequest(photoView);
        Log.d(TAG, "activity destroy");
    }

    //定时器的定时任务
    private class getImgTask extends TimerTask {
        @Override
        public void run() {
            getMapURL = "http://" + nginxip + ":8866/map";

            if (change % 2 == 0){
                try {
                    //get为同步方式获取，fetch为异步
                    bitmap = Picasso.with(context).load(getMapURL)
                            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                            .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                bitmap = (Bitmap) Tool.getPgmImage(getMapURL).get("img");
            }
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = bitmap;
            handler.sendMessage(msg);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "getMapUrl = " + getMapURL);
                    countTextView.setText(String.valueOf(count));
                    count++;
                }
            });
        }
    }

    //处理附带图片的消息的handler
    private class getPictureHandler extends Handler {

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
                Log.d(TAG, "offset[0]" + offset[0]);
                Log.d(TAG, "offset[1]" + offset[1]);

                Log.d(TAG, "scale"+String.valueOf(scale));
                photoView.getSuppMatrix(matrix);
                photoView.setImageBitmap(b);
                if (scale > photoView.getMinimumScale()&& scale < photoView.getMaximumScale()){
                    photoView.setScale(scale);
                }

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

    //设置x,y,theta的get请求
    public void setXYTheta(String x, String y, String theta) {
        setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=" + x + "&y=" + y + "&theta=" + theta;
        Log.d(TAG, "set xytheta url = "+setXYThetaURL);
        try {
            URL GetUrl = new URL(setXYThetaURL);
            HttpURLConnection connection = (HttpURLConnection) GetUrl.openConnection();
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(5000);
            connection.connect();
            Log.d(TAG, "response code = "+connection.getResponseCode());
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
