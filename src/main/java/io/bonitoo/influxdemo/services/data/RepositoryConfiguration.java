package io.bonitoo.influxdemo.services.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import io.bonitoo.influxdemo.services.HubDiscoveryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.CollectionFactory;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.map.MapKeyValueAdapter;
import org.springframework.data.map.repository.config.EnableMapRepositories;

/**
 * @author Jakub Bednar (bednar@github) (22/05/2019 08:45)
 */
@Configuration
@EnableMapRepositories(value = {"io.bonitoo.influxdemo.domain", "io.bonitoo.influxdemo.services.data"},
        keyValueTemplateRef = "persistKeyValue")
public class RepositoryConfiguration {

    private static Logger log = LoggerFactory.getLogger(HubDiscoveryService.class);

    private final String dbPath;

    public RepositoryConfiguration(@Value("${petstore.db:./db.ser}") final String dbPath) {
        this.dbPath = dbPath;
    }

    @Bean
    public KeyValueOperations persistKeyValue() {
        return new KeyValueTemplate(keyValueAdapter());
    }

    @Bean
    public KeyValueAdapter keyValueAdapter() {

        Map<String, Map<Object, Object>> store = null;

        //
        // Populate store from file
        //
        if (new File(dbPath).exists()) {

            log.info("Populate store from {}", dbPath);

            try {

                FileInputStream fis = new FileInputStream(dbPath);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                store = (Map<String, Map<Object, Object>>) ois.readObject();
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (store == null) {

            store = CollectionFactory.createMap(ConcurrentHashMap.class, 100);
        }

        return new PersistMapKeyValueAdapter(store);
    }

    private class PersistMapKeyValueAdapter extends MapKeyValueAdapter {
        
        private final Map<String, Map<Object, Object>> store;

        private PersistMapKeyValueAdapter(final Map<String, Map<Object, Object>> store) {
            super(store);

            this.store = store;
        }

        @Nonnull
        @Override
        public Object put(final Object id, @Nonnull final Object item, final String keyspace) {
            Object stored = super.put(id, item, keyspace);

            persist();

            return stored;
        }

        @Override
        public Object delete(final Object id, @Nonnull final String keyspace) {
            Object deleted = super.delete(id, keyspace);

            persist();

            return deleted;
        }

        @Override
        public void deleteAllOf(@Nonnull final String keyspace) {
            super.deleteAllOf(keyspace);

            persist();
        }

        @Override
        public void clear() {
            persist();
        }

        @Override
        public void destroy() {
            persist();
        }

        private void persist() {

            log.debug("Persisting store {} to {}", store, dbPath);

            try {

                FileOutputStream fos = new FileOutputStream(dbPath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(store);
                oos.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}