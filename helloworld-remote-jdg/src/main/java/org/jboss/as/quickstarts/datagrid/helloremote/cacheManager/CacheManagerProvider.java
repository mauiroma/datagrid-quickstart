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
package org.jboss.as.quickstarts.datagrid.helloremote.cacheManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.as.quickstarts.datagrid.helloremote.domain.Memo;
import org.jboss.as.quickstarts.datagrid.helloremote.marshallers.PersonMarshaller;
import org.jboss.as.quickstarts.datagrid.helloremote.marshallers.PhoneNumberMarshaller;
import org.jboss.as.quickstarts.datagrid.helloremote.marshallers.PhoneTypeMarshaller;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.util.logging.Logger;

import static org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller.getSerializationContext;

/**
 * Creates a DefaultCacheManager which is configured programmatically. Infinispan's libraries need to be bundled with the
 * application.
 * 
 * @author Burr Sutter
 * @author Martin Gencur
 * 
 */
@ApplicationScoped
public class CacheManagerProvider {

    private static final String PROTOBUF_DEFINITION_RESOURCE = "/quickstart/addressbook.proto";

    @Inject
    private Logger log;

    private RemoteCacheManager manager;

    public RemoteCacheManager getCacheManager() {
        if (manager == null) {
            log.info("\n\n RemoteCacheManager does not exist - constructing a new one\n\n");
            ConfigurationBuilder builder = new  ConfigurationBuilder();
            builder.addServer()
//                    .host("localhost")
//                    .port(11222)
                    .host(System.getenv("HOTROD_SERVICE"))
                    .port(Integer.parseInt(System.getenv("HOTROD_SERVICE_PORT")))
                    .marshaller(new ProtoStreamMarshaller()); // The Protobuf based marshaller is required for query capabilities
            manager = new RemoteCacheManager(builder.build());
            try {
                registerSchemasAndMarshallers(manager);
            } catch (IOException e) {
                log.info("\n\nException "+e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return manager;
    }

    /**
     * Register the Protobuf schemas and marshallers with the client and then register the schemas with the server too.
     */
    private void registerSchemasAndMarshallers(RemoteCacheManager cacheManager) throws IOException {
        // Register entity marshallers on the client side ProtoStreamMarshaller instance associated with the remote cache manager.
        SerializationContext ctx = getSerializationContext(cacheManager);
        ctx.registerProtoFiles(FileDescriptorSource.fromResources(PROTOBUF_DEFINITION_RESOURCE));
        ctx.registerMarshaller(new PersonMarshaller());
        ctx.registerMarshaller(new PhoneNumberMarshaller());
        ctx.registerMarshaller(new PhoneTypeMarshaller());

        // generate the 'memo.proto' schema file based on the annotations on Memo class and register it with the SerializationContext of the client
        ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
        String memoSchemaFile = protoSchemaBuilder
                .fileName("memo.proto")
                .packageName("quickstart")
                .addClass(Memo.class)
                .build(ctx);

        // register the schemas with the server too
        RemoteCache<String, String> metadataCache = cacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put(PROTOBUF_DEFINITION_RESOURCE, readResource(PROTOBUF_DEFINITION_RESOURCE));
        metadataCache.put("memo.proto", memoSchemaFile);
        String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors != null) {
            throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
        }
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            Reader reader = new InputStreamReader(is, "UTF-8");
            StringWriter writer = new StringWriter();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                writer.write(buf, 0, len);
            }
            return writer.toString();
        }
    }

/*
    private void testLucene()throws Exception{
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager();
        Cache metadataCache = defaultCacheManager.getCache("metadataCache");
        Cache dataCache = defaultCacheManager.getCache("dataCache");
        Cache lockCache = defaultCacheManager.getCache("lockCache");

        // Create the directory
        Directory directory = DirectoryBuilder.newDirectoryInstance(metadataCache, dataCache, lockCache, "LUCENEIDX").create();

// Use the directory in Lucene
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

// Index a single document
        Document doc = new Document();
        doc.add(new StringField("field", "value", Field.Store.NO));
        indexWriter.addDocument(doc);
        indexWriter.close();

// Querying the inserted document
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(directoryReader);
        TermQuery query = new TermQuery(new Term("field", "value"));
        TopDocs topDocs = searcher.search(query, 10);
        System.out.println(topDocs.totalHits);
    }
*/
    @PreDestroy
    public void cleanUp() {
        manager.stop();
        manager = null;
    }

}
