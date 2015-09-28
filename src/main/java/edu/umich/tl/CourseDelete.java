package edu.umich.tl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umich.tl.ApiCallHandler.CanvasApiEnum;
import edu.umich.tl.CourseDelete.CanvasCallEnum;



public class CourseDelete {
	private static final String SIS_TERM_ID = "sis.term.id";
	private static final String CANVAS_URL = "canvas.url";
	private static final String CANVAS_TOKEN = "canvas.token";
	private static final String ESB_URL = "esb.url";
	private static final String API_CALL_TYPE = "api.call.type";
	private static Log M_log = LogFactory.getLog(CourseDelete.class);
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	protected static String esbURL = null;
	protected static String sisTermIdFromPropsFile = null;
	//This parameter decide to make canvas or ESB api call
	protected static CanvasCallEnum canvasCall;

	public static void main(String[] args) {
		M_log.debug("main(): called");
		readPropertiesFromFile(args);
		ApiCallHandler apiHandler=new ApiCallHandler(canvasCall);
		CoursesForDelete coursesForDelete=new CoursesForDelete();
		getTerms(apiHandler,coursesForDelete);
		String canvasTermIdForSisTermId = getCanvasTermIdForSisTermId(coursesForDelete);
		if(canvasTermIdForSisTermId==null) {
			M_log.error("Cannot find \"CanvasTermId\" for \"sisTermId = "+sisTermIdFromPropsFile+
					"\" provided. Please correct the \"sis.term.id\" property provided from \"canvasCourseDelete.properties\" file.");
			System.exit(0);
		}
		getUnpublishedCourseList(canvasTermIdForSisTermId,coursesForDelete,apiHandler,null);
	}

	public enum CanvasCallEnum{
		 API_DIRECT_CANVAS, 
		 API_ESB_CANVAS
	}

	private static void readPropertiesFromFile(String[] args) {
		M_log.debug("readPropertyFiles(): called");
		Properties properties = null;
		String propFileLocation;
		if(args.length == 0) {
			M_log.error("Command line arguments are not provided");
			System.exit(0);
		}
		propFileLocation = args[0];
		properties = Utils.getPropertiesFromURL(propFileLocation);
		canvasToken = properties.getProperty(CANVAS_TOKEN);
		canvasURL = properties.getProperty(CANVAS_URL);
		esbURL=properties.getProperty(ESB_URL);
		sisTermIdFromPropsFile = properties.getProperty(SIS_TERM_ID);
		checkForApiType(properties);
	}
     /*
      * This function decide if the api call that need to be made is 'CanvasDirect' or
      * 'CanvasEsb'.we will pass this information to the 'ApiCallHandler'. The 'ApiCallHandler'
      * will always be aware what type of call to make
      */
	private static void checkForApiType(Properties properties) {
		M_log.debug("checkForApiType(): called");
		int apiCallType=Integer.parseInt(properties.getProperty(API_CALL_TYPE));
		if(apiCallType==1) {
			canvasCall=CanvasCallEnum.API_DIRECT_CANVAS;
		}else if(apiCallType==2) {
			canvasCall=CanvasCallEnum.API_ESB_CANVAS;
		}
	}
	/*
	 *Get the enrollment_terms that are available in canvas during that point of the time.
	 */
	private static void getTerms(ApiCallHandler apiHandler, CoursesForDelete cfd) {
	    M_log.debug("getTerms(): called");
		HttpResponse httpResponse = apiHandler.getApiResponse(CanvasApiEnum.TERM, null,null);
		ObjectMapper mapper = new ObjectMapper();
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,"Enrollments_Terms",apiHandler));
			System.exit(0);
		}
		HttpEntity entity = httpResponse.getEntity();
		Map<String,Object> terms = new HashMap<String,Object>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			terms = mapper.readValue(jsonResponseString,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, Object>> termsList = (ArrayList<HashMap<String, Object>>) terms.get("enrollment_terms");
			for (HashMap<String, Object> eachTerm : termsList) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				try {
					String startDate = (String)eachTerm.get("start_at");
					String endDate = (String)eachTerm.get("end_at");
					if(startDate!=null&&endDate!=null) {
						Term term =new Term();
						term.setStartDate(dateFormat.parse(Utils.dateChopper(startDate)));
						term.setEndDate(dateFormat.parse(Utils.dateChopper(endDate)));
						term.setCanvasTermId(String.valueOf((Integer)eachTerm.get("id")));
						term.setSisTermId((String)eachTerm.get("sis_term_id"));
						term.setTermName((String)eachTerm.get("name"));
						cfd.addTerm(term);
					}
				} catch (java.text.ParseException e) {
					M_log.error("While parsing the date as string to Date object ParseException occurred",e);
				}
			}
		} catch (JsonParseException e) {
			M_log.error("getPreviousTerms() has JsonParseException",e);
		} catch (JsonMappingException e) {
			M_log.error("getPreviousTerms() has JsonMappingException",e);
		} catch (IOException e) {
			M_log.error("getPreviousTerms() has IOException",e);
		}catch (ParseException e) {
			M_log.error("getPreviousTerms() has ParseException",e);
		}
	}
    /*
     * For Provided sis_term_id from the properties file 'canvasCourseDelete.properties' we try to
     * get corresponding canvasTermId for making further APi call
     */
	private static String getCanvasTermIdForSisTermId(CoursesForDelete courseForDelete) {
		M_log.debug("getCanvasTermIdForSisTermId(): called");
		ArrayList<Term> terms = courseForDelete.getTerms();
		for (Term term : terms) {
			if (term.getSisTermId().equalsIgnoreCase(sisTermIdFromPropsFile)) {
				M_log.info("Found canvasTermId for provided SISTermId for the Term:  "+ term.getTermName());
				return term.getCanvasTermId();
			}
		}
		return null;
	}
	/*
	 * Getting all the unpublished course list for a particular term from the ROOT account which is '1'. All the colleges, school of UOfM fall under ROOT.
	 * This list is huge, canvas only provided 100 result set per api call so we need to get the rest of the courses info from the Pagination links.
	 */
	private static void getUnpublishedCourseList(String canvasTermIdForSisTermId, CoursesForDelete coursesForDelete, ApiCallHandler apiHandler, String url) {
		M_log.debug("getUnpublishedCourseList(): Called");
		HttpResponse httpResponse=null;
		if(url!=null) {
			//sending null for canvasTermId is fine since we get the url framed from the pagination object from the response header. 
			httpResponse=apiHandler.getApiResponse(CanvasApiEnum.UNPUBLISHED_COURSE_LIST_PAGINATION_URL,null,url);
		}else {
			httpResponse = apiHandler.getApiResponse(CanvasApiEnum.UNPUBLISHED_COURSE_LIST,canvasTermIdForSisTermId,url);
		}
		ObjectMapper mapper = new ObjectMapper();
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,"Unpublished_Courses",apiHandler));
			System.exit(0);
		}
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> courseList=new ArrayList<HashMap<String, Object>>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			courseList = mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			for (HashMap<String, Object> course : courseList) {
				Course aCourse=new Course();
				aCourse.setId((Integer)course.get("id"));
				aCourse.setCourseName((String)course.get("name"));
				aCourse.setStartDate((String)course.get("start_at"));
				aCourse.setEndDate((String)course.get("end_at"));

				coursesForDelete.addCourse(aCourse);
			}
			String nextPageUrl = getNextPageUrl(httpResponse);
			if(nextPageUrl!=null) {
				getUnpublishedCourseList(canvasTermIdForSisTermId,coursesForDelete,apiHandler,nextPageUrl);
			}
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured getUnpublishedCourseList() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured getUnpublishedCourseList() : ",e1);
		}  catch (IOException e) {
			M_log.error("IOException occured getUnpublishedCourseList( ):" ,e);
		}
	}
	/*
	 * This helper method pull out the error message that is sent in case of error. Currently code distinguishes error
	 * differently from directCanvas vs esbCanvas. This may not be always the case but ESB has Special Throttling message
	 * than canvas
	 */
	private static String apiCallErrorHandler(HttpResponse httpResponse, String text, ApiCallHandler apiHandler) {
		HttpEntity entity = httpResponse.getEntity();
		String jsonErrRes = null;
		String errMsg=null;
		try {
			jsonErrRes = EntityUtils.toString(entity);
		} catch (ParseException e) {
			M_log.error("ParseException occured apiCallErrorHandler() : ",e);
		} catch (IOException e) {
			M_log.error("IOException occured apiCallErrorHandler() : ",e);
		}
		if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_DIRECT_CANVAS)) {
			errMsg=canvasDirectErrorResponse(jsonErrRes,text);
		}else if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_ESB_CANVAS)) {
			errMsg=canvasEsbErrorResponse(jsonErrRes,text);
		}
		return errMsg;
	}

	/*
	 * { "errors": [ { "message": "Invalid access token." } ],
	 * "error_report_id": 545140 }
	 */
	private static String canvasDirectErrorResponse(String jsonErrRes, String apiText) {
		StringBuilder errMsg = new StringBuilder();
		errMsg.append("Api call for getting \""+apiText+"\" has some errors: < ");
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> errorResponse = new HashMap<String,Object>();
		try {
			errorResponse = mapper.readValue(jsonErrRes,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, String>> error = (ArrayList<HashMap<String, String>>) errorResponse.get("errors");
			for (HashMap<String, String> hashMap : error) {
				errMsg.append((String)hashMap.get("message")+" >");
			}
		} catch (JsonParseException e) {
			M_log.error("JsonParseException occured canvasDirectErrorResponse() : ",e);
		} catch (JsonMappingException e) {
			M_log.error("JsonMappingException occured canvasDirectErrorResponse() : ",e);
		} catch (IOException e) {
			M_log.error("IOException occured canvasDirectErrorResponse() : ",e);
		}
		return errMsg.toString();
	}
	
	private static String canvasEsbErrorResponse(String jsonErrRes,String apiText) {
		//Stub: to be implemented later when ESB to canvas call is ready
		return null;
	}
	
	/* The pagination 'Link' sample from the header 
     * Link → <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="current",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="next",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="first",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="last"
     */
	protected static String getNextPageUrl(HttpResponse response) {
		M_log.debug("getNextPageLink(): Called");
		String result = null;
		if (!response.containsHeader("Link")) {
			return result;
		}
			Header[] linkHeaders = response.getHeaders("Link");
			Header linkHeader = linkHeaders[0];
			M_log.debug("Http response contains the following Link headers: " + linkHeader.getValue());
			// look for the 'rel='next'' header value
			String[] links = linkHeader.getValue().split(",");
			for (int i = 0; i < links.length; i++) {
				String[] linkPart = links[i].split(";");
				if (linkPart[1].indexOf("rel=\"next\"") > 0) {
					result = linkPart[0].trim();
					break;
				}
			}
			if (result != null) {
				if (result.startsWith("<")) {
					result = result.substring(1, result.length() - 1);
				}
			}
		M_log.debug("Returning next page header as: " + (result != null ? result : "NONE"));
		return result;
	}
	

	

}
