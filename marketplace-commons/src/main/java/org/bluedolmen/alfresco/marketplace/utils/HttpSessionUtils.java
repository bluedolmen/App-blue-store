package org.bluedolmen.alfresco.marketplace.utils;

import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WrappingWebScriptRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class HttpSessionUtils {

	private HttpSessionUtils() {}
	
	public static WebScriptServletRequest getWebScriptServletRequest(WebScriptRequest webScriptRequest) {
		
        WebScriptRequest iterator = webScriptRequest;
        do {
            if (iterator instanceof WebScriptServletRequest) {
                return (WebScriptServletRequest) iterator;
            }
            else if (iterator instanceof WrappingWebScriptRequest) {
                iterator = ((WrappingWebScriptRequest) webScriptRequest).getNext();
            }
            else {
                break;
            }
        }
        while (iterator != null);

        throw new WebScriptException("Remote Store access must be executed in HTTP Servlet environment");
		
	}
	
	
	
}
