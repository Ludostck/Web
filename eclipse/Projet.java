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
public class Projet {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
	String name;
	String description;
    Date creationDate;
    @ManyToOne
    Person owner;


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

    public String getDate() {
		return creationDate;
	}

	public void setDate(Date date) {
		this.creationDate = date;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person person) {
		this.owner = person;
	}

	
}

