package ar.edu.unrc.exa.dc.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Utils {

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return pw.toString();
    }


}
