/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package com.acme.multinode.grid.standby;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.infinispan.Cache;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.marshall.Marshaller;
import org.infinispan.server.core.CacheValue;
import org.infinispan.util.ByteArrayKey;

/**
 */
@SuppressWarnings({"rawtypes", "unchecked"}) 
public class StandbyTestServlet extends HttpServlet
{
   private static final long serialVersionUID = 1L;

   @Inject 
   private Cache<String, Integer> cache;

   @Inject 
   private Marshaller marshaller; 
   
   @Override
   public void init() throws ServletException
   {
      cache.getVersion();
      marshaller.isMarshallable(new Object());
   }
   
   @Override
   protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String key = "counter";
      try
      {
         Integer counter = cacheGet(cache, key);
         if (counter != null) {
            Integer newCounter = counter.intValue() + 1;
            cachePut(cache, key, newCounter);
            response.getWriter().append(newCounter.toString());
         }
      }
      catch (Exception e)
      {
         throw new ServletException(e);
      }
   }
   
   private Integer cacheGet(Cache cache, String key) throws Exception {
      ByteArrayKey cacheKey = marshallKey(key);
      CacheValue cacheValue = (CacheValue) cache.get(cacheKey);
      byte[] value = cacheValue.data();
      try {
         // RemoteCacheStore stores ImmortalCacheEntry, not just the value
         ImmortalCacheEntry entry = (ImmortalCacheEntry) marshaller.objectFromByteBuffer(value);
         return (Integer) entry.getValue();
      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      }
   }

   private void cachePut(Cache cache, String key, Integer value) throws Exception {
      ByteArrayKey cacheKey = marshallKey(key);
      CacheValue cacheValue = (CacheValue) cache.get(cacheKey);
      ImmortalCacheEntry entry = null;
      try {
         entry = (ImmortalCacheEntry) marshaller.objectFromByteBuffer(cacheValue.data());
      } catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      }

      entry.setValue(value);
      CacheValue newCacheValue = new CacheValue(marshallValue(entry), cacheValue.version() + 1);
      cache.put(cacheKey, newCacheValue);
   }

   private ByteArrayKey marshallKey(String key) throws Exception {
      byte[] keyBytes = marshaller.objectToByteBuffer(key);
      return new ByteArrayKey(keyBytes);
   }

   private byte[] marshallValue(ImmortalCacheEntry entry) throws Exception {
      return marshaller.objectToByteBuffer(entry);
   }
}
