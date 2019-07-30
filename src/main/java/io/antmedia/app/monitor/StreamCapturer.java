package io.antmedia.app.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.cluster.DBReader;
import io.vertx.core.Vertx;

public class StreamCapturer {

	private Logger logger = LoggerFactory.getLogger(StreamCapturer.class);

	private String origin;
	private String streamId;

	private long pngJobName;
	private long hlsJobName;

	private File pngDir;
	private File hlsDir;

	private File m3u8File;

	private ArrayList<HLSSegment> allSegments = new ArrayList<>();

	private String baseDir;

	private StreamMonitorManager manager;

	CookieStore cookieStore = new BasicCookieStore();
	HttpContext httpContext = new BasicHttpContext();

	private long originCheckJobName;

	private AtomicBoolean stopCalled = new AtomicBoolean(false);
	private Object lock = new Object();

	public StreamCapturer(String streamId, StreamMonitorManager manager, String scopeName) {
		this.streamId = streamId;
		this.manager = manager;

		baseDir = "webapps/" + scopeName;
	}

	public void startCapturing() {
		synchronized (lock) {
			if(stopCalled.get()) {
				logger.warn("Stop invoked before start for {}", streamId);
				return;
			}
			initDirs();

			pngJobName = getVertx().setPeriodic(manager.getPreviewCapturePeriod(), l -> {
				String pngUrl = "http://"+getOrigin()+":5080/"+manager.getSourceApp()+"/previews/"+streamId+".png";
				copyURLtoFile(pngUrl, new File(pngDir, System.currentTimeMillis()+".png"));
			});
			logger.info("PngCapturerJobName for stream {} at {} is {}", streamId, getOrigin(), pngJobName);

			hlsJobName = getVertx().setPeriodic(manager.getHlsCapturePeriod(), (l) -> {
				String m3u8Url = "http://"+getOrigin()+":5080/"+manager.getSourceApp()+"/streams/"+streamId+"_"+manager.getHlsResolution()+"p.m3u8";
				captureM3U8(m3u8Url);
			});
			logger.info("HlsCapturerJobName for stream {} at {} is {}", streamId, getOrigin(), hlsJobName);
		}
	}

	public void captureM3U8(String m3u8Url) {

		HttpGet httpGet = new HttpGet(m3u8Url);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).setSocketTimeout(8*1000).build();

		try (CloseableHttpClient httpClient = HttpClients.createDefault())
		{
			httpGet.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpGet, httpContext);
			
			String m3u8Content = EntityUtils.toString(response.getEntity());
			downloadTsFiles(m3u8Content);
		} catch (ClientProtocolException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}		
	}

	private void initDirs() {
		pngDir = new File(baseDir+"/previews/"+streamId);
		pngDir.mkdirs();

		hlsDir = new File(baseDir+"/streams/"+streamId);
		hlsDir.mkdirs();

		m3u8File = new File(hlsDir, streamId+".m3u8");

	}

	public void downloadTsFiles(String m3u8Content) {
		try {
		ArrayList<HLSSegment> segments = parseM3u8(m3u8Content);
			for (HLSSegment segment : segments) {
				downloadSegment(segment);
			}
			allSegments.addAll(segments);
			updateM3U8Files();
		}
		catch (StringIndexOutOfBoundsException ex) {
			logger.error("Content is {}", m3u8Content);
			logger.error(ExceptionUtils.getStackTrace(ex));
		}
	}

	public void downloadSegment(HLSSegment segment) {
		String tsUrl = "http://"+getOrigin()+":5080/"+manager.getSourceApp()+"/streams/"+segment.name;
		copyURLtoFile(tsUrl, new File(hlsDir, segment.name));
	}

	public void copyURLtoFile(String url, File file) {
		try {
			FileUtils.copyURLToFile(new URL(url), file, 5000, 10000);
		} catch (MalformedURLException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
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
			logger.error(ExceptionUtils.getStackTrace(e));
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

		if (listStartIndex == -1) {
			String lastDownloadedName = lastDownloaded == null ? "no downloaded segment" : lastDownloaded.name;
			logger.warn("!!!!!!\n m3u8 parse warning: lastdownloaded: {}, content:\n{}", lastDownloadedName, m3u8Content);
		}
		else {
			String[] lines = m3u8Content.substring(listStartIndex).split(System.getProperty("line.separator"));
			for (int i = 0; i < lines.length-1; ) {
				String segmentInfo = lines[i++];
				String segmentName = lines[i++];

				segments.add(new HLSSegment(segmentName, segmentInfo));
			}
		}

		return segments;
	}

	public void stopCapturing() {
		synchronized (lock) {
			logger.info("stopCapturing for stream {} at {}", streamId, getOrigin());
			stopCalled.set(true);
			getVertx().cancelTimer(originCheckJobName);
			getVertx().cancelTimer(pngJobName);
			getVertx().cancelTimer(hlsJobName);
		}
	}

	public ArrayList<HLSSegment> getAllSegments() {
		return allSegments;
	}

	public static class HLSSegment {
		public String name;
		public String info;

		public HLSSegment(String name, String info) {
			super();
			this.name = name;
			this.info = info;
		}
	}

	private Vertx getVertx() {
		return manager.getVertx();
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public void checkOrigin(int period, String sourceApp) {
		originCheckJobName = getVertx().setPeriodic(period, id -> {
			String originLocal = DBReader.instance.getHost(streamId, sourceApp);

			if(originLocal != null) {
				setOrigin(originLocal);
				logger.info("origin determined for {} as {}", streamId, origin);
				startCapturing();
				getVertx().cancelTimer(id);
			}
			else {
				logger.info("origin undetermined for {}, stream has not started yet", streamId);
			}
		});
	}
}
