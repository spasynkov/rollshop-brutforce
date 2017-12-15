package bruteforce;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class RequestsSender {
    private URL url;

    public RequestsSender(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public boolean send(long data) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        BufferedReader inputStream = null;

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
                int responseCode = connection.getResponseCode();

                while (inputStream == null) {       // waiting for input stream
                    try {
                        inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } catch (ConnectException ignored) {}
                }

                StringBuilder sb = new StringBuilder();

                // fixing problem with empty response body
                while (!inputStream.ready()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // reading response body
                while (inputStream.ready()) {
                    sb.append(inputStream.readLine());
                }

                System.out.println(data + sb.toString());

                // checking result
                if (responseCode == 200 && sb.toString().contains("RIGHT")) {
                    System.out.println("response code: " + responseCode);
                    System.out.println("response body:\n" + sb + "\n");
                    System.err.println("With value: " + data);
                    return true;
                }

                repeat = false;     // if we've reached this point - then everything was ok and no need to repeat

            } catch (IOException e) {
                System.err.println("Exception while sending " + data + ": " + e.getLocalizedMessage());
            } finally {
                close(connection, inputStream, outputStream);
            }
        } while (repeat);

        return false;
    }

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
}
