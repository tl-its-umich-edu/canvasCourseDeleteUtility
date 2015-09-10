# canvasCourseDeleteUtility

## Build Directions

1. CourseDeleteUtility$ `mvn clean install`
2. Add the following properties to `canvasCourseDelete.properties`:
      `canvas.token= ,canvas.url=, esb.url= `
   
  
## Run Directions
1. `java -jar target/canvasCourseDeleteUtility.jar 1 file:/<path>/canvasCourseDelete.properties`
  1. the first argument decides which Api to call. 1(canvas) or 2(ESB) 
 