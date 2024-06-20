package com.slr207.commons;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    
    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        return sdf.format(new Date());
    }
    
    public static void log(String message) {
        System.out.println(getCurrentTimestamp() + " " + message);
    }

}
