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
package org.jboss.as.quickstarts.datagrid.helloremote.rest;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.as.quickstarts.datagrid.helloremote.domain.Memo;
import org.jboss.as.quickstarts.datagrid.helloremote.domain.Person;
import org.jboss.as.quickstarts.datagrid.helloremote.domain.PhoneNumber;
import org.jboss.as.quickstarts.datagrid.helloremote.domain.PhoneType;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A simple demo for remote query capabilities.
 *
 * @author Adrian Nistor
 */
@RequestScoped
@Path("/addressBook")
public class AddressBookManager {

   //@Inject
   private Logger log = Logger.getLogger(AddressBookManager.class.toString());

   @Inject
   RemoteCacheManager remoteCacheManager ;


   private static final String CACHE_NAME = "addressbook_indexed";
   private static final String PROPERTIES_FILE = "jdg.properties";

   public AddressBookManager() throws Exception {
      log.info(("\n\nUsing cache ["+jdgProperty(CACHE_NAME)+"]\n\n"));
   }


   @GET
   @Path("/search")
   @Produces({ "application/json" })
   public void queryPersonByName(@QueryParam("name") String name) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.from(Person.class)
            .having("name").like(name)
            .build();

      List<Person> results = query.list();
      log.info("Found matches: "+results.size());
      for (Person p : results) {
         log.info(p.toString());
      }
   }

   @GET
   @Path("/searchPhone")
   @Produces({ "application/json" })
   public void queryPersonByPhone(@QueryParam("number") String phoneNumber) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.from(Person.class)
            .having("phone.number").eq(phoneNumber)
            .build();
      List<Person> results = query.list();
      log.info("Found matches: "+results.size());
      for (Person p : results) {
         log.info(p.toString());
      }
   }

   @GET
   @Path("/searchIckle")
   @Produces({ "application/json" })
   public void runIckleQueryString(@QueryParam("Ickle") String queryString) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.create(queryString);

      List<Object> results = query.list();
      log.info("Found matches: "+results.size());
      for (Object o : results) {
         if (o instanceof Object[]) {
            System.out.println(">> " + Arrays.toString((Object[]) o));
         } else {
            System.out.println(">> " + o);
         }
      }
   }

   @GET
   @Path("/addPerson")
   @Produces({ "application/json" })
   public void addPerson(@QueryParam("id")int id, @QueryParam("name") String name, @QueryParam("email")String email) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      Person person = new Person();
      person.setId(id);
      person.setName(name);
      person.setEmail(email);

      if (remoteCache.containsKey(person.getId())) {
         log.info("Person ["+((Person)remoteCache.get(person.getId())).toString()+"] will be updated");
         log.info("Updating person with id " + person.getId());
      }

      // put the Person in cache
      remoteCache.put(person.getId(), person);
   }

   @GET
   @Path("/removePerson")
   @Produces({ "application/json" })
   public void removePerson(@QueryParam("id")int id) {
      // remove from cache
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      Person prevValue = (Person) remoteCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(id);
      log.info("Removed: " + prevValue);
   }

   @GET
   @Path("/addPhone")
   @Produces({ "application/json" })
   public void addPhone(@QueryParam("id")int id,@QueryParam("number")String number,@QueryParam("type")String numberType) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      Person person = (Person) remoteCache.get(id);
      if (person == null) {
         log.info("Person not found");
      }
      log.info("Person ["+((Person)remoteCache.get(person.getId())).toString()+"] will be updated");

      PhoneType type = PhoneType.valueOf("HOME");
      if(numberType!=null) {
         for (PhoneType c : PhoneType.values()) {
            if (c.name().equals(numberType)) {
               type = PhoneType.valueOf(numberType.toUpperCase());
            }
         }
      }

      List<PhoneNumber> phones = person.getPhones();
      if (phones == null) {
         phones = new ArrayList<>();
      }
      PhoneNumber phoneNumber = new PhoneNumber();
      phoneNumber.setNumber(number);
      phoneNumber.setType(type);
      phones.add(phoneNumber);
      person.setPhones(phones);

      // update the Person in cache
      remoteCache.put(person.getId(), person);
   }

   @GET
   @Path("/removePhone")
   @Produces({ "application/json" })
   private void removePhone(@QueryParam("id")int id,@QueryParam("idNumber")int idx) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      Person person = (Person) remoteCache.get(id);
      if (person == null) {
         System.out.println("Person not found");
         return;
      }
      System.out.println("> " + person);

      if (person.getPhones() != null && !person.getPhones().isEmpty()) {
         if (idx < 0 || idx >= person.getPhones().size()) {
            log.info("Person "+person+" does not have that phone index");
         }else {
            person.getPhones().remove(idx);
            remoteCache.put(person.getId(), person);
         }
      } else {
         log.info("The person does not have any phones");
      }
   }

   @GET
   @Path("/list")
   @Produces({ "application/json" })
   public void printAllEntries() {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      for (Object key : remoteCache.keySet()) {
         log.info("key="+key+", value=["+remoteCache.get(key)+"]\n");
      }
   }

   @GET
   @Path("/clear")
   @Produces({ "application/json" })
   public void clearCache() {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      remoteCache.clear();
      log.info("Cache cleared.");
   }

   //TODO TBI
   public void addMemo(@QueryParam("id")int id,@QueryParam("text")String text) {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));
      Memo.Priority priority = Memo.Priority.valueOf("".toUpperCase());

      int authorId = -1;
      Person author = (Person) remoteCache.get(authorId);
      if (author == null) {
         System.out.println("Person not found");
         return;
      }
      System.out.println("> " + author);

      Memo memo = new Memo();
      memo.setId(id);
      memo.setText(text);
      memo.setPriority(priority);
      memo.setAuthor(author);

      // put the Memo in cache
      remoteCache.put(memo.getId(), memo);
   }

   //TODO TBI
   private void queryMemoByAuthor() {
      RemoteCache<Integer, Object> remoteCache = remoteCacheManager.getCache(jdgProperty(CACHE_NAME));

      String namePattern = "";

      QueryFactory qf = Search.getQueryFactory(remoteCache);
      Query query = qf.from(Memo.class)
            .having("author.name").like(namePattern)
            .build();

      List<Memo> results = query.list();
      System.out.printf("Found %d matches:\n", results.size());
      for (Memo p : results) {
         System.out.println(">> " + p);
      }
   }



   private String jdgProperty(String name) {
      return name;
      /*
      InputStream res = null;
      try {
         res = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
         Properties props = new Properties();
         props.load(res);
         return props.getProperty(name);
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      } finally {
         if (res != null) {
            try {
               res.close();
            } catch (IOException e) {
               // ignore
            }
         }
      }
      */
   }


}
