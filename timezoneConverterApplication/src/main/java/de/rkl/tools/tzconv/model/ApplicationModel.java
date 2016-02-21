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
package de.rkl.tools.tzconv.model;

import com.google.common.collect.*;
import de.rkl.tools.tzconv.configuration.ApplicationConfiguration;
import de.rkl.tools.tzconv.configuration.PreferencesProvider;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@Component
public class ApplicationModel {
    public static final int DEFAULT_INITIAL_HOUR = 12;
    public static final int DEFAULT_INITIAL_MINUTE = 30;
    public static final SetMultimap<ZoneOffset, ZoneId> ZONE_OFFSETS_2_ZONE_IDS = sortAvailableZoneIds();
    public final ObjectProperty<ZonedDateTime> mainDateTime;
    public final ListProperty<ZoneId> selectedZoneIds;
    public final ObjectProperty<File> templateFile;
    @SuppressWarnings("unused")
    @Autowired
    private ApplicationConfiguration applicationConfiguration;
    @SuppressWarnings("unused")
    @Autowired
    private Ordering<ZoneId> selectedZoneIdOrdering;
    @SuppressWarnings("unused")
    @Autowired
    private PreferencesProvider preferencesProvider;

    @SuppressWarnings("unused")
    public ApplicationModel() {
        mainDateTime = new SimpleObjectProperty<>();
        selectedZoneIds = new SimpleListProperty<>();
        templateFile = new SimpleObjectProperty<>();
    }

    private static SetMultimap<ZoneOffset, ZoneId> sortAvailableZoneIds() {
        final SortedSetMultimap<ZoneOffset, ZoneId> zoneIdMap = TreeMultimap.create(Ordering.natural().reverse(), new
                Ordering<ZoneId>() {
                    @Override
                    public int compare(final ZoneId zoneId1, final ZoneId zoneId2) {
                        return ComparisonChain.start().compare(zoneId1.toString(), zoneId2.toString()).result();
                    }
                }.nullsFirst());
        ZoneId.getAvailableZoneIds().stream().forEach(zoneId -> {
            final ZoneId zoneIdObject = ZoneId.of(zoneId);
            zoneIdMap.put(zoneIdObject.getRules().getStandardOffset(Instant.now()), zoneIdObject);
        });
        return ImmutableSetMultimap.copyOf(zoneIdMap);
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void fillProperties() {
        mainDateTime.setValue(ZonedDateTime.now().withHour(DEFAULT_INITIAL_HOUR).withMinute
                (DEFAULT_INITIAL_MINUTE).withZoneSameLocal(preferencesProvider.getPreferredReferenceZoneIdIfSet()));
        selectedZoneIds.setValue(getInitialZoneIds());
        templateFile.setValue(preferencesProvider.getPreferredTemplateFile());
        sortSelectedZoneIds();
    }

    private ObservableList<ZoneId> getInitialZoneIds() {
        final java.util.List<ZoneId> preferredZoneIds = preferencesProvider.getPreferredSelectedZoneIds();
        final Collection<ZoneId> initialZoneIds;
        if (isNotEmpty(preferredZoneIds)) {
            initialZoneIds = preferredZoneIds;
        } else {
            final List<ZoneId> defaultListFromConfiguration = applicationConfiguration.getDefaultSelectedZoneIds();
            initialZoneIds = isNotEmpty(defaultListFromConfiguration) ? defaultListFromConfiguration :
                    ZONE_OFFSETS_2_ZONE_IDS.values();
        }
        return observableArrayList(initialZoneIds);
    }

    public void sortSelectedZoneIds() {
        FXCollections.sort(selectedZoneIds.getValue(), selectedZoneIdOrdering);
    }
}
