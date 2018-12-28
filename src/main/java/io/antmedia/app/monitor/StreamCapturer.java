package io.antmedia.app.monitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.app.StreamMonitorApplication;
import io.vertx.core.Vertx;

public class StreamCapturer {

	private Logger logger = LoggerFactory.getLogger(StreamCapturer.class);

	private String origin;
	private String streamId;

	private Vertx vertx;

	private long pngCapturePeriod = 1000;
	private long pngJobName;

	private long hlsCapturePeriod = 2000;
	private long hlsJobName;

	private File pngDir;
	private File hlsDir;

	private int hlsCounter = 0;

	public StreamCapturer(String origin, String streamId) {
		this.origin = origin;
		this.streamId = streamId;

		String baseDir = System.getProperty("red5.root")+"/"
				+ "webapps/"
				+ StreamMonitorApplication.scope.getName()+"/"
				+ "/streams/"+origin+"/"+streamId;

		pngDir = new File(baseDir+"/png");
		pngDir.mkdirs();

		hlsDir = new File(baseDir+"/hls");
		hlsDir.mkdirs();

		initVertx();
	}

	public void startCapturing() {
		pngJobName = vertx.setPeriodic(pngCapturePeriod, (l) -> {
			String pngUrl = "http://"+origin+":5080/WebRTCAppEE/previews/"+streamId+".png";
			InputStream stream = getFileRemote(pngUrl);
			saveAsPng(stream);
		});
		logger.info("PngCapturerJobName for stream {} at {} is {}", streamId, origin, pngJobName);

		hlsJobName = vertx.setPeriodic(hlsCapturePeriod, (l) -> {
			String tsUrl = "http://"+origin+":5080/WebRTCAppEE/streams/"+streamId+"_0p"+String.format("%04d", hlsCounter)+".ts";
			InputStream stream = getFileRemote(tsUrl);
			saveAsTs(stream);

		});
		logger.info("HlsCapturerJobName for stream {} at {} is {}", streamId, origin, hlsJobName);
	}

	private InputStream getFileRemote(String pngUrl) {
		HttpGet httpGet = new HttpGet(pngUrl);
		try {
			CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet);
			InputStream content = response.getEntity().getContent();
			return content;
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return null;
	}


	private void saveAsTs(InputStream stream) {
		File file = new File(hlsDir, hlsCounter+".ts");
		saveFile(stream, file);
	}

	private void saveAsPng(InputStream stream) {
		File file = new File(pngDir, System.currentTimeMillis()+".png");
		saveFile(stream, file);
	}

	private void saveFile(InputStream stream, File file) {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			byte[] data = new byte[4096];
			int length = 0;
			while ((length = stream.read(data, 0, data.length)) != -1) {
				fos.write(data, 0, length);
			}
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public void stopCapturing() {
		logger.info("stopCapturing for stream {} at {}", streamId, origin);
		vertx.cancelTimer(pngJobName);
		vertx.cancelTimer(hlsJobName);
	}

	protected void initVertx() {
		if (StreamMonitorApplication.scope.getContext().getApplicationContext().containsBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME)) {
			vertx = (Vertx)StreamMonitorApplication.scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
			logger.info("vertx exist {}", vertx);
		}
		else {
			logger.info("No vertx bean StreamMonitorApplication");
		}
	}

}
