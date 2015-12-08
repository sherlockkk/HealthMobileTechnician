package com.alpha.healthmobiletechnician;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alpha.healthmobiletechnican.R;
import com.igexin.sdk.PushManager;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.media.QQShareContent;
import com.umeng.socialize.media.QZoneShareContent;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.sso.QZoneSsoHandler;
import com.umeng.socialize.sso.UMQQSsoHandler;
import com.umeng.socialize.weixin.controller.UMWXHandler;
import com.umeng.socialize.weixin.media.CircleShareContent;
import com.umeng.socialize.weixin.media.WeiXinShareContent;

public class MainActivity extends Activity {
	private WebView mWebView;
	ValueCallback<Uri> mUploadMessage;
	public static final int FILECHOOSER_RESULTCODE = 1;
	private static final int REQ_CAMERA = FILECHOOSER_RESULTCODE + 1;
	private static final int REQ_CHOOSE = REQ_CAMERA + 1;
	long waitTime = 2000;
	long touchTime = 0;
	private File mPhotoFile;
	// 链接地址
	// http://zrys.code8086.com/HealthMobile/www_technician/index.html
	private String web_url = Config.HOST_URL;

	// 社会化分享
	final UMSocialService mController = UMServiceFactory
			.getUMSocialService("com.umeng.share");

	UpdataInfo info = new UpdataInfo();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		init();
		initSetting();

		// mController.getConfig().setPlatforms(SHARE_MEDIA.QQ,
		// SHARE_MEDIA.QZONE,
		// SHARE_MEDIA.WEIXIN, SHARE_MEDIA.WEIXIN_CIRCLE);

		PushManager.getInstance().initialize(this.getApplicationContext());

		new Thread() {
			@Override
			public void run() {
				// 需要在线程执行的方法
				try {
					InputStream is = getXml(); // 获取xml内容
					getUpdataInfo(is); // 调用解析方法
					serverVersion = info.getVersion(); // 获得服务器版本
					Log.i("cc",
							"check--infoVersion=" + info.getVersion()
									+ "infoURL=" + info.getUrl() + "infoAbout="
									+ info.getAbout());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// 给handler发送一个消息
				mHandler.sendEmptyMessage(new Message().what = 1);
			}
		}.start();
		mWebView = (WebView) findViewById(R.id.web_view);

		// 配置WebView的基本属性
		// initSetting();
		WebSettings webSettings = mWebView.getSettings();

		// 使能JavaScript
		webSettings.setJavaScriptEnabled(true);

		// 支持中文，否则页面中中文显示乱码
		webSettings.setDefaultTextEncodingName("GBK");

		// 限制在WebView中打开网页，而不用默认浏览器
		// mWebView.setWebViewClient(new WebViewClient());

		// 用JavaScript调用Android函数：
		// 先建立桥梁类，将要调用的Android代码写入桥梁类的public函数
		// 绑定桥梁类和WebView中运行的JavaScript代码
		// 将一个对象起一个别名传入，在JS代码中用这个别名代替这个对象，可以调用这个对象的一些方法
		mWebView.addJavascriptInterface(new WebAppInterface(this),
				"myInterfaceName");

		mWebView.loadUrl(web_url);

	}

	// 调用获得和解析xml的方法，（异步或线程中操作）；

	// Handler消息接收机制
	private Handler mHandler = new Handler() {
		// Handler接收到相应消息进行刷新ui等操作
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 1) {
				// 收到消息，在此进行ui相关操作，如将解析的内容显示出来。
				Log.i("cc", "--检查版本...--");
				checkVersion();
			}
		}
	};

	/**
	 * 自定义的Android代码和JavaScript代码之间的桥梁类
	 * 
	 * @author 1
	 * 
	 */
	public class WebAppInterface {
		Context mContext;

		/** Instantiate the interface and set the context */
		WebAppInterface(Context c) {
			mContext = c;
		}

		// 如果target 大于等于API 17，则需要加上如下注解
		@JavascriptInterface
		public void call(String phoneCode) {

			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ phoneCode));
			startActivity(intent);
		}

		/**
		 * 分享
		 * 
		 * @param view
		 */
		@JavascriptInterface
		public void onClickShare() {
			mController.openShare(MainActivity.this, false);
			mController.getConfig().setPlatforms(SHARE_MEDIA.QQ,
					SHARE_MEDIA.QZONE, SHARE_MEDIA.WEIXIN,
					SHARE_MEDIA.WEIXIN_CIRCLE);
			ssoConfig();
			QZoneShareContent();
			QQShareContent();
			WXShareContent();
			WXCircleShareContent();
		}
	}

	/*
	 * 各平台SSO免登陆配置
	 */
	private void ssoConfig() {
		// TODO Auto-generated method stub
		// 参数1为当前Activity，参数2为开发者在QQ互联申请的APP ID，参数3为开发者在QQ互联申请的APP kEY.
		UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(MainActivity.this,
				Config.QQ_APPID, Config.QQ_APPSECRET);
		qqSsoHandler.addToSocialSDK();
		// 参数1为当前Activity，参数2为开发者在QQ互联申请的APP ID，参数3为开发者在QQ互联申请的APP kEY.
		QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(
				MainActivity.this, Config.QQ_APPID, Config.QQ_APPSECRET);
		qZoneSsoHandler.addToSocialSDK();
		// 添加微信平台
		UMWXHandler wxHandler = new UMWXHandler(MainActivity.this,
				Config.WX_APPID, Config.WX_APPSECRET);
		wxHandler.addToSocialSDK();
		// 添加微信朋友圈
		UMWXHandler wxCircleHandler = new UMWXHandler(MainActivity.this,
				Config.WX_APPID, Config.WX_APPSECRET);
		wxCircleHandler.setToCircle(true);
		wxCircleHandler.addToSocialSDK();
	}

	private void QZoneShareContent() {
		// TODO Auto-generated method stub
		QZoneShareContent qzoneShare = new QZoneShareContent();
		qzoneShare.setTargetUrl(Config.SHARE_URL);
		qzoneShare.setShareContent(Config.SHARE_TEXT);
		qzoneShare.setTitle(Config.SHARE_TITLE);
		mController.setShareMedia(qzoneShare);
	}

	private void QQShareContent() {
		// TODO Auto-generated method stub
		QQShareContent qqShareContent = new QQShareContent();
		qqShareContent.setShareContent(Config.SHARE_TEXT + Config.SHARE_URL);
		qqShareContent.setTargetUrl(Config.SHARE_URL);
		qqShareContent.setTitle(Config.SHARE_TITLE);
		// qqShareContent.setShareImage(new
		// UMImage(MainActivity.this,R.drawable.ic_launcher));
		mController.setShareMedia(qqShareContent);
	}

	private void WXShareContent() {
		// TODO Auto-generated method stub
		WeiXinShareContent wxShareContent = new WeiXinShareContent();
		wxShareContent.setShareContent(Config.SHARE_TEXT + Config.SHARE_URL);
		wxShareContent.setTargetUrl(Config.SHARE_URL);
		wxShareContent.setTitle(Config.SHARE_TITLE);
		wxShareContent.setShareImage(new UMImage(MainActivity.this,
				R.drawable.ic_launcher));
		mController.setShareMedia(wxShareContent);
	}

	private void WXCircleShareContent() {
		// TODO Auto-generated method stub
		CircleShareContent wxCircleShareContent = new CircleShareContent();
		wxCircleShareContent.setShareContent(Config.SHARE_TEXT
				+ Config.SHARE_URL);
		wxCircleShareContent.setTargetUrl(Config.SHARE_URL);
		wxCircleShareContent.setTitle(Config.SHARE_TITLE);
		wxCircleShareContent.setShareImage(new UMImage(MainActivity.this,
				R.drawable.ic_launcher));
		mController.setShareMedia(wxCircleShareContent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.stopLoading();
		mWebView.clearView();
		mWebView.clearCache(true);
		mWebView.clearAnimation();
		mWebView.clearHistory();
		mWebView.clearMatches();
		mWebView.clearSslPreferences();
		mWebView.destroyDrawingCache();
		mWebView.destroy();
		mWebView.setEnabled(false);
	}

	// 实例化控件
	private void init() {
		mWebView = (WebView) findViewById(R.id.web_view);

		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// TODO Auto-generated method stub
				view.loadUrl(url);
				return true;
			}
		});
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& KeyEvent.KEYCODE_BACK == keyCode) {
			long currentTime = System.currentTimeMillis();
			if (mWebView.canGoBack()
					&& !mWebView
							.getUrl()
							.equals("http://zrys.code8086.com/HealthMobile/www_technician/rgister_index.html")) {
				mWebView.goBack();
			} else if ((currentTime - touchTime) >= waitTime) {
				// 让Toast的显示时间和等待时间相同
				Toast.makeText(this, "再按一次退出", (int) waitTime).show();
				touchTime = currentTime;
			} else {
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// // TODO Auto-generated method stub
	// if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
	// // 返回键的事件处理，如弹出确定退出框
	// dialog_Exit(MainActivity.this);
	// return false;
	// }
	// return super.onKeyDown(keyCode, event);
	// }
	//
	// public static void dialog_Exit(Context context) {
	// AlertDialog.Builder builder = new Builder(context);
	// builder.setMessage("确定要退出吗?");
	// builder.setTitle("提示");
	// builder.setIcon(android.R.drawable.ic_dialog_alert);
	// builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int which) {
	// dialog.dismiss();
	// android.os.Process.killProcess(android.os.Process.myPid());
	// }
	// });
	//
	// builder.setNegativeButton("取消",
	// new android.content.DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int which) {
	// dialog.dismiss();
	// }
	// });
	//
	// builder.create().show();
	// }

	// 配置webview的基本属性
	private void initSetting() {
		// 关闭滑动条
		mWebView.setVerticalScrollBarEnabled(true);
		// 设置控制器
		mWebView.setWebChromeClient(new PAWebChromeClient());
		// 配置 WebView属性，开启离线缓存等等
		WebSettings settings = mWebView.getSettings();
		settings.setLoadWithOverviewMode(true);

		String cacheDir = this.getApplicationContext()
				.getDir("cache", Context.MODE_PRIVATE).getPath();
		settings.setAppCachePath(cacheDir);

		settings.setAllowFileAccess(true);
		settings.setCacheMode(WebSettings.LOAD_DEFAULT);
		settings.setAppCacheMaxSize(1024 * 1024 * 8);
		settings.setDomStorageEnabled(true);
		settings.setAppCacheEnabled(true);
		settings.setJavaScriptEnabled(true);
		settings.setDatabaseEnabled(true);

		// 关闭缩放功能
		settings.setSupportZoom(false);

		CookieSyncManager.createInstance(MainActivity.this);

		mWebView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				// 监听下载功能，当用户点击下载链接的时候，直接调用系统的浏览器来下载
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});
	}

	class PAWebChromeClient extends WebChromeClient {
		private WebView newWebView = null;

		@Override
		public void onReachedMaxAppCacheSize(long spaceNeeded,
				long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
			quotaUpdater.updateQuota(spaceNeeded * 2);
		}

		public void onExceededDatabaseQuota(String url,
				String databaseIdentifier, long currentQuota,
				long estimatedSize, long totalUsedQuota,
				WebStorage.QuotaUpdater quotaUpdater) {

			quotaUpdater.updateQuota(estimatedSize * 2);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message,
				final JsResult result) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					view.getContext());
			builder.setMessage(message)
					.setPositiveButton("确定", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							result.confirm();
						}
					}).create().show();

			return true;
		}

		@Override
		public boolean onJsConfirm(WebView view, String url, String message,
				final JsResult result) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					view.getContext());
			builder.setMessage(message)
					.setPositiveButton("确定", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							result.confirm();
						}
					}).setNeutralButton("取消", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							result.cancel();
						}
					}).create().show();
			return true;
		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog,
				boolean isUserGesture, Message resultMsg) {
			newWebView = new WebView(view.getContext());
			view.addView(newWebView);
			WebSettings settings = newWebView.getSettings();
			settings.setJavaScriptEnabled(true);
			newWebView.setWebViewClient(new WebViewClient());
			newWebView.setWebChromeClient(this);
			WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
			transport.setWebView(newWebView);
			resultMsg.sendToTarget();
			return true;
		}

		@Override
		public void onCloseWindow(WebView view) {

			if (newWebView != null) {
				newWebView.setVisibility(View.GONE);
				view.removeView(newWebView);
			}
		}

		/**
		 * 实现文件上传
		 */
		// For Android 3.0+
		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType) {
			// if (mUploadMessage != null)
			// return;
			// mUploadMessage = uploadMsg;
			if (mUploadMessage != null)
				return;
			mUploadMessage = uploadMsg;
			selectImage();
		}

		// For Android < 3.0
		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
			openFileChooser(uploadMsg, "");
		}

		// For Android > 4.1.1
		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType, String capture) {
			openFileChooser(uploadMsg, acceptType);
		}

	}

	/**
	 * 检查更新版本
	 */
	private int serverVersion;

	public void checkVersion() {

		Log.i("ac", "------------");
		if (Config.localVersion < serverVersion) {
			Log.i("checkversion", "==============================");
			// 发现新版本，提示用户更新
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("软件升级")
					.setMessage("发现新版本,建议立即更新使用.")
					.setPositiveButton("更新",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// 开启更新服务UpdateService
									// 这里为了把update更好模块化，可以传一些updateService依赖的值
									// 如布局ID，资源ID，动态获取的标题,这里以app_name为例

									Intent intent = new Intent();
									intent.setAction("android.intent.action.VIEW");
									Uri content_url = Uri.parse(info.getUrl());
									intent.setData(content_url);
									startActivity(intent);
									// startService(updateIntent);

								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
			alert.create().show();
		} else {
			// 清理工作
			// cheanUpdateFile()
		}
	}

	public UpdataInfo getUpdataInfo(InputStream is) throws Exception {

		UpdataInfo info = null;
		XmlPullParser parser = Xml.newPullParser();
		Log.i("cc", "--getUpdataInfo--");
		parser.setInput(is, "UTF-8");// 设置解析的数据源，编码格式
		int event = parser.getEventType();
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_DOCUMENT: // 开始解析
				// 可在此做初始化相关工作
				info = new UpdataInfo();
				Log.i("UpdatePullParser", "--START_DOCUMENT--");
				break;
			case XmlPullParser.START_TAG:
				Log.i("UpdatePullParser", "--START_TAG--");
				String tag = parser.getName();
				if ("version".equals(tag)) {
					info.setVersion(new Integer(parser.nextText())); // 获取版本号
				} else if ("url".equals(tag)) {
					info.setUrl(parser.nextText()); // 获取url地址
				} else if ("about".equals(tag)) {
					info.setAbout(parser.nextText()); // 获取相关信息
				}
				break;
			case XmlPullParser.END_TAG:// 读完一个元素，如有多个元素，存入容器中
				break;
			default:
				break;
			}
			event = parser.next();
		}
		return info; // 返回一个UpdataInfo实体
	}

	public InputStream getXml() throws Exception {

		String httpUrl = Config.UPDATE_URL;// 服务器下存放apk信息的xml文件
		Log.i("cc", "--  getXml  Ready!!!  --");

		HttpURLConnection conn = (HttpURLConnection) new URL(httpUrl)
				.openConnection();

		Log.i("cc", "--连接服务器中...--");
		conn.setReadTimeout(10 * 1000); // 设置连接超时的时间
		// conn.setRequestMethod("GET");
		conn.connect(); // 开始连接
		Log.i("cc", "--开始连接--");

		if (conn.getResponseCode() == 200) {
			InputStream is = conn.getInputStream();
			Log.i("cc", "--连接服务器成功--");
			return is; // 返回InputStream
		} else {
			Log.i("cc", "---连接失败,即将断开连接---");
		}
		conn.disconnect(); // 断开连接
		return null;
	}

	/**
	 * 检查SD卡是否存在
	 * 
	 * @return
	 */
	public final boolean checkSDcard() {
		boolean flag = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (!flag) {
			Toast.makeText(this, "请插入手机存储卡再使用本功能", Toast.LENGTH_SHORT).show();
		}
		return flag;
	}

	String compressPath = "";

	protected final void selectImage() {
		if (!checkSDcard())
			return;
		String[] selectPicTypeStr = { "相机", "图片" };
		new AlertDialog.Builder(this)
				.setOnCancelListener(new ReOnCancelListener())
				.setItems(selectPicTypeStr,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								// 相机拍摄
								case 0:
									openCarcme();
									break;
								// 手机相册
								case 1:
									chosePic();
									break;
								default:
									break;
								}
								compressPath = Environment
										.getExternalStorageDirectory()
										.getPath()
										+ "/zrys_wmp/temp";
								new File(compressPath).mkdirs();
								compressPath = compressPath + File.separator
										+ "compress.jpg";
							}
						}).show();
	}

	String imagePaths;
	Uri cameraUri;

	/**
	 * 打开照相机
	 */
	private void openCarcme() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		imagePaths = Environment.getExternalStorageDirectory().getPath()
				+ "/DICM/camrea/" + (System.currentTimeMillis() + ".jpg");
		// 必须确保文件夹路径存在，否则拍照后无法完成回调
		File vFile = new File(imagePaths);
		if (!vFile.exists()) {
			File vDirPath = vFile.getParentFile();
			vDirPath.mkdirs();
		} else {
			if (vFile.exists()) {
				vFile.delete();
			}
		}
		cameraUri = Uri.fromFile(vFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
		startActivityForResult(intent, REQ_CAMERA);
	}

	/**
	 * 拍照结束后
	 */
	private void afterOpenCamera() {
		File f = new File(imagePaths);
		addImageGallery(f);
		File newFile = FileUtils.compressFile(f.getPath(), compressPath);
		Log.i("cc ", "path=" + f.getPath());
	}

	/** 解决拍照后在相册中找不到的问题 */
	private void addImageGallery(File file) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	/**
	 * 本地相册选择图片
	 */
	private void chosePic() {
		FileUtils.delFile(compressPath);
		Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); // "android.intent.action.GET_CONTENT"
		String IMAGE_UNSPECIFIED = "image/*";
		innerIntent.setType(IMAGE_UNSPECIFIED); // 查看类型
		Intent wrapperIntent = Intent.createChooser(innerIntent, null);
		startActivityForResult(wrapperIntent, REQ_CHOOSE);
	}

	/**
	 * 选择照片后结束
	 * 
	 * @param data
	 */
	private Uri afterChosePic(Intent data) {

		// 获取图片的路径：
		String[] proj = { MediaStore.Images.Media.DATA };
		// 好像是android多媒体数据库的封装接口，具体的看Android文档
		Cursor cursor = managedQuery(data.getData(), proj, null, null, null);
		if (cursor == null) {
			Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
			return null;
		}
		// 按我个人理解 这个是获得用户选择的图片的索引值
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		// 将光标移至开头 ，这个很重要，不小心很容易引起越界
		cursor.moveToFirst();
		// 最后根据索引值获取图片路径
		String path = cursor.getString(column_index);
		Log.i("cc ", "path=" + path);
		if (path != null
				&& (path.endsWith(".png") || path.endsWith(".PNG")
						|| path.endsWith(".jpg") || path.endsWith(".JPG"))) {
			File newFile = FileUtils.compressFile(path, compressPath);
			return Uri.fromFile(newFile);
		} else {
			Toast.makeText(this, "上传的图片仅支持png或jpg格式", Toast.LENGTH_SHORT)
					.show();
		}
		return null;
	}

	/**
	 * 返回文件选择
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		// if (requestCode == FILECHOOSER_RESULTCODE) {
		// if (null == mUploadMessage)
		// return;
		// Uri result = intent == null || resultCode != RESULT_OK ? null
		// : intent.getData();
		// mUploadMessage.onReceiveValue(result);
		// mUploadMessage = null;
		// }

		if (null == mUploadMessage)
			return;
		Uri uri = null;
		if (requestCode == REQ_CAMERA) {
			afterOpenCamera();
			uri = cameraUri;
		} else if (requestCode == REQ_CHOOSE) {
			uri = afterChosePic(intent);
		}
		mUploadMessage.onReceiveValue(uri);
		mUploadMessage = null;
		super.onActivityResult(requestCode, resultCode, intent);
	}

	private class ReOnCancelListener implements
			DialogInterface.OnCancelListener {

		@Override
		public void onCancel(DialogInterface dialogInterface) {
			if (mUploadMessage != null) {
				mUploadMessage.onReceiveValue(null);
				mUploadMessage = null;
			}
		}
	}
}
