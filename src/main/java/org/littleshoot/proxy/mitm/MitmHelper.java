package org.littleshoot.proxy.mitm;

import java.io.Closeable;
import java.io.IOException;

public class MitmHelper {

    // From Apache Commons-IO
    public static void closeQuietly(Closeable is) {
        if(is == null) return;
        try {
            is.close();
        } catch (IOException ignore) {}
    }
}
