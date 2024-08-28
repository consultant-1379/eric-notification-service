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
package com.ericsson.oss.common.service.ns.repository;

import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import java.util.List;

/**
 * Interface to work with current_dispatch table.
 */
public interface DispatchRepository {

  /**
   * Save a dispatch record.
   *
   * @param dispatchRecord The record to be saved
   */
  void save(Dispatch dispatchRecord);

  /**
   * Save a set of dispatch records in transaction.
   * @param dispatchRecords The records to store in the DB table
   */
  void save(List<Dispatch> dispatchRecords);
  
  /**
   * Delete a dispatch record with the given id, address and hash value
   *
   * @param eventId The eventId of the record to be deleted
   * @param address The address of the record to be deleted
   * @param hash The hash of the record to be deleted
   * @return The number of rows deleted
   */
  int delete(String eventId, String address, Integer hash);

  /**
   * Retrieve the list of all dispatch records for a given event id.
   * @param eventId The event identifier
   * @return The list of the dispatch records for the given event id.
   */
  List<Dispatch> findAll(String eventId);

}
