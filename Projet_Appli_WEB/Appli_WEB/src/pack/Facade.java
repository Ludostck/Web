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
	@Path("/login")
	@Produces({ "application/json" })
    public String traiterConnexion(User user) {

	    String message = "{\"message\": \"" + user.getPseudo() + " " + user.getPassword() + "\"}";
	    return message; 
    }
}