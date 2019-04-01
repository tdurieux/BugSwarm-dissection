package com.peterphi.std.types;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimecodeTest
{
	@Test
	public void testResample_IdentityFunction_Zero()
	{
		final Timecode src = Timecode.getInstance("00:00:00:00", Timebase.HZ_25);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals(src.toString(), dst.toString());
	}


	@Test
	public void testResample_IdentityFunction()
	{
		final Timecode src = Timecode.getInstance("01:02:03:04", Timebase.HZ_25);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals(src.toString(), dst.toString());
	}


	@Test
	public void testResample()
	{
		final Timecode src = Timecode.getInstance("09:08:07:04", Timebase.HZ_50);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals("09:08:07:02", dst.toString());
	}


	@Test
	public void testResampleRounding()
	{
		//tests resampling that results in rounding up to the next whole second
		final Timecode src = Timecode.getInstance("09:08:07:999", Timebase.HZ_1000);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals("09:08:08:00", dst.toString());
	}


	@Test
	public void testBounds()
	{
		final Timecode small = Timecode.getInstance("00:00:00:00", Timebase.HZ_50);
		final Timecode big = Timecode.getInstance("00:00:00:01", Timebase.HZ_50);

		assertTrue(small.lt(big));
		assertTrue(big.gt(small));
	}


	@Test
	public void testResamplePrecise_HappyPath()
	{
		final Timecode src = Timecode.getInstance("09:08:07:02", Timebase.HZ_50);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals("09:08:07:01", dst.toString());
	}


	@Test
	public void testResamplePrecise_HappyPath_Zero()
	{
		final Timecode src = Timecode.getInstance("00:00:00:00", Timebase.HZ_50);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals("00:00:00:00", dst.toString());
	}


	@Test(expected = ResamplingException.class)
	public void testResamplePrecise_Imprecise() throws Exception
	{
		final Timecode src = Timecode.getInstance("09:08:07:03", Timebase.HZ_50);

		src.resamplePrecise(Timebase.HZ_25);
	}


	@Test
	public void testResample_Imprecise()
	{
		final Timecode src = Timecode.getInstance("09:08:07:03", Timebase.HZ_50);
		final Timecode dst = src.resample(Timebase.HZ_25);

		assertEquals("09:08:07:02", dst.toString());
	}


	@Test
	public void testGetSampleCountDelta_Zero()
	{
		final Timecode base = Timecode.getInstance("00:00:00:00", Timebase.HZ_50);
		final Timecode tc = Timecode.getInstance("00:00:00:00", Timebase.HZ_25);

		final SampleCount delta = tc.getSampleCount(base);

		assertEquals(0, delta.getSamples());
		assertEquals(0, tc.getSampleCount().getSamples());
	}


	@Test
	public void testGetSampleCountDelta()
	{
		// 2 timecodes which are 1h2m3s and 4 frames apart
		final Timecode base = Timecode.getInstance("01:02:03:04", Timebase.HZ_50);
		final Timecode tc = Timecode.getInstance("02:04:06:08", Timebase.HZ_50);

		final SampleCount delta = tc.getSampleCount(base);
		final Timecode deltaTC = Timecode.getInstance(delta);

		assertEquals("01:02:03:04", deltaTC.toString());
	}


	@Test
	public void testDurationInSeconds()
	{
		final Timecode base = Timecode.getInstance("01:02:03:49", Timebase.HZ_50);

		assertEquals(3723, base.getDurationInSeconds());
	}


	/**
	 * Test that -00:00:00:02 is parsed and correctly and when encoded to samples returns the right number of samples
	 */
	@Test
	public void testSampleCountForNegativeTimecode()
	{
		final Timecode timecode = Timecode.getInstance("-00:00:00:02@25");
		SampleCount samples = new SampleCount(-2, Timebase.HZ_25);

		assertEquals(samples, timecode.getSampleCount());
	}


	@Test
	@Deprecated
	public void testGetInstanceWithNegativeSampleCount()
	{
		final Timecode tc = Timecode.getInstance(new SampleCount(-2, Timebase.HZ_25), false);

		assertEquals("-00:00:00:02@25", tc.toEncodedString());
	}


	@Test
	public void testAddNegativeSamples()
	{
		final SampleCount samples = new SampleCount(-2, Timebase.HZ_25);

		final Timecode base = Timecode.getInstance("-00:00:00:02@25");

		final Timecode tc = base.add(samples);

		assertEquals("-00:00:00:04@25", tc.toEncodedString());
	}


	/**
	 * The SMPTE string shouldn't include days
	 */
	@Test
	public void testAddSmpte()
	{
		final Timecode base = Timecode.getInstance("23:29:59:05", Timebase.HZ_25);
		final Timecode add = Timecode.getInstance("00:59:59:22", Timebase.HZ_25);
		final Timecode tc = base.add(add.getSampleCount());

		Assert.assertEquals("00:29:59:02", tc.toSMPTEString());
	}


	/**
	 * The SMPTE string including days should include days
	 */
	@Test
	public void testAddSmpteIncludingDays()
	{
		final Timecode base = Timecode.getInstance("23:29:59:05", Timebase.HZ_25);
		final Timecode add = Timecode.getInstance("00:59:59:22", Timebase.HZ_25);
		final Timecode tc = base.add(add.getSampleCount());

		Assert.assertEquals("01:00:29:59:02", tc.toSMPTEString(true));
	}


	@Test
	public void testSubtractNegativeSamples()
	{
		final SampleCount samples = new SampleCount(-2, Timebase.HZ_25);

		final Timecode base = Timecode.getInstance("-00:00:00:02@25");

		final Timecode tc = base.subtract(samples);

		assertEquals("00:00:00:00@25", tc.toEncodedString());
	}


	/**
	 * Not a valid timecode representation (frame count is >= 1 second at this timebase)
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDenormalisedFrameField()
	{
		Timecode.getInstance("00:00:00:25@25"); // frames >= timebase
	}


	/**
	 * Negative zero timecode should be converted to positive zero
	 */
	@Test
	public void testNegativeZeroTimecode()
	{
		final Timecode tc = Timecode.getInstance("-00:00:00:00@25");

		assertEquals("00:00:00:00@25", tc.toEncodedString());
	}


	/**
	 * Timecode eq should be based on the instant in time it refers to
	 */
	@Test
	public void testEq()
	{
		final Timecode a = Timecode.getInstance("01:02:03:04@25");
		final Timecode b = Timecode.getInstance("01:02:03:08@50");

		assertTrue(a.eq(b));
	}


	/**
	 * Negative zero timecode should be converted to positive zero
	 */
	@Test
	public void testBetween()
	{
		final Timecode start = Timecode.getInstance("-01:02:03:04@25");
		final Timecode end = Timecode.getInstance("03:02:03:04@25");

		final List<String> yes = Arrays.asList("-01:02:03:04",
		                                       "00:00:00:00",
		                                       "01:00:00:00",
		                                       "02:00:00:00",
		                                       "03:00:00:00",
		                                       "03:02:03:03",
		                                       "03:02:03:04");
		final List<String> no = Arrays.asList("-01:02:03:05", "03:02:03:05", "-02:03:04:05", "04:00:00:00");

		for (String smpte : yes)
		{
			final Timecode tc = Timecode.getInstance(smpte, Timebase.HZ_25);

			final boolean between = tc.between(start, end);

			assertTrue(smpte + " should be between " + start + " and " + end, between);
		}

		for (String smpte : no)
		{
			final Timecode tc = Timecode.getInstance(smpte, Timebase.HZ_25);

			final boolean between = tc.between(start, end);

			assertFalse(smpte + " shouldn't be between " + start + " and " + end, between);
		}
	}


	@Test
	public void testToSMPTEString()
	{
		assertEquals("00:00:00:00", Timecode.valueOf("00:00:00:00@25").toSMPTEString());
		assertEquals("00:00:00:24", Timecode.valueOf("00:00:00:24@25").toSMPTEString());
		assertEquals("00:00:10:24", Timecode.valueOf("00:00:10:24@25").toSMPTEString());
		assertEquals("00:15:10:24", Timecode.valueOf("00:15:10:24@25").toSMPTEString());
		assertEquals("20:15:10:24", Timecode.valueOf("20:15:10:24@25").toSMPTEString());
	}

	@Test
	public void testToEncodedString()
	{
		assertEquals("00:00:00:00@25", Timecode.valueOf("00:00:00:00@25").toEncodedString());
		assertEquals("00:00:00:24@25", Timecode.valueOf("00:00:00:24@25").toEncodedString());
		assertEquals("00:00:10:24@25", Timecode.valueOf("00:00:10:24@25").toEncodedString());
		assertEquals("00:15:10:24@25", Timecode.valueOf("00:15:10:24@25").toEncodedString());
		assertEquals("20:15:10:24@25", Timecode.valueOf("20:15:10:24@25").toEncodedString());
	}

	@Test
	public void testToEncodedStringWithDays()
	{
		assertEquals("20:15:10:24@25", Timecode.valueOf("20:15:10:24@25").toEncodedString(true));
		assertEquals("30:20:15:10:24@25", Timecode.valueOf("30:20:15:10:24@25").toEncodedString(true));
	}


	@Test
	public void testToISODurationWithFrames()
	{
		assertEquals("PT00H00M00S00F", Timecode.valueOf("00:00:00:00@25").toISODurationWithFrames(false));
		assertEquals("PT00H00M00S00F", Timecode.valueOf("00:00:00:00@25").toISODurationWithFrames(true));
		assertEquals("PT00H00M00S24F", Timecode.valueOf("00:00:00:24@25").toISODurationWithFrames(true));
		assertEquals("PT00H00M10S24F", Timecode.valueOf("00:00:10:24@25").toISODurationWithFrames(true));
		assertEquals("PT00H15M10S24F", Timecode.valueOf("00:15:10:24@25").toISODurationWithFrames(true));
		assertEquals("PT20H15M10S24F", Timecode.valueOf("20:15:10:24@25").toISODurationWithFrames(true));

		assertEquals("P01DT20H15M10S24F", Timecode.valueOf("01:20:15:10:24@25").toISODurationWithFrames(true));
		assertEquals("P30DT20H15M10S24F", Timecode.valueOf("30:20:15:10:24@25").toISODurationWithFrames(true));
	}
}
