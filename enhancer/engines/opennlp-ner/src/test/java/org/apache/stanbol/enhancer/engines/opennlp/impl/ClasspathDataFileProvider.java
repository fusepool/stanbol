/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DataFileProvider that looks in our class resources */
public class ClasspathDataFileProvider implements DataFileProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String RESOURCE_BASE_PATH = "org/apache/stanbol/defaultdata/opennlp/";
    
    private final String symbolicName;
    
    ClasspathDataFileProvider(String bundleSymbolicName) {
        symbolicName = bundleSymbolicName;
    }
    
    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) 
    throws IOException {
        //If the symbolic name is not null check that is equals to the symbolic
        //name used to create this classpath data file provider
        if(bundleSymbolicName != null && !symbolicName.equals(bundleSymbolicName)) {
            log.debug("Requested bundleSymbolicName {} does not match mine ({}), request ignored",
                    bundleSymbolicName, symbolicName);
            return null;
        }
        
        // load default OpenNLP models from classpath (embedded in the defaultdata bundle)
        final String resourcePath = RESOURCE_BASE_PATH + filename;
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        log.debug("Resource {} found: {}", (in == null ? "NOT" : ""), resourcePath);
        
        // Returning null is fine - if we don't have the data file, another
        // provider might supply it
        return in;
    }
}