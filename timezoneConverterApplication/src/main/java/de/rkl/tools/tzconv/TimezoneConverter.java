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
package de.rkl.tools.tzconv;

import de.rkl.tools.tzconv.configuration.PreferencesProvider;
import de.rkl.tools.tzconv.model.ApplicationModel;
import de.rkl.tools.tzconv.view.ZoneIdSelectionDialog;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;

import static de.rkl.tools.tzconv.configuration.Constants.BEAN_NAME_DATE_FORMATTER;
import static de.rkl.tools.tzconv.configuration.Constants.BEAN_NAME_DATE_TIME_FORMATTER;
import static de.rkl.tools.tzconv.model.ApplicationModel.DEFAULT_INITIAL_HOUR;
import static de.rkl.tools.tzconv.model.ApplicationModel.DEFAULT_INITIAL_MINUTE;
import static javafx.collections.FXCollections.observableArrayList;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@SuppressWarnings("WeakerAccess")
@ComponentScan
public class TimezoneConverter extends Application {
    private static final int DEFAULT_BOX_SPACING = 5;

    private ApplicationModel applicationModel;

    private PreferencesProvider preferencesProvider;

    private ZoneIdSelectionDialog zoneIdSelectionDialog;
    private VelocityEngine velocityEngine;
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationContext initializeSpringContext() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.scan(TimezoneConverter.class.getPackage().getName());
        context.refresh();
        return context;
    }

    @Override
    public void stop() throws Exception {
        preferencesProvider.flushPreferences();
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        initializeSpringContextAndBeans();
        final VBox rootBox = new VBox(DEFAULT_BOX_SPACING, createDateTimeBox(), createTemplateBox(),
                createMainTextArea(), createZoneIdSelectionButtonBox(), createCopyToClipboard(createMainTextArea()));
        primaryStage.setScene(new Scene(rootBox, 700, 1000));
        primaryStage.show();
    }

    private Node createTemplateBox() {
        return new HBox(DEFAULT_BOX_SPACING, createTemplateCheckbox(), createTemplateFileSelection(),
                createTemplateNameField());
    }

    private Node createTemplateNameField() {
        final TextField templateNameField = new TextField();
        templateNameField.setEditable(false);
        final File templateFile = applicationModel.templateFile.getValue();
        templateNameField.setText(templateFile == null ? StringUtils.EMPTY : templateFile.getName());
        applicationModel.templateFile.addListener((observable, oldValue, newValue) -> {
            templateNameField.setText
                    (newValue == null ? StringUtils.EMPTY : newValue.getName());
        });
        return templateNameField;
    }

    private Node createTemplateFileSelection() {
        final Button templateFileChooserButton = new Button();
        templateFileChooserButton.setText("Select template...");
        templateFileChooserButton.setOnAction(event -> applicationModel.templateFile.setValue
                (createTemplateFileChooser().showOpenDialog(null)));
        return templateFileChooserButton;
    }

    private FileChooser createTemplateFileChooser() {
        final FileChooser templateFileChooser = new FileChooser();
        templateFileChooser.setTitle("Select template");
        templateFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Velocity Template", "*.vm"));
        templateFileChooser.initialFileNameProperty().bind(applicationModel.templateFile.asString());
        return templateFileChooser;
    }

    private Node createTemplateCheckbox() {
        final CheckBox templateCheckBox = new CheckBox("Use text template");
        templateCheckBox.setSelected(applicationModel.templateFile.getValue() != null);
        applicationModel.templateFile.addListener((observable, oldValue, newValue) -> {
            templateCheckBox.setSelected(newValue != null);
        });
        templateCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue)
                applicationModel.templateFile.set(null);
        });
        return templateCheckBox;
    }

    private void initializeSpringContextAndBeans() {
        final ApplicationContext springContext = initializeSpringContext();
        applicationModel = springContext.getBean(ApplicationModel.class);
        preferencesProvider = springContext.getBean(PreferencesProvider.class);
        zoneIdSelectionDialog = springContext.getBean(ZoneIdSelectionDialog.class);
        velocityEngine = springContext.getBean(VelocityEngine.class);
        dateFormatter = springContext.getBean(BEAN_NAME_DATE_FORMATTER, DateTimeFormatter.class);
        dateTimeFormatter = springContext.getBean(BEAN_NAME_DATE_TIME_FORMATTER, DateTimeFormatter.class);
    }

    private Node createZoneIdSelectionButtonBox() {
        final Button zoneIdSelection = new Button("Select zone IDs to be included in text");
        zoneIdSelection.setOnAction(this::openZoneIdSelectionDialog);
        final HBox buttonBox = new HBox(DEFAULT_BOX_SPACING, zoneIdSelection);
        buttonBox.alignmentProperty().setValue(Pos.CENTER);
        return buttonBox;
    }

    private void openZoneIdSelectionDialog(@SuppressWarnings("UnusedParameters") final ActionEvent actionEvent) {
        final Optional<Collection<ZoneId>> selectedZoneIds = zoneIdSelectionDialog.showAndWait();
        if (selectedZoneIds.isPresent()) {
            applicationModel.selectedZoneIds.setAll(selectedZoneIds.get());
            applicationModel.sortSelectedZoneIds();
        }
    }

    private Node createCopyToClipboard(final TextInputControl mainArea) {
        final Button copyToClipboard = new Button("Copy to Clipboard");
        copyToClipboard.setOnAction(event -> {
            final StringSelection stringSelection = new StringSelection(mainArea.textProperty().getValue());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
        });
        final HBox copyToClipboardBox = new HBox(copyToClipboard);
        copyToClipboardBox.alignmentProperty().setValue(Pos.CENTER);
        return copyToClipboardBox;
    }

    private TextArea createMainTextArea() {
        final TextArea mainArea = new TextArea();
        mainArea.setEditable(false);
        updateTextAreaContent(mainArea, applicationModel.mainDateTime.getValue());
        applicationModel.mainDateTime.addListener((observable, oldValue, newValue) -> {
            updateTextAreaContent(mainArea, newValue);
        });
        applicationModel.selectedZoneIds.addListener((observable, oldValue, newValue) -> {
            updateTextAreaContent(mainArea, applicationModel.mainDateTime.getValue());
        });
        applicationModel.templateFile.addListener((observable, oldValue, newValue) -> {
            updateTextAreaContent(mainArea,
                    applicationModel.mainDateTime.getValue());
        });
        return mainArea;
    }

    private void updateTextAreaContent(final TextArea mainArea, final ZonedDateTime dateTime) {
        final StringWriter mainAreaWriter = new StringWriter();
        fillFromTemplate(mainAreaWriter, dateTime);
        mainArea.setText(mainAreaWriter.toString());
    }

    private void fillFromTemplate(final StringWriter mainAreaWriter, final ZonedDateTime dateTime) {
        final File templateFileObject = applicationModel.templateFile.getValue();
        if (templateFileObject == null) {
            mainAreaWriter.append(formatConvertedDateTimes(dateTime));
        } else {
            try {
                velocityEngine.evaluate(initVelocityContext(dateTime), mainAreaWriter, templateFileObject.getName(),
                        new FileReader(templateFileObject));
            } catch (FileNotFoundException e) {
                final Alert fileNotFoundAlert = new Alert(Alert.AlertType.ERROR);
                fileNotFoundAlert.setHeaderText("Template file not found");
                fileNotFoundAlert.setContentText(String.valueOf(e));
                fileNotFoundAlert.showAndWait();
            }
        }
    }

    private VelocityContext initVelocityContext(final ZonedDateTime dateTime) {
        final VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("originalDate", formatOriginalDate(dateTime));
        velocityContext.put("convertedTimes", formatConvertedDateTimes(dateTime));
        return velocityContext;
    }

    private String formatOriginalDate(final ZonedDateTime dateTime) {
        return dateTime.format(dateFormatter);
    }

    private String formatConvertedDateTimes(final ZonedDateTime localDateTime) {
        final StringBuilder convertedDateTimeBuilder = new StringBuilder();
        applicationModel.selectedZoneIds.forEach(zoneId -> {
            convertedDateTimeBuilder.append(localDateTime.withZoneSameInstant(zoneId).format(dateTimeFormatter));
            convertedDateTimeBuilder.append('\n');
        });
        return convertedDateTimeBuilder.toString();
    }

    private HBox createDateTimeBox() {
        return new HBox(DEFAULT_BOX_SPACING, createDatePicker(), createHourSpinner(), createMinuteSpinner(),
                createTimezoneComboBox());
    }

    private ComboBox<ZoneId> createTimezoneComboBox() {
        final ComboBox<ZoneId> zoneIdBox = new ComboBox<>(observableArrayList(ApplicationModel
                .ZONE_OFFSETS_2_ZONE_IDS.values()));
        zoneIdBox.setEditable(false);
        zoneIdBox.setValue(applicationModel.mainDateTime.getValue().getZone());
        zoneIdBox.setOnAction(event -> applicationModel.mainDateTime.setValue(applicationModel.mainDateTime.getValue
                ().withZoneSameLocal(zoneIdBox.getValue())));
        return zoneIdBox;
    }

    private Spinner<Integer> createMinuteSpinner() {
        final Spinner<Integer> minuteSpinner = new Spinner<>(0, 45, DEFAULT_INITIAL_MINUTE, 15);
        minuteSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            applicationModel.mainDateTime.setValue(applicationModel.mainDateTime.getValue().withMinute(newValue));
        });
        return minuteSpinner;
    }

    private Spinner<Integer> createHourSpinner() {
        final Spinner<Integer> hourSpinner = new Spinner<>(0, 23, DEFAULT_INITIAL_HOUR);
        hourSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            applicationModel.mainDateTime.setValue(applicationModel.mainDateTime.getValue().withHour(newValue));
        });
        return hourSpinner;
    }

    private DatePicker createDatePicker() {
        final DatePicker datePicker = new DatePicker(applicationModel.mainDateTime.getValue().toLocalDate());
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            applicationModel.mainDateTime.setValue(applicationModel.mainDateTime.getValue().with(newValue));
        });
        return datePicker;
    }

}
