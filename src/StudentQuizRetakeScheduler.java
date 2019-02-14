import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

public class StudentQuizRetakeScheduler {

	private static final String dataLocation = System.getProperty("user.dir").replace('\\', '/') + "/";
	private static final String separator = ",";
	private static final String courseBase = "course";
	private static final String quizzesBase = "quiz-orig";
	private static final String retakesBase = "quiz-retakes";
	private static final String apptsBase = "quiz-appts";

	private static quizzes quizList;
	private static retakes retakesList;
	private static courseBean course;
	private static int daysAvailable = 14;
	private static ArrayList<SessionID> selectedSessionIDs;

	private static StudentQuizRetakeScheduler thisObj = new StudentQuizRetakeScheduler();

	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);

		getInfo(in);
		showSessionSelection(in);
		requestSessions(in);

		in.close();
		
		CliUtils.print("Goodbye.");
	}

	private static void getInfo(Scanner in) {
		String courseID;
		do {
			courseID = CliUtils.ask(
					"Please enter the course ID given to you by your instructor. It is probably the same as the university course ID, with no spaces.",
					in);
			course = getCourse(courseID);
		} while (course == null);
		daysAvailable = Integer.parseInt(course.getRetakeDuration());
	
		// Filenames to be built from above and the courseID
		String quizzesFileName = dataLocation + quizzesBase + "-" + courseID + ".xml";
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";
	
		// Load the quizzes and the retake times from disk
		quizList = new quizzes();
		retakesList = new retakes();
		quizReader qr = new quizReader();
		retakesReader rr = new retakesReader();
	
		try { // Read the files and print the form
			CliUtils.hidePrints();
			quizList = qr.read(quizzesFileName);
			retakesList = rr.read(retakesFileName);
		} catch (Exception e) {
			CliUtils.showPrints();
			String message = "Can't find the data files for course ID " + courseID + ". You can try again.";
			CliUtils.print(message);
		} finally {
			CliUtils.showPrints();
		}
	}

	private static void showSessionSelection(Scanner in) {
		CliUtils.print("", "Retakes and quizzes scheduled for course " + course.getCourseTitle(), "");

		CliUtils.print("All quiz retake opportunities:");
		for (retakeBean r : retakesList) {
			CliUtils.print(r);
		}
		CliUtils.print("", "------------------------------------------", "");

		boolean retakePrinted = false;

		// Check for a week to skip
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
		CliUtils.print("Today is " + today.getDayOfWeek() + ", " + today.getMonth() + " " + today.getDayOfMonth()
				+ ". Currently scheduling quizzes for the next two weeks, until " + endDay.getDayOfWeek() + ", "
				+ endDay.getMonth() + " " + endDay.getDayOfMonth()
				+ ". The next two weeks are shown in the list below.",
				"Please type the id number of the session you wish to sign up for.",
				"You may only sign up for one session at a time.",
				"Type the session of a currently selected id to deselect it.",
				"Type \"done\" to finish selecting retake sessions", "");

		selectedSessionIDs = new ArrayList<>();
		while (true) {

			for (retakeBean r : retakesList) {
				LocalDate retakeDay = r.getDate();
				if (!(retakeDay.isBefore(today)) && !(retakeDay.isAfter(endDay))) {
					// if skip && retakeDay is after the skip week, print a white bg message
					if (skip && retakeDay.isAfter(origEndDay)) { // A "skip" week such as spring break.
						// Just print for the FIRST retake day after the skip week
						CliUtils.print("------- Skipping a week, no quiz or retakes. -----------");
						skip = false;
					}
					retakePrinted = true;
					// format: Friday, January 12, at 10:00am in EB 4430
					CliUtils.print(retakeDay.getDayOfWeek() + ", " + retakeDay.getMonth() + " " + retakeDay.getDayOfMonth()
							+ ", at " + r.timeAsString() + " in " + r.getLocation());

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
							String sessionIDString = getSessionIDString(r, q);
							CliUtils.print((selectedSessionIDs.contains(thisObj.new SessionID(r.getID(), q.getID()))
									? "Selected: "
									: "") + "Quiz " + q.getID() + " from " + quizDay.getDayOfWeek() + ", "
									+ quizDay.getMonth() + " " + quizDay.getDayOfMonth() + " - ID: " + sessionIDString);
						}
					}
				}
				if (retakePrinted) {
					retakePrinted = false;
					CliUtils.print();
				}
			}

			String sessionIDString = CliUtils.ask("Enter the session id here: ", in);
			if (sessionIDString == null || sessionIDString.isEmpty()) {
				continue;
			}
			if (sessionIDString.equalsIgnoreCase("done")) {
				break;
			}
			String[] sessionIDParts = sessionIDString.split("\\.");
			if (sessionIDParts.length != 2) {
				continue;
			}
			try {
				int retakeID = Integer.parseInt(sessionIDParts[0]);
				int quizID = Integer.parseInt(sessionIDParts[1]);
				SessionID sessionID = thisObj.new SessionID(retakeID, quizID);

				if (selectedSessionIDs.contains(sessionID)) {
					selectedSessionIDs.remove(sessionID);
				} else {
					selectedSessionIDs.add(sessionID);
				}
			} catch (NumberFormatException e) {
				// Invalid id
				CliUtils.print("Invalid session ID. These should take the form \"3.8\"");
			}
		}
	}

	private static void requestSessions(Scanner in) {
		String apptsFileName = dataLocation + apptsBase + "-" + course.getCourseID() + ".txt";

		String studentName = CliUtils.ask("Please enter your name:", in);

		if (!selectedSessionIDs.isEmpty() && studentName != null && !studentName.isEmpty()) {
			// No saving if IOException
			boolean IOerrFlag = false;
			String IOerrMessage = "";

			// Append the new appointment to the file
			try {
				File file = new File(apptsFileName);
				synchronized (file) { // Only one student should touch this file at a time.
					if (!file.exists()) {
						file.createNewFile();
					}
					FileWriter fw = new FileWriter(file.getAbsoluteFile(), true); // append mode
					BufferedWriter bw = new BufferedWriter(fw);

					for (SessionID sessionID : selectedSessionIDs) {
						bw.write(sessionID + separator + studentName + "\n");
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
				CliUtils.print(IOerrMessage);
			} else {
				if (selectedSessionIDs.size() == 1)
					CliUtils.print(studentName + ", your appointment has been scheduled.");
				else
					CliUtils.print(studentName + ", your appointments have been scheduled.");
				CliUtils.print("Please arrive in time to finish the quiz before the end of the retake period.");
				CliUtils.print("If you cannot make it, please cancel by sending email to your professor.");
			}
		} else {
			if (selectedSessionIDs.isEmpty())
				CliUtils.print("You didn't choose any quizzes to retake.");
			if (studentName == null || studentName.isEmpty())
				CliUtils.print("You didn't give a name ... no anonymous quiz retakes.");
		}
	}

	private static courseBean getCourse(String courseID) {
		courseBean course;
		courseReader cr = new courseReader();
		String courseFileName = dataLocation + courseBase + "-" + courseID + ".xml";
		try {
			CliUtils.hidePrints();
			course = cr.read(courseFileName);
			return course;
		} catch (Exception e) {
			CliUtils.showPrints();
			String message = "Can't find the data files for course ID " + courseID + ". You can try again.";
			CliUtils.print(message);
			return null;
		} finally {
			CliUtils.showPrints();
		}
	}

	private static String getSessionIDString(retakeBean retake, quizBean quiz) {
		return retake.getID() + "." + quiz.getID();
	}

	class SessionID {
		public int retakeNum;
		public int quizNum;

		public SessionID(int retakeNum, int quizNum) {
			this.retakeNum = retakeNum;
			this.quizNum = quizNum;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SessionID other = (SessionID) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			if (quizNum != other.quizNum)
				return false;
			if (retakeNum != other.retakeNum)
				return false;
			return true;
		}

		private StudentQuizRetakeScheduler getEnclosingInstance() {
			return StudentQuizRetakeScheduler.this;
		}

		@Override
		public String toString() {
			return retakeNum + separator + quizNum;
		}

	}

}
