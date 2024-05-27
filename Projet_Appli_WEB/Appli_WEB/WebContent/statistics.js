window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (pseudo) {
        loadStatistics(pseudo);
    } else {
        console.error("Pseudo non trouvé dans l'URL");
    }

    document.getElementById('refreshButton').addEventListener('click', function() {
        loadStatistics(pseudo);
    });
};

function loadStatistics(pseudo) {
    fetch('rest/statistics?pseudo=' + encodeURIComponent(pseudo))
        .then(response => response.json())
        .then(statistics => {
            document.getElementById('creationDate').textContent = new Date(statistics.creationDate).toLocaleString();
            document.getElementById('totalElapsedTime').textContent = formatTime(statistics.totalTimeSpent);
            document.getElementById('totalProjects').textContent = statistics.totalProjects;
            document.getElementById('totalFiles').textContent = statistics.totalFiles;
            document.getElementById('totalSize').textContent = formatSize(statistics.totalSize);
            document.getElementById('fileTypes').textContent = formatFileTypes(statistics.fileTypes);

            loadProjectSelector(statistics.projects);
        })
        .catch(error => console.error('Error loading statistics:', error));
}

function formatTime(milliseconds) {
    var seconds = Math.floor((milliseconds / 1000) % 60),
        minutes = Math.floor((milliseconds / (1000 * 60)) % 60),
        hours = Math.floor((milliseconds / (1000 * 60 * 60)) % 24),
        days = Math.floor(milliseconds / (1000 * 60 * 60 * 24));

    return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
}

function formatSize(bytes) {
    var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes == 0) return '0 Byte';
    var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
}

function formatFileTypes(fileTypes) {
    return Object.entries(fileTypes).map(([type, count]) => `${type}: ${count}`).join(', ');
}

function loadProjectSelector(projects) {
    var projectSelector = document.getElementById('projectSelector');
    projectSelector.innerHTML = '';

    projects.forEach(project => {
        var option = document.createElement('option');
        option.value = project.id;
        option.textContent = project.title;
        projectSelector.appendChild(option);
    });

    projectSelector.addEventListener('change', function() {
        var projectId = this.value;
        if (projectId) {
            loadProjectDetails(projectId);
        }
    });
}

function loadProjectDetails(projectId) {
    fetch('rest/projects/' + projectId)
        .then(response => response.json())
        .then(project => {
            var projectDetails = document.getElementById('projectDetails');
            projectDetails.innerHTML = `
                <p>Date de création: ${new Date(project.creationDate).toLocaleString()}</p>
                <p>Nombre de fichiers: ${project.numberOfFiles}</p>
            `;
        })
        .catch(error => console.error('Error loading project details:', error));
}