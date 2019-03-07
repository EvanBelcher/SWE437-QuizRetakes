import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringJoiner;

public class ProfessorQuizRetakeScheduler {
	
	private static final String dataLocation = System.getProperty("user.dir").replace('\\', '/') + "/";
	private static final String courseBase = "course";
	private static final String retakesBase = "quiz-retakes";
	private static final String apptsBase = "quiz-appts";
	private static final String quizzesBase = "quiz-orig";
	
	private static retakes retakesList;
	private static ArrayList<apptBean> apptsList;
	private static quizzes quizzesList;
	private static courseBean course;
	private static String courseID;


	public static void main(String[] args) throws IOException {
		Scanner in = new Scanner(System.in);
		
		getInfo(in);
		displayRetakes();
		showMenu(in);
		
		in.close();
		
		CliUtils.print("Goodbye.");

	}
	
	private static void showMenu(Scanner in) throws IOException {
		System.out.println("Menu: ");
		System.out.println("Quiz - Add a quiz");
		System.out.println("Retake - Add a retake");
		
		String menuOption = in.nextLine();
		
		if(menuOption.equalsIgnoreCase("quiz")) {
			addQuiz(in);
		} else if(menuOption.equalsIgnoreCase("retake")) {
			addRetake(in);
		}
		
	}
	
	private static void addRetake(Scanner in) throws IOException {
		System.out.println("Enter the following retake information:");
		String location = CliUtils.ask("Location given: ", in);
		int month = CliUtils.askInt("Month given:", in);
		int day = CliUtils.askInt("Day given:", in);
		int hour = CliUtils.askInt("Hour given: ", in);
		int minute = CliUtils.askInt("Minute given: ", in);
		
		
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";
		
		byte[] encoded = Files.readAllBytes(Paths.get(retakesFileName));
		String currentRetakeXmlFile = new String(encoded, "UTF-8");
		currentRetakeXmlFile = currentRetakeXmlFile.replace("</retakes>", "");			

		BufferedWriter out = new BufferedWriter(new FileWriter(retakesFileName, false));
		
		StringJoiner xmlJoiner = new StringJoiner("\n");
		xmlJoiner.add("<retake>").add("<id>" + (retakesList.size() + 1) + "</id>")
		.add("<location>" + location + "</location>").add("<dateGiven>").add("<month>" + month + "</month>").add("<day>" + day + "</day>").add("<hour>" + hour + "</hour>")
		.add("<minute>" + minute + "</minute>").add("</dateGiven>").add("</retake>").add("</retakes>");
		
		out.append(currentRetakeXmlFile + xmlJoiner.toString());
		out.close();
	}

	private static void addQuiz(Scanner in) throws IOException {
		System.out.println("Enter the following quiz information:");
		int month = CliUtils.askInt("Month given:", in);
		int day = CliUtils.askInt("Day given:", in);
		int hour = CliUtils.askInt("Hour given: ", in);
		int minute = CliUtils.askInt("Minute given: ", in);
		
		String quizzesFileName = dataLocation + quizzesBase + "-" + courseID + ".xml";
					
		byte[] encoded = Files.readAllBytes(Paths.get(quizzesFileName));
		String currentQuizXmlFile = new String(encoded, "UTF-8");
		currentQuizXmlFile = currentQuizXmlFile.replace("</quizzes>", "");			

		BufferedWriter out = new BufferedWriter(new FileWriter(quizzesFileName, false));
		
		StringJoiner xmlJoiner = new StringJoiner("\n");
		xmlJoiner.add("<quiz>").add("<id>" + (quizzesList.size() + 1) + "</id>")
		.add("<dateGiven>").add("<month>" + month + "</month>").add("<day>" + day + "</day>").add("<hour>" + hour + "</hour>")
		.add("<minute>" + minute + "</minute>").add("</dateGiven>").add("</quiz>").add("</quizzes>");
		
		out.append(currentQuizXmlFile + xmlJoiner.toString());
		out.close();
	}

	private static void displayRetakes() {
		if(apptsList.isEmpty()) {
			CliUtils.print("There are no currently scheduled retakes.");
			return;
		}
		apptsList.sort((apptBean a, apptBean b) -> {
			retakeBean aRetake = findRetake(a.getRetakeID());
			retakeBean bRetake = findRetake(b.getRetakeID());
			int dateCompare = aRetake.getDate().compareTo(bRetake.getDate());
			if(dateCompare != 0)
				return dateCompare;
			return a.getQuizID() - b.getQuizID();
		});
		
		System.out.println("\nHere are your scheduled retakes:\n");
		
		apptBean previousAppt = new apptBean(Integer.MIN_VALUE,Integer.MIN_VALUE,null);
		for(apptBean appt : apptsList) {
			if(appt.getRetakeID() == previousAppt.getRetakeID()) {
				if(appt.getQuizID() == previousAppt.getQuizID()) {
					System.out.print(", " + appt.getName());
				}else {
					System.out.print("\n" + "Quiz " + appt.getQuizID() + " - " + appt.getName());
				}
			} else {
				if(previousAppt.getName() != null) {
					System.out.println("\n");
				}
				retakeBean retake = findRetake(appt.getRetakeID());
				System.out.println(retake.dateAsString() + " - " + retake.getLocation() + ":");
				System.out.print("Quiz " + appt.getQuizID() + " - " + appt.getName());
			}
			
			previousAppt = appt;
		}
		System.out.println("\n");
	}

	@SuppressWarnings("unchecked")
	private static void getInfo(Scanner in) {
		do {
			courseID = CliUtils.ask(
					"Please enter your course ID. It is probably the same as the university course ID, with no spaces.",
					in);
			course = getCourse(courseID);
		} while (course == null);
	
		// Filenames to be built from above and the courseID
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";
		String apptsFileName = dataLocation + apptsBase + "-" + courseID + ".txt";
		String quizzesFileName = dataLocation + quizzesBase + "-" + courseID + ".xml";
	
		// Load the retakes and appts from disk
		retakesList = new retakes();
		apptsList = new ArrayList<>();
		quizzesList = new quizzes();
		retakesReader rr = new retakesReader();
		apptsReader ar = new apptsReader();
		quizReader qr = new quizReader();
	
		try { // Read the files and save data to variables
			CliUtils.hidePrints();
			retakesList = rr.read(retakesFileName);
			apptsList = (ArrayList<apptBean>) ar.read(apptsFileName);
			quizzesList = qr.read(quizzesFileName);
		} catch (Exception e) {
			CliUtils.showPrints();
			String message = "Can't find the data files for course ID " + courseID + ". You can try again.";
			CliUtils.print(message);
		} finally {
			CliUtils.showPrints();
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
	
	private static retakeBean findRetake(int retakeID) {
		for(retakeBean retake : retakesList) {
			if(retake.getID() == retakeID) {
				return retake;
			}
		}
		return null;
	}

}
