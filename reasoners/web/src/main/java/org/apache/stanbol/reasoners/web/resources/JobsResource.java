package org.apache.stanbol.reasoners.web.resources;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceResult;
import org.apache.stanbol.reasoners.web.utils.ResponseTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Return the result of a reasoners background job
 * 
 * @author enridaga
 * 
 */
@Path("/reasoners/jobs")
public class JobsResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;
    private HttpHeaders headers;

    public JobsResource(@Context ServletContext servletContext,@Context HttpHeaders headers) {
        this.context = servletContext;
        this.headers = headers;
    }

    /**
     * To read a /reasoners job output.
     * 
     * @param id
     * @return
     */
    @GET
    @Path("/{jid}")
    public Response get(@PathParam("jid") String id) {
        log.info("Pinging job {}", id);

        // No id
        if(id == null || id.equals("")){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        JobManager m = getJobManager();

        // If the job exists
        if (m.hasJob(id)) {
            log.info("Found job with id {}", id);
            Future<?> f = m.ping(id);
            if(f.isDone() && (!f.isCancelled())){
                /**
                 * We return OK with the result
                 */
                Object o;
                try {
                    o = f.get();
                    if(o instanceof ReasoningServiceResult){
                        log.debug("Is a ReasoningServiceResult");
                        ReasoningServiceResult<?> result = (ReasoningServiceResult<?>) o;
                        return new ResponseTaskBuilder(uriInfo,context,headers).build(result);
                    }else{
                        log.error("Job {} does not belong to reasoners", id);
                        throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }
                } catch (InterruptedException e) {
                    log.error("Error: ",e);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                } catch (ExecutionException e) {
                    log.error("Error: ",e);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
                
            }else{
                /**
                 * We return 404 with additional info
                 */
                String jobService = new StringBuilder().append(getPublicBaseUri()).append("/jobs/").append(id).toString();
                StringBuilder b = new StringBuilder();
                b.append("Result not ready.\n");
                b.append("See: ").append(jobService);
                return Response.status(404).header("Content-Location", jobService).entity( b.toString() ).build();
            }
        } else {
            log.info("No job found with id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Gets the job manager
     * 
     * @return
     */
    private JobManager getJobManager() {
        log.debug("(getJobManager()) ");
        return (JobManager) ContextHelper.getServiceFromContext(JobManager.class, this.context);
    }
}