package pack;

import javax.ejb.Singleton;
import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

@Singleton
@Path("/")
public class Facade {

    @PersistenceContext
    EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(Facade.class.getName());

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response traiterConnexion(User user) {
        try {
            LOGGER.info("Attempting login for user: " + user.getPseudo());
            User foundUser = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo AND u.password = :password", User.class)
                               .setParameter("pseudo", user.getPseudo())
                               .setParameter("password", user.getPassword())
                               .getSingleResult();

            if (foundUser != null) {
                LOGGER.info("Login successful for user: " + foundUser.getPseudo());
                return Response.ok("{\"success\": true, \"message\": \"Connexion réussie\", \"pseudo\": \"" + foundUser.getPseudo() + "\"}").build();
            } else {
                LOGGER.warning("Login failed for user: " + user.getPseudo());
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"success\": false, \"message\": \"Identifiant ou mot de passe incorrect\"}").build();
            }
        } catch (NoResultException e) {
            LOGGER.warning("User not found for pseudo: " + user.getPseudo());
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"success\": false, \"message\": \"Identifiant ou mot de passe incorrect\"}").build();
        } catch (Exception e) {
            LOGGER.severe("Error during login for user: " + user.getPseudo() + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"success\": false, \"message\": \"Erreur lors de la connexion\"}").build();
        }
    }

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createUser(User user) {
        try {
            LOGGER.info("Creating user: " + user.getPseudo());
            Session newSession = new Session();
            newSession.setHeureDebut(new Date());
            newSession.setUser(user);

            user.setSession(newSession);

            em.persist(newSession);
            em.persist(user);

            em.flush();

            LOGGER.info("User created successfully: " + user.getPseudo());
            return Response.ok("{\"pseudo\": \"" + user.getPseudo() + "\"}").build();
        } catch (Exception e) {
            LOGGER.severe("Error creating user: " + user.getPseudo() + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to create user.\"}").build();
        }
    }

    @GET
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects(@QueryParam("pseudo") String pseudo) {
        LOGGER.info("Received request to get projects for pseudo: " + pseudo);
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Found user: " + user.getPseudo() + " with ID: " + user.getId());

            // Assurez-vous que la session est correctement liée à l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("No session found for user: " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }
            LOGGER.info("Found session for user: " + user.getPseudo() + " with session ID: " + session.getId());

            Collection<Projet> projects = session.getProjets();
            LOGGER.info("Number of projects found: " + projects.size());
            return Response.ok(projects).build();
        } catch (NoResultException e) {
            LOGGER.warning("User or session not found for pseudo: " + pseudo);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User or session not found.\"}")
                           .build();
        } catch (Exception e) {
            LOGGER.severe("Error loading projects for pseudo: " + pseudo + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to load projects.\"}")
                           .build();
        }
    }


    @POST
    @Path("/projects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addNewProject(@QueryParam("pseudo") String pseudo, Projet newProject) {
        try {
            LOGGER.info("Adding new project for pseudo: " + pseudo);
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Found user: " + user.getPseudo());

            Session session = em.createQuery("SELECT s FROM Session s WHERE s.user = :user", Session.class)
                                .setParameter("user", user)
                                .getSingleResult();
            LOGGER.info("Found session for user: " + user.getPseudo());

            newProject.setOwner(session);
            em.persist(newProject);
            session.getProjets().add(newProject);
            em.merge(session);

            LOGGER.info("Project added successfully for pseudo: " + pseudo);
            return Response.ok("{\"success\": true, \"message\": \"Projet ajouté avec succès\"}").build();
        } catch (NoResultException e) {
            LOGGER.warning("User or session not found for pseudo: " + pseudo);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"success\": false, \"message\": \"Utilisateur ou session non trouvés.\"}")
                           .build();
        } catch (Exception e) {
            LOGGER.severe("Error adding project for pseudo: " + pseudo + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"success\": false, \"message\": \"Erreur lors de l'ajout du projet\"}").build();
        }
    }
}
