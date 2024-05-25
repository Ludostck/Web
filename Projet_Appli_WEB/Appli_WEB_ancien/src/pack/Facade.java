package pack;

import java.util.Collection;
import java.util.List;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.persistence.TypedQuery;
import pack.Projet;
import pack.User;
import javax.transaction.Transactional;

@Singleton
@Path("/")
public class Facade {

	@PersistenceContext
	EntityManager em;
	
	@POST
	@Path("/login")
	@Produces({ "application/json" })
    public String traiterConnexion(User user) {
		em.persist(user);
	    String message = "{\"message\": \"" + user.getPseudo() + " " + user.getPassword() + "\"}";
	    return message; 
    }
	/*
	@GET
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects(@PathParam("userId") Long userId) {
        TypedQuery<Projet> query = em.createQuery("SELECT p FROM Projet p WHERE p.user.id = :userId", Projet.class);
        query.setParameter("userId", userId);
        List<Projet> projects = query.getResultList();  
        return Response.ok(projects).build();
    }
	
	
    @POST
    @Path("/user/{userId}/projects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createProject(@PathParam("userId") Long userId, Projet project) {
        User user = em.find(User.class, userId);
        if (user == null) {
            return Response.status(Status.NOT_FOUND).entity("User not found").build();
        }

        project.setOwner(user);
        em.persist(project);

        return Response.status(Status.CREATED).entity(project).build();
    }

    @PUT
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    public void updateScript(String contenu, long scriptid) {
        Fichier s = em.find(Fichier.class, scriptid);
        s.setContenu(contenu);
        em.persist(project);
    }
		*/
	
}
