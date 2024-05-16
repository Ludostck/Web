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
public class Analyse {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
    @ManyToOne
	Person user;
    Date beginTime;
    Date endTime;
    int actions;
    int errors;
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUser() {
		return lastcolor;
	}

	public void setUser(Person user) {
		this.user = user;
	}

    public String getBeginTime() {
		return creationDate;
	}

	public void setBeginTime(Date date) {
		this.creationDate = date;
	}

    public String getEndTime() {
		return creationDate;
	}

	public void setEndTime(Date date) {
		this.creationDate = date;
	}


    public String getActions() {
		return actions;
	}

	public void setActions(int actions) {
		this.actions = actions;
	}

    public String getErrors() {
		return actions;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}
	
}
