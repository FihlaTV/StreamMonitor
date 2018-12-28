package io.antmedia.app;

import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class StreamMonitorApplication extends AntMediaApplicationAdapter {

	protected static Logger logger = LoggerFactory.getLogger(StreamMonitorApplication.class);

	public static final String STORAGE_FORWARD_URL = "https://s3.eu-central-1.amazonaws.com/";

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		return true;
	}

}
