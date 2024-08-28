/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.common.service.ns.security;

import com.google.common.annotations.VisibleForTesting;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.BootstrapPropertySource;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newLinkedList;

public abstract class ChangeDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDetector.class);
    private final AtomicBoolean resubscribePending = new AtomicBoolean(false);
    private final AtomicInteger currentReconnectAttempt = new AtomicInteger(0);
    private final ScheduledExecutorService executor;
    private final long initialTimeout;
    private final int maxTimeoutPower;

    protected AbstractEnvironment environment;
    protected KubernetesClient kubernetesClient;
    protected ConfigReloadProperties properties;

    protected ChangeDetector(AbstractEnvironment environment, KubernetesClient kubernetesClient, ConfigReloadProperties properties) {
        this(environment, kubernetesClient, properties, 8L, 4);
    }

    protected ChangeDetector(AbstractEnvironment environment,
                          KubernetesClient kubernetesClient,
                          ConfigReloadProperties properties,
                          long initialTimeout,
                          int maxTimeoutPower) {

        this.environment = environment;
        this.kubernetesClient = kubernetesClient;
        this.properties = properties;
        this.initialTimeout = initialTimeout;
        this.maxTimeoutPower = maxTimeoutPower;
        this.executor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "Executor for re-subscribe " + System.identityHashCode(ChangeDetector.this))
        );
    }

    @PostConstruct
    public abstract void subscribe();

    @PreDestroy
    public final void shutdown() {
        LOGGER.info("Shutdown in {}", this);
        if (!executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.warn("Executor didn't terminate in time after shutdown, killing it in: {}", this);
                    executor.shutdownNow();
                 }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
         }
    }

    @VisibleForTesting
    protected final void scheduleResubscribe() {
        LOGGER.info("Submitting re-subscribe task to the executor");
        executor.submit(() -> {
            if (!resubscribePending.compareAndSet(false, true)) {
                LOGGER.info("Re-subscribe already scheduled");
                return;
            }
            LOGGER.info("Scheduling task for re-subscribe attempt");
            executor.schedule(() -> {
                try {
                    LOGGER.info("Re-subscribe attempt started");
                    subscribe();
                    resubscribePending.set(false);
                } catch (Exception e) {
                    LOGGER.error("Unexpected error in re-subscribe attempt");
                    shutdown();
                }
            }, nextResubscribeInterval(), TimeUnit.SECONDS);
        });
    }

    private long nextResubscribeInterval() {
        int powerOfTwo = currentReconnectAttempt.getAndIncrement();
        if (powerOfTwo > maxTimeoutPower) {
            powerOfTwo = maxTimeoutPower;
        }
        long resubscribeInterval = initialTimeout << powerOfTwo;
        LOGGER.info("Current reconnect backoff is {} seconds (T{})", resubscribeInterval, powerOfTwo);
        return resubscribeInterval;
    }

    protected final <S extends PropertySource<?>> List<S> findPropertySources(Class<S> sourceClass) {
        List<S> managedSources = new LinkedList<>();
        LinkedList<PropertySource<?>> sources = newLinkedList(this.environment.getPropertySources());
        while (!sources.isEmpty()) {
            PropertySource<?> source = sources.pop();
            if (source instanceof CompositePropertySource) {
                CompositePropertySource comp = (CompositePropertySource) source;
                sources.addAll(comp.getPropertySources());
            } else if (sourceClass.isInstance(source)) {
                managedSources.add(sourceClass.cast(source));
            } else if (source instanceof BootstrapPropertySource) {
                PropertySource<?> propertySource = ((BootstrapPropertySource<?>) source).getDelegate();
                if (sourceClass.isInstance(propertySource)) {
                    sources.add(propertySource);
                }
            }
        }

        return managedSources;
    }

    abstract class Subscriber<E> implements Watcher<E> {

        private final String name;

        Subscriber(String detectorName) {
            this.name = detectorName;
        }

        @Override
        public final void onClose(final WatcherException cause) {
            LOGGER.warn("{} is DISABLED", name);
            if (!executor.isShutdown()) {
                scheduleResubscribe();
            }
        }
    }
}
