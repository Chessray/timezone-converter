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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@Component
public class ApplicationConfiguration {
    private HierarchicalConfiguration configuration;

    @SuppressWarnings("unused")
    public ApplicationConfiguration() throws ConfigurationException {
        configuration = new XMLConfiguration("configuration.xml");
    }

    public List<ZoneId> getDefaultSelectedZoneIds() {
        return configuration.getList
                ("defaultSelectedZoneIds.selectedZoneId[@id]").stream().map(selectedZoneId -> ZoneId.of(selectedZoneId
                .toString())).collect(toList());
    }
}
