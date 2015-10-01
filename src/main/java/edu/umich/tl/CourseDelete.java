package edu.umich.tl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umich.tl.ApiCallHandler.RequestTypeEnum;



public class CourseDelete {
	private static final String TERM = "term";
	private static final String TERM_COUNT = "term.count";
	private static final String CANVAS_URL = "canvas.url";
	private static final String CANVAS_TOKEN = "canvas.token";
	private static final String ESB_URL = "esb.url";
	private static final String API_CALL_TYPE = "api.call.type";
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	protected static String esbURL = null;
	protected static HashMap<String, DateTime> termsInfo = new HashMap<String, DateTime>();
	//This parameter decide to make canvas or ESB api call
	protected static CanvasCallEnum canvasCall;
	
	private static Log M_log = LogFactory.getLog(CourseDelete.class);

	public static void main(String[] args) {
		M_log.debug("main(): called");
		readPropertiesFromFile(args);
		ApiCallHandler apiHandler=new ApiCallHandler(canvasCall);
		ArrayList<Term> terms = getTerms(apiHandler);
		ArrayList<String> previousTerms = getPreviousTerms(terms);
		if(previousTerms.isEmpty()) {
			M_log.error("No previous terms could be determined for courses deletion");
			System.exit(1);
		}
		/*
		 * Their may be multiple term courses that needs to be deleted for a cron job at that point in time. The Unpublished course list can be huge so the 
		 * design would be focusing on one term at a time and verifying for each course in a term if people exist, content added and activity happened for deleting the course. 
		 *  
		 */
		for (String canvasTermId : previousTerms) {
			CoursesForDelete coursesForDelete=new CoursesForDelete();
			getUnpublishedCourseList(canvasTermId,coursesForDelete,apiHandler,null);
			//Todo delete below line
			M_log.debug("CourseList: "+coursesForDelete.getCourses().size()+" term: "+canvasTermId);
		}
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
			System.exit(1);
		}
		propFileLocation = args[0];
		properties = Utils.getPropertiesFromURL(propFileLocation);
		canvasToken = properties.getProperty(CANVAS_TOKEN);
		canvasURL = properties.getProperty(CANVAS_URL);
		esbURL=properties.getProperty(ESB_URL);
		getTermsInfoFromProperties(properties);
		checkForApiType(properties);
	}
	/*
	 * The Terms that needs for course deletion are listed in the properties file. Each term has 2 piece of information 
	 * term1=2030;04/06/15, the sisTermCode and the endDate of that term. We will check if the current system date is
	 * past the endDate then that term becomes eligible for deleting the unused/unpublished courses that are created 
	 * as apart of auto-provisioning of courses in canvas
	 * 
	 */
	private static void getTermsInfoFromProperties(Properties properties) {
		M_log.debug("getTermsInfoFromProperties(): called");
		String termCount = properties.getProperty(TERM_COUNT);
		if(termCount.isEmpty()) {
			M_log.error("The property \"term.count\" is not set in the properties file \"canvasCourseDelete.properties\"");
			System.exit(1);
		}
		int termCountInt = Integer.parseInt(termCount);
		for(int i=1;i<=termCountInt;i++) {
			String term = TERM+String.valueOf(i);
			String termProperty = properties.getProperty(term);
			if(Utils.isEmpty(termProperty)) {
				M_log.error("The property \""+term+"\" don't seems to exist in \"canvasCourseDelete.properties\" "
						+ "please make sure all term properties are numerically aligned and number of properties is equal to \"term.count\" property value");
				System.exit(1);
			}
			String[] termWithDate = termProperty.split(";");
			DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
			termsInfo.put(termWithDate[0],dtf.parseDateTime(termWithDate[1]));
		}

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
	private static ArrayList<Term> getTerms(ApiCallHandler apiHandler) {
		M_log.debug("getTerms(): called");
		ArrayList<Term> enrollmentTerms = new ArrayList<Term>();
		HttpResponse httpResponse = apiHandler.getApiResponse(RequestTypeEnum.TERM, null,null);
		ObjectMapper mapper = new ObjectMapper();
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,"Enrollments_Terms",apiHandler));
			System.exit(1);
		}
		HttpEntity entity = httpResponse.getEntity();
		Map<String,Object> terms = new HashMap<String,Object>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			terms = mapper.readValue(jsonResponseString,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, Object>> termsList = (ArrayList<HashMap<String, Object>>) terms.get("enrollment_terms");
			for (HashMap<String, Object> eachTerm : termsList) {
				String startDate = (String)eachTerm.get("start_at");
				String endDate = (String)eachTerm.get("end_at");
				if(startDate!=null&&endDate!=null) {
					Term term =new Term()
							.setCanvasTermId(String.valueOf((Integer)eachTerm.get("id")))
							.setSisTermId((String)eachTerm.get("sis_term_id"))
							.setTermName((String)eachTerm.get("name"));
					enrollmentTerms.add(term);
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
		return enrollmentTerms;
	}
	/*
	 * This method returns the List of terms for deletion of unused and unpublished courses. 
	 */
	private static ArrayList<String> getPreviousTerms(ArrayList<Term> terms) {
		M_log.debug("getPreviousTerms(): called");
		DateTime todaysDate = new DateTime();
		M_log.info("Date the cron job ran: "+todaysDate);
		ArrayList<String> listOftermsForCoursesDelete = new ArrayList<String>();
		for (Entry<String, DateTime> term : termsInfo.entrySet()) {
			if(term.getValue().isBefore(todaysDate)) {
				String canvasTermIdForSisTermId = getCanvasTermIdForSisTermId(terms, term.getKey());
				if(canvasTermIdForSisTermId==null) {
					M_log.error("Cannot find \"CanvasTermId\" for \"sisTermId = "+term.getKey()+
							"\" provided. Please correct the \"sis.term.id\" property provided from \"canvasCourseDelete.properties\" file.");
					continue;
				}
				listOftermsForCoursesDelete.add(canvasTermIdForSisTermId);
			}

		}
		return listOftermsForCoursesDelete;
	}
    /*
     * For Provided sis_term_id from the properties file 'canvasCourseDelete.properties' we try to
     * get corresponding canvasTermId for making further APi call
     */
	private static String getCanvasTermIdForSisTermId(ArrayList<Term> terms, String termSisId) {
		M_log.debug("getCanvasTermIdForSisTermId(): called");
		for (Term term : terms) {
			if (term.getSisTermId().equalsIgnoreCase(termSisId)) {
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
			httpResponse=apiHandler.getApiResponse(RequestTypeEnum.UNPUBLISHED_COURSE_LIST_PAGINATION_URL,null,url);
		}else {
			httpResponse = apiHandler.getApiResponse(RequestTypeEnum.UNPUBLISHED_COURSE_LIST,canvasTermIdForSisTermId,url);
		}
		ObjectMapper mapper = new ObjectMapper();
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse,"Unpublished_Courses",apiHandler));
			System.exit(1);
		}
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> courseList=new ArrayList<HashMap<String, Object>>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			courseList = mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			for (HashMap<String, Object> course : courseList) {
				Course aCourse=new Course()
						.setId((Integer)course.get("id"))
						.setCourseName((String)course.get("name"))
						.setStartDate((String)course.get("start_at"))
						.setEndDate((String)course.get("end_at"));

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
	 * 
	 * return errMsg=Api call for getting "Enrollments_Terms" has some errors with status code: 401: < Invalid access token. >
	 */
	private static String apiCallErrorHandler(HttpResponse httpResponse, String apiText, ApiCallHandler apiHandler) {
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
		String errMsgWithStatus="Api call for getting \""+apiText+"\" has some errors with status code: "+httpResponse.getStatusLine().getStatusCode()+": < ";
		if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_DIRECT_CANVAS)) {
			errMsg=canvasDirectErrorResponse(jsonErrRes,errMsgWithStatus);
		}else if(apiHandler.getCanvasCall().equals(CanvasCallEnum.API_ESB_CANVAS)) {
			errMsg=canvasEsbErrorResponse(jsonErrRes,errMsgWithStatus);
		}
		return errMsg;
	}

	/*
	 * { "errors": [ { "message": "Invalid access token." } ],
	 * "error_report_id": 545140 }
	 */
	private static String canvasDirectErrorResponse(String jsonErrRes, String apiText) {
		StringBuilder errMsg = new StringBuilder();
		errMsg.append(apiText);
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
		M_log.debug("getNextPageUrl(): Called");
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
