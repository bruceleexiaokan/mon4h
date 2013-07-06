package com.mon4h.framework.hbase.client;

import org.apache.hadoop.hbase.io.encoding.DataBlockEncoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

/**
 * @author: xingchaowang
 * @date: 4/28/13 7:17 PM
 */
public class TM {

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println(simpleDateFormat.format(new Date()));

        Date d1 = simpleDateFormat.parse("2013-04-25 16:59:59");
        Date d2 = simpleDateFormat.parse("2013-04-25 17:00:00");

        long time = d1.getTime();
        long time1 = d2.getTime();
        System.out.println(time);
        System.out.println(time1);
    }

    public static long getTime(String d){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return simpleDateFormat.parse(d).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0l;
    }
}
