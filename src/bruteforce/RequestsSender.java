package bruteforce;

import bruteforce.utils.SpinnerAnimation;
import javafx.util.Pair;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class RequestsSender {
    private URL url;                        // url to where send requests
    private boolean isDebugNeeded;          // shows sent data and response from the server if set
    private Pair<Integer, String> result;   // object where to put the result

    private volatile SpinnerAnimation animationThread;  // thread that showing some animation while work in process
    private volatile boolean isAnswerFound;             // skips sending requests if answer already found

    public RequestsSender(String url, boolean isDebugNeeded) throws MalformedURLException {
        this.url = new URL(url);
        this.isDebugNeeded = isDebugNeeded;
    }

    /**
     * <p>Sends given data to server and then checks the response</p>
     *
     * @param data to be sent to server
     * @return true if response contains needed result, false otherwise
     */
    public boolean check(long data) {
        if (isAnswerFound) return false;

        // starting animation if needed
        if (!isDebugNeeded && animationThread == null) {
            synchronized (this) {
                if (animationThread == null) {
                    animationThread = new SpinnerAnimation();
                }
            }
        }

        Pair<Integer, String> result = sendRequest(data);

        if (isDebugNeeded) {
            System.out.print("\nData: " + data + "; response: " + result.getValue());
        }

        if (result.getKey() == 200 && result.getValue().contains("RIGHT")) {
            isAnswerFound = true;           // setting flag to skip all other attempts
            if (animationThread != null) {
                done();                     // stops animation thread
                System.out.println(" Success!");
            }
            this.result = result;
            return true;
        }

        return false;
    }

    public Pair<Integer, String> getResult() {
        return result;
    }

    /**
     * <p>Sends data to server and returns the pair of response code and response body</p>
     *
     * @param data to be sent to server
     * @return the pair of Integer and String, where Integer is response code, and String is response body
     */
    private Pair<Integer, String> sendRequest(long data) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        BufferedReader inputStream = null;

        int responseCode = -1;
        String responseBody = "";

        boolean repeat = true;  // to prevent skipping some data because of exception

        do {
            try {
                // setting connection
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                // sending data
                while (outputStream == null) {      // waiting for output stream
                    try {
                        outputStream = new DataOutputStream(connection.getOutputStream());
                    } catch (ConnectException ignored) {}
                }
                outputStream.writeBytes("code=" + data);
                outputStream.flush();

                // getting response
                responseCode = connection.getResponseCode();

                while (inputStream == null) {       // waiting for input stream
                    try {
                        inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (ConnectException ignored) {}
                }

                // fixing problem with empty response body
                while (!inputStream.ready()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // reading response body
                StringBuilder sb = new StringBuilder();
                while (inputStream.ready()) {
                    sb.append(inputStream.readLine());
                }

                responseBody = sb.toString();

                repeat = false;     // if we've reached this point - then everything was ok and no need to repeat

            } catch (IOException e) {
                if (isDebugNeeded) {
                    System.err.println("Exception while sending " + data + ": " + e.getLocalizedMessage());
                }
            } finally {
                close(connection, inputStream, outputStream);
            }
        } while (repeat);

        return new Pair<>(responseCode, responseBody);
    }

    /**
     * <p>Closes connections and streams</p>
     *
     * @param connection connection object to be closed
     * @param closeables vararg of objects that could be closed
     */
    private void close(HttpURLConnection connection, Closeable... closeables) {
        for (Closeable c: closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException ignored) {}
            }
        }

        if (connection != null) {
            connection.disconnect();
        }
    }

    /**
     * <p>Stops animations if needed</p>
     */
    public void done() {
        if (animationThread != null) {
            animationThread.interrupt();
            try {
                animationThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
