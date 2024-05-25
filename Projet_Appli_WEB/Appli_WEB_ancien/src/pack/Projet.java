package pack;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import pack.User;

@Entity
public class Projet {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
	String name;
	String description;
    Date creationDate;
    @ManyToOne
    User owner;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    public Date getDate() {
		return this.creationDate;
	}

	public void setDate(Date date) {
		this.creationDate = date;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User person) {
		this.owner = person;
	}

	
}

