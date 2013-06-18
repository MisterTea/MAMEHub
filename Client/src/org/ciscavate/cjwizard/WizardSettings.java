/**
 * Copyright 2008  Eugene Creswick
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ciscavate.cjwizard;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * @author rcreswick
 *
 */
public class WizardSettings implements Map<String, Object> {
   
   private final Stack<IdMapTuple> _pageStack =
      new Stack<IdMapTuple>();

   private final Map<String, IdMapTuple> _oldPageMaps =
      new HashMap<String, IdMapTuple>();
   
   
   public WizardSettings(){
      // start with an empty new page:
      newPage("");
   }
   
   /**
    * Gets the set of keys on this WizardSettings object.
    * 
    * @return A set of all the keys currently active.
    */
   public Set<String> keySet(){
      Set<String> keys = new HashSet<String>();
      
      for (IdMapTuple tuple : _pageStack){
         keys.addAll(tuple.map.keySet());
      }
      return keys;
   }
   
   public boolean containsKey(String key){
      return keySet().contains(key);
   }
      
   /**
    * 
    */
   public void rollBack() {
      _pageStack.pop();
   }

   /**
    * @param id
    */
   public void newPage(String id) {
      if (0 != _pageStack.size()){
         // if there was a previous map, then
         // store the previous page map by id:
         _oldPageMaps.put(current().id, current());
      }
      
      // If we've seen this ID before, use it again:
      IdMapTuple curTuple;
      if (_oldPageMaps.containsKey(id)){
         curTuple = _oldPageMaps.get(id);
      }else{
         curTuple = new IdMapTuple(id, new HashMap<String, Object>());
      }
      
      // push the new map:
      _pageStack.push(curTuple);
   }

   public Object put(String key, Object value) {
      return current().map.put(key, value);
   }
   
   /**
    * Gets the value associated with the key.
    * 
    * @param key
    * @return
    */
   public Object get(String key){
      Object value = null;
      
      for (int i=_pageStack.size()-1; null == value && i >= 0; i--){
         IdMapTuple tuple = _pageStack.get(i);
         value = tuple.map.get(key);
      }
      return value;
   }

   /* (non-Javadoc)
    * @see java.util.Map#get(java.lang.Object)
    */
   @Override
   public Object get(Object key) {
      if (key instanceof String) {
         return get((String)key);
      }
      return null;
   }
   
   private IdMapTuple current(){
      if (0 == _pageStack.size())
         return null;
      
      return _pageStack.peek();
   }

   public String toString(){
      StringBuilder str = new StringBuilder("WizardSettings: ");
      
      for (String key : keySet()){
         str.append("["+key+"="+get(key)+"] ");
      }
      return str.toString();
   }

   /* (non-Javadoc)
    * @see java.util.Map#clear()
    */
   @Override
   public void clear() {
      _oldPageMaps.clear();
      _pageStack.clear();
      // initialize the first page again:
      newPage("");
   }

   /* (non-Javadoc)
    * @see java.util.Map#containsKey(java.lang.Object)
    */
   @Override
   public boolean containsKey(Object key) {
      // TODO this is innefficient.
      return keySet().contains(key);
   }

   /* (non-Javadoc)
    * @see java.util.Map#containsValue(java.lang.Object)
    */
   @Override
   public boolean containsValue(Object value) {
      boolean containsVal = false;
      
      for (int i=_pageStack.size()-1; !containsVal && i >= 0; i--){
         IdMapTuple tuple = _pageStack.get(i);
         containsVal = tuple.map.containsValue(value);
      }
      return containsVal;
   }

   /**
    * Not supported.
    */
   @Override
   public Set<java.util.Map.Entry<String, Object>> entrySet() {
      // TODO auto-generated method stub
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.Map#isEmpty()
    */
   @Override
   public boolean isEmpty() {
      return keySet().size() == 0;
   }


   /**
    * Not supported.
    */
   @Override
   public void putAll(Map<? extends String, ? extends Object> m) {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException();
   }

   /**
    * Not supported.
    */
   @Override
   public Object remove(Object key) {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.util.Map#size()
    */
   @Override
   public int size() {
      return keySet().size();
   }

   /**
    * Not supported.
    */
   @Override
   public Collection<Object> values() {
      // TODO Auto-generated method stub
      throw new UnsupportedOperationException();
   }

   private class IdMapTuple{
      public final String id;
      public final Map<String, Object> map;
      
      public IdMapTuple(String id, Map<String, Object> map) {
         this.id = id;
         this.map = map;
      }
   }
}
