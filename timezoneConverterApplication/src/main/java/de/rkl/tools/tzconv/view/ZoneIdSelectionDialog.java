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
package de.rkl.tools.tzconv.view;

import de.rkl.tools.tzconv.model.ApplicationModel;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.partition;
import static javafx.collections.FXCollections.observableSet;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@Component
public class ZoneIdSelectionDialog extends Dialog<Collection<ZoneId>> {
    private final SetProperty<ZoneId> pendingSelectedZoneIds;
    private final List<CheckBox> zoneIdCheckboxes;
    @SuppressWarnings("unused")
    @Autowired
    private ApplicationModel applicationModel;

    @SuppressWarnings("unused")
    public ZoneIdSelectionDialog() {
        zoneIdCheckboxes = newArrayList();
        pendingSelectedZoneIds = new SimpleSetProperty<>();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void initContent() {
        setOnShowing(this::resetFromModel);
        getDialogPane().setContent(createZoneIdSelectionBox());
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        setResultConverter(buttonType -> buttonType == ButtonType.OK ? newArrayList(pendingSelectedZoneIds.getValue()) :
                null);
    }

    private void resetFromModel(@SuppressWarnings("UnusedParameters") final DialogEvent dialogEvent) {
        pendingSelectedZoneIds.setValue(observableSet(applicationModel.selectedZoneIds.toArray(new
                ZoneId[applicationModel.selectedZoneIds.size()])));
        zoneIdCheckboxes.stream().forEach(zoneIdCheckBox -> zoneIdCheckBox.setSelected(pendingSelectedZoneIds
                .contains(ZoneId.of(zoneIdCheckBox.getText()))));
    }

    private Node createZoneIdSelectionBox() {
        final HBox mainListBox = new HBox(5);
        partition(newArrayList(ApplicationModel.ZONE_OFFSETS_2_ZONE_IDS.values()), 40).forEach(zoneIds -> {
            final VBox columnBox = new VBox(5);
            zoneIds.forEach(zoneId -> {
                final CheckBox zoneIdCheckbox = new CheckBox(zoneId.toString());
                zoneIdCheckbox.setSelected(applicationModel.selectedZoneIds.contains(zoneId));
                zoneIdCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        pendingSelectedZoneIds.add(zoneId);
                    } else {
                        pendingSelectedZoneIds.remove(zoneId);
                    }
                });
                columnBox.getChildren().add(zoneIdCheckbox);
                zoneIdCheckboxes.add(zoneIdCheckbox);
            });
            mainListBox.getChildren().add(columnBox);
        });
        return mainListBox;
    }
}
