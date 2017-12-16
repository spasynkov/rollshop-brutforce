package bruteforce.utils;

/**
 * Self-running object thread to animate output while work in process
 */
public class SpinnerAnimation extends Thread {

    public SpinnerAnimation() {
        this.setName("Spinner animation thread");
        this.start();
    }

    @Override
    public void run() {
        String animationSymbols = "|/-\\";

        int x = 0;
        while (!isInterrupted()) {
            System.out.print("\r Working " + animationSymbols.charAt(x++ % animationSymbols.length()));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                interrupt();  // stopping thread
            }
        }
    }
}
