window.onload = function() {
    // Récupérer le paramètre 'pseudo' de l'URL
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        // Afficher le pseudo dans l'élément avec l'ID 'userPseudo'
        document.getElementById('userPseudo').textContent = pseudo;
        // Charger les projets de l'utilisateur
        loadUserProjects(pseudo);
    } else {
        console.error("Pseudo non trouvé dans l'URL");
    }
};

function loadUserProjects(pseudo) {
    // Faire une requête pour récupérer les projets de l'utilisateur
    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo))
        .then(response => {
            if (!response.ok) {
                // Gérer les erreurs de réponse
                return response.json().then(error => {
                    throw new Error(error.error || 'Network response was not ok');
                });
            }
            // Retourner la réponse au format JSON
            return response.json();
        })
        .then(projects => {
            if (!Array.isArray(projects)) {
                // Gérer les erreurs de format de réponse
                throw new Error('Response is not an array');
            }
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = ''; // Vider le contenu précédent
            if (projects.length === 0) {
                // Afficher un message si aucun projet n'est disponible
                projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
            } else {
                // Parcourir les projets et les ajouter à la page
                projects.forEach(project => {
                    const projectElement = document.createElement('div');
                    projectElement.className = 'project';
                    projectElement.innerHTML = `
                        <div class="project-title"><h3>${project.title}</h3></div>
                        <div class="file-container">
                            ${project.files.map(file => `
                                <div class="file"><img src="${file.icon}" alt="${file.type}"> <span>${file.name}</span></div>
                            `).join('')}
                        </div>
                    `;
                    projectsGrid.appendChild(projectElement);
                });
            }
        })
        .catch(error => {
            // Gérer les erreurs lors du chargement des projets
            console.error('Error loading projects:', error);
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
        });
}


document.getElementById('new-project-button').addEventListener('click', function(event) {
    event.preventDefault();
    addNewProject();
});

function addNewProject() {
    const pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (!pseudo) {
        alert("Pseudo non trouvé !");
        return;
    }

    const newProject = {
        title: "Nouveau Projet",
        files: []
    };

    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(newProject)
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Erreur lors de la création du projet');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            window.location.href = 'code.html';
        } else {
            alert('Erreur lors de la création du projet');
        }
    })
    .catch(error => {
        console.error('Error adding new project:', error);
        alert('Erreur lors de la création du projet');
    });
}
