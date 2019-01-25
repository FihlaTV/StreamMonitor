package io.antmedia.app.monitor;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.app.datastore.StreamMonitorStore;
import io.antmedia.app.datastore.Stream;

public class StreamMonitorManager {

	public static final String BEAN_NAME = "monitor.manager";
	
	private StreamMonitorStore store;
	private String sourceApp;
	private String hlsResolution;
	private int previewCapturePeriod;
	private int hlsCapturePeriod;
	
	private Logger logger = LoggerFactory.getLogger(StreamMonitorManager.class);
	private ConcurrentHashMap<String, StreamCapturer> activeStreams = new ConcurrentHashMap<>();

	public void newHookMessage(String origin, String streamId, String action, String streamName, String category) {
		if(action.contentEquals("liveStreamStarted")) {
			newStreamStrated(origin, streamId, streamName);
		} 
		else if(action.contentEquals("liveStreamEnded")) {
			streamEnded(origin, streamId);
		} 
		else {
			logger.info("unknown hook message");
		}
	}

	private void newStreamStrated(String origin, String streamId, String streamName) {
		logger.info("new stream started at {} with id:{}", origin, streamId);

		Stream stream = getStore().updateStreamStarted(streamId, origin, true);
		if(stream.isRecord()) {
			startCapturing(stream);	
		}
	}

	public void recordStream(String streamId) {
		logger.info("recordStream with id:{}", streamId);

		Stream stream = getStore().updateStreamRecord(streamId, true);
		if(stream.isStarted()) {
			startCapturing(stream);		
		}
	}
	
	private void streamEnded(String origin, String streamId) {
		logger.info("stream stoped at {} with id:{}", origin, streamId);
		Stream stream = getStore().updateStreamStarted(streamId, origin, false);
		
		if(stream.isRecord()) {
			//this means there is an active recording but now stream finished
			stopCapturing(stream);
		} 
		else {
			getStore().deleteStream(streamId);
		}
	}
	
	public void stopStreamRecording(String streamId) {
		logger.info("stopStreamRecording with id:{}", streamId);

		Stream stream = getStore().updateStreamRecord(streamId, false);
		if(stream.isStarted()) {
			//this means stream started before this message so there is capturer
			stopCapturing(stream);
		} else {
			getStore().deleteStream(streamId);
		}
	}
	
	public void startCapturing(Stream stream) {
		StreamCapturer capturer = new StreamCapturer(stream.getOrigin(), stream.getStreamId(), this);
		getActiveStreams().put(stream.getStreamId(), capturer);
		capturer.startCapturing();
	}
	
	public void stopCapturing(Stream stream) {
		StreamCapturer capturer = getActiveStreams().get(stream.getStreamId());
		capturer.stopCapturing();	
		getActiveStreams().remove(stream.getStreamId());
	}

	public StreamMonitorStore getStore() {
		return store;
	}

	public void setStore(StreamMonitorStore store) {
		this.store = store;
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
}
