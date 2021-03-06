package com.pranveraapp.common.extensibility.jpa;

import com.pranveraapp.common.exception.ExceptionHelper;
import com.pranveraapp.common.extensibility.jpa.convert.EntityMarkerClassTransformer;
import com.pranveraapp.common.extensibility.jpa.convert.PranveraAppClassTransformer;
import com.pranveraapp.common.extensibility.jpa.copy.NullClassTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Created by elion on 24/01/16.
 */
public class MergePersistenceUnitManager extends DefaultPersistenceUnitManager{

    private static final Log LOG =LogFactory.getLog(MergePersistenceUnitManager.class);

    protected HashMap<String,PersistenceUnitInfo> mergedPus = new HashMap<String,PersistenceUnitInfo>();
    protected List<PranveraAppClassTransformer> classTransformers = new ArrayList<PranveraAppClassTransformer>();

    @Resource(name="elMergedPersistenceXmlLocations")
    protected Set<String> mergedPersistenceXmlLocations;

    @Resource(name="elMergedDataSources")
    protected Map<String, DataSource> mergedDataSources;

    @Resource(name = "elMergedClassTransformers")
    protected Set<PranveraAppClassTransformer> mergedClassTransformers;

    @Resource(name = "elEntityMarkerClassTransformer")
    protected EntityMarkerClassTransformer entityMarkerClassTransformer;

    @Override
    protected boolean isPersistenceUnitOverrideAllowed(){
        return true;
    }

    @PostConstruct
    public void configureMergedItems(){
        String[] tempLocations;
        try{
            Field persistenceXmlLocations = DefaultPersistenceUnitManager.class.getDeclaredField("persistenceXmlLocations");
            persistenceXmlLocations.setAccessible(true);
            tempLocations = (String []) persistenceXmlLocations.get(this);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

        for(String legacyLocation : tempLocations){
            if(!legacyLocation.endsWith("/persistence.xml")){
                //do not add the Default JPA persistence location dy default
                mergedPersistenceXmlLocations.add(legacyLocation);
            }
        }
        setPersistenceXmlLocations(mergedPersistenceXmlLocations.toArray(new String[mergedPersistenceXmlLocations.size()]));

        if(!mergedDataSources.isEmpty()){
            setDataSources(mergedDataSources);
        }
    }

    @PostConstruct
    public void configureClassTransformers()throws InstantiationException,IllegalArgumentException,ClassNotFoundException{
            classTransformers.addAll(mergedClassTransformers);
    }

    protected MutablePersistenceUnitInfo getMergedUnit(String persistenceUnitName, MutablePersistenceUnitInfo newPU){
        if(!mergedPus.containsKey(persistenceUnitName)){
            mergedPus.put(persistenceUnitName,newPU);
        }
        return (MutablePersistenceUnitInfo)mergedPus.get(persistenceUnitName);
    }

    @Override
    @SuppressWarnings({"unchecked","ToArrayCallWithZeroLengthArrayArgument"})
    public void preparePersistenceUnitInfos(){
        //Need to use reflection to try and execute the logic in the DefaultPersistenceUnitManager
        //SpringSource added a block of code in version 3.1 to "protect" the user from having more than one PU with
        //the same name. Of course, in our case, this happens before a merge occurs.
        //They have added a block of code to throw an exception if more than one PU has the same name.
        //We want to use the logic of DefauldPeristenceUnitManager without the exception in the case of
        // a duplicate name. This will require reflection in order to do what we need.

        try {
            //Occhio questo qui erra null con il reflection ovviamente non lancia in exception ma nn lo so fare per il momento
            Set<String> persistenceUnitInfoNames = new HashSet<String>();


            Map<String, PersistenceUnitInfo> persistenceUnitInfos = null;
            ResourcePatternResolver resourcePatternResolver = null;

            Field[] fields = getClass().getSuperclass().getDeclaredFields();

            for (Field field : fields) {
                if ("persistenceUnitInfoNames".equals(field.getName())) {
                    field.setAccessible(true);
                } else if ("persistenceUnitInfos".equals(field.getName())) {
                    field.setAccessible(true);
                    persistenceUnitInfos = (Map<String, PersistenceUnitInfo>) field.get(this);
                } else if ("resourcePatternResolver".equals(field.getName())) {
                    field.setAccessible(true);
                    resourcePatternResolver = (ResourcePatternResolver) field.get(this);
                }
            }
            persistenceUnitInfoNames.clear();
            persistenceUnitInfos.clear();

            Method readPersistenceUnitInfos =
                    getClass().
                            getSuperclass().
                            getDeclaredMethod("readPersistenceUnitInfos");
            readPersistenceUnitInfos.setAccessible(true);

            //in spring 3.0 this return an array
            //in spring 3.1 this retrun a list
            Object pInfosObject = readPersistenceUnitInfos.invoke(this);
            Object[] puis;

            if (pInfosObject.getClass().isArray()) {
                puis = (Object[]) pInfosObject;
            } else {
                puis = ((Collection) pInfosObject).toArray();
            }

            for (Object pui : puis) {
                MutablePersistenceUnitInfo mPui = (MutablePersistenceUnitInfo) pui;
                if (mPui.getPersistenceUnitRootUrl() == null) {
                    Method determineDefaultPersistenceUnitRootUrl =
                            getClass().
                                    getSuperclass().
                                    getDeclaredMethod("determineDefaultPersistenceUnitRootUrl");
                    determineDefaultPersistenceUnitRootUrl.setAccessible(true);
                    mPui.setPersistenceUnitRootUrl((URL) determineDefaultPersistenceUnitRootUrl.invoke(this));
                }
                ConfigurationOnlyState state = ConfigurationOnlyState.getState();
                if ((state == null || !state.isConfigurationOnly()) && mPui.getNonJtaDataSource() == null) {
                    mPui.setNonJtaDataSource(getDefaultDataSource());
                }
                if (super.getLoadTimeWeaver() != null) {
                    Method puiInitMethod = mPui.getClass().getDeclaredMethod("init", LoadTimeWeaver.class);
                    puiInitMethod.setAccessible(true);
                    puiInitMethod.invoke(pui, getLoadTimeWeaver());
                } else {
                    Method puiInitMethod = mPui.getClass().getDeclaredMethod("init", ClassLoader.class);
                    puiInitMethod.setAccessible(true);
                    puiInitMethod.invoke(pui, resourcePatternResolver.getClassLoader());
                }
                postProcessPersistenceUnitInfo((MutablePersistenceUnitInfo) pui);
                String name = mPui.getPersistenceUnitName();
                persistenceUnitInfoNames.add(name);

                persistenceUnitInfos.put(name, mPui);
            }
            }catch (Exception e) {
            throw new RuntimeException("An error occured reflectively invoking methods on " +
                    "class: " + getClass().getSuperclass().getName(), e);
            }


        try {
            List<String> managedClassNames = new ArrayList<String>();

            boolean weaverRegistered = true;
            for (PersistenceUnitInfo pui : mergedPus.values()) {
                for (PranveraAppClassTransformer transformer : classTransformers) {
                    try {
                        if (!(transformer instanceof NullClassTransformer) && pui.getPersistenceUnitName().equals("elPU")) {
                            pui.addTransformer(transformer);
                        }
                    } catch (Exception e) {
                        Exception refined = ExceptionHelper.refineException(IllegalStateException.class, RuntimeException.class, e);
                        if (refined instanceof IllegalStateException) {
                            LOG.warn("A PranveraAppClassTransformer is configured for this persistence unit, but Spring " +
                                    "reported a problem (likely that a LoadTimeWeaver is not registered). As a result, " +
                                    "the PranveraApp ClassTransformer ("+transformer.getClass().getName()+") is " +
                                    "not being registered with the persistence unit.");
                            weaverRegistered = false;
                        } else {
                            throw refined;
                        }
                    }
                }
            }

            // Only validate transformation results if there was a LoadTimeWeaver registered in the first place
            if (weaverRegistered) {
                for (PersistenceUnitInfo pui : mergedPus.values()) {
                    for (String managedClassName : pui.getManagedClassNames()) {
                        if (!managedClassNames.contains(managedClassName)) {
                            // Force-load this class so that we are able to ensure our instrumentation happens globally.
                            // If transformation is happening, it should be tracked in EntityMarkerClassTransformer
                            Class.forName(managedClassName, true, getClass().getClassLoader());
                            managedClassNames.add(managedClassName);
                        }
                    }
                }

                // If a class happened to be loaded by the ClassLoader before we had a chance to set up our instrumentation,
                // it may not be in a consistent state. This verifies with the EntityMarkerClassTransformer that it
                // actually saw the classes loaded by the above process
                List<String> nonTransformedClasses = new ArrayList<String>();
                for (PersistenceUnitInfo pui : mergedPus.values()) {
                    for (String managedClassName : pui.getManagedClassNames()) {
                        // We came across a class that is not a real persistence class (doesn't have the right annotations)
                        // but is still being transformed/loaded by
                        // the persistence unit. This might have unexpected results downstream, but it could also be benign
                        // so just output a warning
                        if (entityMarkerClassTransformer.getTransformedNonEntityClassNames().contains(managedClassName)) {
                            LOG.warn("The class " + managedClassName + " is marked as a managed class within the MergePersistenceUnitManager"
                                    + " but is not annotated with @Entity, @MappedSuperclass or @Embeddable."
                                    + " This class is still referenced in a persistence.xml and is being transformed by"
                                    + " PersistenceUnit ClassTransformers which may result in problems downstream"
                                    + " and represents a potential misconfiguration. This class should be removed from"
                                    + " your persistence.xml");
                        } else if (!entityMarkerClassTransformer.getTransformedEntityClassNames().contains(managedClassName)) {
                            // This means the class not in the 'warning' list, but it is also not in the list that we would
                            // expect it to be in of valid entity classes that were transformed. This means that we
                            // never got the chance to transform the class AT ALL even though it is a valid entity class
                            nonTransformedClasses.add(managedClassName);
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(nonTransformedClasses)) {
                    String message = "The classes\n" + Arrays.toString(nonTransformedClasses.toArray()) + "\nare managed classes within the MergePersistenceUnitManager"
                            + "\nbut were not detected as being transformed by the EntityMarkerClassTransformer. These"
                            + "\nclasses are likely loaded earlier in the application startup lifecyle by the servlet"
                            + "\ncontainer. Verify that an empty <absolute-ordering /> element is contained in your"
                            + "\nweb.xml to disable scanning for ServletContainerInitializer classes by your servlet"
                            + "\ncontainer which can trigger early class loading. If the problem persists, ensure that"
                            + "\nthere are no bean references to your entity class anywhere else in your Spring applicationContext"
                            + "\nand consult the documentation for your servlet container to determine if classes are loaded"
                            + "\nprior to the Spring context initialization. Also, it is a necessity that "
                            + "\n'-javaagent:/path/to/spring-instrument-4.1.5.jar' be added to the JVM args of the server."
                            + "\nFinally, ensure that Session Persistence is also disabled by your Servlet Container."
                            + "\nTo do this in Tomcat, add <Manager pathname=\"\" /> inside of the <Context> element"
                            + "\nin context.xml in your app's META-INF folder or your server's conf folder.";
                    LOG.error(message);
                    throw new IllegalStateException(message);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo newPU) {
        super.postProcessPersistenceUnitInfo(newPU);
        ConfigurationOnlyState state = ConfigurationOnlyState.getState();
        String persistenceUnitName = newPU.getPersistenceUnitName();
        MutablePersistenceUnitInfo pui = getMergedUnit(persistenceUnitName, newPU);

        List<String> managedClassNames = newPU.getManagedClassNames();
        for (String managedClassName : managedClassNames){
            if (!pui.getManagedClassNames().contains(managedClassName)) {
                pui.addManagedClassName(managedClassName);
            }
        }
        List<String> mappingFileNames = newPU.getMappingFileNames();
        for (String mappingFileName : mappingFileNames) {
            if (!pui.getMappingFileNames().contains(mappingFileName)) {
                pui.addMappingFileName(mappingFileName);
            }
        }
        pui.setExcludeUnlistedClasses(newPU.excludeUnlistedClasses());
        for (URL url : newPU.getJarFileUrls()) {
            // Avoid duplicate class scanning by Ejb3Configuration. Do not re-add the URL to the list of jars for this
            // persistence unit or duplicate the persistence unit root URL location (both types of locations are scanned)
            if (!pui.getJarFileUrls().contains(url) && !pui.getPersistenceUnitRootUrl().equals(url)) {
                pui.addJarFileUrl(url);
            }
        }
        if (pui.getProperties() == null) {
            pui.setProperties(newPU.getProperties());
        } else {
            Properties props = newPU.getProperties();
            if (props != null) {
                for (Object key : props.keySet()) {
                    pui.getProperties().put(key, props.get(key));
                    for (PranveraAppClassTransformer transformer : classTransformers) {
                        try {
                            transformer.compileJPAProperties(props, key);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        if (state == null || !state.isConfigurationOnly()) {
            if (newPU.getJtaDataSource() != null) {
                pui.setJtaDataSource(newPU.getJtaDataSource());
            }
            if (newPU.getNonJtaDataSource() != null) {
                pui.setNonJtaDataSource(newPU.getNonJtaDataSource());
            }
        } else {
            pui.getProperties().setProperty("hibernate.hbm2ddl.auto", "none");
            pui.getProperties().setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        }
        pui.setTransactionType(newPU.getTransactionType());
        if (newPU.getPersistenceProviderClassName() != null) {
            pui.setPersistenceProviderClassName(newPU.getPersistenceProviderClassName());
        }
        if (newPU.getPersistenceProviderPackageName() != null) {
            pui.setPersistenceProviderPackageName(newPU.getPersistenceProviderPackageName());
        }
    }


    @Override
    public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) {
        return mergedPus.get(persistenceUnitName);
    }

     @Override
    public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() {
        throw new IllegalStateException("Default Persistence Unit is not supported. The persistence unit name must be specified at the entity manager factory.");
    }

    public List<PranveraAppClassTransformer> getClassTransformers() {
        return classTransformers;
    }

    public void setClassTransformers(List<PranveraAppClassTransformer> classTransformers) {
        this.classTransformers = classTransformers;
    }


}































































