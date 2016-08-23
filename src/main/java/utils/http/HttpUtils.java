package utils.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.gzip.GZipUtils;

public class HttpUtils {
	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final int CONNECT_TIMEOUT = NumberUtils.toInt("http.connect.timeout", 5000);
	private static final int SO_TIMEOUT = NumberUtils.toInt("http.so.timeout", 10000);

	public static String doGet(String url) throws Exception {
		return new String(request("GET", url, null, false, true));
	}

	public static String doPost(String url, String data) throws Exception {
		return new String(request("POST", url, data.getBytes("UTF-8"), true, true));
	}

	public static byte[] request(String method, String url, byte[] postData, boolean postGZip, boolean acceptGZip) throws Exception {
		HttpURLConnection connection = getHttpURLConnection(url);
		try {
			connection.setRequestMethod(method);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "text/html;charset=UTF-8");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(SO_TIMEOUT);

			if (postData == null) {
				postData = new byte[0];
			}

			if (postGZip) {
				postData = GZipUtils.compress(postData);
				connection.setRequestProperty("Content-Encoding", "gzip");
				connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
			}
			if (acceptGZip) {
				connection.setRequestProperty("Accept-Encoding", "gzip");
			}

			return requestMethod(connection, postData);
		} finally {
			connection.disconnect();
		}
	}

	private static byte[] requestMethod(HttpURLConnection connection, byte[] postData) throws Exception {
		// 发送post请求
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(connection.getOutputStream());
			bos.write(postData);
			bos.flush();
		} finally {
			if (bos != null) {
				bos.close();
			}
		}

		boolean acceptGzip = StringUtils.trimToEmpty(connection.getHeaderField("Accept-Encoding")).contains("gzip");

		int code = connection.getResponseCode();
		// 接收响应
		if (code == HttpURLConnection.HTTP_OK) {
			InputStream inputStream = connection.getInputStream();
			try {
				int bytesRead;
				byte[] buffer = new byte[4096];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
				if (acceptGzip) {
					return GZipUtils.decompress(baos.toByteArray());
				} else {
					return baos.toByteArray();
				}
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		} else {
			throw new HttpRetryException("Http Response Error.", code);
		}
	}

	private static HttpURLConnection getHttpURLConnection(String urlString) throws MalformedURLException, IOException {
		if (!urlString.toLowerCase().startsWith("http://") && !urlString.toLowerCase().startsWith("https://")) {
			throw new IllegalArgumentException("url(" + urlString + ") must start with \"http://\" or \"https://\"");
		}

		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		String protocal = url.getProtocol();
		if (protocal != null && protocal.equalsIgnoreCase("https")) {
			try {
				HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
				httpsConn.setSSLSocketFactory(getTrustAllSSLContext().getSocketFactory());

				HostnameVerifier hv = new HostnameVerifier() {
					@Override
					public boolean verify(String arg0, SSLSession arg1) {
						return true;
					}
				};
				httpsConn.setHostnameVerifier(hv);

			} catch (Exception e) {
				logger.warn("init httpmanager error", e);
				throw new RuntimeException("init httpmanager error", e);
			}
		}

		return conn;
	}

	public static SSLContext getTrustAllSSLContext() throws Exception {
		javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
		javax.net.ssl.TrustManager trust = new CustomTrustManager();
		trustAllCerts[0] = trust;
		javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		return sc;
	}

	static class CustomTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
			return;
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
			return;
		}
	}
}
