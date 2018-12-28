package io.antmedia.app.monitor;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamMonitorManager {

	public final static String BEAN_NAME = "monitor.manager";
	private Logger logger = LoggerFactory.getLogger(StreamMonitorManager.class);
	ConcurrentHashMap<String, StreamCapturer> activeStreams = new ConcurrentHashMap<>();

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
		String key = origin+"_"+streamId;
		StreamCapturer capturer = new StreamCapturer(origin, streamId);
		activeStreams.put(key, capturer);
		capturer.startCapturing();		
	}
	
	private void streamEnded(String origin, String streamId) {
		logger.info("stream stoped at {} with id:{}", origin, streamId);
		String key = origin+"_"+streamId;
		StreamCapturer capturer = activeStreams.get(key);
		capturer.stopCapturing();
		activeStreams.remove(key);
	}

}
