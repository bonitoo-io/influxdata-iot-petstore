package io.bonitoo.influxdemo.services.data;

import java.util.List;
import javax.annotation.Nonnull;

import io.bonitoo.influxdemo.domain.DeviceInfo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Jakub Bednar (bednar@github) (22/05/2019 09:51)
 */
@Repository
public interface DeviceInfoRepository extends CrudRepository<DeviceInfo, String> {

    @Nonnull
    List<DeviceInfo> findAllByOrderByCreatedAt();
}