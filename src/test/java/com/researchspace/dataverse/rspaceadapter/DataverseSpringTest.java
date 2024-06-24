package com.researchspace.dataverse.rspaceadapter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Meta-annotation for setting up Spring-tests in this project.
 */
@TestPropertySource(locations = "classpath:/test.properties")
@ContextConfiguration(classes = { DataverseSpringConfig.class })
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface DataverseSpringTest {

}
