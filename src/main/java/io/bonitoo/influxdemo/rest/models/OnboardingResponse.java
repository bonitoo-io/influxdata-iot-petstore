package io.bonitoo.influxdemo.rest.models;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Response from registration service after the device is authorized.
 */

@ApiModel
public class OnboardingResponse {

    @ApiModelProperty(position = 1, required = true, value = "valid device number")
    private String deviceId;

    @ApiModelProperty(required = true, value = "InfluxDB 2.0 server url")
    private String url;

    @ApiModelProperty(required = true, value = "InfluxDB organization id")
    private String orgId;

    @ApiModelProperty(required = true, value = "InfluxDB authToken")
    private String authToken;

    /**
     * Target bucket
     */
    @ApiModelProperty(required = true, value = "Bucket used for storing measurements")
    private String bucket;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(final String orgId) {
        this.orgId = orgId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(final String authToken) {
        this.authToken = authToken;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(final String bucket) {
        this.bucket = bucket;
    }
}
