package concurrent.synchronizers;

import java.util.StringJoiner;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CarParking_SemaphorePhaserExamples {

  private final static Consumer<String> LOGGER = System.out::println;
  private final static Consumer<String> TABLO_LOGGER = System.err::println;
  private final static Supplier<Integer> NEW_CAR_INTERVAL_SECONDS = () -> (int) (Math.random() * 5);
  private final static Supplier<Integer> CAR_STAY_INTERVAL_SECONDS = () -> 1 + (int) (Math.random() * 40);
  private static final Supplier<Integer> CAR_WAIT_INTERVAL_SECONDS = () -> (int) (Math.random() * 10);

  private static final int PARKING_PLACES_COUNT = 20;

  // Парковочное место занято - true, свободно - false
  private static final boolean[] PARKING_PLACES = new boolean[PARKING_PLACES_COUNT];

  // Устанавливаем флаг "справедливый", в таком случае метод
  // aсquire() будет раздавать разрешения в порядке очереди
  private static final Semaphore PARKING_PLACE_AVAILABLE_SEMAPHORE = new Semaphore(PARKING_PLACES_COUNT, true);

  private static AtomicInteger OK_CARS = new AtomicInteger(0);
  private static AtomicInteger FAILED_CARS = new AtomicInteger(0);

  static class Car implements Runnable {

    private final int carNumber;
    private final int stayIntervalSeconds;
    private final int waitForPlaceIntervalSeconds;

    public Car(int carNumber, int stayIntervalSeconds, int waitForPlaceIntervalSeconds) {
      this.carNumber = carNumber;
      this.stayIntervalSeconds = stayIntervalSeconds;
      this.waitForPlaceIntervalSeconds = waitForPlaceIntervalSeconds;
    }

    @Override
    public void run() {
      WAIT_FOR_THE_LAST_CAR_PHASER.register();
      LOGGER.accept(String.format("Автомобиль №%d подъехал к парковке", carNumber));
      try {
	if (!PARKING_PLACE_AVAILABLE_SEMAPHORE.tryAcquire(waitForPlaceIntervalSeconds, TimeUnit.SECONDS)) {
	  LOGGER.accept(String.format("Автомобиль №%d недождавшись свободного места уехал", carNumber));
	  FAILED_CARS.incrementAndGet();
	} else {
	  int parkingNumber;

	  // Ищем свободное место и паркуемся
	  synchronized (PARKING_PLACES) {
	    do {
	      parkingNumber = (int) (Math.random() * PARKING_PLACES.length);
	    } while (PARKING_PLACES[parkingNumber]);
	    PARKING_PLACES[parkingNumber] = true; // занимаем место
	  }

	  LOGGER.accept(String.format("Автомобиль №%d припарковался на месте %d на %d секунд", carNumber,
	      parkingNumber + 1, stayIntervalSeconds));
	  OK_CARS.incrementAndGet();

	  Thread.sleep(stayIntervalSeconds * 1000);

	  synchronized (PARKING_PLACES) {
	    PARKING_PLACES[parkingNumber] = false;
	  }

	  PARKING_PLACE_AVAILABLE_SEMAPHORE.release();

	  LOGGER.accept(String.format("Автомобиль №%d покинул парковку освободив место %d", carNumber,
	      parkingNumber + 1));
	}

      } catch (InterruptedException e) {
      }
      WAIT_FOR_THE_LAST_CAR_PHASER.arriveAndDeregister();
    }
  }

  static class MainRunner implements Runnable {
    private boolean stopRequested = false;

    public void run() {
      try {
	for (int autoNumber = 1; !stopRequested; autoNumber++) {
	  new Thread(new Car(autoNumber, //
	      CAR_STAY_INTERVAL_SECONDS.get(), //
	      CAR_WAIT_INTERVAL_SECONDS.get())).start();
	  Thread.sleep(NEW_CAR_INTERVAL_SECONDS.get() * 1000);
	}
      } catch (InterruptedException e) {
      }
    }
  }

  static class TabloRunner implements Runnable {
    private boolean stopRequested = false;

    public void run() {
      try {
	final int currentPhase = WAIT_FOR_THE_LAST_CAR_PHASER.getPhase();
	int nextPhase = currentPhase;
	do {
	  printTablo();
	  try {
	    nextPhase = WAIT_FOR_THE_LAST_CAR_PHASER.awaitAdvanceInterruptibly(currentPhase, 2, TimeUnit.SECONDS);
	  } catch (TimeoutException e) {
	  }
	} while (!stopRequested || currentPhase == nextPhase);
      } catch (InterruptedException e) {
      }
      printTablo();
      printSummary();
    }

    private void printSummary() {
      TABLO_LOGGER.accept(String.format("Всего автомобилей воспользовавшихся парковкой %d", OK_CARS.get()));
      TABLO_LOGGER.accept(String.format("Всего автомобилей недождавшихся своей очереди %d", FAILED_CARS.get()));
    }

    void printTablo() {
      StringJoiner sj = new StringJoiner(" ", "Табло парковки: [", "]");
      synchronized (PARKING_PLACES) {
	for (int i = 0; i < PARKING_PLACES.length; i++) {
	  sj.add(String.format(PARKING_PLACES[i] ? "X" : "-"));
	}
      }
      TABLO_LOGGER.accept(sj.toString());
    }
  }

  private static Phaser WAIT_FOR_THE_LAST_CAR_PHASER = new Phaser();

  public static void main(String[] args) throws InterruptedException {
    MainRunner main = new MainRunner();
    Thread mainThread = new Thread(main);
    mainThread.start();

    TabloRunner tablo = new TabloRunner();
    Thread tabloThread = new Thread(tablo);
    tabloThread.start();

    StopDaemon.waitForUserToStop();

    TABLO_LOGGER.accept("Паркова закрывается");

    main.stopRequested = true;
    tablo.stopRequested = true;
    
    mainThread.join();
    tabloThread.join();
  }
}
