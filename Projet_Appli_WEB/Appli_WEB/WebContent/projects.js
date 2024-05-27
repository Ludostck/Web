function loadUserProjects(pseudo) {
    console.log('Loading projects for pseudo:', pseudo);
    fetch('rest/projects?pseudo=' + encodeURIComponent(pseudo), {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        }
    })
    .then(response => {
        console.log('Received response from backend');
        if (!response.ok) {
            return response.json().then(error => {
                console.error('Error response from backend:', error);
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
        projectsGrid.innerHTML = '';
        if (projects.length === 0) {
            projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
        } else {
            projects.forEach(project => {
                console.log('Adding project to grid:', project.title);
                const projectButton = document.createElement('button');
                projectButton.className = 'project-button';
                projectButton.innerHTML = `<i class="material-icons">folder</i> ${project.title}`;
                projectButton.addEventListener('click', function() {
                    console.log('Project clicked:', project.title);
                    window.location.href = `code.html?pseudo=${encodeURIComponent(pseudo)}&projectName=${encodeURIComponent(project.title)}`;
                });
                projectsGrid.appendChild(projectButton);
            });
        }
    })
    .catch(error => {
        console.error('Error loading projects:', error);
        const projectsGrid = document.getElementById('projectsGrid');
        projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
    });
}

window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        console.log('Pseudo found in URL:', pseudo);
        document.getElementById('userPseudo').textContent = pseudo;
        startSession(pseudo);
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
      .catch(error => console.error('Error stopping session:', error));
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
        console.log('Received response from backend for project creation');
        return response.json();
    })
    .then(data => {
        if (data.success) {
            console.log('Project created successfully:', projectName);
            window.location.href = 'code.html?pseudo=' + pseudo + '&projectName=' + projectName;
        } else {
            console.error('Error creating project:', data);
            alert("Erreur lors de la création du projet.");
        }
    })
    .catch(error => console.error('Error creating project:', error));
}

function getAuthToken() {
    const name = "auth_token=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i].trim();
        if (c.indexOf(name) === 0) {
            const token = c.substring(name.length, c.length);
            console.log('Auth token found:', token);
            return token;
        }
    }
    console.log('Auth token not found');
    return "";
}
