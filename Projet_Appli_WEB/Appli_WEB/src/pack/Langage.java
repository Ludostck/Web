package pack;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.HashMap;

public class Langage {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	private String nom;
	
	private String extension;
	
	private HashMap<String, String> mots_cle;

	public Langage() {}
	
	public long getId() {
		return this.id;
	}
	
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public HashMap<String, String> getMots_cle() {
		return mots_cle;
	}

	public void setMots_cle(HashMap<String, String> mots_cle) {
		this.mots_cle = mots_cle;
	}

}
