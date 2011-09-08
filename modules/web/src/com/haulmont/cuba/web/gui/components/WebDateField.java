/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 22.12.2008 18:12:13
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.gui.data.DsManager;
import com.haulmont.cuba.web.toolkit.ui.*;
import com.vaadin.data.Property;
import com.vaadin.ui.HorizontalLayout;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.util.*;

public class WebDateField
        extends
        WebAbstractComponent<HorizontalLayout>
        implements
        DateField, Component.Wrapper {

    private Log log = LogFactory.getLog(WebDateField.class);

    private Resolution resolution;

    private Object prevValue = null;
    private boolean editable;
    private boolean updatingInstance;
    private boolean valid;

    private com.haulmont.cuba.web.toolkit.ui.DateField dateField;
    private WebTimeField timeField;

    protected List<ValueListener> listeners = new ArrayList<ValueListener>();
    protected List<Field.Validator> validators = new ArrayList<Field.Validator>();

    private Datasource datasource;
    private DsManager dsManager;
    private MetaPropertyPath metaPropertyPath;
    private MetaProperty metaProperty;

    private boolean closeWhenDateSelected = false;

    private boolean required;

    private String requiredMessage;

    private String dateTimeFormat;
    private String dateFormat;
    private String timeFormat;

    public WebDateField() {
        component = new HorizontalLayout();
        component.setWidth("150px");

        component.setSpacing(true);
        dateField = new com.haulmont.cuba.web.toolkit.ui.DateField();
        dateField.setResolution(com.haulmont.cuba.web.toolkit.ui.DateField.RESOLUTION_DAY);
        dateField.setWidth("100%");

        dateField.setImmediate(true);
        dateField.setInvalidAllowed(true);
        dateField.addValidator(new com.vaadin.data.Validator() {
            @Override
            public void validate(Object value) throws InvalidValueException {
                if (value instanceof Date)
                    return;
                if (!isValid(value)) {
                    dateField.requestRepaint();
                    throw new InvalidValueException("Unable to parse value: " + value);
                }
            }

            @Override
            public boolean isValid(Object value) {
                return true;
            }
        });

        /*component.addComponent(dateField);
        component.setExpandRatio(dateField, 1.0f);*/
        timeField = new WebTimeField();
        /*component.addComponent(timeField.<com.vaadin.ui.Component>getComponent());*/

        dateField.setImmediate(true);
        dateField.setInvalidCommitted(true);
        timeField.<MaskedTextField>getComponent().setImmediate(true);
        timeField.<MaskedTextField>getComponent().setInvalidAllowed(false);
        timeField.<MaskedTextField>getComponent().setInvalidCommitted(true);

        dateField.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                updateInstance();
            }
        });

        timeField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                updateInstance();
            }
        });
        setResolution(Resolution.MIN);
        if (ConfigProvider.getConfig(WebConfig.class).getCloseCalendarWhenDateSelected()) {
            setCloseWhenDateSelected(true);
        }

    }

    public com.haulmont.cuba.web.toolkit.ui.DateField getDateField() {
        return dateField;
    }

    public WebTimeField getTimeField() {
        return timeField;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
        __setResolution(resolution);
        updateLayout();
    }

    public String getDateFormat() {
        return dateTimeFormat;
    }

    public void updateLayout() {
        component.removeAllComponents();
        component.addComponent(dateField);
        component.setExpandRatio(dateField, 1.0f);
        if (resolution.ordinal() < Resolution.DAY.ordinal()) {
            int width = timeField.isAmPmUsed() ? 23 : 0;

            if (isHourUsed() && !isMinUsed()) {
                timeField.setWidth((23 + width) + "px");
            } else if (isHourUsed() && isMinUsed()) {
                timeField.setResolution(resolution);
                timeField.setWidth((46 + width) + "px");
            }
            component.addComponent(timeField.<com.vaadin.ui.Component>getComponent());
        }
    }

    public void setDateFormat(String dateFormat) {
        dateTimeFormat = dateFormat;
        StringBuilder date = new StringBuilder(dateFormat);
        StringBuilder time = new StringBuilder(dateFormat);
        int timeStartPos = dateFormat.indexOf('h');
        if (timeStartPos < 0) {
            timeStartPos = dateFormat.indexOf('H');
        }
        if (timeStartPos >= 0) {
            time.delete(0, timeStartPos);
            date.delete(timeStartPos, dateFormat.length());
            timeFormat = StringUtils.trimToEmpty(time.toString());
            timeField.setFormat(timeFormat);
            setResolution(resolution);
        } else setResolution(Resolution.DAY);

        this.dateFormat = StringUtils.trimToEmpty(date.toString());
        dateField.setDateFormat(this.dateFormat);
    }

    public boolean isCloseWhenDateSelected() {
        return closeWhenDateSelected;
    }

    public void setCloseWhenDateSelected(boolean closeWhenDateSelected) {
        this.closeWhenDateSelected = closeWhenDateSelected;
        __setCloseWhenDateSelected(closeWhenDateSelected);
    }

    protected void __setResolution(Resolution resolution) {
        if (resolution.ordinal() < Resolution.DAY.ordinal()) {
            timeField.setResolution(resolution);
        } else {
            dateField.setResolution(WebComponentsHelper.convertDateFieldResolution(resolution));
        }
    }

    protected void __setCloseWhenDateSelected(boolean autoClose) {
        dateField.setCloseWhenDateSelected(autoClose);
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public void setRequiredMessage(String msg) {
        requiredMessage = msg;
    }

    public String getRequiredMessage() {
        return requiredMessage;
    }

    @Override
    public <T> T getValue() {
        return (T) constructDate();
    }

    @Override
    public void setValue(Object value) {
        prevValue = getValue();
        if (!editable)
            return;
        dateField.setValue(value);
        timeField.setValue(value);
    }

    private void setValueFromDs(Object value) {
        boolean isEditable = editable;
        if (!editable)
            setEditable(true);
        setValue(value);
        setEditable(isEditable);
    }

    private boolean isHourUsed() {
        return resolution != null && resolution.ordinal() <= Resolution.HOUR.ordinal();
    }

    private boolean isMinUsed() {
        return resolution != null && resolution.ordinal() <= Resolution.MIN.ordinal();
    }

    private void updateInstance() {
        if (updatingInstance)
            return;

        updatingInstance = true;
        try {
            if (datasource != null && metaPropertyPath != null) {
                Date value = constructDate();
                if (datasource.getItem() != null) {
                    Object obj = value;
                    Datatype<Object> datatype = metaProperty.getRange().asDatatype();
                    if (!datatype.getJavaClass().equals(Date.class)) {
                        String str = Datatypes.get(Date.class).format(value);
                        try {
                            obj = datatype.parse(str);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    InstanceUtils.setValueEx(datasource.getItem(), metaPropertyPath.getPath(), obj);
                }
            }
            valid = true;
        } catch (RuntimeException e) {
            valid = false;
        } finally {
            updatingInstance = false;
        }
        if (valid) {
            Object newValue = getValue();
            fireValueChanged(newValue);
        }
    }

    @Override
    public void addListener(ValueListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ValueListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addValidator(Validator validator) {
        validators.add(validator);
    }

    @Override
    public void removeValidator(Validator validator) {
        validators.remove(validator);
    }

    @Override
    public Datasource getDatasource() {
        return datasource;
    }

    @Override
    public MetaProperty getMetaProperty() {
        return metaProperty;
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        this.datasource = datasource;

        dsManager = new DsManager(datasource, this);

        final MetaClass metaClass = datasource.getMetaClass();
        metaPropertyPath = metaClass.getPropertyEx(property);
        try {
            metaProperty = metaPropertyPath.getMetaProperty();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Metaproperty name is possibly wrong: " + property, e);
        }

        datasource.addListener(
                new DsListenerAdapter() {
                    @Override
                    public void itemChanged(Datasource ds, Entity prevItem, Entity item) {
                        if (updatingInstance)
                            return;
                        Date value = InstanceUtils.getValueEx(item, metaPropertyPath.getPath());
                        setValueFromDs(value);
                        fireValueChanged(value);
                    }

                    @Override
                    public void valueChanged(Entity source, String property, Object prevValue, Object value) {
                        if (updatingInstance)
                            return;
                        if (property.equals(metaPropertyPath.toString())) {
                           setValueFromDs(value);
                            fireValueChanged(value);
                        }
                    }
                }
        );

        if (datasource.getState() == Datasource.State.VALID && datasource.getItem() != null) {
            if (property.equals(metaPropertyPath.toString())) {
                Date value = InstanceUtils.getValueEx(datasource.getItem(), metaPropertyPath.getPath());
                setValueFromDs(value);
                fireValueChanged(value);
            }
        }


        setRequired(metaProperty.isMandatory());
    }

    protected void fireValueChanged(Object value) {
        for (ValueListener listener : listeners) {
            listener.valueChanged(this, "value", prevValue, value);
        }
    }

    private Date constructDate() {
        final Date datePickerDate = (Date) dateField.getValue();
        if (datePickerDate == null) {
            return null;
        }
        Calendar c = Calendar.getInstance(UserSessionProvider.getLocale());
        c.setTime(datePickerDate);
        if (timeField.getValue() == null) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);

        } else {
            Calendar c2 = Calendar.getInstance(UserSessionProvider.getLocale());
            c2.setTime(timeField.<Date>getValue());

            c.set(Calendar.HOUR_OF_DAY, c2.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c2.get(Calendar.MINUTE));
            c.set(Calendar.SECOND, c2.get(Calendar.SECOND));
        }

        Date time = c.getTime();
        return time;
    }

    @Override
    public String getCaption() {
        return component.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return component.getDescription();
    }

    @Override
    public void setDescription(String description) {
        component.setDescription(description);
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        if (this.editable == editable)
            return;
        this.editable = editable;
        timeField.setEditable(editable);
        dateField.setReadOnly(!editable);
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void validate() throws ValidationException {
        if (!isVisible() || !isEditable() || !isEnabled())
            return;

        Object value = getValue();
        if (value == null) {
            if (isRequired())
                throw new RequiredValueMissingException(requiredMessage, this);
            else
                return;
        }

        for (Field.Validator validator : validators) {
            validator.validate(value);
        }
    }
}