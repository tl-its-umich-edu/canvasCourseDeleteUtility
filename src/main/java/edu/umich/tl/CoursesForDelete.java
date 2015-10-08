package edu.umich.tl;

import java.util.ArrayList;



public class CoursesForDelete {
	private ArrayList<Course> totalUnpublishedCourses=new ArrayList<Course>();
	private ArrayList<Course> deletedUnpublishedCourses=new ArrayList<Course>();

	public ArrayList<Course> getCourses() {
		return totalUnpublishedCourses;
	}

	public void setCourses(ArrayList<Course> courses) {
		this.totalUnpublishedCourses = courses;
	}

	public void addCourse(Course course) {
		this.totalUnpublishedCourses.add(course);
	}

	public ArrayList<Course> getDeletedUnpublishedCourses() {
		return deletedUnpublishedCourses;
	}

	public void setDeletedUnpublishedCourses(ArrayList<Course> deletedUnpublishedCourses) {
		this.deletedUnpublishedCourses = deletedUnpublishedCourses;
	}
	public void addDeletedCourse(Course course) {
		this.deletedUnpublishedCourses.add(course);
	}


}
