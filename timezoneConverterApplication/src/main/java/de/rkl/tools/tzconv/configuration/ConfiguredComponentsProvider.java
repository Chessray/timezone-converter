/**
 * This file is part of timezoneConverterApplication.
 * <p>
 * timezoneConverterApplication is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * timezoneConverterApplication is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with timezoneConverterApplication.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.rkl.tools.tzconv.configuration;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static de.rkl.tools.tzconv.configuration.Constants.BEAN_NAME_DATE_FORMATTER;
import static de.rkl.tools.tzconv.configuration.Constants.BEAN_NAME_DATE_TIME_FORMATTER;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@SuppressWarnings("unused")
@Configuration
@Lazy
public class ConfiguredComponentsProvider {

    @Bean
    public Ordering<ZoneId> createZoneIdOrderingByOffsetAndName() {
        return Ordering.<ZoneId>from((left, right) -> {
            final Instant comparisonInstant = Instant.now();
            final ZoneOffset leftOffset = left.getRules().getStandardOffset(comparisonInstant);
            final ZoneOffset rightOffset = right.getRules().getStandardOffset(comparisonInstant);
            return ComparisonChain.start().compare(rightOffset, leftOffset).compare(left.toString(),
                    right.toString()).result();
        }).nullsFirst();
    }

    @Bean
    public VelocityEngine createVelocityEngine() {
        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();
        return velocityEngine;
    }

    @Bean(name = BEAN_NAME_DATE_FORMATTER)
    public DateTimeFormatter configureDateFormatter() {
        return DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy");
    }

    @Bean(name = BEAN_NAME_DATE_TIME_FORMATTER)
    public DateTimeFormatter configureDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("VV: dd-MM-yyyy kk:mm");
    }
}
