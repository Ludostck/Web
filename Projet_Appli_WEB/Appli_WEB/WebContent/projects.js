function loadUserProjects(pseudo) {
    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo))
        .then(response => {
            if (!response.ok) {
                return response.json().then(error => {
                    throw new Error(error.error || 'Network response was not ok');
                });
            }
            return response.json();
        })
        .then(projects => {
            console.log("Projects received from backend:", projects);
            if (!Array.isArray(projects)) {
                throw new Error('Response is not an array');
            }
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = ''; // Vider le contenu précédent
            if (projects.length === 0) {
                projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
            } else {
                projects.forEach(project => {
                    const projectElement = document.createElement('div');
                    projectElement.className = 'project';
                    projectElement.innerHTML = `
                        <div class="project-title"><h3>${project.title}</h3></div>
                        <div class="file-container">
                            ${(project.fichiers || []).map(file => `
                                <div class="file"><span>${file.nom}</span></div>
                            `).join('')}
                        </div>
                    `;
                    projectsGrid.appendChild(projectElement);
                });
            }
        })
        .catch(error => {
            console.error('Error loading projects:', error);
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
        });
}


function createProject(pseudo, projectName) {
    fetch('rest/projects', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ pseudo: pseudo, projectName: projectName })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            window.location.href = 'code.html?pseudo=' + pseudo + '&projectName=' + projectName;
        } else {
            alert("Erreur lors de la création du projet.");
        }
    })
    .catch(error => console.error('Error creating project:', error));
}

window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        document.getElementById('userPseudo').textContent = pseudo;
        loadUserProjects(pseudo);
    } else {
        console.error("Pseudo non trouvé dans l'URL");
    }

    document.getElementById('new-project-button').addEventListener('click', function() {
        document.getElementById('new-project-modal').classList.remove('hidden');
    });

    document.querySelector('.close').addEventListener('click', function() {
        document.getElementById('new-project-modal').classList.add('hidden');
    });

    document.getElementById('create-project-btn').addEventListener('click', function() {
        var projectName = document.getElementById('new-project-name').value;
        if (projectName) {
            createProject(pseudo, projectName);
        } else {
            alert("Veuillez entrer un nom de projet.");
        }
    });
};

