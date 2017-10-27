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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private PhotoView photoView;
    private TextView countTextView;
    private TextView responseCodeT;
    private int count = 1;
    private Context context;
    
    private String nginxip = "192.168.31.26";
    private Handler handler;
    private Switch aSwitch;
    private Button goalBtn;

    private String getMapURL;
    private String setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=?&y=?&theta=?";
    private Timer timer = new Timer();
    //初始化定时任务
    private TimerTask task = new getImgTask();
    private String x;
    private String y;
    private String theta;

    private String lastX;
    private String lastY;
    private String lastTheta;

    private String interval = "500";
    private boolean isRunning = true;
    private String TAG = "MainActivity";
    private int change = 0;


    ServerSocket ss = null;
    DatagramSocket logSs = null;
    Socket recvMapSocket = null;
    Socket sendXYSocket = null;
    String text = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        //初始化控件
        init();

        //启动定时任务
        timer.schedule(task, Long.parseLong(interval), Long.parseLong(interval));



        try {
            logSs = new DatagramSocket(10101);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new recvLogThread().start();
//        Log.d(TAG,"new serverSocket!!!!!!!!!");
//        new SocketThread(recvMapSocket).start();
//
//        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked){
//                    try {
//                        if(!ss.isClosed()) {
//                            ss.close();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//
//                    timer.schedule(task, Long.parseLong(interval), Long.parseLong(interval));
//                }else {
//                    if (task != null){
//                        task.cancel();
//                    }
//                    if (timer != null){
//                        timer.cancel();
//                    }
//                    try {
//                        ss = new ServerSocket(10101);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG,"new serverSocket!!!!!!!!!");
//                    new SocketThread(recvMapSocket).start();
//                }
//            }
//        });



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
//        aSwitch = (Switch) findViewById(R.id.changeFlag);
        handler = new getPictureHandler();
        goalBtn = (Button) findViewById(R.id.goal);
        goalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

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

    class recvLogThread extends Thread{
        @Override
        public void run() {

            Bitmap rebitmap = null;
            try {
                while (true) {
                    byte[] b = new byte[4096];
                    DatagramPacket recv = new DatagramPacket(b,4096);
                    logSs.receive(recv);
                    b = recv.getData();
                    String tmp = new String(b,0,recv.getLength());

                    Log.d(TAG, "port = "+recv.getPort());
                    Log.d(TAG,"client ip = "+recv.getAddress().getHostAddress());
                    Log.d(TAG,"tmp recv data = "+tmp);
                    text = text+tmp;
                    Log.d(TAG,"text = "+text);
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
//        if (id == R.id.action_settings) {
//            showInputDialog();
//        } else
        if (id == R.id.action_setIp) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View inflate = factory.inflate(R.layout.onedialog, null);

            final EditText editTextIp = (EditText) inflate.findViewById(R.id.editTextOneDialog);

            editTextIp.requestFocus();
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

            inputDialog.setTitle("输入请求的ip地址").setView(inflate);
            editTextIp.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            editTextIp.setText(nginxip);
            inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    nginxip = editTextIp.getText().toString();
                    setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=" + x + "&y=" + y + "&theta=" + theta;
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
            inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
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
            final TextView Tview = (TextView) inflate.findViewById(R.id.textIn);
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
            inputDialog.setTitle("详细信息").setView(inflate);
            Log.d(TAG, interval + " " + getMapURL + " " + setXYThetaURL);
            Tview.setText("当前请求间隔 : " + interval + "ms" + "\n" + "请求的URL : " + getMapURL + "\n" + "设置目标的URL : " + setXYThetaURL+"\n"+"本机IP地址 :"+Tool.getIP(context));
            inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        } else if(id == R.id.log){
            LayoutInflater factory = LayoutInflater.from(this);
            final View inflate = factory.inflate(R.layout.about_textview, null);
            final TextView Tview = (TextView) inflate.findViewById(R.id.textIn);
            final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
            inputDialog.setTitle("日志").setView(inflate);
            Tview.setText(text);
            inputDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    //清理一些资源，特别注意的是isRunning来控制最后一个handleMessage
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        task.cancel();
        timer.cancel();
//        try {
//            ss.close();
//            recvMapSocket.close();
//            sendXYSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //Picasso.with(context).cancelRequest(photoView);
        Log.d(TAG, "activity destroy");
    }

    //定时器的定时任务
    private class getImgTask extends TimerTask {
        @Override
        public void run() {
            Bitmap bitmap = null;
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
//                bitmap = Tool.getHttpBitmap(getMapURL);
            }else {
                bitmap = (Bitmap) Tool.getPgmImage(getMapURL).get("img");
            }
            if (null != bitmap){
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
            }else {
                Log.d(TAG, "http get bitmap is null!");
            }
           
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
        editTextx.setText(lastX);
        final EditText editTexty = (EditText) inflate.findViewById(R.id.editTextY);
        editTexty.setHint("input your y");
        editTexty.setText(lastY);
        final EditText editTextTheta = (EditText) inflate.findViewById(R.id.editTextTheta);
        editTextTheta.setHint("input your theta");
        editTextTheta.setText(lastTheta);
        //请求焦点并且强制弹出软键盘
        editTextx.setFocusable(true);
        editTextx.setFocusableInTouchMode(true);
        editTextx.requestFocus();

        final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

        editTextx.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        editTexty.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        editTextTheta.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        inputDialog.setTitle("输入你需要的x，y和theta").setView(inflate);

        inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                x = editTextx.getText().toString();
                y = editTexty.getText().toString();
                theta = editTextTheta.getText().toString();
                Log.d("x,y,theta", x + " " + y + " " + theta);
                lastX = x;
                lastY = y;
                lastTheta = theta;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        try {
                            setXYThetaHttp(x, y, theta);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }
                }).start();
            }
        }).show();
    }

    //设置x,y,theta的get请求
    public void setXYThetaHttp(String x, String y, String theta) {
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
    public void setXYThetaTcp(String x , String y,String theta) throws IOException {
        float xf = Float.parseFloat(x);
        float yf = Float.parseFloat(y);
        float thetaf = Float.parseFloat(theta);
        Log.d(TAG,"x = "+xf+" y = "+yf+" theta = "+thetaf);
        byte[] temp = null;
        byte[] sendBuff = new byte[12];
        //装入x
        temp = Tool.toLH(xf);
        System.arraycopy(temp,0,sendBuff,0,temp.length);
        //装入y
        temp = Tool.toLH(yf);
        System.arraycopy(temp,0,sendBuff,4,temp.length);
        //装入theta
        temp = Tool.toLH(thetaf);
        System.arraycopy(temp,0,sendBuff,8,temp.length);

        //新建socket
        sendXYSocket = new Socket(nginxip,12346);
        //强制关闭socket
//        sendXYSocket.setSoLinger(true,0);
        OutputStream out = sendXYSocket.getOutputStream();
        out.write(sendBuff);
        out.flush();
        out.close();
    }

}
