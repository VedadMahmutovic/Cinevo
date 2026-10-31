package com.vedad.cinevo.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RuntimeFormatterTest {

    @Test
    public void format_shortUsesMinutesOnly() {
        assertEquals("45 min", RuntimeFormatter.format(45));
    }

    @Test
    public void format_wholeHoursOmitMinutes() {
        assertEquals("2h", RuntimeFormatter.format(120));
    }

    @Test
    public void format_hoursAndMinutes() {
        assertEquals("2h 15min", RuntimeFormatter.format(135));
    }
}