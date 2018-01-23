package com.example.a91752.display;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.yalantis.phoenix.PullToRefreshView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
	private PhotoView photoView;
	private TextView countTextView;
	private int count = 1;
	private Context context;

	private String nginxip = "192.168.31.27";
	private String mobileWifi = "192.168.2.1";
	private Handler handler;

	private String getMapURL;
	private String setXYThetaURL = "http://" + nginxip + ":8866/goal?" + "x=?&y=?&theta=?";
	private Timer timer = new Timer();
	// 初始化定时任务
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
	private List<ScanResult> scanResults;
	String wifiPassWord = null;

	boolean listenStatus = true;
	public WifiManager wifiManager;
	ServerSocket ss = null;
	DatagramSocket logSs = null;
	Socket recvMapSocket = null;
	Socket sendXYSocket = null;
	String text = "";

	Paint frontierPaint, posePaint, currentPosePaint, planPaint;
	Path frontierPath, posePath;
	int width = 480, height = 480;
	String[] frontier;

	List<String> plan = new ArrayList<>();
	float poseX = 0, poseY = 0, startPoseX = 0, startPoseY = 0, lastPoseX = 0, lastPoseY = 0;
	private float frontierStrokeWidth = 1.0f;
	private float poseStrokeWidth = 0.5f;
	private String wifiID;

	private List<Float> trajectoryX = new ArrayList<>();
	private List<Float> trajectoryY = new ArrayList<>();

	private final OkHttpClient client = new OkHttpClient();
	int isStart = 1;

	int httpTimeout = 5; // unit seconds

	private PullToRefreshView pullToRefreshView;
	private BaseAdapter adapter;
	boolean showPlan = true;

	private String pose_s = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		// 初始化控件
		init();

		// 启动定时任务
		timer.schedule(task, Long.parseLong(interval), Long.parseLong(interval));

		// UDP 接收SeNavigation日志
		try {
			logSs = new DatagramSocket(10101);
		} catch (IOException e) {
			e.printStackTrace();
		}
		new recvLogThread().start();

	}

	// 双击图片一次，图片被放大scale=4f，双击第二次，图片放大scale=12f，双击第三次还原
	private void init() {
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		photoView = (PhotoView) findViewById(R.id.img);
		// 设置放大的中等规模和最大规模
		photoView.setMaximumScale(12f);
		photoView.setMediumScale(4f);

		countTextView = (TextView) findViewById(R.id.countT);

		handler = new getPictureHandler();
		Button showPlanBtn = (Button) findViewById(R.id.show_plan);
		showPlanBtn.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					showPlan = false;
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					showPlan = true;
				}
				return false;
			}

		});

		// 初始化画笔，绘制地图
		frontierPaint = new Paint();
		frontierPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		frontierPaint.setColor(Color.rgb(107, 209, 255));
		frontierPaint.setStrokeWidth(frontierStrokeWidth);

		posePaint = new Paint();
		posePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		posePaint.setColor(Color.RED);
		posePaint.setStrokeWidth(poseStrokeWidth);
		posePaint.setAntiAlias(true);
		posePaint.setPathEffect(new CornerPathEffect(10));
		// posePaint.setPathEffect(new CornerPathEffect(100));

		currentPosePaint = new Paint();
		currentPosePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		currentPosePaint.setColor(Color.YELLOW);
		currentPosePaint.setStrokeWidth(1f);

		planPaint = new Paint();
		planPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		planPaint.setColor(Color.BLUE);
		planPaint.setStrokeWidth(0.5f);
		planPaint.setAntiAlias(true);
		// planPaint.setPathEffect(new CornerPathEffect(50));

		frontierPath = new Path();
		posePath = new Path();

		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	}

	@Deprecated
	class SocketThread extends Thread {
		private Socket socket_;

		public SocketThread(Socket socket) {
			this.socket_ = socket;
		}

		@Override
		public void run() {
			Bitmap rebitmap;
			try {
				while (true) {
					socket_ = ss.accept();
					socket_.setKeepAlive(true);
					socket_.setTcpNoDelay(true);

					long startTime = System.currentTimeMillis();
					Log.d(TAG, "begin time = " + startTime);
					Log.d(TAG, "local port = " + socket_.getLocalPort());
					Log.d(TAG, "port = " + socket_.getPort());
					String ip = socket_.getInetAddress().getHostAddress();
					Log.d(TAG, "client ip = " + ip);
					InputStream in = socket_.getInputStream();

					rebitmap = BitmapFactory.decodeStream(in);
					if (null != rebitmap) {
						Message msg = handler.obtainMessage();
						msg.what = 0;
						msg.obj = rebitmap;
						handler.sendMessage(msg);
						long endTime = System.currentTimeMillis();
						Log.d(TAG, "end time = " + endTime);
						Log.d(TAG, "opt time = " + (endTime - startTime));
					} else {
						Log.d(TAG, "bitmap is null or bitmap is error format");
					}
					// ss.close();
					// socket.close();
					Log.d(TAG, "socket is closed? = " + socket_.isClosed());

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

	// 接收小车程序的日志
	class recvLogThread extends Thread {
		@Override
		public void run() {

			try {
				while (listenStatus) {
					Log.d(TAG, "run: recvlogThread!!!!!!!!");
					byte[] b = new byte[4096];
					DatagramPacket recv = new DatagramPacket(b, 4096);

					// logSs.setSoTimeout(2000);
					logSs.receive(recv);
					b = recv.getData();
					String tmp = new String(b, 0, recv.getLength());

					Log.d(TAG, "port = " + recv.getPort());
					Log.d(TAG, "client ip = " + recv.getAddress().getHostAddress());
					Log.d(TAG, "tmp recv data = " + tmp);
					text = text + tmp;
					Log.d(TAG, "text = " + text);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 创建菜单及其子项目
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	// 控制toolbar右上角的menu弹出dialog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_setIp) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View inflate = factory.inflate(R.layout.one_dialog, null);
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
					setXYThetaURL = "http://" + nginxip + ":8866/set?" + "x=" + x + "&y=" + y
					        + "&theta=" + theta;
					Toast.makeText(getApplicationContext(), "设置IP地址为 " + nginxip, Toast.LENGTH_LONG)
					        .show();
				}
			}).show();
		} else if (id == R.id.action_setInterval) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View inflate = factory.inflate(R.layout.one_dialog, null);

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
					Toast.makeText(getApplicationContext(),
					        "you set the interval is " + interval + " ms", Toast.LENGTH_LONG)
					        .show();
					// 用新的间隔重启任务
					task.cancel();
					timer.cancel();
					timer = new Timer();
					task = new getImgTask();
					timer.scheduleAtFixedRate(task, Long.parseLong(interval),
					        Long.parseLong(interval));
				}
			}).show();
		} else if (id == R.id.about) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View inflate = factory.inflate(R.layout.about_textview, null);
			final TextView Tview = (TextView) inflate.findViewById(R.id.textIn);
			final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
			inputDialog.setTitle("详细信息").setView(inflate);
			Log.d(TAG, interval + " " + getMapURL + " " + setXYThetaURL);
			Tview.setText("当前请求间隔 : " + interval + "ms" + "\n" + "请求的URL : " + getMapURL + "\n"
			        + "设置目标的URL : " + setXYThetaURL + "\n" + "本机IP地址 :" + Tool.getIP(context));
		} else if (id == R.id.log) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View inflate = factory.inflate(R.layout.about_textview, null);
			final TextView Tview = (TextView) inflate.findViewById(R.id.textIn);
			final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
			inputDialog.setTitle("日志").setView(inflate);
			Tview.setMovementMethod(ScrollingMovementMethod.getInstance());

			Tview.setText(text);
			Tview.setTextSize(8);
			Tview.setLines(40);
			// 实时滚动
			int offset = Tview.getLineCount() * Tview.getLineHeight();
			if (offset > Tview.getHeight()) {
				Tview.scrollTo(0, offset - Tview.getHeight());
			}

			inputDialog.show();
		} else if (id == R.id.set_wifi) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View inflate = factory.inflate(R.layout.list_view, null);
			final ListView listView = (ListView) inflate.findViewById(R.id.wifi_list_view);
			Log.d(TAG, "onOptionsItemSelected: set listview height = " + height);

			pullToRefreshView = (PullToRefreshView) inflate.findViewById(R.id.pull_to_refresh);

			pullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
				@Override
				public void onRefresh() {
					wifiManager.startScan();
					scanResults = wifiManager.getScanResults();
					Log.d(TAG, "onOptionsItemSelected: scan results size = " + scanResults.size());
					adapter.notifyDataSetChanged();
					pullToRefreshView.setRefreshing(false);
				}
			});
			final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);

			wifiManager.startScan();
			scanResults = wifiManager.getScanResults();
			adapter = new MyAdapter(inflate.getContext());
			listView.setAdapter(adapter);
			Log.d(TAG, "onOptionsItemSelected: scan results size = " + scanResults.size());
			for (int i = 0; i < scanResults.size(); ++i) {
				Log.d(TAG, "onOptionsItemSelected: i" + i + " " + scanResults.get(i).SSID);
				if (null == scanResults.get(i).SSID || "".equals(scanResults.get(i).SSID)) {
					scanResults.remove(i);
				}
			}
			inputDialog.setTitle("WIFI").setView(inflate);

			final AlertDialog dialog = inputDialog.create();

			dialog.show();
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.d(TAG, "onItemClick: id" + parent.getItemAtPosition(position));
					TextView textView = (TextView) view.findViewById(R.id.wifi_id_item);
					wifiID = String.valueOf(textView.getText());
					Log.d(TAG, "onItemClick: texteview = " + textView.getText());
					dialog.dismiss();
					showSetWIFIInfo();
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								sendWIFIInfo();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();

				}
			});
		}
		return super.onOptionsItemSelected(item);
	}

	// 显示wifi信息，供设置密码
	private void showSetWIFIInfo() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View inflate = factory.inflate(R.layout.wifi_info, null);
		final EditText wifiNameText = (EditText) inflate.findViewById(R.id.wifi_name);
		final EditText wifiPassWordText = (EditText) inflate.findViewById(R.id.wifi_password);

		final AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
		wifiNameText.setText(wifiID);
		inputDialog.setTitle("输入WIFI信息").setView(inflate);
		inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				if (wifiPassWordText.getText() != null) {
					wifiPassWord = String.valueOf(wifiPassWordText.getText());
				}
				Log.d(TAG, "onClick: wifi pass word = " + wifiPassWord);
			}
		}).show();
	}

	// 清理一些资源，特别注意的是isRunning来控制最后一个handleMessage
	@Override
	protected void onDestroy() {
		super.onDestroy();
		isRunning = false;
		task.cancel();
		timer.cancel();

		logSs.close();
		listenStatus = false;
		try {
			ss.close();
			recvMapSocket.close();
			sendXYSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Picasso.with(context).cancelRequest(photoView);
		Log.d(TAG, "activity destroy");
	}

	// 定时器的定时任务
	private class getImgTask extends TimerTask {
		@Override
		public void run() {
			Bitmap bitmap = null;
			getMapURL = "http://" + nginxip + ":8866/gmap.txt";
			String getPlanURL = "http://" + nginxip + ":8866/plan.txt";
			if (true) {
				// try {
				// get为同步方式获取，fetch为异步
				// Bitmap bitmapTmp = Picasso.with(context).load(getMapURL)
				// .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
				// .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE).get();
				getMapFileTXT(getMapURL);
				getPlanFileTXT(getPlanURL);
				Bitmap bitmapTmp = drawBitmap();
				Matrix matrix = new Matrix();
				matrix.postRotate(+90);
				// 绕中心垂直轴正方向旋转90度
				matrix.postScale(1, -1);

				bitmap = Bitmap.createBitmap(bitmapTmp, 0, 0, width, height, matrix, true);

			} else {
				bitmap = (Bitmap) Tool.getPgmImage(getMapURL).get("img");
			}
			if (null != bitmap) {
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
			} else {
				Log.d(TAG, "http get bitmap is null!");
			}

		}
	}

	// 获取地图的文件
	private void getMapFileTXT(String url) {
		URL myFileUrl = null;

		Log.d("TAG", "url = " + url);
		try {
			myFileUrl = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			// HttpURLConnection conn = (HttpURLConnection) myFileUrl
			// .openConnection();
			// conn.setConnectTimeout(5000);
			// conn.setReadTimeout(8000);
			// conn.setDoInput(true);
			// conn.setUseCaches(false);
			// conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64;
			// rv:57.0) Gecko/20100101 Firefox/57.0");
			// conn.setRequestProperty("Accept",
			// "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			// conn.setRequestProperty("Accept-Language",
			// "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
			//// conn.setRequestProperty("Cache-Control", "max-age=0");
			//// conn.setRequestProperty("Connection", "close");
			// conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
			//
			// conn.connect();
			// Log.d("TAG", "response code = " + conn.getResponseCode());
			// InputStream is = conn.getInputStream();

			Request request = new Request.Builder().url(myFileUrl).addHeader("Connection", "close")
			        .build();
			OkHttpClient client30s = client.newBuilder().readTimeout(httpTimeout, TimeUnit.SECONDS)
			        .connectTimeout(httpTimeout, TimeUnit.SECONDS)
			        .writeTimeout(httpTimeout, TimeUnit.SECONDS).retryOnConnectionFailure(true)
			        .build();
			Response response = client30s.newCall(request).execute();

			Log.d(TAG, "response code = " + response.code());
			if (response.code() != 200) {
				// reset all status
				isStart = 1;
				trajectoryX.clear();
				trajectoryY.clear();
				frontier = null;
			}
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			Headers responseHeaders = response.headers();
			for (int i = 0; i < responseHeaders.size(); i++) {
				Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
			}

			InputStream is = response.body().byteStream();

			BufferedReader reader = null;
			try {

				reader = new BufferedReader(new InputStreamReader(is));
				String tempString = null;

				// height width
				String size_s = reader.readLine();
				if (size_s == null) {
					Log.d(TAG, "readline is null");
					return;
				}
				String[] size = size_s.split(" ");
				height = Integer.parseInt(size[0]);
				width = Integer.parseInt(size[1]);
				// the size of the frontier
				int frontierSize = Integer.parseInt(reader.readLine());
				Log.d(TAG, "frontier_size = " + frontierSize);
				frontier = new String[frontierSize];
				int line = 0;
				while ((tempString = reader.readLine()) != null) {
					frontier[line] = tempString;
					if (line == frontierSize - 1)
						break;
					line++;
				}
				if (frontier == null) {
					Log.d(TAG, "getMapFileTXT: frontier is null");
					return;
				}
				pose_s = reader.readLine();
				if (pose_s == null) {
					Log.d(TAG, "getMapFileTXT: pose_s is null");
					return;
				}
				// current pose of the
				String[] pose = pose_s.split(" ");
				poseX = Float.valueOf(pose[0]);
				poseY = Float.valueOf(pose[1]);
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
			is.close();
			// conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "width = " + width + " height = " + height + " x = " + poseX + " y = " + poseY);
		Log.d(TAG, "pose_s = " + pose_s);

		if (isStart == 1 && poseX != 0 && poseY != 0) {
			startPoseX = poseX;
			startPoseY = poseY;
			lastPoseX = startPoseX;
			lastPoseY = startPoseY;
			posePath.moveTo(startPoseX, startPoseY);
			trajectoryX.add(startPoseX);
			trajectoryY.add(startPoseY);
			Log.d(TAG, "posePath move to start pose x y = " + startPoseX + " , " + startPoseY);
			isStart = 0;
		}

	}

	// 获取路径的文件
	private void getPlanFileTXT(String url) {
		plan.clear();
		URL myFileUrl = null;
		Log.d("TAG", "url = " + url);
		try {
			myFileUrl = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			// HttpURLConnection conn = (HttpURLConnection) myFileUrl
			// .openConnection();
			// conn.setConnectTimeout(5000);
			// conn.setReadTimeout(8000);
			// conn.setDoInput(true);
			// conn.setUseCaches(false);
			// conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64;
			// rv:57.0) Gecko/20100101 Firefox/57.0");
			// conn.setRequestProperty("Accept",
			// "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			// conn.setRequestProperty("Accept-Language",
			// "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
			//// conn.setRequestProperty("Cache-Control", "max-age=0");
			//// conn.setRequestProperty("Connection", "close");
			// conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
			//
			// conn.connect();
			// Log.d("TAG", "response code = " + conn.getResponseCode());
			// InputStream is = conn.getInputStream();

			Request request = new Request.Builder().url(myFileUrl).addHeader("Connection", "close")
			        .build();
			OkHttpClient client30s = client.newBuilder().readTimeout(httpTimeout, TimeUnit.SECONDS)
			        .connectTimeout(httpTimeout, TimeUnit.SECONDS)
			        .writeTimeout(httpTimeout, TimeUnit.SECONDS).retryOnConnectionFailure(true)
			        .build();
			Response response = client30s.newCall(request).execute();

			Log.d(TAG, "response code = " + response.code());

			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			Headers responseHeaders = response.headers();
			for (int i = 0; i < responseHeaders.size(); i++) {
				Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
			}

			InputStream is = response.body().byteStream();

			BufferedReader reader = null;
			try {

				reader = new BufferedReader(new InputStreamReader(is));
				String tempString = null;

				int line = 0;
				// 丢掉第一行显示的size，暂时不需要
				reader.readLine();
				while ((tempString = reader.readLine()) != null) {
					line++;
					if (line == 1) {
						continue;
					}
					plan.add(tempString);
					// Log.d(TAG, "getPlanFileTXT: plan line "+line+" "+tempString);
				}
				if (plan == null) {
					return;
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
			is.close();
			// conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Bitmap drawBitmap() {
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic);
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(bitmap);
		// canvas.drawColor(Color.rgb(20,90,160));
		if (frontier == null || frontier.length == 0 || pose_s == null) {
			return bitmap;
		}
		Log.d(TAG, "drawBitmap: frontier length = " + frontier.length);
		// 用点画边界
		for (int i = 0; i < frontier.length - 1; ++i) {
			String[] frontierPoseFirst = frontier[i].split(" ");
			canvas.drawPoint(Integer.parseInt(frontierPoseFirst[0]),
			        Integer.parseInt(frontierPoseFirst[1]), frontierPaint);
		}
		canvas.drawPoint(poseX, poseY, currentPosePaint);

		Log.d(TAG, "last pose = " + lastPoseX + " " + lastPoseY);
		Log.d(TAG, "pose = " + poseX + " " + poseY);

		int poseX_i = (int) poseX, poseY_i = (int) poseY;
		int lastPoseX_i = (int) lastPoseX, lastPoseY_i = (int) lastPoseY;
		if (poseX_i != lastPoseX_i || poseY_i != lastPoseY_i && poseX_i != 0 && poseY_i != 0) {
			trajectoryX.add(poseX);
			trajectoryY.add(poseY);
		}

		Log.d(TAG, "size of x and y = " + trajectoryX.size() + " " + trajectoryY.size());
		// 取出所有的pose，进行连线
		for (int i = 0; i < trajectoryX.size() - 1; ++i) {
			poseX = trajectoryX.get(i);
			poseY = trajectoryY.get(i);
			Float nextPoseX = trajectoryX.get(i + 1);
			Float nextPoseY = trajectoryY.get(i + 1);
			// Log.d(TAG,poseX+" ,"+poseY);
			// Log.d(TAG,"line "+poseX+" "+poseY+" to "+nextPoseX+" "+nextPoseY);
			if (poseX != 0 && poseY != 0 && nextPoseX != 0 && nextPoseY != 0) {
				canvas.drawLine(poseX, poseY, nextPoseX, nextPoseY, posePaint);
			}

		}

		if (plan != null && !plan.isEmpty() && showPlan) {
			for (int i = 0; i < plan.size(); ++i) {
				// Log.d(TAG, "plan: "+i+" = "+plan.get(i));
				String[] planString = plan.get(i).split(" ");
				canvas.drawPoint(Float.parseFloat(planString[0]), Float.parseFloat(planString[1]),
				        planPaint);
			}
		}
		Log.d(TAG, "drawBitmap: plan size = " + plan.size() + " pose size = " + trajectoryX.size());
		lastPoseX = poseX;
		lastPoseY = poseY;

		return bitmap;
	}

	// 处理附带图片的消息的handler
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

				Log.d(TAG, "scale" + String.valueOf(scale));
				photoView.getSuppMatrix(matrix);
				photoView.setImageBitmap(b);
				if (scale > photoView.getMinimumScale() && scale < photoView.getMaximumScale()) {
					photoView.setScale(scale);
				}

				photoView.setSuppMatrix(matrix);
			}
			super.handleMessage(msg);
		}
	}

	public class MyAdapter extends BaseAdapter {

		LayoutInflater inflater;

		public MyAdapter(Context context) {
			// TODO Auto-generated constructor stub
			this.inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			// return list.size();
			return scanResults.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			Tool.ViewHolder viewHolder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item, null);
				viewHolder = new Tool.ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.wifi_id_item);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (Tool.ViewHolder) convertView.getTag();
			}
			ScanResult scanResult = scanResults.get(position);
			Log.d(TAG, "getView: wifi ssid = " + scanResult.SSID);
			viewHolder.textView.setText(scanResult.SSID);
			return convertView;
		}

	}

	// 弹出dialog对话框，含有三个edittext
	@Deprecated
	private void showInputDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View inflate = factory.inflate(R.layout.xytheta_dialog, null);

		final EditText editTextx = (EditText) inflate.findViewById(R.id.editTestX);
		editTextx.setHint("input your x");
		editTextx.setText(lastX);
		final EditText editTexty = (EditText) inflate.findViewById(R.id.editTextY);
		editTexty.setHint("input your y");
		editTexty.setText(lastY);
		final EditText editTextTheta = (EditText) inflate.findViewById(R.id.editTextTheta);
		editTextTheta.setHint("input your theta");
		editTextTheta.setText(lastTheta);
		// 请求焦点并且强制弹出软键盘
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
						// try {
						setXYThetaHttp(x, y, theta);
						// } catch (IOException e) {
						// e.printStackTrace();
						// }
					}
				}).start();
			}
		}).show();
	}

	// 设置x,y,theta的get请求
	@Deprecated
	public void setXYThetaHttp(String x, String y, String theta) {
		setXYThetaURL = "http://" + nginxip + ":8866/goal?" + "x=" + x + "&y=" + y + "&theta="
		        + theta;
		Log.d(TAG, "set xytheta url = " + setXYThetaURL);
		try {
			URL GetUrl = new URL(setXYThetaURL);
			HttpURLConnection connection = (HttpURLConnection) GetUrl.openConnection();
			connection.setRequestProperty("Connection", "close");
			connection.setConnectTimeout(5000);
			connection.connect();
			Log.d(TAG, "response code = " + connection.getResponseCode());
			connection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Deprecated
	public void setXYThetaTcp(String x, String y, String theta) throws IOException {
		float xf = Float.parseFloat(x);
		float yf = Float.parseFloat(y);
		float thetaf = Float.parseFloat(theta);
		Log.d(TAG, "x = " + xf + " y = " + yf + " theta = " + thetaf);
		byte[] temp = null;
		byte[] sendBuff = new byte[12];
		// 装入x
		temp = Tool.toLH(xf);
		System.arraycopy(temp, 0, sendBuff, 0, temp.length);
		// 装入y
		temp = Tool.toLH(yf);
		System.arraycopy(temp, 0, sendBuff, 4, temp.length);
		// 装入theta
		temp = Tool.toLH(thetaf);
		System.arraycopy(temp, 0, sendBuff, 8, temp.length);

		// 新建socket
		sendXYSocket = new Socket(nginxip, 12346);
		// 强制关闭socket
		// sendXYSocket.setSoLinger(true,0);
		OutputStream out = sendXYSocket.getOutputStream();
		out.write(sendBuff);
		out.flush();
		out.close();
	}

	// 发送wifi信息到小车供小车连接指定wifi
	private void sendWIFIInfo() throws IOException {

		Socket socket = new Socket(mobileWifi, 1111);
		OutputStream outputStream = socket.getOutputStream();
		byte[] wifiNameBytes = wifiID.getBytes();
		byte[] wifiPassWordBytes = wifiPassWord.getBytes();
		Log.d(TAG, "createSocketClient: bytes of name and password = " + wifiNameBytes.length + " "
		        + wifiPassWordBytes.length);
		int byteLength = wifiNameBytes.length + wifiPassWordBytes.length;

		outputStream.write(wifiNameBytes.length);
		outputStream.write(wifiPassWordBytes.length);
		outputStream.write(wifiNameBytes);
		outputStream.write(wifiPassWordBytes);
		outputStream.flush();
		outputStream.close();
		socket.close();
		// outputStream.write();
	}

}
