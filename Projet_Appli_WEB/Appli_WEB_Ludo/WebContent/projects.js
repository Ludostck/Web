function loadUserProjects(pseudo) {
    console.log('Loading projects for pseudo:', pseudo); // Log au début du chargement des projets

    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo))
        .then(response => {
            console.log('Received response from backend'); // Log après réception de la réponse
            if (!response.ok) {
                return response.json().then(error => {
                    console.error('Error response from backend:', error); // Log en cas d'erreur de la réponse
                    throw new Error(error.error || 'Network response was not ok');
                });
            }
            return response.json();
        })
        .then(projects => {
            console.log("Projects received from backend:", projects); // Log les projets reçus
            if (!Array.isArray(projects)) {
                throw new Error('Response is not an array');
            }
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = ''; // Vider le contenu précédent
            if (projects.length === 0) {
                projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
            } else {
                projects.forEach(project => {
                    console.log('Adding project to grid:', project.title); // Log chaque projet ajouté à la grille
                    const projectButton = document.createElement('button');
                    projectButton.className = 'project-button';
                    projectButton.textContent = project.title;
                    projectButton.addEventListener('click', function() {
                        console.log('Project clicked:', project.title); // Log lors du clic sur un projet
                        window.location.href = `code.html?pseudo=${encodeURIComponent(pseudo)}&projectName=${encodeURIComponent(project.title)}`;
                    });
                    projectsGrid.appendChild(projectButton);
                });
            }
        })
        .catch(error => {
            console.error('Error loading projects:', error); // Log en cas d'erreur lors du chargement des projets
            const projectsGrid = document.getElementById('projectsGrid');
            projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
        });
}

window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        console.log('Pseudo found in URL:', pseudo);
        document.getElementById('userPseudo').textContent = pseudo;
        startSession(pseudo);  // Start session when the page loads
        loadUserProjects(pseudo);
    } else {
        console.error("Pseudo non trouvé dans l'URL");
    }

    document.getElementById('create-project-btn').addEventListener('click', function() {
        var projectName = document.getElementById('new-project-name').value;
        if (projectName) {
            console.log('Creating new project:', projectName);
            createProject(pseudo, projectName);
        } else {
            alert("Veuillez entrer un nom de projet.");
        }
    });

    document.getElementById('config-button').addEventListener('click', function() {
        console.log('Configuration button clicked');
        window.location.href = 'configuration.html?pseudo=' + pseudo;
    });

    document.getElementById('stats-button').addEventListener('click', function() {
        console.log('Statistics button clicked');
        window.location.href = 'statistics.html?pseudo=' + pseudo;
    });

    window.onunload = function() {
        stopSession(pseudo);  // Stop session when the page unloads
    };
};

function startSession(pseudo) {
    fetch('rest/startSession?pseudo=' + encodeURIComponent(pseudo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(response => response.json())
      .then(data => console.log('Session started:', data))
      .catch(error => console.error('Error starting session:', error));
}

function stopSession(pseudo) {
    fetch('rest/stopSession?pseudo=' + encodeURIComponent(pseudo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(response => response.json())
      .then(data => console.log('Session stopped:', data))
      .catch(error => console.error('Error stopping session:', error));
}


function createProject(pseudo, projectName) {
    fetch('rest/projects', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ pseudo: pseudo, projectName: projectName })
    })
    .then(response => {
        console.log('Received response from backend for project creation'); // Log après réception de la réponse de création de projet
        return response.json();
    })
    .then(data => {
        if (data.success) {
            console.log('Project created successfully:', projectName); // Log en cas de succès de la création de projet
            window.location.href = 'code.html?pseudo=' + pseudo + '&projectName=' + projectName;
        } else {
            console.error('Error creating project:', data); // Log en cas d'erreur lors de la création de projet
            alert("Erreur lors de la création du projet.");
        }
    })
    .catch(error => console.error('Error creating project:', error)); // Log en cas d'erreur lors de la création de projet
}
