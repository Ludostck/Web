window.onload = function() {
    var projectName = new URLSearchParams(window.location.search).get('projectName');
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    loadFoldersAndFiles(projectName, pseudo); // Charger les dossiers dans la sidebar

    document.getElementById('new-folder').addEventListener('click', function() {
        document.getElementById('new-folder-input').classList.toggle('hidden');
    });

    document.getElementById('create-folder').addEventListener('click', function() {
        var folderName = document.getElementById('folder-name').value;
        if (folderName) {
            createNewFolder(folderName, projectName, pseudo);
        }
    });
};

// Fonction pour basculer la visibilité des fichiers dans un dossier
function toggleFolder(folderElement) {
    var filesList = folderElement.querySelector(".files");
    filesList.classList.toggle("hidden");
}

// Fonction pour gérer la sélection d'un fichier
function selectFile(event) {
    var allFiles = document.querySelectorAll('.file');
    allFiles.forEach(function(file) {
        file.classList.remove('selected');
    });

    event.currentTarget.classList.add('selected');
    event.stopPropagation();
}

function loadFoldersAndFiles(projectName, pseudo) {
    fetch('rest/dossiers?pseudo=' + encodeURIComponent(pseudo) + '&projectName=' + encodeURIComponent(projectName))
        .then(response => {
            if (!response.ok) {
                return response.json().then(error => {
                    throw new Error(error.error || 'Network response was not ok');
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Data received:', data); // Log the data to inspect its structure

            if (!data || !data.dossiers) {
                throw new Error('Invalid response structure: "dossiers" property is missing');
            }

            var foldersContainer = document.getElementById('folders-container');
            foldersContainer.innerHTML = ''; // Efface les anciens dossiers

            data.dossiers.forEach(folder => {
                var folderElement = document.createElement('div');
                folderElement.className = 'folder';

                var folderNameElement = document.createElement('span');
                folderNameElement.className = 'folder-name';
                folderNameElement.textContent = folder.nom;

                var filesListElement = document.createElement('div');
                filesListElement.className = 'files hidden';

                folder.fichiers.forEach(file => {
                    var fileElement = document.createElement('div');
                    fileElement.className = 'file';
                    fileElement.textContent = file.nom;
                    fileElement.addEventListener('click', selectFile);
                    filesListElement.appendChild(fileElement);
                });

                folderElement.appendChild(folderNameElement);
                folderElement.appendChild(filesListElement);
                foldersContainer.appendChild(folderElement);

                folderNameElement.addEventListener('click', function() {
                    toggleFolder(folderElement);
                });
            });
        })
        .catch(error => console.error('Error loading folders and files:', error));
}

// Fonction pour créer un nouveau dossier
function createNewFolder(folderName, projectName, pseudo) {
    fetch('rest/dossiers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: folderName, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        return response.json();
    })
    .then(data => {
        document.getElementById('folder-name').value = ''; // Clear the input field
        document.getElementById('new-folder-input').classList.add('hidden'); // Hide the input field
        loadFoldersAndFiles(projectName, pseudo); // Reload folders to reflect the new folder
    })
    .catch(error => console.error('Error creating new folder:', error));
}

// Configuration pour charger les fichiers nécessaires de Monaco Editor
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.27.0/min/vs' }});
require(['vs/editor/editor.main'], function () {
    var editor = monaco.editor.create(document.getElementById('editor'), {
        value: [
            'function x() {',
            '\tconsole.log("Hello, world!");',
            '}'
        ].join('\n'),
        language: 'java',
        theme: 'vs-dark'
    });
});
