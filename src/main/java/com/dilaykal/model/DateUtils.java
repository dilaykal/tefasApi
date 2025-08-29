package com.dilaykal.model;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class DateUtils {
    private static Set<LocalDate> officialHolidays = new HashSet<>();

    static {
        // Yılbaşı
        officialHolidays.add(LocalDate.of(2025, 1, 1));
        // Ulusal Egemenlik ve Çocuk Bayramı
        officialHolidays.add(LocalDate.of(2025, 4, 23));
        // Emek ve Dayanışma Günü
        officialHolidays.add(LocalDate.of(2025, 5, 1));
        // Zafer Bayramı
        officialHolidays.add(LocalDate.of(2025, 8, 30));
        // Cumhuriyet Bayramı
        officialHolidays.add(LocalDate.of(2025, 10, 29));
    }
    public static boolean isWorkingDay(LocalDate date){
        //Haftasonu kontrolü
        if(date.getDayOfWeek()== DayOfWeek.SATURDAY||date.getDayOfWeek() == DayOfWeek.SUNDAY){
            return false;
        }
        return !officialHolidays.contains(date);
    }
}
