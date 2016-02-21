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

import de.rkl.tools.tzconv.model.ApplicationModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@Component
public final class PreferencesProvider {
    private static final String PREFERENCES_KEY_REFERENCE_ZONE_ID = "referenceZoneId";
    private static final String PREFERENCES_SELECTED_ZONE_IDS_DELIMITER = ",";
    private static final String PREFERENCES_KEY_SELECTED_ZONE_IDS = "selectedZoneIds";
    private static final String PREFERENCES_KEY_TEMPLATE_FILE_PATH = "templateFilePath";
    private final Preferences applicationPreferences;

    @SuppressWarnings("unused")
    @Autowired
    private ApplicationModel applicationModel;

    @SuppressWarnings("unused")
    public PreferencesProvider() {
        applicationPreferences = Preferences.userNodeForPackage(PreferencesProvider.class);
    }


    private void dumpReferenceZoneIdIntoPreferences() {
        applicationPreferences.put(PREFERENCES_KEY_REFERENCE_ZONE_ID, applicationModel.mainDateTime.getValue()
                .getZone().toString());
    }

    private void dumpSelectedZoneIdsIntoPreferences() {
        applicationPreferences.put(PREFERENCES_KEY_SELECTED_ZONE_IDS, applicationModel.selectedZoneIds.stream().map
                (ZoneId::toString).collect(Collectors.joining(PREFERENCES_SELECTED_ZONE_IDS_DELIMITER)));
    }

    private void dumpTemplateFilePathIntoPreferences() {
        final File templateFile = applicationModel.templateFile.getValue();
        applicationPreferences.put(PREFERENCES_KEY_TEMPLATE_FILE_PATH, templateFile == null ? StringUtils.EMPTY :
                templateFile.getAbsolutePath());
    }

    public ZoneId getPreferredReferenceZoneIdIfSet() {
        final String preferredReferenceZoneId = applicationPreferences.get(PREFERENCES_KEY_REFERENCE_ZONE_ID, null);
        return StringUtils.isNotBlank(preferredReferenceZoneId) ? ZoneId.of
                (preferredReferenceZoneId) : ZoneId.systemDefault();
    }

    public List<ZoneId> getPreferredSelectedZoneIds() {
        final String preferredZoneIdsString = applicationPreferences.get(PREFERENCES_KEY_SELECTED_ZONE_IDS, null);
        return (isBlank(preferredZoneIdsString) ? null : Arrays.stream(preferredZoneIdsString.split
                (PREFERENCES_SELECTED_ZONE_IDS_DELIMITER)).map(ZoneId::of).collect(toList()));
    }

    public File getPreferredTemplateFile() {
        final String preferredTemplateFilePath = applicationPreferences.get(PREFERENCES_KEY_TEMPLATE_FILE_PATH, null);
        final File templateFile;
        if (isBlank(preferredTemplateFilePath)) {
            templateFile = null;
        } else {
            final File rawTemplateFile = new File(preferredTemplateFilePath);
            if (rawTemplateFile.exists() && rawTemplateFile.isFile() && rawTemplateFile.canRead()) {
                templateFile = rawTemplateFile;
            } else {
                templateFile = null;
            }
        }
        return templateFile;
    }

    public void flushPreferences() throws BackingStoreException {
        dumpSelectedZoneIdsIntoPreferences();
        dumpReferenceZoneIdIntoPreferences();
        dumpTemplateFilePathIntoPreferences();
        applicationPreferences.flush();
    }
}
