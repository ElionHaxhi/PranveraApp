package com.pranveraapp.common.cache;

import com.pranveraapp.common.time.SystemTime;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.stereotype.Service;

import javax.management.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by elion on 25/02/16.
 */
@Service("elStatisticsService")
public class StatisticsServiceImpl implements DynamicMBean, StatisticsService, SelfNaming {

    private static final Log LOG = LogFactory.getLog(StatisticsServiceImpl.class);

    //@Value(value = "${30000}")
    protected Long logResolution = 30000L;

    protected String appName = "pranverapp";

    protected StatisticsServiceLogAdapter adapter;

    protected Map<String, CacheStat> cacheStats = Collections.synchronizedMap(new HashMap<String, CacheStat>());

    @Override
    public void addCacheStat(String key, boolean isHit) {
        CacheStat myStat = getCacheStat(key);
        if (isHit) {
            myStat.incrementHit();
        }
        myStat.incrementRequest();
        if (myStat.getLastLogTime() + logResolution < SystemTime.asMillis()) {
            myStat.setLastLogTime(SystemTime.asMillis());
            BigDecimal percentage = myStat.getHitRate();
            if (LOG.isInfoEnabled()) {
                LOG.info("Cache hit percentage for " + key + " is: " + percentage.toString() + "%");
            }
        }
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    protected CacheStat getCacheStat(String key) {
        if (!cacheStats.containsKey(key)) {
            CacheStat stat = new CacheStat();
            cacheStats.put(key, stat);
        }
        return cacheStats.get(key);
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attribute.equals("LOG_RESOLUTION")) {
            return getLogResolution();
        }
        return getCacheStat(attribute).getHitRate().doubleValue();
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        if (attribute.getName().equals("LOG_RESOLUTION")) {
            setLogResolution((Long) attribute.getValue());
        }
        //do nothing - not allowed
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList list = new AttributeList();
        for (Map.Entry<String, CacheStat> stats : cacheStats.entrySet()) {
            list.add(new Attribute(stats.getKey(), stats.getValue().getHitRate().doubleValue()));
        }
        return list;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        for (Object attr : attributes) {
            try {
                setAttribute((Attribute) attr);
            } catch (Exception e) {
                LOG.error("cannot set attribute: " + ((Attribute) attr).getName(), e);
            }
        }
        return attributes;
    }

    @Override
    public Long getLogResolution() {
        return logResolution;
    }

    @Override
    public void setLogResolution(Long logResolution) {
        this.logResolution = logResolution;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
            ReflectionException {
        if (actionName.equals("activate")) {
            activateLogging();
            return null;
        } else if (actionName.equals("disable")) {
            disableLogging();
            return null;
        }
        throw new MBeanException(new RuntimeException("Not Supported"));
    }

    @Override
    public void activateLogging() {
        if (getAdapter() != null) {
            getAdapter().activateLogging(StatisticsServiceImpl.class);
        }
    }

    @Override
    public void disableLogging() {
        if (getAdapter() != null) {
            getAdapter().disableLogging(StatisticsServiceImpl.class);
        }
    }

    public StatisticsServiceLogAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(StatisticsServiceLogAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        SortedSet<String> names = new TreeSet<String>();
        for (Map.Entry<String, CacheStat> stats : cacheStats.entrySet()) {
            names.add(stats.getKey());
        }
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[names.size()];
        Iterator<String> it = names.iterator();
        for (int i = 0; i < attrs.length; i++) {
            String name = it.next();
            attrs[i] = new MBeanAttributeInfo(
                    name,
                    "java.lang.Double",
                    name,
                    true,   // isReadable
                    false,   // isWritable
                    false); // isIs
        }
        attrs = ArrayUtils.add(attrs, new MBeanAttributeInfo(
                "LOG_RESOLUTION",
                "java.lang.Double",
                "LOG_RESOLUTION",
                true,   // isReadable
                true,   // isWritable
                false) // isIs
        );
        MBeanOperationInfo[] opers = {
                new MBeanOperationInfo(
                        "activate",
                        "Activate statistic logging",
                        null,   // no parameters
                        "void",
                        MBeanOperationInfo.ACTION),
                new MBeanOperationInfo(
                        "disable",
                        "Disable statistic logging",
                        null,   // no parameters
                        "void",
                        MBeanOperationInfo.ACTION)
        };
        return new MBeanInfo(
                "com.pranveraapp:name=StatisticsService." + appName,
                "Runtime Statistics",
                attrs,
                null,  // constructors
                opers,
                null); // notifications
    }

    @Override
    public ObjectName getObjectName() throws MalformedObjectNameException {
        return ObjectNameManager.getInstance("com.pranveraapp:name=StatisticsService." + appName);
    }

}
