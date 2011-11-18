package org.onehippo.forge.externalresource.api.utils;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.*;

/**
 * @version $Id$
 */
public class NodePluginConfig implements IPluginConfig {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(NodePluginConfig.class);

    private Node node;
    private Set<IPluginConfig> children;
    private Map<String, Object> map;
    private String name;

    public NodePluginConfig(Node node) {
        try {
            if (!(node.isNodeType("frontend:plugin") || node.isNodeType("frontend:pluginconfig")))
                throw new IllegalArgumentException("node is not frontend:plugin of frontend:pluginconfig");

            this.node = node;
            //eager loading
            this.name = node.getName();

            this.map = new HashMap<String, Object>();
            PropertyIterator propertyIterator = node.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                this.map.put(property.getName(), getValue(property.getValue()));
            }

            this.children = new HashSet<IPluginConfig>();
            NodeIterator nodeIterator = node.getNodes();
            while (nodeIterator.hasNext()) {
                Node subNode = nodeIterator.nextNode();
                NodePluginConfig subConfig = new NodePluginConfig(subNode);
                this.children.add(subConfig);
                this.map.put(subNode.getName(), subConfig);
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    private Object getValue(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                return Boolean.valueOf(value.getBoolean());
            case PropertyType.LONG:
                return Long.valueOf(value.getLong());
            case PropertyType.DOUBLE:
                return Double.valueOf(value.getDouble());
            case PropertyType.STRING:
            default:
                return value.getString();
        }
    }

    /* Helper method for default value */
    public Object get(String key, Object defaultValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    public IPluginConfig getPluginConfig(Object key) {
        return (IPluginConfig) map.get(key);
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        return children;
    }

    /*Boolean*/
    public boolean getBoolean(String key) throws StringValueConversionException {
        return (Boolean) get(key, false);
    }

    public Boolean getAsBoolean(String key) {
        return (Boolean) map.get(key);
    }

    public boolean getAsBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf((Boolean) get(key, defaultValue));
    }

    /*Double */
    public Double getAsDouble(String key) {
        return (Double) map.get(key);
    }

    public double getAsDouble(String key, double defaultValue) {
        return (Double) get(key, defaultValue);
    }

    public double getDouble(String key) throws StringValueConversionException {
        return (Double) map.get(key);
    }

    public double getDouble(String key, double defaultValue) throws StringValueConversionException {
        return (Double) get(key, defaultValue);
    }

    /*INt = long */
    public int getInt(String key) throws StringValueConversionException {
        return (int) getLong(key);
    }

    public int getInt(String key, int defaultValue) throws StringValueConversionException {
        return (int) getLong(key, defaultValue);
    }

    public Integer getAsInteger(String key) {
        return new Integer((int) getLong(key));
    }

    public int getAsInteger(String key, int defaultValue) {
        return (int) getLong(key, defaultValue);
    }

    /*long is int*/
    public long getLong(String key) throws StringValueConversionException {
        return (Long) map.get(key);
    }

    public long getLong(String key, long defaultValue) throws StringValueConversionException {
        return (Long) get(key, defaultValue);
    }

    public Long getAsLong(String key) {
        return (Long) map.get(key);
    }

    public long getAsLong(String key, long defaultValue) {
        return (Long) get(key, defaultValue);
    }

    /*String*/

    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    public String getString(String key) {
        return (String) map.get(key);
    }

    public String getKey(String key) {
        return (String) map.get(key);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Object> m) {
        map.putAll(m);
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Object> values() {
        return map.values();
    }

    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    public boolean isImmutable() {
        return false;
    }

    /*Wicket unsupported*/
    public StringValue getStringValue(String key) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public String[] getStringArray(String key) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public CharSequence getCharSequence(String key) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Time getTime(String key) throws StringValueConversionException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public IValueMap makeImmutable() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Duration getAsDuration(String key) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Duration getAsDuration(String key, Duration defaultValue) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Time getAsTime(String key) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Time getAsTime(String key, Time defaultValue) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public <T extends Enum<T>> T getAsEnum(String key, Class<T> eClass) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public <T extends Enum<T>> T getAsEnum(String key, T defaultValue) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public <T extends Enum<T>> T getAsEnum(String key, Class<T> eClass, T defaultValue) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void startObservation() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void stopObservation() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Duration getDuration(String key) throws StringValueConversionException {
        throw new UnsupportedOperationException("not implemented yet");
    }

}
