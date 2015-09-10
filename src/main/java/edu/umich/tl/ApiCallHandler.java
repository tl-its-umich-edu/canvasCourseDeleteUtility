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

public class ApiCallHandler {
	private static Log M_log = LogFactory.getLog(ApiCallHandler.class);
	private String canvasURL=CourseDelete.canvasURL;
	private String esbURL=CourseDelete.esbURL;
	private static String canvasToken=CourseDelete.canvasToken;
	

	public enum ApiTypes{
		Term(1), UnPublishedCourseList(2);
		private int apiType;
		private ApiTypes(int apiType) {
			this.apiType=apiType;
		}
		public int getApiType() {
			return apiType;
		}
	}
	public HttpResponse getApiResponse(ApiTypes apiType, int typeOfApiCall) {
		HttpResponse httpResponse = null;
		String url=null;
		switch(apiType.getApiType()) {
		case 1: 
			if(typeOfApiCall==1) {
				url= canvasURL+"/api/v1/accounts/1/terms?per_page=100";
				 httpResponse = canvalApiCall(url);
			}else if(typeOfApiCall==2) {
				url= esbURL+"/api/v1/accounts/1/terms?per_page=100";
				httpResponse = esbApiCall(url);
			}
			M_log.info("TermURL: "+url);
			break;
		}
		return httpResponse;
	}

	

	private static HttpResponse canvalApiCall(String url) {
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

	private static HttpResponse esbApiCall(String url) {
		//Stub: to be implemented later when ESB to canvas call is ready
		return null;
	}

}
