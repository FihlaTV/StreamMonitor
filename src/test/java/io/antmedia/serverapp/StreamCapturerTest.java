package io.antmedia.serverapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.red5.server.api.scope.IScope;

import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.app.monitor.StreamCapturer;
import io.antmedia.app.monitor.StreamCapturer.HLSSegment;
import io.antmedia.app.monitor.StreamMonitorManager;

public class StreamCapturerTest {
	
	String m3u8Content1 = "#EXTM3U" + System.lineSeparator() + 
			"#EXT-X-VERSION:3" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0000.ts" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0001.ts" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0002.ts" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0003.ts";
	
	String m3u8Content2 = "#EXTM3U" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0002.ts" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0003.ts" + System.lineSeparator() +
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0004.ts" + System.lineSeparator() + 
			"#EXTINF:2," + System.lineSeparator() + 
			"streamId_480p0005.ts";
	
	@Test
	public void testParseM3U8() {
		StreamMonitorApplication.scope = mock(IScope.class);
		StreamMonitorManager manager = new StreamMonitorManager();
		StreamCapturer scReal = new StreamCapturer("", manager, "scope");
		StreamCapturer sc = spy(scReal);
		doNothing().when(sc).downloadSegment(any());
		doNothing().when(sc).updateM3U8Files();
		
		sc.downloadTsFiles(m3u8Content1);
		assertEquals(4, sc.getAllSegments().size());
		assertEquals("streamId_480p0003.ts", sc.getAllSegments().get(sc.getAllSegments().size()-1).name);
		
		sc.downloadTsFiles(m3u8Content2);
		assertEquals(6, sc.getAllSegments().size());
		assertEquals("streamId_480p0005.ts", sc.getAllSegments().get(sc.getAllSegments().size()-1).name);
	}
	
	
	
	boolean methodReturned = false;
	
	@Test
	public void testTimeout() {
		
		StreamMonitorManager manager = new StreamMonitorManager();
		manager.setSourceApp("sourceApp");
		
		StreamCapturer capturer = new StreamCapturer("streamId", manager, "scope");
		
		capturer.setOrigin("www.google.com");
		methodReturned = false;
		
		new Thread() {
			@Override
			public void run() {
				capturer.downloadSegment(new HLSSegment("streamId", null));
				methodReturned = true;
			}
		}.start();
		
		
		Awaitility.await().atMost(15000, TimeUnit.MILLISECONDS).until(() -> methodReturned);
		
		methodReturned = false;
		
		new Thread() {
			@Override
			public void run() {
				capturer.captureM3U8("http://123.33.22.1");
				methodReturned = true;
			}
		}.start();
		
		Awaitility.await().atMost(15000, TimeUnit.MILLISECONDS).until(() -> methodReturned);
		
		
	}
	
	@Test
	public void testSettings() {
		StreamMonitorApplication.scope = mock(IScope.class);
		StreamMonitorManager manager = new StreamMonitorManager();
		manager.setVertx(io.vertx.core.Vertx.vertx());
		manager.setHlsCapturePeriod(2000);
		manager.setPreviewCapturePeriod(1000);
		manager.setSourceApp("DummyApp");
		manager.setHlsResolution("240");
		
		StreamCapturer scReal = new StreamCapturer("test_stream", manager, "scope");
		scReal.setOrigin("origin_url");
		
		StreamCapturer sc = spy(scReal);
		doNothing().when(sc).captureM3U8(any());
		doNothing().when(sc).copyURLtoFile(any(), any());
		

		sc.startCapturing();
		try {
			Thread.sleep(5500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sc.stopCapturing();
		
		String pngUrl = "http://origin_url:5080/DummyApp/previews/test_stream.png";
		String m3u8Url = "http://origin_url:5080/DummyApp/streams/test_stream_240p.m3u8";

		
		verify(sc, times(2)).captureM3U8(m3u8Url);
		verify(sc, times(5)).copyURLtoFile(eq(pngUrl), any());

	}

}
