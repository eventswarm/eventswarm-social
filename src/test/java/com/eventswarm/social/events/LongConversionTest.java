package com.eventswarm.social.events;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */

public class LongConversionTest {
    public static final long MAXINT = new Integer(Integer.MAX_VALUE).longValue();
    public static final long MININT = new Integer(Integer.MIN_VALUE).longValue();
    public static final long BITMASK31 = ~0L >>> (Long.SIZE - Integer.SIZE + 1);


    @Test
    public void castToIntZero() throws Exception {
        int result = (int) 0L;
        assertThat(result, is(0));
    }

    @Test
    public void castToIntPositiveBelow() throws Exception {
        int result = (int) 1L;
        assertThat(result, is(1));
    }

    @Test
    public void castToIntNegativeBelow() throws Exception {
        int result = (int) -1L;
        assertThat(result, is(-1));
    }

    @Test
    public void castToIntMaxInt() throws Exception {
        int result = (int) MAXINT;
        assertThat(result, is(Integer.MAX_VALUE));
    }

    @Test
    public void castToIntAbove() throws Exception {
        long bigLong = MAXINT << 2;
        System.out.println("Max int is " + Long.toString(MAXINT));
        System.out.println("Our big long is " + Long.toString(bigLong));
        int result = (int) bigLong;
        System.out.println("Our result is " + Long.toString(result));
        assertThat(result, is(Integer.MAX_VALUE << 2));
    }

    @Test
    public void maskIntAbove() throws Exception {
        long mask = ~0L >>> (Long.SIZE - Integer.SIZE + 1);
        long bigLong = MAXINT << 2;
        int result = (int) (bigLong & mask);
        assertThat(Integer.MAX_VALUE - result, is(3));
    }

    @Test
    public void maskedIntIncrementNowrap() throws Exception {
        long bigLong = MAXINT << 2;
        int result1 = (int) (bigLong & BITMASK31);
        int result2 = (int) ((bigLong + 1) & BITMASK31);
        assertThat(result2 - result1, is(1));
    }

    @Test
    public void maskedIntIncrementWrap() throws Exception {
        long bigLong = MAXINT;
        int result1 = (int) (bigLong & BITMASK31);
        int result2 = (int) ((bigLong + 1) & BITMASK31);
        assertThat(result2 - result1, is(-Integer.MAX_VALUE));
    }

    @Test
    public void castToIntMinInt() throws Exception {
        int result = (int) MININT;
        assertThat(result, is(Integer.MIN_VALUE));
    }
}
