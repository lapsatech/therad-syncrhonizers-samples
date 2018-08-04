package concurrent.synchronizers;

import java.util.Scanner;

final class StopDaemon {

  private StopDaemon() {
  }

  static void waitForUserToStop() throws InterruptedException {
    Thread t = new Thread(() -> {
      try (Scanner s = new Scanner(System.in)) {
	String line;
	do {
	  System.out.println("Stop (y/n) ?: ");
	  line = s.nextLine();
	} while (!line.equals("y"));
	System.out.println("Stop is required");
      }
    });
    t.start();
    t.join();
  }
}
