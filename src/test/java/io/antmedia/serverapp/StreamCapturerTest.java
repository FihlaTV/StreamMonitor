package io.antmedia.serverapp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.red5.server.api.scope.IScope;

import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.app.datastore.Stream;
import io.antmedia.app.monitor.StreamCapturer;
import io.antmedia.app.monitor.StreamCapturer.HLSSegment;

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
		StreamCapturer scReal = new StreamCapturer("", "");
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

}
