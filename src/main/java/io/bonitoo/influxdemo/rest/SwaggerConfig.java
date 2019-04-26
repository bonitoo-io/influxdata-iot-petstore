package io.bonitoo.influxdemo.rest;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static springfox.documentation.builders.PathSelectors.regex;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket registerDeviceApi() {

        return new Docket(DocumentationType.SWAGGER_2)
            .select().apis(RequestHandlerSelectors.basePackage("io.bonitoo.influxdemo.rest"))
            .paths(regex("/api.*"))
            .build().apiInfo(metaData());
    }

    private ApiInfo metaData() {
        ApiInfo apiInfo = new ApiInfo(
            "Spring Boot REST API",
            "Spring Boot REST API for IoT Petstore",
            "1.0",
            "Terms of service",
            new Contact("Robert Hajek", "https://bonitoo.io", "robert.hajek@bonitoo.io"),
            "Apache License Version 2.0",
            "https://www.apache.org/licenses/LICENSE-2.0", new ArrayList<VendorExtension>());

        return apiInfo;
    }


}
