package pack;

import java.util.Collection;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/")
public class Facade {

	@PersistenceContext
	EntityManager em;
	
	@POST
	@Path("/test")
	@Produces({ "application/json" })
    public String traiterConnexion(Person person) {

	    String message = "{\"message\": \"" + person.getFirstName() + " " + person.getLastName() + "\"}";
	    return message; 
    }
}