package edu.umich.tl;

import java.util.ArrayList;



public class CoursesForDelete {
	public ArrayList<Term> terms=new ArrayList<Term>();
	public ArrayList<Course> courses=new ArrayList<Course>();
	

	public ArrayList<Term> getTerms() {
		return terms;
	}

	public void setTerms(ArrayList<Term> terms) {
		this.terms = terms;
	}
	
	public void addTerm(Term term) {
		this.terms.add(term);
	}

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
