package pack;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Dossier {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    private String nom;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private List<Dossier> enfants = new ArrayList<>();

    @ManyToOne
    private Dossier parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    @JsonBackReference
    private Projet projet;

    @OneToMany(mappedBy = "dossier", fetch = FetchType.EAGER)
    @JsonBackReference
    private List<Fichier> fichiers = new ArrayList<>();

    public Dossier() {}

    public long getId() {
        return this.id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return this.nom;
    }

    public void addEnfant(Dossier enf) {
        this.enfants.add(enf);
        enf.setParent(this);
    }

    public List<Dossier> getEnfants(){
        return this.enfants;
    }

    public void setParent(Dossier par) {
        this.parent = par;
    }

    public Dossier getParent() {
        return this.parent;
    }

    public void addFichier(Fichier fich) {
        this.fichiers.add(fich);
        if (fich.getDossier() != this) {
            fich.setDossier(this);
        }
    }

    public List<Fichier> getFichiers(){
        return this.fichiers;
    }

    public void setFichiers(List<Fichier> fichiers) {
        this.fichiers = fichiers;
    }

    public Projet getProjet(){
        return this.projet;
    }

    public void setProjet(Projet proj) {
        this.projet = proj;
    }
}
