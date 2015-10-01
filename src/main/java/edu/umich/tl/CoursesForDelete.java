package edu.umich.tl;

import java.util.ArrayList;



public class CoursesForDelete {
	public ArrayList<Course> courses=new ArrayList<Course>();

	public ArrayList<Course> getCourses() {
		return courses;
	}

	public void setCourses(ArrayList<Course> courses) {
		this.courses = courses;
	}

	public void addCourse(Course course) {
		this.courses.add(course);
	}


}
