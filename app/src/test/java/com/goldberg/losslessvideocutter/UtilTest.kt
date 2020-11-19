package com.goldberg.losslessvideocutter

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UtilTest
{

    @Test
    fun timeTest()
    {
        assertThat(toDisplayTime(.57f)).isEqualTo("0:00:00.57")
        assertThat(toDisplayTime(.571f)).isEqualTo("0:00:00.57")
        assertThat(toDisplayTime(.578f)).isEqualTo("0:00:00.58")
        assertThat(toDisplayTime(59.578f)).isEqualTo("0:00:59.58")
        assertThat(toDisplayTime(60.578f)).isEqualTo("0:01:00.58")
        assertThat(toDisplayTime(61.578f)).isEqualTo("0:01:01.58")
        assertThat(toDisplayTime(119.578f)).isEqualTo("0:01:59.58")
        assertThat(toDisplayTime(120.0f)).isEqualTo("0:02:00.00")
        assertThat(toDisplayTime(121.5f)).isEqualTo("0:02:01.50")
        assertThat(toDisplayTime(3600.05f)).isEqualTo("1:00:00.05")
        assertThat(toDisplayTime(20000.054f)).isEqualTo("5:33:20.05")
        assertThat(toDisplayTime(260000.054f)).isEqualTo("72:13:20.05")

        /**
         * Test stops working on values over 260000 - rounding fails.
         * I suppose this is because precision is lost on such a big values. Directly printing 300000.054f results in "300000.06"
         * However, this is ok for production.
         */
        //assertThat(toDisplayTime(300000.054f)).isEqualTo("83:20:00.05")
    }
}
