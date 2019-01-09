/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.datagrid.helloworld;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * Creates a DefaultCacheManager which is configured programmatically. Infinispan's libraries need to be bundled with the
 * application.
 * 
 * @author Burr Sutter
 * @author Martin Gencur
 * 
 */
@ApplicationScoped
public class MyCacheManagerProvider {

    private static final long ENTRY_LIFESPAN = 60 * 1000; // 60 seconds

    @Inject
    private Logger log;

    private RemoteCacheManager manager;


    public RemoteCacheManager getCacheManager() {
        if (manager == null) {
            log.info("\n\n RemoteCacheManager does not exist - constructing a new one\n\n");
            ConfigurationBuilder builder = new  ConfigurationBuilder();
            builder.addServer().host(System.getenv("HOTROD_SERVICE")).port(Integer.parseInt(System.getenv("HOTROD_SERVICE_PORT")));
            manager = new RemoteCacheManager(builder.build());

        }
        return manager;
    }

    @PreDestroy
    public void cleanUp() {
        manager.stop();
        manager = null;
    }

}
