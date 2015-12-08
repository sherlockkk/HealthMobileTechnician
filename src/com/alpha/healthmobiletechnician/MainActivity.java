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
	// ���ӵ�ַ
	// http://zrys.code8086.com/HealthMobile/www_technician/index.html
	private String web_url = Config.HOST_URL;

	// ��ữ����
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
				// ��Ҫ���߳�ִ�еķ���
				try {
					InputStream is = getXml(); // ��ȡxml����
					getUpdataInfo(is); // ���ý�������
					serverVersion = info.getVersion(); // ��÷������汾
					Log.i("cc",
							"check--infoVersion=" + info.getVersion()
									+ "infoURL=" + info.getUrl() + "infoAbout="
									+ info.getAbout());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// ��handler����һ����Ϣ
				mHandler.sendEmptyMessage(new Message().what = 1);
			}
		}.start();
		mWebView = (WebView) findViewById(R.id.web_view);

		// ����WebView�Ļ�������
		// initSetting();
		WebSettings webSettings = mWebView.getSettings();

		// ʹ��JavaScript
		webSettings.setJavaScriptEnabled(true);

		// ֧�����ģ�����ҳ����������ʾ����
		webSettings.setDefaultTextEncodingName("GBK");

		// ������WebView�д���ҳ��������Ĭ�������
		// mWebView.setWebViewClient(new WebViewClient());

		// ��JavaScript����Android������
		// �Ƚ��������࣬��Ҫ���õ�Android����д���������public����
		// ���������WebView�����е�JavaScript����
		// ��һ��������һ���������룬��JS�������������������������󣬿��Ե�����������һЩ����
		mWebView.addJavascriptInterface(new WebAppInterface(this),
				"myInterfaceName");

		mWebView.loadUrl(web_url);

	}

	// ���û�úͽ���xml�ķ��������첽���߳��в�������

	// Handler��Ϣ���ջ���
	private Handler mHandler = new Handler() {
		// Handler���յ���Ӧ��Ϣ����ˢ��ui�Ȳ���
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 1) {
				// �յ���Ϣ���ڴ˽���ui��ز������罫������������ʾ������
				Log.i("cc", "--���汾...--");
				checkVersion();
			}
		}
	};

	/**
	 * �Զ����Android�����JavaScript����֮���������
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

		// ���target ���ڵ���API 17������Ҫ��������ע��
		@JavascriptInterface
		public void call(String phoneCode) {

			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ phoneCode));
			startActivity(intent);
		}

		/**
		 * ����
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
	 * ��ƽ̨SSO���½����
	 */
	private void ssoConfig() {
		// TODO Auto-generated method stub
		// ����1Ϊ��ǰActivity������2Ϊ��������QQ���������APP ID������3Ϊ��������QQ���������APP kEY.
		UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(MainActivity.this,
				Config.QQ_APPID, Config.QQ_APPSECRET);
		qqSsoHandler.addToSocialSDK();
		// ����1Ϊ��ǰActivity������2Ϊ��������QQ���������APP ID������3Ϊ��������QQ���������APP kEY.
		QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(
				MainActivity.this, Config.QQ_APPID, Config.QQ_APPSECRET);
		qZoneSsoHandler.addToSocialSDK();
		// ���΢��ƽ̨
		UMWXHandler wxHandler = new UMWXHandler(MainActivity.this,
				Config.WX_APPID, Config.WX_APPSECRET);
		wxHandler.addToSocialSDK();
		// ���΢������Ȧ
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

	// ʵ�����ؼ�
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
				// ��Toast����ʾʱ��͵ȴ�ʱ����ͬ
				Toast.makeText(this, "�ٰ�һ���˳�", (int) waitTime).show();
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
	// // ���ؼ����¼������絯��ȷ���˳���
	// dialog_Exit(MainActivity.this);
	// return false;
	// }
	// return super.onKeyDown(keyCode, event);
	// }
	//
	// public static void dialog_Exit(Context context) {
	// AlertDialog.Builder builder = new Builder(context);
	// builder.setMessage("ȷ��Ҫ�˳���?");
	// builder.setTitle("��ʾ");
	// builder.setIcon(android.R.drawable.ic_dialog_alert);
	// builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int which) {
	// dialog.dismiss();
	// android.os.Process.killProcess(android.os.Process.myPid());
	// }
	// });
	//
	// builder.setNegativeButton("ȡ��",
	// new android.content.DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int which) {
	// dialog.dismiss();
	// }
	// });
	//
	// builder.create().show();
	// }

	// ����webview�Ļ�������
	private void initSetting() {
		// �رջ�����
		mWebView.setVerticalScrollBarEnabled(true);
		// ���ÿ�����
		mWebView.setWebChromeClient(new PAWebChromeClient());
		// ���� WebView���ԣ��������߻���ȵ�
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

		// �ر����Ź���
		settings.setSupportZoom(false);

		CookieSyncManager.createInstance(MainActivity.this);

		mWebView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {
				// �������ع��ܣ����û�����������ӵ�ʱ��ֱ�ӵ���ϵͳ�������������
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
					.setPositiveButton("ȷ��", new OnClickListener() {

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
					.setPositiveButton("ȷ��", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							result.confirm();
						}
					}).setNeutralButton("ȡ��", new OnClickListener() {

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
		 * ʵ���ļ��ϴ�
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
	 * �����°汾
	 */
	private int serverVersion;

	public void checkVersion() {

		Log.i("ac", "------------");
		if (Config.localVersion < serverVersion) {
			Log.i("checkversion", "==============================");
			// �����°汾����ʾ�û�����
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("�������")
					.setMessage("�����°汾,������������ʹ��.")
					.setPositiveButton("����",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// �������·���UpdateService
									// ����Ϊ�˰�update����ģ�黯�����Դ�һЩupdateService������ֵ
									// �粼��ID����ԴID����̬��ȡ�ı���,������app_nameΪ��

									Intent intent = new Intent();
									intent.setAction("android.intent.action.VIEW");
									Uri content_url = Uri.parse(info.getUrl());
									intent.setData(content_url);
									startActivity(intent);
									// startService(updateIntent);

								}
							})
					.setNegativeButton("ȡ��",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
			alert.create().show();
		} else {
			// ������
			// cheanUpdateFile()
		}
	}

	public UpdataInfo getUpdataInfo(InputStream is) throws Exception {

		UpdataInfo info = null;
		XmlPullParser parser = Xml.newPullParser();
		Log.i("cc", "--getUpdataInfo--");
		parser.setInput(is, "UTF-8");// ���ý���������Դ�������ʽ
		int event = parser.getEventType();
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_DOCUMENT: // ��ʼ����
				// ���ڴ�����ʼ����ع���
				info = new UpdataInfo();
				Log.i("UpdatePullParser", "--START_DOCUMENT--");
				break;
			case XmlPullParser.START_TAG:
				Log.i("UpdatePullParser", "--START_TAG--");
				String tag = parser.getName();
				if ("version".equals(tag)) {
					info.setVersion(new Integer(parser.nextText())); // ��ȡ�汾��
				} else if ("url".equals(tag)) {
					info.setUrl(parser.nextText()); // ��ȡurl��ַ
				} else if ("about".equals(tag)) {
					info.setAbout(parser.nextText()); // ��ȡ�����Ϣ
				}
				break;
			case XmlPullParser.END_TAG:// ����һ��Ԫ�أ����ж��Ԫ�أ�����������
				break;
			default:
				break;
			}
			event = parser.next();
		}
		return info; // ����һ��UpdataInfoʵ��
	}

	public InputStream getXml() throws Exception {

		String httpUrl = Config.UPDATE_URL;// �������´��apk��Ϣ��xml�ļ�
		Log.i("cc", "--  getXml  Ready!!!  --");

		HttpURLConnection conn = (HttpURLConnection) new URL(httpUrl)
				.openConnection();

		Log.i("cc", "--���ӷ�������...--");
		conn.setReadTimeout(10 * 1000); // �������ӳ�ʱ��ʱ��
		// conn.setRequestMethod("GET");
		conn.connect(); // ��ʼ����
		Log.i("cc", "--��ʼ����--");

		if (conn.getResponseCode() == 200) {
			InputStream is = conn.getInputStream();
			Log.i("cc", "--���ӷ������ɹ�--");
			return is; // ����InputStream
		} else {
			Log.i("cc", "---����ʧ��,�����Ͽ�����---");
		}
		conn.disconnect(); // �Ͽ�����
		return null;
	}

	/**
	 * ���SD���Ƿ����
	 * 
	 * @return
	 */
	public final boolean checkSDcard() {
		boolean flag = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (!flag) {
			Toast.makeText(this, "������ֻ��洢����ʹ�ñ�����", Toast.LENGTH_SHORT).show();
		}
		return flag;
	}

	String compressPath = "";

	protected final void selectImage() {
		if (!checkSDcard())
			return;
		String[] selectPicTypeStr = { "���", "ͼƬ" };
		new AlertDialog.Builder(this)
				.setOnCancelListener(new ReOnCancelListener())
				.setItems(selectPicTypeStr,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								// �������
								case 0:
									openCarcme();
									break;
								// �ֻ����
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
	 * �������
	 */
	private void openCarcme() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		imagePaths = Environment.getExternalStorageDirectory().getPath()
				+ "/DICM/camrea/" + (System.currentTimeMillis() + ".jpg");
		// ����ȷ���ļ���·�����ڣ��������պ��޷���ɻص�
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
	 * ���ս�����
	 */
	private void afterOpenCamera() {
		File f = new File(imagePaths);
		addImageGallery(f);
		File newFile = FileUtils.compressFile(f.getPath(), compressPath);
		Log.i("cc ", "path=" + f.getPath());
	}

	/** ������պ���������Ҳ��������� */
	private void addImageGallery(File file) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	/**
	 * �������ѡ��ͼƬ
	 */
	private void chosePic() {
		FileUtils.delFile(compressPath);
		Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); // "android.intent.action.GET_CONTENT"
		String IMAGE_UNSPECIFIED = "image/*";
		innerIntent.setType(IMAGE_UNSPECIFIED); // �鿴����
		Intent wrapperIntent = Intent.createChooser(innerIntent, null);
		startActivityForResult(wrapperIntent, REQ_CHOOSE);
	}

	/**
	 * ѡ����Ƭ�����
	 * 
	 * @param data
	 */
	private Uri afterChosePic(Intent data) {

		// ��ȡͼƬ��·����
		String[] proj = { MediaStore.Images.Media.DATA };
		// ������android��ý�����ݿ�ķ�װ�ӿڣ�����Ŀ�Android�ĵ�
		Cursor cursor = managedQuery(data.getData(), proj, null, null, null);
		if (cursor == null) {
			Toast.makeText(this, "��ѡ��ͼƬ", Toast.LENGTH_SHORT).show();
			return null;
		}
		// ���Ҹ������ ����ǻ���û�ѡ���ͼƬ������ֵ
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		// �����������ͷ ���������Ҫ����С�ĺ���������Խ��
		cursor.moveToFirst();
		// ����������ֵ��ȡͼƬ·��
		String path = cursor.getString(column_index);
		Log.i("cc ", "path=" + path);
		if (path != null
				&& (path.endsWith(".png") || path.endsWith(".PNG")
						|| path.endsWith(".jpg") || path.endsWith(".JPG"))) {
			File newFile = FileUtils.compressFile(path, compressPath);
			return Uri.fromFile(newFile);
		} else {
			Toast.makeText(this, "�ϴ���ͼƬ��֧��png��jpg��ʽ", Toast.LENGTH_SHORT)
					.show();
		}
		return null;
	}

	/**
	 * �����ļ�ѡ��
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
