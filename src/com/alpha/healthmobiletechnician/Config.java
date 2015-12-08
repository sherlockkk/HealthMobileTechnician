package com.alpha.healthmobiletechnician;

public class Config {
	// 版本信息
	public static int localVersion = 0;
	public static int serverVersion;

	// 首页url
	public static final String HOST_URL = "http://www.51zrys.com/HealthMobile/www_technician/index.html";
	// 更新URL
	public static final String UPDATE_URL = "http://www.51zrys.com/HealthMobile/update/upgrade_technician.xml";

	/* 下载包安装路径 */
	public static final String savePath = "/sdcard/download/";

	public static final String saveFileName = savePath + "test.apk";

	public static final String SHARE_TEXT = "欢迎下载早日养生APP技师版，做人类身心健康的连接器";
	public static final String SHARE_URL = "http://www.51zrys.com/HealthMobile/update/HealthMobileTechnician.apk";
	public static final String SHARE_TITLE = "早日养生";

	// appID,AppSecret
	public static final String QQ_APPID = "1104911029";
	public static final String QQ_APPSECRET = "4WVr2PMolD8UPpfw";
	public static final String WX_APPID = "wx4fec9e44be45c0cd";
	public static final String WX_APPSECRET = "d4624c36b6795d1d99dcf0547af5443d";
}
