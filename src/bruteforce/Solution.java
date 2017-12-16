package bruteforce;

import bruteforce.exceptions.BadArgumentsException;
import bruteforce.exceptions.ExitCommandException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Solution {
    private long timeLimitInSeconds = TimeUnit.MINUTES.toSeconds(30);
    private long numberOfThreads = 6;
    private long startFrom = 0;

    private boolean isStatisticsToBeShown = false;
    private boolean isDebugEnabled = false;

    public static void main(String[] args) throws IOException {
        Solution solution = new Solution();

        // parsing args
        try {
            solution.parseArgs(args);
        } catch (BadArgumentsException e) {
            System.err.println("Bad arguments. " + e.getLocalizedMessage());
            try {
                TimeUnit.MILLISECONDS.sleep(10);    // showing info messages in right order
            } catch (InterruptedException ignored) {
            }

            printUsage();
            return;
        } catch (ExitCommandException e) {
            return;
        }

        // starting
        if (solution.isDebugEnabled) {
            System.out.println("Started at: " + new Date());
        }

        solution.start();

        if (solution.isDebugEnabled) {
            System.out.println("Finished at: " + new Date());
        }
    }

    private static void printUsage() {
        System.out.println("USAGE\n\tSolution [OPTIONS]");
        System.out.println();
        System.out.println("OPTIONS");
        System.out.println("\t-l, --timelimit number\n\t\tsets the time limit in seconds for brute forcing\n");
        System.out.println("\t-t, --threads number\n\t\tsets the number of threads to run\n");
        System.out.println("\t-i, --init number\n\t\tstarts with the given number\n");
        System.out.println("\t-s, --statistics\n\t\tshows statistics after run\n");
        System.out.println("\t-d, --debug\n\t\tshows debug info while run\n");
        System.out.println("\t-h, --help\n\t\tshows this page and exit\n");
        System.out.println("\t--author\n\t\tprints the author's info and exit");
    }

    private static void printAuthor() {
        System.out.println("Created by Stanislav Pasynkov (s.pasynkov@gmail.com)");
    }

    private void start() throws MalformedURLException {
        RequestsSender sender = new RequestsSender("http://www.rollshop.co.il/test.php", isDebugEnabled);

        // calculating the max number of requests we can send in a given time with given number of threads
        // no need to iterate over every longs.
        // iterating over all 2^63 elements with 6 seconds delay will take billions years...
        long maxNumber = calculateMaxNumber();
        if (isDebugEnabled) {
            System.out.println("Max number to proceed: " + maxNumber);
        }

        // setting new ForkJoinPool size
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                String.valueOf(numberOfThreads));


        System.out.print("Working...");
        long start = System.nanoTime();
        Optional<Long> result = Stream
                .iterate(startFrom, x -> ++x)     // creating infinite stream from elements 0, 1, 2, 3...
                .limit(maxNumber)                 // limiting stream with max number we can proceed in given time
                .parallel()                       // and with given number of threads, so making stream parallel
                .filter(sender::check)            // filter only that number that gives us the right link
                .findAny();                       // taking that element
        long end = System.nanoTime();

        sender.done();
        System.out.println("\n");

        if (result.isPresent()) {
            long password = result.get();
            String response = sender.getResult().getValue();

            System.out.println("Result is: " + response.substring(
                    response.indexOf("http"),
                    response.indexOf("</body>")));
            System.out.println("with password: " + password);
            System.out.println();
        } else {
            System.err.println("\nFailed to find result!");
            System.err.println("Try to set more threads using -t argument, higher time limit with -l one, " +
                    "or set initial number using -i argument.");
        }

        if (isStatisticsToBeShown) {
            System.out.println("Time took:\t\t" + (end - start) / 1000000.0 + " ms.");
            Runtime r = Runtime.getRuntime();
            System.out.println("Memory used:\t" + (r.totalMemory() - r.freeMemory()) + " bytes");
        }
    }

    /**
     * <p>Calculates the approximate max number that can be reached with given time limit and number of threads</p>
     * <p>
     * <p>For example, if the time limit is 30 mins (1800 seconds) and the latency between each request and response is
     * around 6 seconds - then one thread can send 1800 / 6 = 300 requests in given time. And 10 threads can send
     * 300 * 10 = 3000 requests. So it will return the number: 3000</p>
     *
     * @return the approximate max number
     */
    private long calculateMaxNumber() {
        byte approximateMaxLatencyInSeconds = 6;
        return (timeLimitInSeconds / approximateMaxLatencyInSeconds) * numberOfThreads;
    }

    /**
     * <p>Tries to resolve known arguments and set relevant field values.</p>
     *
     * @param args arguments array
     * @throws BadArgumentsException if there was bad arguments syntax
     * @throws ExitCommandException  if the program is supposed to be terminated
     */
    private void parseArgs(String[] args) throws BadArgumentsException, ExitCommandException {
        for (int i = 0; i < args.length; i++) {
            if ("-l".equals(args[i]) || "--timelimit".equals(args[i])) {
                this.timeLimitInSeconds = getNumberedArgument(args, i++);
            } else if ("-t".equals(args[i]) || "--threads".equals(args[i])) {
                this.numberOfThreads = getNumberedArgument(args, i++);
            } else if ("-i".equals(args[i]) || "--init".equals(args[i])) {
                this.startFrom = getNumberedArgument(args, i++);
            } else if ("-s".equals(args[i]) || "--statistics".equals(args[i])) {
                this.isStatisticsToBeShown = true;
            } else if ("-d".equals(args[i]) || "--debug".equals(args[i])) {
                this.isDebugEnabled = true;
            } else if ("--author".equals(args[i])) {
                printAuthor();
                throw new ExitCommandException();
            } else {
                printUsage();
                throw new ExitCommandException();
            }
        }
    }

    /**
     * <p>Parses the pair of the arguments, where first argument is the key, and next one is it's value.</p>
     *
     * @param args  arguments array
     * @param index index of the first argument
     * @return the value of the argument with a given index
     * @throws BadArgumentsException if there was bad arguments syntax
     */
    private long getNumberedArgument(String[] args, int index) throws BadArgumentsException {
        long value;
        if (args.length > index + 1) {
            try {
                value = Long.parseLong(args[index + 1]);
                if (value < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new BadArgumentsException(
                        "There should be a positive number after argument '" + args[index] + "'!");
            }
        } else throw new BadArgumentsException("No value found for argument '" + args[index] + "'!");

        return value;
    }
}
