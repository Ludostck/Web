document.getElementById('search-button').addEventListener('click', function() {
    const keyword = document.getElementById('search-keyword').value;
    loadPublicProjects(keyword);
});

window.onload = function() {
    loadPublicProjects();
};

function loadPublicProjects(keyword = '') {
    console.log('Loading public projects with keyword:', keyword);
    fetch('rest/public-projects/search?keyword=' + encodeURIComponent(keyword), {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                console.error('Error response from backend:', error);
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        return response.json();
    })
    .then(projects => {
        if (!Array.isArray(projects)) {
            throw new Error('Response is not an array');
        }
        const projectsGrid = document.getElementById('publicProjectsGrid');
        projectsGrid.innerHTML = '';
        if (projects.length === 0) {
            projectsGrid.innerHTML = '<p>Aucun projet disponible</p>';
        } else {
            projects.forEach(project => {
                const projectContainer = document.createElement('div');
                projectContainer.className = 'project-container';

                const projectButton = document.createElement('button');
                projectButton.className = 'project-button';
                projectButton.innerHTML = `<i class="material-icons">folder</i> ${project.title}`;
                projectButton.addEventListener('click', function() {
                    window.location.href = `public-project-details.html?projectId=${encodeURIComponent(project.id)}`;
                });

                projectContainer.appendChild(projectButton);
                projectsGrid.appendChild(projectContainer);
            });
        }
    })
    .catch(error => {
        console.error('Error loading public projects:', error);
        const projectsGrid = document.getElementById('publicProjectsGrid');
        projectsGrid.innerHTML = '<p>Erreur de chargement des projets</p>';
    });
}
