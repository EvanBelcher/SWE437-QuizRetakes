import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.StringJoiner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfessorQuizRetakeSchedulerTest {
	
	private static final String QUIZZES_FILE_NAME = System.getProperty("user.dir").replace('\\', '/') + "/quiz-orig-swe437.xml";
	private static final String RETAKES_FILE_NAME = System.getProperty("user.dir").replace('\\', '/') + "/quiz-retakes-swe437.xml";
	
	@Test
	void testAddQuiz() throws Exception {
		setupInput("swe437", "Quiz", "3", "8", "11", "0");
		
		quizReader qr = new quizReader();
		quizzes beforeQuizList = qr.read(QUIZZES_FILE_NAME);
		
		ProfessorQuizRetakeScheduler.main(null);
		
		quizzes afterQuizList = qr.read(QUIZZES_FILE_NAME);
				
		assertEquals(beforeQuizList.size() + 1, afterQuizList.size());
	}
	
	@Test
	void testAddRetake() throws Exception {
		setupInput("swe437", "Retake", "EB 4430", "3", "10", "12", "30");
		
		retakesReader rr = new retakesReader();
		retakes beforeRetakesList = rr.read(RETAKES_FILE_NAME);
		
		ProfessorQuizRetakeScheduler.main(null);
		
		retakes afterRetakesList = rr.read(RETAKES_FILE_NAME);
		
		assertEquals(beforeRetakesList.size() + 1, afterRetakesList.size());
	}
	
	private void setupInput(String... lines) {
		StringJoiner inputJoiner = new StringJoiner("\n");
		for(String line: lines) {
			inputJoiner.add(line);
		}
		InputStream in = new ByteArrayInputStream(inputJoiner.toString().getBytes());
		System.setIn(in);
	}

}
