package concurrent.synchronizers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class CarRace_CountDownLatchExample {

  private static final int COMPETITORS_COUNT = 5;

  private final static Consumer<String> LOGGER = System.out::println;
  private static final IntSupplier CAR_SPEED = () -> new Random().nextInt(100) + 50; // скорость
										     // автомобиля
  private static final IntSupplier CAR_DELAY = () -> new Random().nextInt(10000); // скорость
										  // автомобиля

  private static enum CountDownMark {

    FIRST("На старт!"), SECOND("Внимание!"), START("Марш!");

    private final String message;

    private CountDownMark(String message) {
      this.message = message;
    }

    @Override
    public String toString() {
      return message;
    }

  }

  private static final CountDownLatch START_COUNTER = new CountDownLatch(CountDownMark.values().length);

  private static final CountDownLatch STARTERS_COUNTER = new CountDownLatch(COMPETITORS_COUNT);
  private static final CountDownLatch FINISHERS_COUNTER = new CountDownLatch(COMPETITORS_COUNT);

  // Условная длина гоночной трассы
  private static final int TRACK_LENGTH = 500_000;

  public static void main(String[] args) throws InterruptedException {
    Car[] cars = new Car[COMPETITORS_COUNT];

    for (int i = 0; i < COMPETITORS_COUNT; i++) {
      cars[i] = new Car(i + 1);
      new Thread(cars[i]).start();
    }

    STARTERS_COUNTER.await();
    for (CountDownMark m : CountDownMark.values()) {
      Thread.sleep(1000);
      LOGGER.accept(m.toString());
      START_COUNTER.countDown();// Команда дана, уменьшаем счетчик на 1
    }

    FINISHERS_COUNTER.await();
    LOGGER.accept(String.format("Гонка завершена. Победитель - автомобиль №%s", //
	Arrays.stream(cars) //
	    .max(Comparator.comparingInt(c -> c.carSpeed)) // макс.скорость
	    .get().carNumber // стартовый номер авто
    ));
  }

  public static class Car implements Runnable {

    private final int carNumber;
    private final int carSpeed;
    private final int carDelayToStart;

    public Car(final int carNumber) {
      this.carNumber = carNumber;
      this.carSpeed = CAR_SPEED.getAsInt();
      this.carDelayToStart = CAR_DELAY.getAsInt();
    }

    @Override
    public void run() {
      try {
	drive();
	STARTERS_COUNTER.countDown();
	START_COUNTER.await(); // ждем на-старт внимание марш
	race();
	FINISHERS_COUNTER.countDown();
      } catch (InterruptedException e) {
      }
    }

    private void drive() throws InterruptedException {
      LOGGER.accept(String.format("Объявляется участник автомобиль №%d.", carNumber));
      Thread.sleep(carDelayToStart);
      LOGGER.accept(String.format("Автомобиль №%d подъехал к стартовой прямой.", carNumber));
    }

    private void race() throws InterruptedException {
      LOGGER.accept(String.format("Автомобиль №%d стартовал!", carNumber));
      Thread.sleep(TRACK_LENGTH / carSpeed);
      LOGGER.accept(String.format("Автомобиль №%d финишировал!", carNumber));
    }

  }
}