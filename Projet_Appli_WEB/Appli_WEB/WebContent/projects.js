function loadUserProjects(pseudo) {
    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo), {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                console.error('erreur backend:', error);
                throw new Error(error.error || 'erreur reseau');
            });
        }
        return response.json();
    })
    .then(projects => {
        if (!Array.isArray(projects)) {
            throw new Error('erreur');
        }
        const projectsGrid = document.getElementById('projectsGrid');
        projectsGrid.innerHTML = '';
        if (projects.length === 0) {
            projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
        } else {
            projects.forEach(project => {
                const projectContainer = document.createElement('div');
                projectContainer.className = 'project-container';

                const projectButton = document.createElement('button');
                projectButton.className = 'project-button';
                projectButton.innerHTML = `<i class="material-icons">folder</i> ${project.title} <i class="fas fa-trash-alt delete-button"></i>`;
                
                projectButton.addEventListener('click', function(event) {
                    if (event.target.classList.contains('delete-button')) {
                        event.stopPropagation(); 
                        if (confirm(`Voulez-vous vraiment supprimer le projet "${project.title}"?`)) {
                            deleteProject(pseudo, project.title);
                        }
                    } else {
                        window.location.href = `code.html?pseudo=${encodeURIComponent(pseudo)}&projectName=${encodeURIComponent(project.title)}`;
                    }
                });

                projectContainer.appendChild(projectButton);
                projectsGrid.appendChild(projectContainer);
            });
        }
    })
    .catch(error => {
        console.error('erreur chargement projets:', error);
        const projectsGrid = document.getElementById('projectsGrid');
        projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
    });
}

function deleteProject(pseudo, projectName) {
    fetch('rest/supprimer-projet', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ pseudo: pseudo, projectName: projectName })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur reseau');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            loadUserProjects(pseudo);
        } else {
            console.error('erreur suppression:', data);
            alert("Erreur lors de la suppression du projet.");
        }
    })
    .catch(error => console.error('erreur suppression:', error));
}



window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        document.getElementById('userPseudo').textContent = pseudo;
        startSession(pseudo);
        loadUserProjects(pseudo);
    } else {
        console.error("Pseudo non trouvé dans l'URL");
    }

    document.getElementById('create-project-btn').addEventListener('click', function() {
        var projectName = document.getElementById('new-project-name').value;
        if (projectName) {
            createProject(pseudo, projectName);
        } else {
            alert("Veuillez entrer un nom de projet.");
        }
    });

    document.getElementById('config-button').addEventListener('click', function() {
        window.location.href = 'configuration.html?pseudo=' + pseudo;
    });

    document.getElementById('stats-button').addEventListener('click', function() {
        window.location.href = 'statistics.html?pseudo=' + pseudo;
    });

    document.getElementById('public-projects-button').addEventListener('click', function() {
        window.location.href = 'public-projects.html';
    });

    window.onunload = function() {
        stopSession(pseudo);
    };
};

function startSession(pseudo) {
    fetch('rest/startSession?pseudo=' + encodeURIComponent(pseudo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        }
    }).then(response => response.json())
      .then(data => console.log('Session started:', data))
      .catch(error => console.error('Error starting session:', error));
}

function stopSession(pseudo) {
    fetch('rest/stopSession?pseudo=' + encodeURIComponent(pseudo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        }
    }).then(response => response.json())
      .then(data => console.log('Session stopped:', data))
      .catch(error => console.error('erreur session:', error));
}

function createProject(pseudo, projectName) {
    fetch('rest/projects', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ pseudo: pseudo, projectName: projectName })
    })
    .then(response => {
        return response.json();
    })
    .then(data => {
        if (data.success) {
            window.location.href = 'code.html?pseudo=' + pseudo + '&projectName=' + projectName;
        } else {
            alert("Erreur lors de la création du projet.");
        }
    })
    .catch(error => console.error('erreur creation projet:', error));
}

function getAuthToken() {
    const name = "auth_token=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i].trim();
        if (c.indexOf(name) === 0) {
            const token = c.substring(name.length, c.length);
            return token;
        }
    }
    return "";
}
