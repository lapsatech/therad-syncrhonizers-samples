package concurrent.synchronizers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class SomeJobs_CyclicBarierExample {

  private static final int N = 10;

  private final static IntSupplier TIME_GENERATOR = () -> (int) (Math.random() * 10) + 1;
  private final static Consumer<String> LOGGER = System.out::println;

  private final CyclicBarrier barrier;

  class Job implements Runnable {
    final int jobId;

    private boolean askForStop = false;

    private Duration duration;

    Job(int jobId) {
      this.jobId = jobId;
    }

    public void run() {
      try {
	while (!askForStop) {
	  int sleepSeconds = TIME_GENERATOR.getAsInt();
	  LOGGER.accept(String.format("Job %1$s on %2$s seconds STARTED", jobId, sleepSeconds));
	  long startedMillis = System.nanoTime();
	  try {
	    Thread.sleep(sleepSeconds * 1000);
	  } finally {
	    duration = Duration.ofNanos(System.nanoTime() - startedMillis);
	    LOGGER.accept(String.format("Job %1$s %2$s after %3$s seconds", jobId,
		askForStop ? "FINISHED" : "COMPLETED", sleepSeconds));
	  }
	  barrier.await();
	}
      } catch (InterruptedException | BrokenBarrierException ex) {
      }
    }

    public Duration getDuration() {
      return duration;
    }
  }

  public SomeJobs_CyclicBarierExample(Runnable barrierAction) throws InterruptedException {
    barrier = new CyclicBarrier(N, barrierAction);
  }

  public static final void main(String[] args) throws InterruptedException {

    final List<Job> workers = new ArrayList<>(N);

    SomeJobs_CyclicBarierExample example = new SomeJobs_CyclicBarierExample(() -> {

      final DoubleSummaryStatistics durationStatistics = workers.stream() //
	  .map(Job::getDuration)
	  .peek(System.out::println)
	  .mapToDouble(x -> x.getSeconds() + (x.getNano() / 1_000_000_000d))
	  .summaryStatistics();

      LOGGER.accept(String.format(Locale.ENGLISH, "Average duration of jobs is %1$.3f seconds", //
	  durationStatistics.getAverage()));

      LOGGER.accept(String.format(Locale.ENGLISH, "Summary duration of jobs is %1$.3f seconds", //
	  durationStatistics.getSum()));

    });

    List<Thread> threads = new ArrayList<Thread>(N);
    for (int i = 0; i < N; i++) {
      Job w = example.new Job(i + 1);
      workers.add(w);
      Thread thread = new Thread(w);
      threads.add(thread);
      thread.start();
    }

    StopDaemon.waitForUserToStop();

    LOGGER.accept("Finishing...");
    workers.forEach(w -> w.askForStop = true);
    for (Thread thread : threads)
      thread.join();
    LOGGER.accept("Finished");
  }
}

class DurationMutable {
  Duration d = Duration.ZERO;

  static void accumulate(DurationMutable acc, Duration add) {
    acc.d = acc.d.plus(add);
  }

  static void combine(DurationMutable add1, DurationMutable add2) {
    accumulate(add1, add2.d);
  }
}
