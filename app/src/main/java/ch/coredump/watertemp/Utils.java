package ch.coredump.watertemp;

import java.util.List;

public class Utils {

    /**
     * Join a string list using a delimiter.
     * @param delimiter The delimiter.
     * @param elements List of string elements.
     * @return The joined string.
     */
    public static String join(String delimiter,
                              List<String> elements) {
        StringBuilder builder = new StringBuilder();
        final int size = elements.size();
        for (int i = 0; i < size - 1; i++) {
            builder.append(elements.get(i));
            builder.append(delimiter);
        }
        builder.append(elements.get(size - 1));
        return builder.toString();
    }
}
