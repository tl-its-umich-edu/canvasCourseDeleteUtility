package edu.umich.tl;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import edu.umich.tl.CourseDelete.CanvasCallEnum;

public class ApiCallHandler {
	private static Log M_log = LogFactory.getLog(ApiCallHandler.class);
	private String canvasURL=CourseDelete.canvasURL;
	private String esbURL=CourseDelete.esbURL;
	private CanvasCallEnum canvasCall;
	
	private static String canvasToken=CourseDelete.canvasToken;
	private static final String PER_PAGE = "per_page=100";
	private static final String API_VERSION = "/api/v1";
	
	public ApiCallHandler(CanvasCallEnum apiCallType) {
		this.canvasCall=apiCallType;
	}
	
	public enum CanvasApiEnum{
		TERM, UNPUBLISHED_COURSE_LIST;
	}

	public HttpResponse getApiResponse(CanvasApiEnum canvasApi) {
		HttpResponse httpResponse = null;
		String url = null;
		switch (canvasApi) {
		case TERM:
			String urlSuffix=API_VERSION+"/accounts/1/terms?"+PER_PAGE;
			if (canvasCall.equals(CanvasCallEnum.API_DIRECT_CANVAS)) {
				url = canvasURL + urlSuffix;
				httpResponse = apiDirectCanvas(url);
			} else if (canvasCall.equals(CanvasCallEnum.API_ESB_CANVAS)) {
				url = esbURL + urlSuffix;
				httpResponse =apiESBCanvas(url);
			}
			M_log.debug("TermURL: " + url);
			break;
		case UNPUBLISHED_COURSE_LIST:
			break;
		default:
			break;
		}
		return httpResponse;
	}

	

	private HttpResponse apiDirectCanvas(String url) {
		HttpUriRequest clientRequest = null;
		HttpResponse httpResponse=null;
		try {
			clientRequest = new HttpGet(url);
			HttpClient client = new DefaultHttpClient();
			final ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
			nameValues.add(new BasicNameValuePair("Authorization", "Bearer" + " " + canvasToken));
			nameValues.add(new BasicNameValuePair("content-type", "application/json"));
			for (final NameValuePair h : nameValues) {
				clientRequest.addHeader(h.getName(), h.getValue());
			}
			try {
				httpResponse = client.execute(clientRequest);

			} catch (IOException e) {
				M_log.error("Canvas API call did not complete successfully", e);
			}
		} catch (Exception e) {
			M_log.error("GET request has some exceptions", e);
		}
		return httpResponse;
	}

	private HttpResponse apiESBCanvas(String url) {
		//Stub: to be implemented later when ESB to canvas call is ready
		return null;
	}

}
