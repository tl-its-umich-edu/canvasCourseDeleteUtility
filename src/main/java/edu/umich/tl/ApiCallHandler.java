package edu.umich.tl;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.base.Stopwatch;

import edu.umich.tl.CourseDelete.CanvasCallEnum;

public class ApiCallHandler {
	private static final String DELETE = "DELETE";

	private static final String GET = "GET";

	private static Log M_log = LogFactory.getLog(ApiCallHandler.class);
	
	private String canvasURL=CourseDelete.canvasURL;
	private String esbURL=CourseDelete.esbURL;
	private CanvasCallEnum canvasCall;
	private static String canvasToken=CourseDelete.canvasToken;
	private static final String PER_PAGE = "per_page=100";
	private static final String API_VERSION = "/api/v1";
	private static final String COURSES = "/courses/";
	private static final String ACCOUNTS = "/accounts/1";
	
	public ApiCallHandler(CanvasCallEnum canvasCall) {
		this.canvasCall=canvasCall;
	}
	
	public enum RequestTypeEnum{
		TERM, UNPUBLISHED_COURSE_LIST, UNPUBLISHED_COURSE_LIST_PAGINATION_URL, 
		ASSIGNMENT,ANNOUNCEMENT,CONFERENCE,DISCUSSION_TOPICS,FILES,GRADE_CHANGES,GROUPS,MODULES,
		PAGES,QUIZZES,EXTERNAL_TOOLS,COURSE_AUDIT_LOGS,COURSE_DELETE,SYLLABUS;
	}

	public HttpResponse getApiResponse(RequestTypeEnum requestType, String canvasTermIdForSisTermId, String url,String courseId) {
		String urlSuffix = null;
		boolean shouldAddPrefix=true;
		String request=GET;
		switch (requestType) {
		case TERM:
			urlSuffix=API_VERSION+ACCOUNTS+"/terms?"+PER_PAGE;
			break;
		case UNPUBLISHED_COURSE_LIST:
			urlSuffix=API_VERSION+ACCOUNTS+"/courses?enrollment_term_id="+canvasTermIdForSisTermId+"&published=false&"+PER_PAGE;
			break;
			// we are having separate case for unpublished course list since pagination object in the response header has fully framed url and hence we can use it directly,
		case UNPUBLISHED_COURSE_LIST_PAGINATION_URL:
			urlSuffix=url;
			shouldAddPrefix=false;
			break;
		case ASSIGNMENT:
			urlSuffix= API_VERSION+COURSES+courseId+"/assignments";
			break;
		case ANNOUNCEMENT:
			urlSuffix=API_VERSION+COURSES+courseId+"/discussion_topics?only_announcements=true";
			break;
		case CONFERENCE:
			urlSuffix=API_VERSION+COURSES+courseId+"/conferences";
			break;
		case DISCUSSION_TOPICS:
			urlSuffix=API_VERSION+COURSES+courseId+"/discussion_topics";
			break;
		case FILES:
			urlSuffix=API_VERSION+COURSES+courseId+"/files";
			break;
		case GRADE_CHANGES:
			urlSuffix=API_VERSION+"/audit/grade_change"+COURSES+courseId;
			break;
		case GROUPS:
			urlSuffix=API_VERSION+COURSES+courseId+"/groups";
			break;
		case MODULES:
			urlSuffix=API_VERSION+COURSES+courseId+"/modules";
			break;
		case PAGES:
			urlSuffix=API_VERSION+COURSES+courseId+"/pages";
			break;
		case QUIZZES:
			urlSuffix=API_VERSION+COURSES+courseId+"/quizzes";
			break;
		case EXTERNAL_TOOLS:
			urlSuffix=API_VERSION+COURSES+courseId+"/external_tools";
			break;
		case COURSE_AUDIT_LOGS:
			urlSuffix=API_VERSION+"/audit/course"+COURSES+courseId;
			break;
		case COURSE_DELETE:
			urlSuffix=API_VERSION+COURSES+courseId+"?event=delete";
			request=DELETE;
			break;
		case SYLLABUS:	
			urlSuffix=API_VERSION+COURSES+courseId+"?include=syllabus_body";
			break;
		default:
			M_log.warn("Unknown RequestType \""+requestType+"\" encounted");
			break;
		}
		return urlConstructorAndCanvasCallManager(urlSuffix, shouldAddPrefix, request);
	}
	
	private HttpResponse urlConstructorAndCanvasCallManager(String url, Boolean shouldAddPrefix, String httpReqType) {
		HttpResponse httpResponse = null;
		if(url==null) {
			M_log.error("The api call Url is null in \"urlConstructorAndCanvasCallManager()\"");
			return httpResponse;
		}
		String urlFull = null;
		if (isThisADirectCanvasCall()) {
			if(shouldAddPrefix) {
				urlFull = canvasURL + url;
			}else {
				urlFull=url;
			}
			httpResponse = apiDirectCanvas(urlFull, httpReqType);
		} else if (isThisAEsbCanvasCall()) {
			if(shouldAddPrefix) {
				urlFull = esbURL + url;
			}else {
				urlFull=url;
			}
			httpResponse =apiESBCanvas(urlFull, httpReqType);
		}
		return httpResponse;
	}
	

	private boolean isThisAEsbCanvasCall() {
		return canvasCall.equals(CanvasCallEnum.API_ESB_CANVAS);
	}

	private boolean isThisADirectCanvasCall() {
		return canvasCall.equals(CanvasCallEnum.API_DIRECT_CANVAS);
	}

	
	private HttpResponse apiDirectCanvas(String url, String httpReqType) {
		HttpUriRequest clientRequest = null;
		HttpResponse httpResponse=null;
		if(httpReqType.equals(GET)) {
			clientRequest = new HttpGet(url);
		}else if (httpReqType.equals(DELETE)) {
			clientRequest = new HttpDelete(url);
		}
		HttpClient client = new DefaultHttpClient();
		final ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
		nameValues.add(new BasicNameValuePair("Authorization", "Bearer" + " " + canvasToken));
		nameValues.add(new BasicNameValuePair("content-type", "application/json"));
		for (final NameValuePair h : nameValues) {
			clientRequest.addHeader(h.getName(), h.getValue());
		}
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			httpResponse = client.execute(clientRequest);
		} catch (ClientProtocolException e) {
			M_log.error("Canvas API call did not complete successfully has some Protocol Exceptions", e);
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully has some IOExceptions", e);
		}finally {
			stopwatch.stop();
			M_log.info("The Api call \"" + url+ "\" took \""+stopwatch+"\" and ResponseCode: "+httpResponse.getStatusLine().getStatusCode());
		}
		return httpResponse;
	}

	private HttpResponse apiESBCanvas(String url, String httpReqType) {
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
