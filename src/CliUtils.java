import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class CliUtils {
	
	private static PrintStream originalOut = System.out;

	static String ask(String message, Scanner in) {
		String answer;
		do {
			System.out.println(message);
			answer = in.nextLine().trim();
		} while (answer == null || answer.isEmpty());
		return answer;
	}
	
	static int askInt(String message, Scanner in) {
		int answer = 0;
		boolean invalid = false;
		do {
			invalid = false;
			System.out.println(message);
			try {
				answer = in.nextInt();
			} catch(Exception e) {
				invalid = true;
			}
		} while (invalid);
		return answer;
	}

	static void print(Object... things) {
		if (things.length == 0) {
			System.out.println();
		}
		for (Object thing : things) {
			System.out.println(thing);
		}
	}

	static void hidePrints() {
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int arg0) throws IOException {
			}
		}));
	}

	static void showPrints() {
		System.setOut(originalOut);
	}
	
}
