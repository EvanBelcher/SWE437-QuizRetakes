

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class quizschedTest {	
	/*
	 * (Copied for convenience from quizsched.java:
	 * 
	 * There were two changes to the printQuizScheduleForm() method:
	 * - Changed this method from private to package-private. This adresses controllability by allowing us to isolate the invocation of this method in the tests.
	 * - Added the "LocalDate today" parameter, replacing the declaration of this variable inside the method. This uses dependency injection to address controllability,
	 * 		allowing us to pass in our own date for today and thereby making the tests consistent.
	 */
	
	PrintStream consoleOut = System.out;
	
	ByteArrayOutputStream baos;
	PrintStream ps;
	
	quizzes quizList;
	retakes retakeList;
	courseBean course;
	
	LocalDate todayForTest = LocalDate.of(2019, 2, 24);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		//Get the console output. This addresses observability by allowing us to get the output that would be sent to console.
		baos = new ByteArrayOutputStream();
		ps = new PrintStream(baos, true, "UTF-8");
		System.setOut(ps);
		
		//Set up parameters. This leverages the change we made in making the method package-private for increased controllability.
		quizList = new quizzes();
		quizList.addQuiz(new quizBean(1, 2, 10, 10, 30));
		quizList.addQuiz(new quizBean(2, 2, 11, 10, 30));
		
		retakeList = new retakes();
		retakeList.addRetake(new retakeBean(0, "A location", 2, 24, 10, 30));
		retakeList.addRetake(new retakeBean(1, "A location", 2, 25, 18, 00));
		retakeList.addRetake(new retakeBean(2, "A different location", 2, 25, 18, 30));

		course = new courseBean("swe437", "Software testing", "14", LocalDate.of(2019, 1, 21), LocalDate.of(2019, 1, 25), "/var/www/CS/webapps/offutt/WEB-INF/data/");
	}

	@AfterEach
	void tearDown() throws Exception {
		ps.close();
		baos.close();
	}

	/**
	 * If the quiz list is null, a NullPointerException should be thrown
	 * It's arguable that the program should handle this more gracefully, but this seems acceptable.
	 */
	@Test
	void testQuizListNull() {
		assertThrows(NullPointerException.class, () -> quizsched.printQuizScheduleForm(null, retakeList, course, todayForTest));
	}
	
	/**
	 * If the quiz list is empty, there should be no exception thrown
	 */
	@Test
	void testQuizListEmpty() {
		quizsched.printQuizScheduleForm(new quizzes(), retakeList, course, todayForTest); // Fail if an exception is thrown
	}
	
	/**
	 * If the quiz list is populated out of order, the quizzes should still be presented in id or chronological order
	 * This test fails on the current implementation
	 */
	@Test
	void testOutOfOrderQuizzes() {
		quizList = new quizzes();
		quizList.addQuiz(new quizBean(2, 2, 11, 10, 30));
		quizList.addQuiz(new quizBean(1, 2, 10, 10, 30));
		
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest);
		String output = getOutput();
		
		assertTrue(output.indexOf("Quiz 1") < output.indexOf("Quiz 2"));
	}
	
	/**
	 * If the method is run with normal parameters, the quizzes should be cut off at exactly two weeks
	 */
	@Test
	void testQuiz2WeekCutoff() {
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest); // Fail if an exception is thrown
		String output = getOutput();
		
		// Test that the cut-off for quizzes is exactly two weeks
		assertEquals(1, countOccurrenceInString(output, "Quiz 1 from SUNDAY, FEBRUARY 10"));
		assertEquals(3, countOccurrenceInString(output, "Quiz 2 from MONDAY, FEBRUARY 11"));
	}
	
	/**
	 * If the retake list is null, a NullPointerException should be thrown
	 * It's arguable that the program should handle this more gracefully, but this seems acceptable.
	 */
	@Test
	void testRetakeListNull() {
		assertThrows(NullPointerException.class, () -> quizsched.printQuizScheduleForm(quizList, null, course, todayForTest));
	}
	
	/**
	 * If the retake list is empty, there should be no exception thrown
	 */
	@Test
	void testRetakeListEmpty() {
		quizsched.printQuizScheduleForm(quizList, new retakes(), course, todayForTest); // Fail if an exception is thrown
	}
	
	/**
	 * If the retake list is populated out of order, the method should still print them out in id or chronological order
	 * This test fails on the current implementation
	 */
	@Test
	void testOutOfOrderRetakes() {
		retakeList = new retakes();
		retakeList.addRetake(new retakeBean(0, "A location", 2, 24, 10, 30));
		retakeList.addRetake(new retakeBean(2, "A different location", 2, 25, 18, 30));
		retakeList.addRetake(new retakeBean(1, "A location", 2, 25, 18, 00));
		
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest);
		String output = getOutput();
		
		assertTrue(output.lastIndexOf("MONDAY, FEBRUARY 25, at 18:00 in A location") < output.lastIndexOf("MONDAY, FEBRUARY 25, at 18:30 in A different location"));
	}
	
	/**
	 * If a retake is before today or after the end day, it should not be shown
	 */
	@Test
	void testRetakeOutOfBounds() {
		retakeList = new retakes();
		retakeList.addRetake(new retakeBean(0, "A location", 2, 22, 10, 30));
		retakeList.addRetake(new retakeBean(1, "A location", 2, 25, 18, 00));
		retakeList.addRetake(new retakeBean(2, "A different location", 3, 15, 18, 30));
		
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest);
		String output = getOutput();

		assertFalse(output.contains("FRIDAY, FEBRUARY 22, at 10:30 in A location"));
		assertFalse(output.contains("FRIDAY, MARCH 15, at 18:30 in A different location"));
	}
	
	/**
	 * If a retake is before the quiz day or after the last available day to retake the quiz, it should not be shown
	 */
	@Test
	void testRetakeOutOfBoundsForQuiz() {
		quizList = new quizzes();
		quizList.addQuiz(new quizBean(2, 2, 11, 10, 30));
		
		retakeList = new retakes();
		retakeList.addRetake(new retakeBean(0, "A location", 1, 22, 10, 30));
		retakeList.addRetake(new retakeBean(1, "A location", 2, 25, 18, 00));
		retakeList.addRetake(new retakeBean(2, "A different location", 2, 27, 18, 30));
		
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest);
		String output = getOutput();

		assertFalse(output.contains("TUESDAY, JANUARY 22, at 10:30 in A location"));
		assertFalse(output.contains("WEDNESDAY, FEBRUARY 27, at 18:30 in A different location"));
	}
	
	/**
	 * If the course is null, a NullPointerException should be thrown
	 * It's arguable that the program should handle this more gracefully, but this seems acceptable.
	 */
	@Test
	void testCourseNull() {
		assertThrows(NullPointerException.class, () -> quizsched.printQuizScheduleForm(quizList, retakeList, null, todayForTest));
	}
	
	@Test
	void testSkipWeek() {
		course = new courseBean("swe437", "Software testing", "14", LocalDate.of(2019, 3, 8), LocalDate.of(2019, 3, 15), "/var/www/CS/webapps/offutt/WEB-INF/data/");
		retakeList.addRetake(new retakeBean(2, "A different location", 3, 16, 18, 30));
		retakeList.addRetake(new retakeBean(2, "A different location", 3, 17, 18, 30));
		
		quizsched.printQuizScheduleForm(quizList, retakeList, course, todayForTest);
		String output = getOutput();
		
		assertTrue(output.contains("Currently scheduling quizzes for the next two weeks, until SUNDAY, MARCH 17"));
		assertEquals(1, countOccurrenceInString(output, "Skipping a week, no quiz or retakes.")); // Makes sure this prints exactly once
	}
	
	/**
	 * If the LocalDate representing today's date is null, a NullPointerException should be thrown
	 * It's arguable that the program should handle this more gracefully, but this seems acceptable.
	 */
	@Test
	void testTodayNull() {
		assertThrows(NullPointerException.class, () -> quizsched.printQuizScheduleForm(quizList, retakeList, course, null));
	}
	
	/*
	 * 
	 * Utility methods
	 * 
	 */
	
	/**
	 * Gets the output that would have been sent to console as a string.
	 * Also prints it to the console for debugging purposes.
	 * 
	 * This addresses observability by allowing us to get the output that would be sent to console.
	 */
	private String getOutput() {
		System.setOut(consoleOut);
		String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		System.out.println(output);
		return output;
	}
	
	/**
	 * Counts the number of occurrences of token in str
	 */
	private int countOccurrenceInString(String str, String token) {
		int count = 0;
		int index = 0;
		while(index >= 0) {
			index = str.indexOf(token, index);
			if(index >= 0) {
				count++;
				index++;
			}
		}
		return count;
	}

}
