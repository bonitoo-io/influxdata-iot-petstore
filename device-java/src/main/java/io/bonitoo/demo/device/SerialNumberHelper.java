package io.bonitoo.demo.device;

public class SerialNumberHelper {

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    /**
     * Generates random device number.
     *
     * return random string in "%s-%s-%s-%s" format
     **/
    public static String randomSerialNumber() {

        return String.format("%s-%s-%s-%s", randomAlphaNumeric(4), randomAlphaNumeric(4), randomAlphaNumeric(4), randomAlphaNumeric(4));

    }
}
