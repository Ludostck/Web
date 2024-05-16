package pack;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import pack.Person;

@Entity
public class Configuration {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
	String color;
    @ManyToOne
	Person user;
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getUser() {
		return lastcolor;
	}

	public void setUser(Person user) {
		this.user = user;
	}

	
}
