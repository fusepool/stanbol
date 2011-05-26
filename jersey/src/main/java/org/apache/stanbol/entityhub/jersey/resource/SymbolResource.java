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
package org.apache.stanbol.entityhub.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * RESTful interface for The {@link Entityhub}. To access referenced sites directly see
 * {@link ReferencedSiteRootResource}.
 */
@Path("/entityhub/symbol")
public class SymbolResource extends BaseStanbolResource {
    /**
     * used to extract the mediaType for the response based on the Accept
     * header of the request.
     */
    private static Collection<String> ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML;
    static {
        ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML = new HashSet<String>(
                JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML.add(TEXT_HTML);
    }
    /**
     * The default search field for /find queries is the entityhub-maodel:label
     */
    private static final String DEFAULT_FIND_FIELD = RdfResourceEnum.label.getUri();
    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The default result fields for /find queries is the entityhub-maodel:label and the
     * entityhub-maodel:description.
     */
    private static final Collection<? extends String> DEFAULT_FIND_SELECTED_FIELDS = Arrays.asList(
        RdfResourceEnum.label.getUri(), RdfResourceEnum.description.getUri());
    
    private ServletContext context;
    
    // bind the job manager by looking it up from the servlet request context
    public SymbolResource(@Context ServletContext context) {
        super();
        this.context = context;
    }
    
    
    @GET
    @Path("/")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response getSymbol(@QueryParam("id") String symbolId, @Context HttpHeaders headers) throws WebApplicationException {
        log.info("/symbol/lookup Request");
        log.info("  > id: " + symbolId);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && symbolId == null){
            //return HTML docu
            return Response.ok(new Viewable("index", this), TEXT_HTML).build();
        }
        if (symbolId == null || symbolId.isEmpty()) {
            // TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Entity entity;
        try {
            entity = entityhub.getEntity(symbolId);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (entity == null) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            return Response.ok(entity, acceptedMediaType).build();
        }
    }
        
    @GET
    @Path("/lookup")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response lookupSymbol(@QueryParam("id") String reference,
                                 @QueryParam("create") boolean create,
                                 @Context HttpHeaders headers) throws WebApplicationException {
        log.info("/symbol/lookup Request");
        log.info("  > id: " + reference);
        log.info("  > create   : " + create);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && reference == null){
            //return docu
            return Response.ok(new Viewable("lookup", this), TEXT_HTML).build();
        } else {
            if (reference == null || reference.isEmpty()) {
                // TODO: how to parse an error message
                throw new WebApplicationException(BAD_REQUEST);
            }
            Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
            Entity entity;
            try {
                entity = entityhub.lookupLocalEntity(reference, create);
            } catch (EntityhubException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
            if (entity == null) {
                return Response.status(Status.NOT_FOUND).entity("No symbol found for '" + reference + "'.")
                        .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            } else {
                return Response.ok(entity, acceptedMediaType).build();
            }
        }
    }
    
    @GET
    @Path("/find")
    @Produces(TEXT_HTML)
    public Response getFindDocu(){
        return Response.ok(new Viewable("find", this), TEXT_HTML).build();
    }
    
    @POST
    @Path("/find")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String field,
                               @FormParam(value = "lang") String language,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               // TODO: Jersey supports parsing multiple values in Collections.
                               // Use this feature here instead of using this hand crafted
                               // solution!
                               @FormParam(value = "select") String select,
                               @Context HttpHeaders headers) {
        log.debug("symbol/find Request");
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            MediaType.APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && name == null){
            //return HTML docu
            return getFindDocu();
        } else {
            if (field == null || field.trim().isEmpty()) {
                field = DEFAULT_FIND_FIELD;
            } else {
                field = field.trim();
            }
            FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(name, field, language,
                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset);
            
            // For the Entityhub we support to select additional fields for results
            // of find requests. For the Sites and {site} endpoint this is currently
            // deactivated because of very bad performance with OPTIONAL graph patterns
            // in SPARQL queries.
            Collection<String> additionalSelectedFields = new ArrayList<String>();
            if (select == null || select.isEmpty()) {
                additionalSelectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
            } else {
                for (String selected : select.trim().split(" ")) {
                    if (selected != null && !selected.isEmpty()) {
                        additionalSelectedFields.add(selected);
                    }
                }
            }
            query.addSelectedFields(additionalSelectedFields);
            return executeQuery(query, acceptedMediaType);
        }
    }
    
    @DELETE
    @Path("/{id}")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response removeSymbol() {
        return null;
    }
    
    /**
     * Allows to parse any kind of {@link FieldQuery} in its JSON Representation.
     * <p>
     * TODO: as soon as the entityhub supports multiple query types this need to be refactored. The idea is
     * that this dynamically detects query types and than redirects them to the referenced site
     * implementation.
     * 
     * @param query The field query in JSON format
     * @param headers the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    @Consumes( {APPLICATION_FORM_URLENCODED + ";qs=1.0", MULTIPART_FORM_DATA + ";qs=0.9"})
    public Response queryEntities(@FormParam("query") FieldQuery query,
                                  @Context HttpHeaders headers) {
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,
            JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES,
            MediaType.APPLICATION_JSON_TYPE);
        return executeQuery(query, acceptedMediaType);
    }
    
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)}
     * or created based {@link #findEntity(String, String, String, String, HttpHeaders)
     * @param query The query to execute
     * @param headers The headers used to determine the media types
     * @return the response (results of error)
     */
    private Response executeQuery(FieldQuery query, MediaType acceptedMediaType) throws WebApplicationException {
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        try {
            return Response.ok(entityhub.find(query), acceptedMediaType).build();
        } catch (EntityhubException e) {
            log.error("Exception while performing the FieldQuery on the EntityHub", e);
            log.error("Query:\n" + query);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
