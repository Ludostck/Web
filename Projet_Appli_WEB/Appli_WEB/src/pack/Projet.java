package pack;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
public class Projet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Fichier> fichiers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private Session owner;

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Session getOwner() {
        return owner;
    }

    public void setOwner(Session owner) {
        this.owner = owner;
    }
    
    public List<Fichier> getFichiers() {
    	return this.fichiers;
    }
    
    public void setFichiers(List<Fichier> fichiers) {
    	this.fichiers = fichiers;
    }
    
}

