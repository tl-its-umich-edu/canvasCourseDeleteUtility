# canvasCourseDeleteUtility
One of the dangers with an auto-provisioning course approach is the proliferation of unused course shells. One approach to solving this is deleting course shells at the end of each semester, when it can be deemed that they are unused. This utility will automatically delete course shells at the end date of each term. The current term is determined programmatically. The workflow is 

1. Check to see if there are people in the course. If there are no people, the course can be deleted. 
2. Check to see if there is content in the course for tools [ Announcements, Assignments, Conferences, Discussion topics, Files, Grade changes, Groups from a course, Modules, Pages, Quizzes, external tools ] . If there is content of any type that we have access to, do not delete the course. 
3. If there is no content, check the course audit log to see if the course has ever been published manually. 'Published' events from the 'sis' event_source are to be expected for every course, so should not be used as criteria that the course has ever been used. If the course has never been published and has no content, delete the course. 

## Build Directions

1. CourseDeleteUtility$ `mvn clean install`
2. Add the following properties to `canvasCourseDelete.properties`. 
  * There can be many `term` property like `term1, term2...termN` and should be numerically aligned. The value to the term property formatted as `MM/dd/yyy`
  * The `term.count` property sets the number of term property lines that will be read from the file. This property should match the number of term properties defined.
    
    ```
    canvas.token=canvas token  
    canvas.url=target canvas server e.g. https://umich.test.instructure.com  
    use.test.url=true  
    esb.url=esb server e.g. https://api-qa.its.umich.edu:9443/store/  
    api.call.type=1 or 2  1 means canvas call 2 means canvas call via ESB
    term1=1990;08/12/2014
    term2=2000;08/12/2014
    .....
    termN=2010;01/04/2015
    term.count=N
    ```
    
    
   
## Run Directions
1. `java -jar target/canvasCourseDeleteUtility.jar file:/<path>/canvasCourseDelete.properties`
  
 