package concurrent.synchronizers;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

public class CarFerry_CyclicBarrierExample {

  private static final int FERRY_CAPACITY = 3;

  private final static Consumer<String> LOGGER = System.out::println;

  private final static DoubleFunction<Integer> SECONDS_GENERATOR = (rr) -> ((int) (Math.random() * rr) + 1) * 1000;

  private static final CyclicBarrier BARRIER = new CyclicBarrier(FERRY_CAPACITY, new FerryBoat());

  public static void main(String[] args) throws InterruptedException {
    MainRunner main = new MainRunner();
    Thread mainThread = new Thread(main);
    mainThread.start();

    StopDaemon.waitForUserToStop();
    LOGGER.accept("Паром закрывается");

    main.stopRequested = true;
  }

  // Инициализируем барьер на три потока и таском, который будет выполняться,
  // когда
  // у барьера соберется три потока. После этого, они будут освобождены.

  static class MainRunner implements Runnable {
    private boolean stopRequested = false;

    public void run() {
      try {
	for (int autoNumber = 1; !stopRequested; autoNumber++) {
	  new Thread(new Car(autoNumber)).start();
	  Thread.sleep(SECONDS_GENERATOR.apply(2));
	}
      } catch (InterruptedException e) {
      }
    }
  }

  // Таск, который будет выполняться при достижении сторонами барьера
  public static class FerryBoat implements Runnable {
    @Override
    public void run() {
      try {
	LOGGER.accept("Паром начал переправлять автомобили!");
	Thread.sleep(7000);
	System.out.println("Паром переправил автомобили!");
      } catch (InterruptedException e) {
      }
    }
  }

  // Стороны, которые будут достигать барьера
  public static class Car implements Runnable {

    private final int carNumber;

    public Car(int carNumber) {
      this.carNumber = carNumber;
    }

    @Override
    public void run() {
      try {
	LOGGER.accept(String.format("Автомобиль №%d подъехал к паромной переправе", carNumber));
	// Для указания потоку о том что он достиг барьера, нужно
	// вызвать метод await()
	// После этого данный поток блокируется, и ждет пока остальные
	// стороны достигнут барьера
	BARRIER.await();
	LOGGER.accept(String.format("Автомобиль №%d продолжил движение", carNumber));
      } catch (InterruptedException | BrokenBarrierException e) {
      }
    }

  }
}