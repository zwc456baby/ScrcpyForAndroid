package org.server.scrcpy.util;


import java.io.InputStream;
import java.util.Scanner;
public final class IO {
    private IO() {
        // not instantiable
    }

    public static String toString(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine()).append('\n');
        }
        return builder.toString();
    }

}
