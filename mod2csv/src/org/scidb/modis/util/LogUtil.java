package org.scidb.modis.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.log4j.Logger;

public class LogUtil {

    public static Logger getLogger() {
        final Throwable t = new Throwable();
        t.fillInStackTrace();
        return Logger.getLogger(t.getStackTrace()[1].getClassName());
    }

    public static String getStackTraceString(Throwable e) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        return writer.toString();
    }
}
