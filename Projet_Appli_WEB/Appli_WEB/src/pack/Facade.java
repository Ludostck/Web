package pack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.ejb.Singleton;
import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.transaction.Transactional;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.Base64;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Path("/")
public class Facade {

    private static final String AUTH_COOKIE_NAME = "auth_token";
    @PersistenceContext
    EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(Facade.class.getName());

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response traiterConnexion(User user) {
        try {
            LOGGER.info("Tentative de connexion pour l'utilisateur : " + user.getPseudo());
            
            String hashedPassword = hashPassword(user.getPassword());
            
            User foundUser = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo AND u.password = :password", User.class)
                               .setParameter("pseudo", user.getPseudo())
                               .setParameter("password", hashedPassword)
                               .getSingleResult();

            if (foundUser != null) {
                String authToken = generateToken();
                foundUser.setAuth_token(authToken);
                NewCookie authCookie = new NewCookie(AUTH_COOKIE_NAME, authToken, "/", null, null, NewCookie.DEFAULT_MAX_AGE, false, false);

                LOGGER.info("Connexion réussie pour l'utilisateur : " + foundUser.getPseudo());
                return Response.ok("{\"success\": true, \"message\": \"Connexion réussie\", \"pseudo\": \"" + foundUser.getPseudo() + "\"}")
                        .cookie(authCookie)
                        .build();
            } else {
                LOGGER.warning("Échec de la connexion pour l'utilisateur : " + user.getPseudo());
                return Response.status(Response.Status.UNAUTHORIZED).entity("{\"success\": false, \"message\": \"Identifiant ou mot de passe incorrect\"}").build();
            }
        } catch (NoResultException e) {
            LOGGER.warning("Utilisateur non trouvé pour le pseudo : " + user.getPseudo());
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"success\": false, \"message\": \"Identifiant ou mot de passe incorrect\"}").build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la connexion pour l'utilisateur : " + user.getPseudo() + " - " + e.getMessage());
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
            LOGGER.info("Création de l'utilisateur : " + user.getPseudo());

            long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.pseudo = :pseudo", Long.class)
                           .setParameter("pseudo", user.getPseudo())
                           .getSingleResult();
            if (count > 0) {
                LOGGER.warning("Un utilisateur avec le pseudo " + user.getPseudo() + " existe déjà.");
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Un utilisateur avec ce pseudo existe déjà.\"}").build();
            }

            String hashedPassword = hashPassword(user.getPassword());
            user.setPassword(hashedPassword);
            user.setCreationDate(new Date());

            Session newSession = new Session();
            newSession.setHeureDebut(new Date());
            newSession.setUser(user);
            em.persist(newSession);

            user.setSession(newSession);
            String authToken = generateToken();
            user.setAuth_token(authToken);

            NewCookie authCookie = new NewCookie("auth_token", authToken, "/", null, null, NewCookie.DEFAULT_MAX_AGE, false, false);
            em.persist(user);

            LOGGER.info("Utilisateur créé avec succès : " + user.getPseudo());
            return Response.ok("{\"pseudo\": \"" + user.getPseudo() + "\"}").cookie(authCookie).build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la création de l'utilisateur : " + user.getPseudo() + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Impossible de créer l'utilisateur.\"}").build();
        }
    }

    @GET
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getUserProjects(@QueryParam("pseudo") String pseudo, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        LOGGER.info("Demande de projets pour le pseudo : " + pseudo);
        
        try {
            if (!isValidToken(authToken + "=")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                               .build();
            }

            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Utilisateur trouvé : " + user.getPseudo() + " avec ID : " + user.getId());

            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("Aucune session trouvée pour l'utilisateur : " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}")
                               .build();
            }
            LOGGER.info("Session trouvée pour l'utilisateur : " + user.getPseudo() + " avec ID de session : " + session.getId());

            List<Projet> projects = session.getProjets();
            LOGGER.info("Nombre de projets trouvés : " + projects.size());
            return Response.ok(projects).build();
        } catch (NoResultException e) {
            LOGGER.warning("Utilisateur ou session non trouvé pour le pseudo : " + pseudo);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou session non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors du chargement des projets pour le pseudo : " + pseudo + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de charger les projets.\"}")
                           .build();
        }
    }

    @POST
    @Path("/projects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createProject(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

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

            Long count = em.createQuery("SELECT COUNT(p) FROM Projet p WHERE p.title = :projectName AND p.owner.user = :user", Long.class)
                           .setParameter("projectName", projectName)
                           .setParameter("user", user)
                           .getSingleResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                               .entity("{\"error\": \"Le projet existe déjà.\"}")
                               .build();
            }

            Projet newProject = new Projet();
            newProject.setTitle(projectName);
            newProject.setOwner(session);
            newProject.setPublic(true);
            
            Dossier dossier = new Dossier();
            dossier.setProjet(newProject);
            dossier.setNom(projectName);
            
            em.persist(dossier);
            if (session.getProjets() == null) {
                session.setProjets(new ArrayList<>());
            }

            session.getProjets().add(newProject);

            em.persist(newProject);
            em.merge(session);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de créer le projet.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updatePseudo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePseudo(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String pseudo = requestData.get("pseudo");
        String newPseudo = requestData.get("newPseudo");

        try {
            long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.pseudo = :newPseudo", Long.class)
                           .setParameter("newPseudo", newPseudo)
                           .getSingleResult();
            if (count > 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("{\"error\": \"Le pseudo existe déjà.\"}")
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
                           .entity("{\"error\": \"Utilisateur non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de mettre à jour le pseudo.\"}")
                           .build();
        }
    }

    @POST
    @Path("/dossiers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createFolder(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");
        String folderName = requestData.get("folderName");
        
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}")
                               .build();
            }

            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Projet non trouvé.\"}")
                               .build();
            }

            Dossier newFolder = new Dossier();
            newFolder.setNom(folderName);
            newFolder.setProjet(projet);

            if (projet.getDossiers() == null) {
                projet.setDossiers(new ArrayList<>());
            }
            projet.getDossiers().add(newFolder);

            em.persist(newFolder);
            em.merge(projet);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou projet non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de créer le dossier.\"}")
                           .build();
        }
    }

    @POST
    @Path("/fichiers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createFile(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");
        String fileName = requestData.get("fileName");
        Long folderId = Long.valueOf(requestData.get("folderId"));

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}")
                               .build();
            }

            Dossier dossier = em.find(Dossier.class, folderId);
            if (dossier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Dossier non trouvé.\"}")
                               .build();
            }

            Fichier newFile = new Fichier();
            newFile.setNom(fileName);
            newFile.setDossier(dossier);
            newFile.setContenu("test");

            em.persist(newFile);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou projet non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de créer le fichier.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updatePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updatePassword(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String pseudo = requestData.get("pseudo");
        String oldPassword = requestData.get("oldPassword");
        String newPassword = requestData.get("newPassword");

        try {
            String hashedOldPassword = hashPassword(oldPassword);
            String hashedNewPassword = hashPassword(newPassword);

            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo AND u.password = :password", User.class)
                          .setParameter("pseudo", pseudo)
                          .setParameter("password", hashedOldPassword)
                          .getSingleResult();

            user.setPassword(hashedNewPassword);
            em.merge(user);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur non trouvé ou mot de passe incorrect.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de mettre à jour le mot de passe.\"}")
                           .build();
        }
    }

    @POST
    @Path("/updateTheme")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateTheme(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

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
                           .entity("{\"error\": \"Utilisateur non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de mettre à jour le thème.\"}")
                           .build();
        }
    }

    @GET
    @Path("/dossiers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectFolders(@QueryParam("pseudo") String pseudo, @QueryParam("projectName") String projectName, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Utilisateur trouvé : " + user.getPseudo() + " avec ID : " + user.getId());

            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("Aucune session trouvée pour l'utilisateur : " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}")
                               .build();
            }

            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Projet non trouvé.\"}")
                               .build();
            }

            List<Dossier> folders = projet.getDossiers();
            List<Map<String, Object>> folderDetails = new ArrayList<>();

            for (Dossier dossier : folders) {
                Map<String, Object> dossierMap = new HashMap<>();
                dossierMap.put("id", dossier.getId());
                dossierMap.put("nom", dossier.getNom());
                dossierMap.put("enfants", dossier.getEnfants());
                dossierMap.put("fichiers", dossier.getFichiers());
                folderDetails.add(dossierMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("dossiers", folderDetails);

            return Response.ok(response).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Projet ou utilisateur non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de charger les dossiers.\"}")
                           .build();
        }
    }

    @GET
    @Path("/fichiers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileContent(@PathParam("id") Long id, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            Fichier fichier = em.find(Fichier.class, id);
            if (fichier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Fichier non trouvé.\"}")
                               .build();
            }
            return Response.ok(fichier).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de charger le contenu du fichier.\"}")
                           .build();
        }
    }

    @PUT
    @Path("/fichiers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateFileContent(@PathParam("id") Long id, Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            Fichier fichier = em.find(Fichier.class, id);
            if (fichier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Fichier non trouvé.\"}")
                               .build();
            }
            fichier.setContenu(requestData.get("contenu"));
            em.merge(fichier);
            return Response.ok("{\"success\": true}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible d'enregistrer le contenu du fichier.\"}")
                           .build();
        }
    }

    @POST
    @Path("/startSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response startSession(@QueryParam("pseudo") String pseudo, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                session = new Session();
                session.setUser(user);
                user.setSession(session);
            }
            session.setHeureDebut(new Date());
            em.merge(session);
            em.merge(user);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Utilisateur non trouvé.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Impossible de démarrer la session.\"}").build();
        }
    }

    @POST
    @Path("/stopSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response stopSession(@QueryParam("pseudo") String pseudo, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session != null) {
                session.setHeureFin(new Date());
                session.updateTotalTimeSpent();
                em.merge(session);
            }

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Utilisateur non trouvé.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Impossible d'arrêter la session.\"}").build();
        }
    }

    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getUserStatistics(@QueryParam("pseudo") String pseudo, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}").build();
            }

            List<Projet> projects = session.getProjets();
            long totalFiles = projects.stream().flatMap(p -> p.getDossiers().stream()).flatMap(d -> d.getFichiers().stream()).count();
            long totalSize = projects.stream().flatMap(p -> p.getDossiers().stream()).flatMap(d -> d.getFichiers().stream()).mapToLong(f -> f.getContenu().length()).sum();
            Map<String, Long> fileTypes = projects.stream()
                .flatMap(p -> p.getDossiers().stream())
                .flatMap(d -> d.getFichiers().stream())
                .collect(Collectors.groupingBy(Fichier::getType, Collectors.counting()));

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("creationDate", user.getCreationDate());
            statistics.put("totalTimeSpent", session.getTotalTimeSpent());
            statistics.put("totalProjects", projects.size());
            statistics.put("totalFiles", totalFiles);
            statistics.put("totalSize", totalSize);
            statistics.put("fileTypes", fileTypes);

            return Response.ok(statistics).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Utilisateur non trouvé.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Impossible de charger les statistiques.\"}").build();
        }
    }

    @GET
    @Path("/projects/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getProjectDetails(@PathParam("projectId") Long projectId, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        try {
            Projet project = em.find(Projet.class, projectId);
            if (project == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Projet non trouvé.\"}").build();
            }
            Map<String, Object> projectDetails = new HashMap<>();
            projectDetails.put("creationDate", project.getCreationDate());
            projectDetails.put("numberOfFiles", project.getDossiers().stream().flatMap(d -> d.getFichiers().stream()).count());

            return Response.ok(projectDetails).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Impossible de charger les détails du projet.\"}").build();
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getEncoder().encodeToString(token);
    }

    @GET
    @Path("/validate-token")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean isValidToken(@QueryParam("token") String token) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.auth_token = :token", User.class)
                          .setParameter("token", token)
                          .getSingleResult();
            
            return user != null;
        } catch (NoResultException e) {
            return false;
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la validation du token : " + e.getMessage());
            return false;
        }
    }
    
    @POST
    @Path("/supprimer-dossier")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response supprimerDossier(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        Long folderId = Long.valueOf(requestData.get("folderId"));
        String pseudo = requestData.get("pseudo");
        

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Dossier dossier = em.find(Dossier.class, folderId);
            if (dossier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Dossier non trouvé.\"}")
                               .build();
            }

            em.remove(dossier);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou dossier non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de supprimer le dossier.\"}")
                           .build();
        }
    }
    
    @POST
    @Path("/supprimer-fichier")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response supprimerFichier(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        Long fileId = Long.valueOf(requestData.get("fileId"));
        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Fichier fichier = em.find(Fichier.class, fileId);
            if (fichier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Fichier non trouvé.\"}")
                               .build();
            }

            em.remove(fichier);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou fichier non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de supprimer le fichier.\"}")
                           .build();
        }
    }
    
    @POST
    @Path("/supprimer-projet")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response supprimerProjet(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session non trouvée pour l'utilisateur.\"}")
                               .build();
            }

            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Projet non trouvé.\"}")
                               .build();
            }

            for (Dossier dossier : projet.getDossiers()) {
                for (Fichier fichier : dossier.getFichiers()) {
                    em.remove(fichier);
                }
                em.remove(dossier);
            }

            em.remove(projet);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou projet non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de supprimer le projet.\"}")
                           .build();
        }
    }
    
    @GET
    @Path("/public-projects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicProjects() {
        try {
            List<Projet> publicProjects = em.createQuery("SELECT p FROM Projet p WHERE p.isPublic = true", Projet.class)
                                            .getResultList();
            return Response.ok(publicProjects).build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors du chargement des projets publics : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de charger les projets publics.\"}")
                           .build();
        }
    }

    @GET
    @Path("/public-projects/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPublicProjects(@QueryParam("keyword") String keyword) {
        try {
            String queryStr = "SELECT p FROM Projet p JOIN p.motscles m WHERE p.isPublic = true";
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryStr += " AND m.motcle = :keyword";
            }
            
            TypedQuery<Projet> query = em.createQuery(queryStr, Projet.class);
            if (keyword != null && !keyword.trim().isEmpty()) {
                query.setParameter("keyword", keyword);
            }
            
            List<Projet> publicProjects = query.getResultList();
            return Response.ok(publicProjects).build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la recherche de projets publics : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible de rechercher des projets publics.\"}")
                           .build();
        }
    }

    @POST
    @Path("/keywords")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addKeywordToProject(Map<String, String> requestData, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        if (!isValidToken(authToken + "=")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("{\"error\": \"Token invalide. Veuillez vous reconnecter.\"}")
                           .build();
        }

        String keyword = requestData.get("keyword");
        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            MotCle motCle = em.find(MotCle.class, keyword);
            if (motCle == null) {
            	
                motCle = new MotCle();
                motCle.setMotcle(keyword);
                motCle.setProjet(new ArrayList<Projet>());
                em.persist(motCle);
            }

            if (!projet.getMotscles().contains(motCle)) {
                projet.getMotscles().add(motCle);
                
                motCle.getProjets().add(projet);
                
                em.merge(projet);
                em.merge(motCle);
            }

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Utilisateur ou projet non trouvé.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Impossible d'ajouter le mot-clé.\"}")
                           .build();
        }
    }
}
