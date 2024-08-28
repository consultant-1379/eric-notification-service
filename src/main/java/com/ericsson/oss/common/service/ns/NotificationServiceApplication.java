/*******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.common.service.ns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Application, the starting point of the application.
 */
@SpringBootApplication
@EnableAutoConfiguration
public class NotificationServiceApplication {

  /**
   * Main entry point of the application.
   *
   * @param args Command line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(NotificationServiceApplication.class, args);
  }

}
