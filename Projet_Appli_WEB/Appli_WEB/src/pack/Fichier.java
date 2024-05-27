package pack;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class Fichier {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    private String nom;
    private String type;
    private String contenu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "projet_id")
    @JsonIgnore 
    private Projet projet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dossier_id")
    @JsonManagedReference
    private Dossier dossier;

    public Fichier() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Projet getProjet() {
        return projet;
    }

    public void setProjet(Projet projet) {
        this.projet = projet;
    }

    public Dossier getDossier() {
        return dossier;
    }

    public void setDossier(Dossier dossier) {
        this.dossier = dossier;
        if (dossier != null && !dossier.getFichiers().contains(this)) {
            dossier.addFichier(this);
        }
    }
}
