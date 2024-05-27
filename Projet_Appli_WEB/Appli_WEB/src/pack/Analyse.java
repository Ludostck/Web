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
public class Analyse {

	@Id
    @GeneratedValue(strategy=GenerationType.AUTO)  
	int id;
    @ManyToOne
	User user;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

    public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date date) {
		this.beginTime = date;
	}

    public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date date) {
		this.endTime = date;
	}


    public int getActions() {
		return actions;
	}

	public void setActions(int actions) {
		this.actions = actions;
	}

    public int getErrors() {
		return actions;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}
	
}
