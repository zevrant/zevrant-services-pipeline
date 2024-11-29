package com.zevrant.services.utils


import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeUtils {

    static ZonedDateTime unixToLocalDateTime(long unixTimestamp) {
        return Instant.ofEpochSecond(unixTimestamp)
                .atZone(ZoneId.of("GMT-4"));
    }


}