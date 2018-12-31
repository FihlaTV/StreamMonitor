package io.antmedia.app.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

	private File m3u8File;

	private ArrayList<HLSSegment> allSegments = new ArrayList<>();

	private String baseDir;

	public StreamCapturer(String origin, String streamId) {
		this.origin = origin;
		this.streamId = streamId;

		baseDir = System.getProperty("red5.root")+"/"
				+ "webapps/"
				+ StreamMonitorApplication.scope.getName();
	}

	public void startCapturing() {
		initVertx();
		initDirs();
		pngJobName = vertx.setPeriodic(pngCapturePeriod, (l) -> {
			String pngUrl = "http://"+origin+":5080/WebRTCAppEE/previews/"+streamId+".png";
			try {
				FileUtils.copyURLToFile(new URL(pngUrl), new File(pngDir, System.currentTimeMillis()+".png"));
			} catch (MalformedURLException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		});
		logger.info("PngCapturerJobName for stream {} at {} is {}", streamId, origin, pngJobName);

		hlsJobName = vertx.setPeriodic(hlsCapturePeriod, (l) -> {
			String m3u8Url = "http://"+origin+":5080/WebRTCAppEE/streams/"+streamId+"_480p.m3u8";
			HttpGet httpGet = new HttpGet(m3u8Url);

			try {
				CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet);
				String m3u8Content = EntityUtils.toString(response.getEntity());
				downloadTsFiles(m3u8Content);
			} catch (ClientProtocolException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		});
		logger.info("HlsCapturerJobName for stream {} at {} is {}", streamId, origin, hlsJobName);
	}


	private void initDirs() {
		pngDir = new File(baseDir+"/previews/"+streamId);
		pngDir.mkdirs();

		hlsDir = new File(baseDir+"/streams/"+streamId);
		hlsDir.mkdirs();

		m3u8File = new File(hlsDir, streamId+".m3u8");
		
	}

	public void downloadTsFiles(String m3u8Content) {
		
		ArrayList<HLSSegment> segments = parseM3u8(m3u8Content);
		for (HLSSegment segment : segments) {
			downloadSegment(segment);
		}
		allSegments.addAll(segments);
		updateM3U8Files();
	}

	public void downloadSegment(HLSSegment segment) {
		String tsUrl = "http://"+origin+":5080/WebRTCAppEE/streams/"+segment.name;
		
		try {
			FileUtils.copyURLToFile(new URL(tsUrl), new File(hlsDir, segment.name));
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public void updateM3U8Files() {
		try {
			FileWriter fw = new FileWriter(m3u8File);
			fw.write("#EXTM3U"+System.lineSeparator());
			fw.write("#EXT-X-VERSION:3"+System.lineSeparator());
			for (HLSSegment segment : allSegments) {
				fw.write(segment.info+System.lineSeparator());
				fw.write(segment.name+System.lineSeparator());
			}
			fw.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		
	}

	private ArrayList<HLSSegment> parseM3u8(String m3u8Content) {
		
		ArrayList<HLSSegment> segments = new ArrayList<>();
		int listStartIndex = -1;
		HLSSegment lastDownloaded = allSegments.isEmpty() ? null : allSegments.get(allSegments.size()-1);

		if(lastDownloaded != null) {
			listStartIndex = m3u8Content.indexOf(lastDownloaded.name)+
					lastDownloaded.name.length()+System.getProperty("line.separator").length();
		}

		if(listStartIndex == -1) {
			listStartIndex = m3u8Content.indexOf("#EXTINF:");
		}

		String[] lines = m3u8Content.substring(listStartIndex).split(System.getProperty("line.separator"));
		for (int i = 0; i < lines.length-1; ) {
			String segmentInfo = lines[i++];
			String segmentName = lines[i++];

			segments.add(new HLSSegment(segmentName, segmentInfo));
		}

		return segments;
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

	public ArrayList<HLSSegment> getAllSegments() {
		return allSegments;
	}

	public class HLSSegment {
		public String name;
		public String info;

		public HLSSegment(String name, String info) {
			super();
			this.name = name;
			this.info = info;
		}
	}

}
