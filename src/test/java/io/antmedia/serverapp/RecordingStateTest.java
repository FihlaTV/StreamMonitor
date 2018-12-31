package io.antmedia.serverapp;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.verification.VerificationMode;

import io.antmedia.app.datastore.Stream;
import io.antmedia.app.datastore.StreamMonitorStore;
import io.antmedia.app.monitor.StreamMonitorManager;

public class RecordingStateTest {
	
	static final String origin = "origin.ams";
	static final String STREAM_STARTED = "liveStreamStarted";
	static final String STREAM_ENDED = "liveStreamEnded";
	static StreamMonitorStore dbStore;
	
	@BeforeClass
	public static void before() {
		dbStore = new StreamMonitorStore("test.db");
	}
	
	@AfterClass
	public static void after() {
		deleteStore();
	}
	
	
	private static void deleteStore() {
		File f = new File("test.db");
		if(f.exists()) {
			f.delete();
		}
	}

	@Test
	public void testRecordAfterStreamStarted() {
		
		String streamId = "testStream"+System.currentTimeMillis();

		StreamMonitorManager smmReal = new StreamMonitorManager();
		StreamMonitorManager smm = spy(smmReal);
		
		doNothing().when(smm).startCapturing(any());
		
		smm.setStore(dbStore);
		
		smm.newHookMessage(origin, streamId, STREAM_STARTED, "", "");
		Stream stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertEquals(origin, stream.getOrigin());
		assertTrue(stream.isStarted());
		assertFalse(stream.isRecord());
		
		verify(smm, never()).startCapturing(any());
		
		smm.recordStream(streamId);
		stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertEquals(origin, stream.getOrigin());
		assertTrue(stream.isStarted());
		assertTrue(stream.isRecord());	
		
		verify(smm, times(1)).startCapturing(any());
		
		smm.getStore().deleteStream(streamId);
		
	}
	
	@Test
	public void testStreamStartedAfterRecord() {
		String streamId = "testStream"+System.currentTimeMillis();

		StreamMonitorManager smmReal = new StreamMonitorManager();
		StreamMonitorManager smm = spy(smmReal);
		
		doNothing().when(smm).startCapturing(any());
		
		smm.setStore(dbStore);
		
		smm.recordStream(streamId);
		Stream stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertNull(stream.getOrigin());
		assertFalse(stream.isStarted());
		assertTrue(stream.isRecord());
		
		verify(smm, never()).startCapturing(any());
		
		smm.newHookMessage(origin, streamId, STREAM_STARTED, "", "");
		stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertEquals(origin, stream.getOrigin());
		assertTrue(stream.isStarted());
		assertTrue(stream.isRecord());	
		
		verify(smm, times(1)).startCapturing(any());
		
		smm.getStore().deleteStream(streamId);
	}
	
	@Test
	public void testStopRecoringdAfterStreamEnded() {
		
		String streamId = "testStream"+System.currentTimeMillis();

		StreamMonitorManager smmReal = new StreamMonitorManager();
		StreamMonitorManager smm = spy(smmReal);
		
		doNothing().when(smm).startCapturing(any());
		doNothing().when(smm).stopCapturing(any());

		
		smm.setStore(dbStore);

		smm.newHookMessage(origin, streamId, STREAM_STARTED, "", "");
		smm.recordStream(streamId);
		verify(smm, times(1)).startCapturing(any());
		
		//Here we have a running capturer

		smm.newHookMessage(origin, streamId, STREAM_ENDED, "", "");
		Stream stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertEquals(origin, stream.getOrigin());
		assertFalse(stream.isStarted());
		assertTrue(stream.isRecord());	
		
		verify(smm, times(1)).stopCapturing(any());
		
		smm.stopStreamRecording(streamId);

		stream = dbStore.getStream(streamId);
		assertNull(stream);
	}		
	
	@Test
	public void testStreamEndedAfterStopRecoringd() {
		
		String streamId = "testStream"+System.currentTimeMillis();

		StreamMonitorManager smmReal = new StreamMonitorManager();
		StreamMonitorManager smm = spy(smmReal);
		
		doNothing().when(smm).startCapturing(any());
		doNothing().when(smm).stopCapturing(any());

		
		smm.setStore(dbStore);

		smm.newHookMessage(origin, streamId, STREAM_STARTED, "", "");
		smm.recordStream(streamId);
		verify(smm, times(1)).startCapturing(any());
		
		//Here we have a running capturer

		smm.stopStreamRecording(streamId);
		Stream stream = dbStore.getStream(streamId);
		
		assertEquals(streamId, stream.getStreamId());
		assertEquals(origin, stream.getOrigin());
		assertTrue(stream.isStarted());
		assertFalse(stream.isRecord());	
		
		verify(smm, times(1)).stopCapturing(any());
		smm.newHookMessage(origin, streamId, STREAM_ENDED, "", "");

		stream = dbStore.getStream(streamId);
		assertNull(stream);
	}		
	

}
