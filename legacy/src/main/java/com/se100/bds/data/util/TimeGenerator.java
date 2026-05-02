package com.se100.bds.data.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class TimeGenerator {

    public LocalDateTime getRandomTime() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        // 1. Calculate the difference between the two dates in seconds
        long secondsBetween = ChronoUnit.SECONDS.between(start, end);

        // 2. Generate a random number of seconds to add
        long randomSeconds = ThreadLocalRandom.current().nextLong(secondsBetween + 1);

        // 3. Return the start time plus the random offset
        return start.plusSeconds(randomSeconds);
    }

    public LocalDateTime getRandomTimeAfter(LocalDateTime time, LocalDateTime maxTime) {
        if (maxTime == null)
            maxTime = LocalDateTime.now();

        // 1. Guard against null or invalid ranges
        if (time == null || !time.isBefore(maxTime)) {
            log.error("Start time must be before maxTime and neither can be null");
            maxTime = LocalDateTime.now();
        }

        // 2. Calculate the total seconds available in the window
        long secondsBetween = ChronoUnit.SECONDS.between(time, maxTime);

        // 3. Pick a random number of seconds within that window
        // nextLong(bound) is exclusive, so we add 1 to make maxTime inclusive
        long randomOffset = ThreadLocalRandom.current().nextLong(secondsBetween + 1);

        // 4. Add the offset to the starting 'time'
        return time.plusSeconds(randomOffset);
    }

    public LocalDateTime getRandomTimeBeforeDays(LocalDateTime time, int days) {
        LocalDateTime startRange = time.minusDays(days);
        long secondsBetween = ChronoUnit.SECONDS.between(startRange, time);
        long randomSeconds = ThreadLocalRandom.current().nextLong(secondsBetween + 1);
        return startRange.plusSeconds(randomSeconds);
    }

    public LocalDateTime getRandomTimeAfterDays(LocalDateTime time, int days) {
        LocalDateTime endRange = time.plusDays(days);
        long secondsBetween = ChronoUnit.SECONDS.between(endRange, time);
        long randomSeconds = ThreadLocalRandom.current().nextLong(secondsBetween + 1);
        return endRange.plusSeconds(randomSeconds);
    }
}
