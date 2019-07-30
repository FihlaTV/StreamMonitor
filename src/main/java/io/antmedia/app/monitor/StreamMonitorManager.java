package io.antmedia.app.monitor;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.cluster.DBReader;
import io.vertx.core.Vertx;

public class StreamMonitorManager {

	public static final String BEAN_NAME = "monitor.manager";

	private String sourceApp;
	private String hlsResolution;
	private int previewCapturePeriod;
	private int hlsCapturePeriod;
	private Vertx vertx;

	private Logger logger = LoggerFactory.getLogger(StreamMonitorManager.class);
	private ConcurrentHashMap<String, StreamCapturer> activeStreams = new ConcurrentHashMap<>();

	public String recordStream(String streamId, String scopeName) {
		logger.info("recordStream with id:{}", streamId);

		String message;
		if(!getActiveStreams().containsKey(streamId)) {
			startCapturing(streamId, scopeName);		
			message = streamId+" added.";
		}
		else {
			logger.warn("capturer is already working for {}", streamId);
			message = "capturer is already working for " + streamId;
		}

		return message;
	}

	public String stopStreamRecording(String streamId) {
		logger.info("stopStreamRecording with id:{}", streamId);

		stopCapturing(streamId);
		String message = streamId+" removed.";

		return message;
	}

	public void startCapturing(String streamId, String scopeName) {
		if(vertx == null) {
			initVertx();
		}
		
		StreamCapturer capturer = new StreamCapturer(streamId, this, scopeName);
		getActiveStreams().put(streamId, capturer);
		capturer.checkOrigin(Math.min(previewCapturePeriod, hlsCapturePeriod), sourceApp);
	}

	public void stopCapturing(String streamId) {
		StreamCapturer capturer = getActiveStreams().get(streamId);
		capturer.stopCapturing();	
		getActiveStreams().remove(streamId);
	}

	public String getSourceApp() {
		return sourceApp;
	}

	public void setSourceApp(String sourceApp) {
		this.sourceApp = sourceApp;
	}

	public String getHlsResolution() {
		return hlsResolution;
	}

	public void setHlsResolution(String hlsResolution) {
		this.hlsResolution = hlsResolution;
	}

	public int getPreviewCapturePeriod() {
		return previewCapturePeriod;
	}

	public void setPreviewCapturePeriod(int previewCapturePeriod) {
		this.previewCapturePeriod = previewCapturePeriod;
	}

	public int getHlsCapturePeriod() {
		return hlsCapturePeriod;
	}

	public void setHlsCapturePeriod(int hlsCapturePeriod) {
		this.hlsCapturePeriod = hlsCapturePeriod;
	}

	public ConcurrentHashMap<String, StreamCapturer> getActiveStreams() {
		return activeStreams;
	}

	public void initVertx() {
		if (StreamMonitorApplication.scope.getContext().getApplicationContext().containsBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME)) {
			setVertx((Vertx)StreamMonitorApplication.scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME));
			logger.info("vertx exist {}", getVertx());
		}
		else {
			logger.info("No vertx bean StreamMonitorApplication");
		}
	}

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
}
