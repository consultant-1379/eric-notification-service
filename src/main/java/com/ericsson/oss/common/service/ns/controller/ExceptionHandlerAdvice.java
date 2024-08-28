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

package com.ericsson.oss.common.service.ns.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import com.ericsson.oss.common.service.ns.exception.IncorrectHttpHeaderException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.ericsson.oss.common.service.ns.controller.subscription.RequestContext;
import com.ericsson.oss.common.service.ns.exception.DuplicateSubscriptionException;
import com.ericsson.oss.common.service.ns.exception.MissingArgumentException;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.orchestration.so.common.error.message.factory.builders.ErrorMessageBuilder;
import com.ericsson.oss.orchestration.so.common.error.message.factory.message.ErrorMessage;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages exceptions thrown in the scope of REST calls execution.
 * Fills and returns the relevant ErrorMessage as appropriate.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionHandlerAdvice extends ExceptionHandlerExceptionResolver {

    private ErrorMessageBuilder errorMessageBuilder;

    /**
     * The PerformanceMetrics bean injected by the framework (through the requiredArgsConstructor).
     */
    private final PerformanceMetrics metrics;
    /**
     * The Springboot framework context, allowing to retrieve some needed beans.
     */
    private final RequestContext requestContext;

    private static final String DELETE = "delete";
    private static final String GET = "get";
    private static final String CREATE = "create";
    
    /**
     * Initialize the ErrorMessageBuilder instance, used to create the error messages.
     */
    @PostConstruct
    void init() {  
      errorMessageBuilder = new ErrorMessageBuilder();
    }

    /**
     * Create a new ErrorMessage from the error code and the error data. Must be synchronized as the ErrorMessageBuilder class
     * is not thread safe.
     * @param code The error code as in error.properties file
     * @param errorData The list of strings representing the error parameters used to fill the error message
     * @return The ErrorMessage structure to be returned in REST response
     */
    private synchronized ErrorMessage buildErrorMessage(String code, List<String> errorData) {
      return errorMessageBuilder.errorCode(code).errorData(errorData).build();       
    }

    /**
     * Manages the NoSuchElementException, returning a NOT_FOUND Http Status and an ErrorMessage structure using an error code
     * depending on the request context (GET or DELETE).
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class)
    public ErrorMessage handleNoSuchElementException(NoSuchElementException ex) {
        log.error(ex.getLocalizedMessage(), ex);
        metrics.getFailDeleteSubscrCounter().increment();      
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getLocalizedMessage());
        if (requestContext.getOperation() == null) {
          return buildErrorMessage("ENS-J-06", errorData);          
        } else {
          switch (requestContext.getOperation()) {
            case DELETE:
              return buildErrorMessage("ENS-J-09", errorData);
            case GET:
            default:
              return buildErrorMessage("ENS-J-06", errorData);
          }
        }
    }
    
    /**
     * Manages the MissingArgumentException, returning a BAD_REQUEST Http Status and an ErrorMessage structure using the error code
     * ENS-B-00 (The scope is always a subscription creation request).
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MissingArgumentException.class)
    public ErrorMessage handleMissingArgumentException(Exception ex) {
        log.error(ex.getLocalizedMessage(), ex);
        metrics.getFailCreateSubscrCounter().increment();
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getMessage());
        return buildErrorMessage("ENS-B-00", errorData);
    }

    /**
     * Manages the IncorrectHttpHeaderException, returning a BAD_REQUEST Http Status and an ErrorMessage structure using the error code
     * ENS-B-00 (The scope is always a subscription creation request).
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(IncorrectHttpHeaderException.class)
    public ErrorMessage handleMissingHeaderException(Exception ex) {
        log.error(ex.getLocalizedMessage(), ex);
        metrics.getFailCreateSubscrCounter().increment();
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getMessage());
        return buildErrorMessage("ENS-B-02", errorData);
    }

    /**
     * Manages the MethodArgumentNotValidException, TypeMismatchException and IllegalArgumentexception returning a BAD_REQUEST 
     * Http Status and an ErrorMessage structure using an error code depending on the collected error details.
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, TypeMismatchException.class, IllegalArgumentException.class})
    public ErrorMessage handleMethodArgumentNotValidException(Exception ex) {
        log.error(ex.getLocalizedMessage(), ex);
        metrics.getFailCreateSubscrCounter().increment();
        var code = "ENS-B-12";
        String message;
        if (ex instanceof MethodArgumentNotValidException) {
            code = "ENS-B-01";
            var result = ((MethodArgumentNotValidException)ex).getBindingResult();
            message = result.getFieldErrors().stream()
                    .map(fe -> "Field " + fe.getField() + " " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            if (message.contains("must not be null")) {
              code = "ENS-B-00";
            }
        } else {
            message = ex.getLocalizedMessage();
        }

        List<String> errorData = new ArrayList<>();
        errorData.add(message);
        if (requestContext.getOperation() == null) {
            return buildErrorMessage(code, errorData);
        } else {
          return buildErrorMessage("ENS-C-02", errorData);            
        }
    }

    /**
     * Manages the DuplicateSubscriptionException, returning a CONFLICE Http Status and an ErrorMessage structure with the 
     * error code "ENS-K-03".
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(CONFLICT)
    @ExceptionHandler(DuplicateSubscriptionException.class)
    public ErrorMessage handleDuplicateSubscriptionException(DuplicateSubscriptionException ex) {
        log.error(ex.getLocalizedMessage(), ex);
        metrics.getFailCreateSubscrCounter().increment();
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getDuplicate().getId().toString());
        return buildErrorMessage("ENS-K-03", errorData);
    }
    
    /**
     * Manages the DataAccessException, SQLTransientConnectionException returning a INTERNAL_SERVER_ERROR Http Status and an 
     * ErrorMessage structure using an error code depending on the request context (POST, GET or DELETE).
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler({DataAccessException.class, SQLTransientConnectionException.class})
    public ErrorMessage handleDbExceptions(Exception ex) {
        log.error(ex.getLocalizedMessage(), ex);
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getMessage());
        if (requestContext.getOperation() == null) {
          return buildErrorMessage("ENS-E-04", errorData);                      
        } else {
          switch (requestContext.getOperation()) {
            case GET:
              return buildErrorMessage("ENS-E-07", errorData);            
            case DELETE:
              return buildErrorMessage("ENS-E-10", errorData);
            case CREATE:
            default:
              return buildErrorMessage("ENS-E-04", errorData);                        
          }
        }
    }
    
    /**
     * Manages the exceptions returned by Jackson during the parsing of the request, returning a BAD_REQUEST Http Status and a
     * generic ErrorMessage structure with code "ENS-B-12". No request scope is defined here because the exceptions get thrown
     * by Jackson BEFORE an access is provided to the REST callback code.
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({JsonMappingException.class, UnrecognizedPropertyException.class, JsonParseException.class, 
      NoSuchMethodError.class, InvalidFormatException.class, HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ErrorMessage handleJacksonErrors(Exception ex) { 
      List<String> errorData = new ArrayList<>();
      errorData.add(ex.getMessage());
      return buildErrorMessage("ENS-B-12", errorData);            
    }
  
    /**
     * Manages all other exceptions not mapped in the other handlers, returning an INTERNAL_SERVER_ERROR Http Status and an 
     * ErrorMessage structure using an error code depending on the request context (POST, GET or DELETE).
     * @param ex The exception
     * @return The ErrorMessage structure to be returned in REST response
     */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    ErrorMessage handleOtherServerExceptions(Exception ex) {
        log.error(ex.getLocalizedMessage(), ex);
        List<String> errorData = new ArrayList<>();
        errorData.add(ex.getMessage());
        if (requestContext.getOperation() == null) {
            return buildErrorMessage("ENS-Z-05", errorData);                      
        } else {
          switch (requestContext.getOperation()) {
            case GET:
              return buildErrorMessage("ENS-Z-08", errorData);            
            case DELETE:
              return buildErrorMessage("ENS-Z-11", errorData);
            default:
            case CREATE:
              return buildErrorMessage("ENS-Z-05", errorData);                        
          }
        }
    }
}
