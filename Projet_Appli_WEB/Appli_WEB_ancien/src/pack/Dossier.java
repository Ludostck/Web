package pack;

import java.util.List;

import javax.persistence.*;

public class Dossier {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	private String nom;
	
	@OneToMany (mappedBy = "parent")
	private List<Dossier> enfants;
	
	@ManyToOne
	private Dossier parent;
	
	@OneToMany
	private List<Fichier> fichiers;
	
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
	}
	
	public List<Fichier> getFichiers(){
		return this.fichiers;
	}
	
}
