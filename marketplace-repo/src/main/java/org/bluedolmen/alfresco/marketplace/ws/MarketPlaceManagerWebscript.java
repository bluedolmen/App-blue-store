package org.bluedolmen.alfresco.marketplace.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class MarketPlaceManagerWebscript  extends AbstractWebScript {

	// Logger
	private static final Log logger = LogFactory.getLog(MarketPlaceManagerWebscript.class);

	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {
		
		try {
			streamContentImpl(req, res);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		res.setStatus(Status.STATUS_OK);
		
	}
	
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

		// final ProcessBuilder pb = new ProcessBuilder("/tmp/blah.sh");
		// try {
		// pb.start();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		status.setCode(Status.STATUS_OK);

		final Map<String, Object> result_ = new HashMap<String, Object>();

		return result_;

	}

	protected void streamContentImpl(
			WebScriptRequest req,
			WebScriptResponse res 
	) throws IOException, InterruptedException {

		// set mimetype for the content and the character encoding + length for
		// the stream
		res.setContentType(MimetypeMap.MIMETYPE_JSON);
//		res.setContentEncoding(reader.getEncoding());
//		res.setHeader("Content-Length", Long.toString(reader.getSize()));

		// set caching
		Cache cache = new Cache();
		cache.setNeverCache(false);
		cache.setMustRevalidate(true);
		cache.setMaxAge(0L);
//		cache.setLastModified(modified);
//		cache.setETag(eTag);
		res.setCache(cache);

		// get the content and stream directly to the response output stream
		// assuming the repository is capable of streaming in chunks, this
		// should allow large files
		// to be streamed directly to the browser response stream.
		OutputStream output = null;
		try {
			
			output = res.getOutputStream();
			
			for (int i = 0; i < 10; i++) {
				
				final String message = String.format("{ message : \"%s\"}\n", "message n°" + i);
				Thread.sleep(1000);
				output.write(message.getBytes());
				output.flush();
				
			}
			
			
		} 
		catch (SocketException e1) {
			// the client cut the connection - our mission was accomplished
			// apart from a little error message
			if (logger.isInfoEnabled())
				logger.info("Client aborted stream read");
		}
		finally {
			IOUtils.closeQuietly(output);
		}
	}


}
