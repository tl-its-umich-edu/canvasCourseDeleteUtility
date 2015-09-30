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
	
	public ApiCallHandler(CanvasCallEnum canvasCall) {
		this.canvasCall=canvasCall;
	}
	
	public enum RequestTypeEnum{
		TERM, UNPUBLISHED_COURSE_LIST, UNPUBLISHED_COURSE_LIST_PAGINATION_URL;
	}

	public HttpResponse getApiResponse(RequestTypeEnum requestType, String canvasTermIdForSisTermId, String url) {
		HttpResponse httpResponse = null;
		String urlSuffix;
		switch (requestType) {
		case TERM:
			urlSuffix=API_VERSION+"/accounts/1/terms?"+PER_PAGE;
			httpResponse = urlConstructorAndCanvasCallManager(urlSuffix, true);
			break;
		case UNPUBLISHED_COURSE_LIST:
			urlSuffix=API_VERSION+"/accounts/1/courses?enrollment_term_id="+canvasTermIdForSisTermId+"&published=false&"+PER_PAGE;
			httpResponse = urlConstructorAndCanvasCallManager(urlSuffix, true);
			break;
			// we are having separate case for unpublished course list since pagination object in the response header has fully framed url and hence we can use it directly,
		case UNPUBLISHED_COURSE_LIST_PAGINATION_URL:
			httpResponse=urlConstructorAndCanvasCallManager(url, false);;
			break;
		default:
			M_log.warn("Unknown RequestType \""+requestType+"\" encounted");
			break;
		}
		return httpResponse;
	}
	
	private HttpResponse urlConstructorAndCanvasCallManager(String url, Boolean shouldAddPrefix) {
		String urlFull = null;
		HttpResponse httpResponse = null;

		if (isThisADirectCanvasCall()) {
			if(shouldAddPrefix) {
				urlFull = canvasURL + url;
			}else {
				urlFull=url;
			}
			httpResponse = apiDirectCanvas(urlFull);
		} else if (isThisAEsbCanvasCall()) {
			if(shouldAddPrefix) {
				urlFull = esbURL + url;
			}else {
				urlFull=url;
			}
			httpResponse =apiESBCanvas(urlFull);
		}
		M_log.info("The Api call \"" + urlFull+ "\" has StatusCode: "+httpResponse.getStatusLine().getStatusCode());
		return httpResponse;
	}
	

	private boolean isThisAEsbCanvasCall() {
		return canvasCall.equals(CanvasCallEnum.API_ESB_CANVAS);
	}

	private boolean isThisADirectCanvasCall() {
		return canvasCall.equals(CanvasCallEnum.API_DIRECT_CANVAS);
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

	public CanvasCallEnum getCanvasCall() {
		return canvasCall;
	}

	public void setCanvasCall(CanvasCallEnum canvasCall) {
		this.canvasCall = canvasCall;
	}

}
