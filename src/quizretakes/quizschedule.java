// JO 3-Jan-2019
package quizretakes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.time.*;
import java.lang.Long;
import java.lang.String;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

/**
 * @author Jeff Offutt Date: January, 2019
 *         Modified by Sam Crater, Chris McCready
 *
 *         Wiring the pieces together: quizschedule.java -- Servlet entry point
 *         for students to schedule quizzes quizReader.java -- reads XML file
 *         and stores in quizzes. Used by quizschedule.java quizzes.java -- A
 *         list of quizzes from the XML file Used by quizschedule.java
 *         quizBean.java -- A simple quiz bean Used by quizzes.java and
 *         readQuizzesXML.java retakesReader.java -- reads XML file and stores
 *         in retakes. Used by quizschedule.java retakes.java -- A list of
 *         retakes from the XML file Used by quizschedule.java retakeBean.java
 *         -- A simple retake bean Used by retakes.java and readRetakesXML.java
 *         apptBean.java -- A bean to hold appointments
 * 
 *         quizzes.xml -- Data file of when quizzes were given retakes.xml --
 *         Data file of when retakes are given
 */

public class quizschedule {
	// Data files
	// location maps to /webapps/offutt/WEB-INF/data/ from a terminal window.
	// These names show up in all servlets
	private static final String dataLocation = "";
	static private final String separator = ",";
	private static final String courseBase = "course";
	private static final String quizzesBase = "quiz-orig";
	private static final String retakesBase = "quiz-retakes";
	private static final String apptsBase = "quiz-appts";

	// Filenames to be built from above and the courseID parameter
	private String courseFileName;

	// Passed as parameter and stored in course.xml file (format: "swe437")
	private String courseID;
	// Stored in course.xml file, default 14
	// Number of days a retake is offered after the quiz is given
	private int daysAvailable = 14;

	private Scanner scan;
	
	public quizschedule() {
		scan = new Scanner(System.in);
	}
	
	public void run() {
		String input = "";
		while (!input.equals("s") && !input.equals("i")) {
			System.out.println("Student or instructor view? (s/i):");
			input = scan.nextLine().toLowerCase().trim();
		}
		
		if (input.equals("s")) {
			studentView();
		} else {
			instructorView();
		}
	}
	
	public static void main(String[] args) {
		quizschedule qs = new quizschedule();
		qs.run();
	}
	
	public void instructorView() {
		apptsReader reader = new apptsReader();
		ArrayList<apptBean> appointments = null;
		while(appointments == null) {
			System.out.println("Input a course id (e.g. swe437):");
			courseID = scan.nextLine().trim();
			courseFileName = dataLocation + apptsBase + "-" + courseID + ".txt";
			try {
				appointments = reader.read(courseFileName);
			} catch (IOException e) {
				System.out.println("Looking for " + courseFileName + " in " + System.getProperty("user.dir"));
				System.out.println("Can't find an appointment file for course ID " + courseID + ". Please try again.");
			}
		}
		
		HashSet<Integer> retakeIDsTemp = new HashSet<Integer>();
		for (apptBean appointment : appointments) {
			retakeIDsTemp.add(appointment.getRetakeID());
		}
		ArrayList<Integer> retakeIDs = new ArrayList<>(retakeIDsTemp);
		Collections.sort(retakeIDs);
		
		retakesReader rr = new retakesReader();
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";
		retakes retakesList = new retakes();
		try {
			retakesList = rr.read(retakesFileName);
		} catch (Exception e) {
			System.out.println("Error looking for " + courseFileName + " in " + System.getProperty("user.dir"));
		}
		
		HashMap<Integer, LocalDate> aptDates = new HashMap<>();
		for (retakeBean r : retakesList) {
			if (retakeIDs.contains(r.getID())) {
				aptDates.put(r.getID(), r.getDate());
			}
		}
		
		LocalDate today = LocalDate.now();
		LocalDate endDay = today.plusDays(new Long(daysAvailable));
		
		System.out.println("Appointments within next " + daysAvailable + " days:");
		for (int i : retakeIDs) {
			if (aptDates.get(i).isBefore(endDay)) {
				System.out.println("Appointments for retake ID " + i + " on " + aptDates.get(i) + ":");
				for (apptBean appointment : appointments) {
					if (appointment.getRetakeID() == i) {
						try {
							System.out.println("\t" + appointment.getName() + " is taking quiz " + appointment.getQuizID());
						} catch (Exception e) {
							System.out.println("\tInvalid quiz retake.");
						}
					}
				}
			}
		}
		//loop through appointments, make list of retake IDs (arraylist)
		//open retake sessions file
		//loop through retake sessions, taking times of all used sessions (hashmap id -> time)
		//loop through appointments, printing those with time in next two weeks (if hashmap(bean.id).isWithinTwoWeeks() print)
	}

	public void studentView() {
		courseBean course = null;
		
		courseReader cr = new courseReader();

		while (courseID == null || course == null) {
			System.out.println("Input a course id (e.g. swe437):");
			courseID = scan.nextLine().trim();
			courseFileName = dataLocation + courseBase + "-" + courseID + ".xml";
			
			try {
				course = cr.read(courseFileName);
			} catch (Exception e) {
				System.out.println("Looking for " + courseFileName + " in " + System.getProperty("user.dir"));
				System.out.println("Can't find the data files for course ID " + courseID + ". Please try again.");
			}
		}

		// CourseID must be a parameter (also in course XML file, but we need to know
		// which course XML file ...)
		// courseID = request.getParameter("courseID"); // while loop prompt for input

		daysAvailable = Integer.parseInt(course.getRetakeDuration());

		// Filenames to be built from above and the courseID
		String quizzesFileName = dataLocation + quizzesBase + "-" + courseID + ".xml";
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";

		// Load the quizzes and the retake times from disk
		quizzes quizList = new quizzes();
		retakes retakesList = new retakes();
		quizReader qr = new quizReader();
		retakesReader rr = new retakesReader();

		try { // Read the files and print the form
			quizList = qr.read(quizzesFileName);
			retakesList = rr.read(retakesFileName);
			printQuizScheduleForm(quizList, retakesList, course);
		} catch (Exception e) {
			System.out.println("Can't find the data files for course ID " + courseID + ". Please restart the application because I don't want to loop this.");
		}

	}

// doPost saves an appointment in a file and prints an acknowledgement
	protected void writeRetakeData(String courseID, String studentName, ArrayList<String> IDPairs) throws IOException {
		// No saving if IOException
		boolean IOerrFlag = false;
		String IOerrMessage = "Internal IO error.";

		// Filename to be built from above and the courseID
		String apptsFileName = dataLocation + apptsBase + "-" + courseID + ".txt";

		// Get name and list of retake requests from parameters

		if (IDPairs != null && studentName != null && studentName.length() > 0) {
			// Append the new appointment to the file
			try {
				File file = new File(apptsFileName);
				synchronized (file) { // Only one student should touch this file at a time.
					if (!file.exists()) {
						file.createNewFile();
					}
					FileWriter fw = new FileWriter(file.getAbsoluteFile(), true); // append mode
					BufferedWriter bw = new BufferedWriter(fw);

					for (String oneIDPair : IDPairs) {
						bw.write(oneIDPair + separator + studentName + "\n");
					}

					bw.flush();
					bw.close();
				} // end synchronize block
			} catch (IOException e) {
				IOerrFlag = true;
				IOerrMessage = "I failed and could not save your appointment." + e;
			}

			// Respond to the student
			if (IOerrFlag) {
				System.out.println(IOerrMessage);
			} else {
				if (IDPairs.size() == 1)
					System.out.println(studentName + ", your appointment has been scheduled.");
				else
					System.out.println(studentName + ", your appointments have been scheduled.");
				System.out.println("Please arrive in time to finish the quiz before the end of the retake period.");
				System.out.println("If you cannot make it, please cancel by sending an email to your professor.");
			}

		} else { // IDPairs == null or name is null
			if (IDPairs.size() == 0)
				System.out.println("You didn't choose any quizzes to retake.");
			if (studentName == null || studentName.length() == 0)
				System.out.println("You didn't give a name ... no anonymous quiz retakes.");
		}
	}

	/**
	 * Print the body of HTML
	 * 
	 * @param out PrintWriter
	 * @throws ServletException
	 * @throws IOException
	 */
	private void printQuizScheduleForm(quizzes quizList, retakes retakesList, courseBean course)
			throws IOException {
		
		boolean skip = false;
		LocalDate startSkip = course.getStartSkip();
		LocalDate endSkip = course.getEndSkip();

		LocalDate today = LocalDate.now();
		LocalDate endDay = today.plusDays(new Long(daysAvailable));
		LocalDate origEndDay = endDay;
		// if endDay is between startSkip and endSkip, add 7 to endDay
		if (!endDay.isBefore(startSkip) && !endDay.isAfter(endSkip)) { // endDay is in a skip week, add 7 to endDay
			endDay = endDay.plusDays(new Long(7));
			skip = true;
		}

		String startDate = (today.getDayOfWeek()) + ", " + today.getMonth() + " " + today.getDayOfMonth();
		String endDate = (endDay.getDayOfWeek()) + ", " + endDay.getMonth() + " " + endDay.getDayOfMonth();
		
		System.out.println("This is the quiz retake scheduler for " + course.getCourseTitle() + ".");
		System.out.println("You may sign up for quiz retake sessions that are within " + startDate + " to " + endDate + ".");
		System.out.println("Please enter your name (as it appears on the class roster):");
		String studentName = scan.nextLine().trim();
		
		System.out.println("\nTo sign up for a quiz retake:");
		System.out.println("    Enter the retake session number, followed by a comma, followed by the quiz number (e.g. \"1,2\").");
		System.out.println("To finish:");
		System.out.println("    Enter \"0\".");
		System.out.println("The following retake sessions have the following quizzes available:\n");

		ArrayList<String> IDPairs = new ArrayList<>();
		String input = "";
		
		while (!input.equals("0")) {
		
			for (retakeBean r : retakesList) {
				LocalDate retakeDay = r.getDate();
				if (!(retakeDay.isBefore(today)) && !(retakeDay.isAfter(endDay))) {
					// if skip && retakeDay is after the skip week, print a white bg message
					if (skip && retakeDay.isAfter(origEndDay)) { // A "skip" week such as spring break.
						System.out.println(retakeDay + " falls within a skip week.");
						// Just print for the FIRST retake day after the skip week
						skip = false;
					}
					
					// format: Friday, January 12, at 10:00am in EB 4430
					System.out.println(r.getID() + ": " + retakeDay.getDayOfWeek() + ", " + retakeDay.getMonth() + " "
							+ retakeDay.getDayOfMonth() + ", at " + r.timeAsString() + " in " + r.getLocation());
	
					for (quizBean q : quizList) {
						LocalDate quizDay = q.getDate();
						LocalDate lastAvailableDay = quizDay.plusDays(new Long(daysAvailable));
						// To retake a quiz on a given retake day, the retake day must be within two
						// ranges:
						// quizDay <= retakeDay <= lastAvailableDay --> (!quizDay > retakeDay) &&
						// !(retakeDay > lastAvailableDay)
						// today <= retakeDay <= endDay --> !(today > retakeDay) && !(retakeDay >
						// endDay)
	
						if (!quizDay.isAfter(retakeDay) && !retakeDay.isAfter(lastAvailableDay)
								&& !today.isAfter(retakeDay) && !retakeDay.isAfter(endDay)) {
							System.out.println("    " + q.getID() + ": " + "Quiz " + q.getID() + " from "
									+ quizDay.getDayOfWeek() + ", " + quizDay.getMonth() + " "
									+ quizDay.getDayOfMonth());
						}
					}
				}
			}
			
			System.out.print("Enter input: ");
			input = scan.nextLine().trim();
			
			if (!input.equals("0")) {
				IDPairs.add(input);
			}
		}
		System.out.println("\nAll quiz retake opportunities:");
		for (retakeBean r : retakesList) {
			System.out.println(r);
		}
		
		writeRetakeData(course.getCourseID(), studentName, IDPairs);
	}

} // end quizschedule class
