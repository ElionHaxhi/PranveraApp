
package com.pranveraapp.common.logging;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * This is not hooked up by default so that log4j is not required. If you are using Log4j, you can add this class to your
 * Spring applicationContext to enable it.
 * 
 * @author elion
 */
@ManagedResource(objectName="org.broadleafcommerce:name=Log4JManangement", description="Logging Management", currencyTimeLimit=15)
public class Log4jManagementBean {

    @ManagedOperation(description="Activate info level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category to set")})
    public void activateInfo(String category) {
        LogManager.getLogger(category).setLevel(Level.INFO);
    }

    @ManagedOperation(description="Activate debug level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category to set")})
    public void activateDebug(String category) {
        LogManager.getLogger(category).setLevel(Level.DEBUG);
    }

    @ManagedOperation(description="Activate warn level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category to set")})
    public void activateWarn(String category) {
        LogManager.getLogger(category).setLevel(Level.WARN);
    }

    @ManagedOperation(description="Activate error level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category to set")})
    public void activateError(String category) {
        LogManager.getLogger(category).setLevel(Level.ERROR);
    }

    @ManagedOperation(description="Activate fatal level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category to set")})
    public void activateFatal(String category) {
        LogManager.getLogger(category).setLevel(Level.FATAL);
    }

    @ManagedOperation(description="Retrieve the category log level")
    @ManagedOperationParameters({@ManagedOperationParameter(name = "category", description = "the log4j category")})
    public String getLevel(String category) {
        return LogManager.getLogger(category).getLevel().toString();
    }

}
