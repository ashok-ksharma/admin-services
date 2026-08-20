package io.mosip.admin.bulkdataupload.batch;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;

import io.mosip.kernel.masterdata.entity.BaseEntity;
import io.mosip.kernel.masterdata.entity.Device;
import io.mosip.kernel.masterdata.entity.Machine;
import io.mosip.kernel.masterdata.entity.RegistrationCenter;
import io.mosip.kernel.masterdata.entity.UserDetails;
import io.mosip.kernel.masterdata.entity.ZoneUser;
import io.mosip.kernel.masterdata.entity.DeviceHistory;
import io.mosip.kernel.masterdata.entity.MachineHistory;
import io.mosip.kernel.masterdata.entity.RegistrationCenterHistory;
import io.mosip.kernel.masterdata.entity.UserDetailsHistory;
import io.mosip.kernel.masterdata.entity.ZoneUserHistory;
import io.mosip.admin.config.Mapper;
import io.mosip.admin.config.MapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MethodInvoker;

import io.mosip.admin.bulkdataupload.constant.BulkUploadErrorCode;
import io.mosip.admin.packetstatusupdater.exception.RequestException;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

/**
 * This class will write the information in database
 * @author dhanendra
 *
 * @param <T>
 */
public class RepositoryListItemWriter<T> implements ItemWriter<T> {
	private static final Logger LOGGER =  LoggerFactory.getLogger(RepositoryListItemWriter.class);

    private String methodName;
    private EntityManager em;
    private EntityManagerFactory emf;
    private Class<?> entity;
    private Mapper mapper;
    private ApplicationContext applicationContext;
    private String repoBeanName;
    private String operation;

    public RepositoryListItemWriter() {
    }
    
    public RepositoryListItemWriter(EntityManager em,EntityManagerFactory emf,Class<?> entity,Mapper mapper,ApplicationContext applicationContext) {
    	this.em=em;
    	this.emf=emf;
    	this.entity=entity;
    	this.mapper=mapper;
    	this.applicationContext=applicationContext;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setRepoBeanName(String repoBeanName) {
        this.repoBeanName = repoBeanName;
    }

    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        if(!items.isEmpty()) {
            this.doWrite(items);
        }
    }

    protected void doWrite(Chunk<? extends T> items) throws Exception {
    	LOGGER.info("Writing to the repository with " + items.size() + " items.");
        try {
            BaseRepository baseRepository = (BaseRepository) applicationContext.getBean(this.repoBeanName);
            Iterator i$ = items.iterator();
            while(i$.hasNext()) {
                Object object = i$.next();
                PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
                Object identifier = util.getIdentifier(object);
                T existingRecord = (T) em.find(entity, identifier);
                switch (this.operation) {
                    case "insert":
                        MethodInvoker invoker = this.createMethodInvoker(baseRepository, this.methodName);
                        if(existingRecord !=null) {
                            throw new RequestException(BulkUploadErrorCode.ENTRY_EXISTS_SAME_IDENTIFIER.getErrorCode(),
                                    "Entry already exists with this id >> " + identifier);
                        }
                        invoker.setArguments(new Object[]{object});
                        this.doInvoke(invoker);
                        break;

                    case "update":
                        BaseRepository baseRepositoryToUpdate = (BaseRepository) applicationContext.getBean(this.repoBeanName);
                        if(existingRecord == null) {
                            throw new RequestException(BulkUploadErrorCode.BULK_OPERATION_ERROR.getErrorCode(),
                                    "No entry found with this id >> " + identifier);
                        }
                        ((BaseEntity)object).setCreatedBy(((BaseEntity)existingRecord).getCreatedBy());
                        ((BaseEntity)object).setCreatedDateTime(((BaseEntity)existingRecord).getCreatedDateTime());
                        ((BaseEntity)object).setIsDeleted(((BaseEntity)existingRecord).getIsDeleted());
                        baseRepositoryToUpdate.save(object);
                        break;

                    case "delete":
                        BaseRepository baseRepositoryToDelete = (BaseRepository) applicationContext.getBean(this.repoBeanName);
                        if(existingRecord == null) {
                            throw new RequestException(BulkUploadErrorCode.BULK_OPERATION_ERROR.getErrorCode(),
                                    "No entry found with this id >> " + identifier);
                        }
                        ((BaseEntity)object).setCreatedBy(((BaseEntity)existingRecord).getCreatedBy());
                        ((BaseEntity)object).setCreatedDateTime(((BaseEntity)existingRecord).getCreatedDateTime());
                        ((BaseEntity)object).setUpdatedBy(((BaseEntity)existingRecord).getUpdatedBy());
                        ((BaseEntity)object).setUpdatedDateTime(((BaseEntity)existingRecord).getUpdatedDateTime());
                        baseRepositoryToDelete.save(object);
                        break;
                }
                createHistoryRecord(object);
            }
        } catch (Throwable t) {
            LOGGER.error(BulkUploadErrorCode.BATCH_ERROR.getErrorCode(), t);
            throw new JobExecutionException(t.getMessage() + (t.getCause() != null ? t.getCause().getMessage() : ""));
        }
    }

    /**
     * Writes the matching {@code *_h} history row for the entity just written.
     *
     * <p>
     * This used to switch on {@code entity.getCanonicalName()} against five hard-coded
     * fully-qualified {@code io.mosip.admin.bulkdataupload.entity.*} strings. Those strings
     * went stale the moment the entity sets were consolidated onto masterdata's canonical
     * classes, and the failure would have been silent: every branch would miss, control
     * would fall through the default, and bulk upload would simply stop writing history to
     * zone_user_h, user_detail_h, machine_master_h, device_master_h and
     * registration_center_h - no exception, no log.
     * </p>
     *
     * <p>
     * Comparing {@code Class} objects instead means the compiler now resolves these names.
     * A future package move breaks the build rather than the data.
     * </p>
     */
    private void createHistoryRecord(Object object) {
        if (ZoneUser.class.equals(entity)) {
            ZoneUserHistory userHistory = new ZoneUserHistory();
            MapperUtils.map(object, userHistory);
            MapperUtils.setBaseFieldValue(object, userHistory);
            userHistory.setEffDTimes(userHistory.getCreatedDateTime());
            saveHistory(ZoneUserHistory.class, userHistory);
        } else if (UserDetails.class.equals(entity)) {
            UserDetailsHistory userDetailHistory = new UserDetailsHistory();
            MapperUtils.map(object, userDetailHistory);
            MapperUtils.setBaseFieldValue(object, userDetailHistory);
            userDetailHistory.setEffDTimes(userDetailHistory.getCreatedDateTime());
            saveHistory(UserDetailsHistory.class, userDetailHistory);
        } else if (Machine.class.equals(entity)) {
            MachineHistory machineHistory = new MachineHistory();
            MapperUtils.map(object, machineHistory);
            MapperUtils.setBaseFieldValue(object, machineHistory);
            machineHistory.setEffectDateTime(machineHistory.getCreatedDateTime());
            saveHistory(MachineHistory.class, machineHistory);
        } else if (Device.class.equals(entity)) {
            DeviceHistory deviceHistory = new DeviceHistory();
            MapperUtils.map(object, deviceHistory);
            MapperUtils.setBaseFieldValue(object, deviceHistory);
            deviceHistory.setEffectDateTime(deviceHistory.getCreatedDateTime());
            saveHistory(DeviceHistory.class, deviceHistory);
        } else if (RegistrationCenter.class.equals(entity)) {
            RegistrationCenterHistory registrationCenterHistory = new RegistrationCenterHistory();
            MapperUtils.map(object, registrationCenterHistory);
            MapperUtils.setBaseFieldValue(object, registrationCenterHistory);
            registrationCenterHistory.setEffectivetimes(registrationCenterHistory.getCreatedDateTime());
            saveHistory(RegistrationCenterHistory.class, registrationCenterHistory);
        }
    }

    private void saveHistory(Class<?> historyType, Object historyRecord) {
        String historyRepoBeanName = mapper.getRepo(historyType);
        BaseRepository historyBaseRepo = (BaseRepository) applicationContext.getBean(historyRepoBeanName);
        historyBaseRepo.save(historyRecord);
    }

    /*public void afterPropertiesSet() throws Exception {
        Assert.state(this.repository != null, "A CrudRepository implementation is required");
    }*/

    private Object doInvoke(MethodInvoker invoker) throws Exception {
        try {
            invoker.prepare();
        } catch (ClassNotFoundException var3) {
            throw new DynamicMethodInvocationException(var3);
        } catch (NoSuchMethodException var4) {
            throw new DynamicMethodInvocationException(var4);
        }

        try {
            return invoker.invoke();
        } catch (InvocationTargetException var5) {
            if(var5.getCause() instanceof Exception) {
                throw (Exception)var5.getCause();
            } else {
                throw new AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper(var5.getCause());
            }
        } catch (IllegalAccessException var6) {
            throw new DynamicMethodInvocationException(var6);
        }
    }

    private MethodInvoker createMethodInvoker(Object targetObject, String targetMethod) {
        MethodInvoker invoker = new MethodInvoker();
        invoker.setTargetObject(targetObject);
        invoker.setTargetMethod(targetMethod);
        return invoker;
    }

}
