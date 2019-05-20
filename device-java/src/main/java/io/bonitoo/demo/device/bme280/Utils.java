package io.bonitoo.demo.device.bme280;

public class Utils {

    /**
     * @param howMuch in ms.
     */
    public static void delay(long howMuch) {
        try {
            Thread.sleep(howMuch);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Left pad, with blanks
     *
     * @param s
     * @param len
     * @return
     */
    public static String lpad(String s, int len) {
        return lpad(s, len, " ");
    }

    public static String lpad(String s, int len, String pad) {
        String str = s;
        while (str.length() < len) {
            str = pad + str;
        }
        return str;
    }



}
