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

package com.ericsson.oss.common.service.ns.controller.subscription;

import javax.annotation.ManagedBean;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.annotation.RequestScope;

/**
 * The context of a given REST request. It includes just the type of REST operation being performed.
 */
@ManagedBean
@RequestScope
@Getter
@Setter
public class RequestContext {
  /**
   * The type of operation being performend by the current REST request.
   */
  private String operation;
}  
