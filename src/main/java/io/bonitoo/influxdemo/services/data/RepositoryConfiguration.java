package io.bonitoo.influxdemo.services.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.bonitoo.influxdemo.domain.DeviceInfo;
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

    public RepositoryConfiguration(@Value("${petstore.db:./db}") final String dbPath) {
        this.dbPath = dbPath;
    }

    private Map<String, Type> types = new HashMap<String, Type>() {{
        put("devices", new TypeToken<HashMap<String, HashMap<String, DeviceInfo>>>() {}.getType());
    }};

    @Bean
    public KeyValueOperations persistKeyValue() {
        return new KeyValueTemplate(keyValueAdapter());
    }

    @Bean
    public KeyValueAdapter keyValueAdapter() {

        Map<String, Map<Object, Object>> store = CollectionFactory.createMap(ConcurrentHashMap.class, 100);

        //
        // Populate store from file
        //
        if (new File(dbPath).exists()) {
            for (final String typeKey : types.keySet()) {
                Type type = types.get(typeKey);

                String file = getTypeFile(typeKey);
                log.info("Populate store from {}", file);

                try (JsonReader reader = new JsonReader(new FileReader(file))) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Map data = gson.fromJson(reader, type);
                    store.putAll(data);

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return new PersistMapKeyValueAdapter(store);
    }

    private String getTypeFile(final String typeKey) {
        return dbPath + "-" + typeKey + ".json";
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

            for (final String typeKey : types.keySet()) {
                Type type = types.get(typeKey);

                log.debug("Persisting store {} to {}", store, getTypeFile(typeKey));

                try (FileWriter writer = new FileWriter(getTypeFile(typeKey))) {
                    Gson gson = new GsonBuilder().create();
                    gson.toJson(store, type, writer);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

}