package pack;

import java.util.Collection;
import java.util.Date;

import javax.persistence.*;;


@Entity
public class Session {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	private Date heure_debut;
	
	private Date heure_fin;
	
	@OneToMany
	private Collection<Projet> projets;

	@ManyToOne
	private User user;
	
	public Session() {}

	public long getId() {
		return id;
	}

	public Date getDebut() {
		return heure_debut;
	}

	public void setDebut(Date heure_debut) {
		this.heure_debut = heure_debut;
	}

	public Date getFin() {
		return heure_fin;
	}

	public void setFin(Date heure_fin) {
		this.heure_fin = heure_fin;
	}

	public Collection<Projet> getProjet() {
		return projets;
	}

	public void setProjet(Collection<Projet> projet) {
		this.projets = projet;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	
}
