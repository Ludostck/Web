package pack;

import javax.persistence.*;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @ManyToOne
    @JsonBackReference
    private Session owner;

    @OneToMany(mappedBy = "projet", fetch = FetchType.EAGER)
    private List<Dossier> dossiers;

    // Constructeurs, getters et setters
    public Projet() {
        this.creationDate = new Date(); // Initialiser la date de création à la date actuelle
    }

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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Session getOwner() {
        return owner;
    }

    public void setOwner(Session owner) {
        this.owner = owner;
    }

    public List<Dossier> getDossiers() {
        return dossiers;
    }

    public void setDossiers(List<Dossier> dossiers) {
        this.dossiers = dossiers;
    }
}

