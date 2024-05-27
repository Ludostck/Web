package pack;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pack.User;

@Entity
public class Configuration {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
	boolean Theme;
	
    @OneToOne
    @JsonIgnore
	Session session;
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean getTheme() {
		return Theme;
	}

	public void setTheme(boolean color) {
		this.Theme = color;
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	
}
