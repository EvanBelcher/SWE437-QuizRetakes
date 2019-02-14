import java.util.ArrayList;
import java.util.Scanner;

public class ProfessorQuizRetakeScheduler {
	
	private static final String dataLocation = System.getProperty("user.dir").replace('\\', '/') + "/";
	private static final String courseBase = "course";
	private static final String retakesBase = "quiz-retakes";
	private static final String apptsBase = "quiz-appts";
	
	private static retakes retakesList;
	private static ArrayList<apptBean> apptsList;
	private static courseBean course;


	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		
		getInfo(in);
		displayRetakes();
		
		in.close();
		
		CliUtils.print("Goodbye.");

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
		String courseID;
		do {
			courseID = CliUtils.ask(
					"Please enter your course ID. It is probably the same as the university course ID, with no spaces.",
					in);
			course = getCourse(courseID);
		} while (course == null);
	
		// Filenames to be built from above and the courseID
		String retakesFileName = dataLocation + retakesBase + "-" + courseID + ".xml";
		String apptsFileName = dataLocation + apptsBase + "-" + courseID + ".txt";
	
		// Load the retakes and appts from disk
		retakesList = new retakes();
		apptsList = new ArrayList<>();
		retakesReader rr = new retakesReader();
		apptsReader ar = new apptsReader();
	
		try { // Read the files and save data to variables
			CliUtils.hidePrints();
			retakesList = rr.read(retakesFileName);
			apptsList = (ArrayList<apptBean>) ar.read(apptsFileName);
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
