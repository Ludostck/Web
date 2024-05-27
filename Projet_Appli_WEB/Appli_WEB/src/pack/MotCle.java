package pack;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;


import java.util.List;

@Entity
public class MotCle {
	
	@Id
	private String motcle;
	
	@ManyToMany
	private List<Projet> projets;
	
	public MotCle() {}

	public String getMotcle() {
		return motcle;
	}

	public void setMotcle(String motcle) {
		this.motcle = motcle;
	}
	
	public List<Projet> getProjets() {
		return projets;
	}
	
	public void addProjet(Projet p) {
		this.projets.add(p);
	}
	
	public void setProjet(List<Projet> lp) {
		this.projets = lp;
	}

}