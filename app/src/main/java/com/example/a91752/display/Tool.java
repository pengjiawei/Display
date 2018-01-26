package com.example.a91752.display;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 91752 on 2017/8/17.
 */

public class Tool {
    public static class ViewHolder{
        TextView textView;
    }
    public static String getIP(Context context){

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }
    //自定义http请求加载网络图片
    public static Bitmap getHttpBitmap(String url) {
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
    //解析pgm图片
    public static Bitmap decodePgmBitmap(InputStream is) throws IOException {
        /***********************/
        DataInputStream in = new DataInputStream(is);
        Log.d("TAG", in.toString());
        Bitmap img;
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

        img = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        img.setPixels(pixels, 0, (int) (width), 0, 0, (int) (width * scaleRate), (int) (height * scaleRate));
        /********************************/
        Log.d("TAG", String.valueOf(img.getByteCount()));
        return img;
    }
    //附带宽高
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

            conn.setConnectTimeout(6000);//设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);//不缓存
            conn.connect();

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
            e.printStackTrace();
        }
        Log.d("TAG", String.valueOf(responseCode));
        return mapimg;
    }

    /**
     * 将int转为低字节在前，高字节在后的byte数组
     */
    public static byte[] int2byte(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }
    /**
     * 将float转为低字节在前，高字节在后的byte数组
     */
    public static byte[] float2byte(float f) {
        return int2byte(Float.floatToRawIntBits(f));
    }


    public static int byte2int(byte[] res) {
        // 一个byte数据左移24位变成0x??000000，再右移8位变成0x00??0000

        int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) // | 表示安位或
                | ((res[2] << 24) >>> 8) | (res[3] << 24);
        return targets;
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }
}
