package pack;

import javax.ejb.Singleton;
import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    @Transactional
    public Response getUserProjects(@QueryParam("pseudo") String pseudo) {
        LOGGER.info("Received request to get projects for pseudo: " + pseudo);
        try {
            // Récupération de l'utilisateur par son pseudo
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Found user: " + user.getPseudo() + " with ID: " + user.getId());

            // Récupération de la session de l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("No session found for user: " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }
            LOGGER.info("Found session for user: " + user.getPseudo() + " with session ID: " + session.getId());

            // Forcer l'initialisation des projets et fichiers
            session.getProjets().forEach(projet -> {
                LOGGER.info("Initializing project: " + projet.getTitle() + " with ID: " + projet.getId());
                if (projet.getFichiers() == null) {
                    LOGGER.info("Project " + projet.getTitle() + " has no files, initializing empty list.");
                    projet.setFichiers(new ArrayList<>());
                } else {
                    LOGGER.info("Project " + projet.getTitle() + " has " + projet.getFichiers().size() + " files.");
                    projet.getFichiers().size(); // Forcer l'initialisation des fichiers
                }
            });

            List<Projet> projects = session.getProjets();
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
    public Response createProject(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                session = new Session();
                session.setUser(user);
                session.setHeureDebut(new Date());
                em.persist(session);
                user.setSession(session);
                em.merge(user);
            }

            Projet newProject = new Projet();
            newProject.setTitle(projectName);
            newProject.setOwner(session);

            session.getProjets().add(newProject);

            em.persist(newProject);
            em.merge(session);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to create project.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updatePseudo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePseudo(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        String newPseudo = requestData.get("newPseudo");

        try {
            // Check if new pseudo already exists
            long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.pseudo = :newPseudo", Long.class)
                           .setParameter("newPseudo", newPseudo)
                           .getSingleResult();
            if (count > 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("{\"error\": \"Pseudo already exists.\"}")
                               .build();
            }

            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            user.setPseudo(newPseudo);
            em.merge(user);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to update pseudo.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updatePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePassword(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        String oldPassword = requestData.get("oldPassword");
        String newPassword = requestData.get("newPassword");

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo AND u.password = :password", User.class)
                          .setParameter("pseudo", pseudo)
                          .setParameter("password", oldPassword)
                          .getSingleResult();

            user.setPassword(newPassword);
            em.merge(user);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User not found or incorrect password.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to update password.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updateTheme")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateTheme(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        Boolean theme = Boolean.valueOf(requestData.get("theme"));

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            Configuration configuration = session.getConfiguration();
            if (configuration == null) {
                configuration = new Configuration();
                configuration.setSession(session);
                em.persist(configuration);
            }
            configuration.setTheme(theme);
            em.merge(configuration);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to update theme.\"}")
                           .build();
        }
    }







}
