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
            LOGGER.info("Attempting login for user: " + user.getPseudo());
            
            // Chiffrer le mot de passe avec SHA-256
            String hashedPassword = hashPassword(user.getPassword());
            
            User foundUser = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo AND u.password = :password", User.class)
                               .setParameter("pseudo", user.getPseudo())
                               .setParameter("password", hashedPassword)
                               .getSingleResult();

            if (foundUser != null) {
                // Si la connexion est r�ussie, g�n�rez un token unique
                String authToken = generateToken();
                foundUser.setAuth_token(authToken);
                // Cr�ez un cookie pour stocker le token
                NewCookie authCookie = new NewCookie(AUTH_COOKIE_NAME, authToken, "/", null, null, NewCookie.DEFAULT_MAX_AGE, false, false);

                LOGGER.info("Login successful for user: " + foundUser.getPseudo());
                return Response.ok("{\"success\": true, \"message\": \"Connexion r�ussie\", \"pseudo\": \"" + foundUser.getPseudo() + "\"}")
                        .cookie(authCookie)
                        .build();
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

            long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.pseudo = :pseudo", Long.class)
                           .setParameter("pseudo", user.getPseudo())
                           .getSingleResult();
            if (count > 0) {
                LOGGER.warning("User with pseudo " + user.getPseudo() + " already exists.");
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"User with this pseudo already exists.\"}").build();
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

            LOGGER.info("User created successfully: " + user.getPseudo());
            return Response.ok("{\"pseudo\": \"" + user.getPseudo() + "\"}").cookie(authCookie).build();
        } catch (Exception e) {
            LOGGER.severe("Error creating user: " + user.getPseudo() + " - " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to create user.\"}").build();
        }
    }

    @GET
    @Path("/projects")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getUserProjects(@QueryParam("pseudo") String pseudo, @CookieParam(AUTH_COOKIE_NAME) String authToken) {
        LOGGER.info("Received request to get projects for pseudo: " + pseudo);
        
        try {
        	LOGGER.info("token facade" + authToken);
            // V�rifier la validit� du token
            if (!isValidToken(authToken + "=")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity("{\"error\": \"Invalid token. Please log in again.\"}")
                               .build();
            }

            // R�cup�ration de l'utilisateur par son pseudo
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Found user: " + user.getPseudo() + " with ID: " + user.getId());

            // R�cup�ration de la session de l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("No session found for user: " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }
            LOGGER.info("Found session for user: " + user.getPseudo() + " with session ID: " + session.getId());

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

            // V�rifier si le projet existe d�j�
            Long count = em.createQuery("SELECT COUNT(p) FROM Projet p WHERE p.title = :projectName AND p.owner.user = :user", Long.class)
                           .setParameter("projectName", projectName)
                           .setParameter("user", user)
                           .getSingleResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                               .entity("{\"error\": \"Project already exists.\"}")
                               .build();
            }

            Projet newProject = new Projet();
            newProject.setTitle(projectName);
            newProject.setOwner(session);
            
            Dossier dossier = new Dossier();
            dossier.setProjet(newProject);
            dossier.setNom(projectName);
            
            em.persist(dossier);
            if (session.getProjets() == null) {
                session.setProjets(new ArrayList<>());
            }

            session.getProjets().add(newProject);

            // Persister le projet, le dossier et le fichier
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
    @Path("/dossiers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createFolder(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");
        String folderName = requestData.get("folderName");
        
        try {
            // R�cup�ration de l'utilisateur par son pseudo
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            // R�cup�ration de la session de l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }

            // R�cup�rer le projet � partir du nom du projet et de la session utilisateur
            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Project not found.\"}")
                               .build();
            }

            // Cr�ation du nouveau dossier
            Dossier newFolder = new Dossier();
            newFolder.setNom(folderName);
            newFolder.setProjet(projet);

            // Ajout du dossier au projet
            if (projet.getDossiers() == null) {
                projet.setDossiers(new ArrayList<>());
            }
            projet.getDossiers().add(newFolder);

            // Persister le dossier
            em.persist(newFolder);
            em.merge(projet);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User or project not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to create folder.\"}")
                           .build();
        }
    }

    @POST
    @Path("/fichiers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createFile(Map<String, String> requestData) {
        String pseudo = requestData.get("pseudo");
        String projectName = requestData.get("projectName");
        String fileName = requestData.get("fileName");
        Long folderId = Long.valueOf(requestData.get("folderId"));

        try {
            // R�cup�ration de l'utilisateur par son pseudo
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            // R�cup�ration de la session de l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }

            // R�cup�rer le projet � partir du nom du projet et de la session utilisateur
            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Project not found.\"}")
                               .build();
            }

            // R�cup�rer le dossier par son ID
            Dossier dossier = em.find(Dossier.class, folderId);
            if (dossier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Folder not found.\"}")
                               .build();
            }

            // Cr�ation du nouveau fichier
            Fichier newFile = new Fichier();
            newFile.setNom(fileName);
            newFile.setDossier(dossier);
            newFile.setProjet(projet);
            newFile.setContenu("test");

            // Persister le fichier
            em.persist(newFile);

            return Response.ok("{\"success\": true}").build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"User or project not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to create file.\"}")
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
            // Chiffrer les mots de passe avec SHA-256
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



    @GET
    @Path("/dossiers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectFolders(@QueryParam("pseudo") String pseudo, @QueryParam("projectName") String projectName) {
        try {
            // R�cup�ration de l'utilisateur par son pseudo
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();
            LOGGER.info("Found user: " + user.getPseudo() + " with ID: " + user.getId());

            // R�cup�ration de la session de l'utilisateur
            Session session = user.getSession();
            if (session == null) {
                LOGGER.warning("No session found for user: " + user.getPseudo());
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Session not found for user.\"}")
                               .build();
            }

            // R�cup�rer le projet � partir du nom du projet et de la session utilisateur
            Projet projet = em.createQuery("SELECT p FROM Projet p WHERE p.title = :title AND p.owner = :owner", Projet.class)
                              .setParameter("title", projectName)
                              .setParameter("owner", session)
                              .getSingleResult();

            if (projet == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"Project not found.\"}")
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

            // Create a map to hold the response
            Map<String, Object> response = new HashMap<>();
            response.put("dossiers", folderDetails);

            return Response.ok(response).build();
        } catch (NoResultException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\": \"Project or user not found.\"}")
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to load folders.\"}")
                           .build();
        }
    }




 // M�thode pour hasher le mot de passe avec SHA-256
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
            LOGGER.severe("Error validating token: " + e.getMessage());
            return false;
        }
    }


    
    @GET
    @Path("/fichiers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileContent(@PathParam("id") Long id) {
        try {

            Fichier fichier = em.find(Fichier.class, id);
            if (fichier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"File not found.\"}")
                               .build();
            }
            return Response.ok(fichier).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to load file content.\"}")
                           .build();
        }
    }

    @PUT
    @Path("/fichiers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateFileContent(@PathParam("id") Long id, Map<String, String> requestData) {
        try {
            Fichier fichier = em.find(Fichier.class, id);
            if (fichier == null) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("{\"error\": \"File not found.\"}")
                               .build();
            }
            fichier.setContenu(requestData.get("contenu"));
            em.merge(fichier);
            return Response.ok("{\"success\": true}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("{\"error\": \"Unable to save file content.\"}")
                           .build();
        }
    }

    @POST
    @Path("/startSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response startSession(@QueryParam("pseudo") String pseudo) {
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
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"User not found.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to start session.\"}").build();
        }
    }

    @POST
    @Path("/stopSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response stopSession(@QueryParam("pseudo") String pseudo) {
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
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"User not found.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to stop session.\"}").build();
        }
    }
    
    
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getUserStatistics(@QueryParam("pseudo") String pseudo) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.pseudo = :pseudo", User.class)
                          .setParameter("pseudo", pseudo)
                          .getSingleResult();

            Session session = user.getSession();
            if (session == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Session not found for user.\"}").build();
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
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"User not found.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to load statistics.\"}").build();
        }
    }
    
    @GET
    @Path("/projects/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getProjectDetails(@PathParam("projectId") Long projectId) {
        try {
            Projet project = em.find(Projet.class, projectId);
            if (project == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Project not found.\"}").build();
            }
            Map<String, Object> projectDetails = new HashMap<>();
            projectDetails.put("creationDate", project.getCreationDate());
            projectDetails.put("numberOfFiles", project.getDossiers().stream().flatMap(d -> d.getFichiers().stream()).count());

            return Response.ok(projectDetails).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Unable to load project details.\"}").build();
        }
    }



}