window.onload = function() {
    var projectName = new URLSearchParams(window.location.search).get('projectName');
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    
    document.getElementById('project-name-display').textContent = projectName; // Affiche le nom du projet

    loadFoldersAndFiles(projectName, pseudo); // Charger les dossiers dans la sidebar

    document.getElementById('new-folder').addEventListener('click', function() {
        document.getElementById('new-folder-modal').classList.remove('hidden');
        document.getElementById('new-folder-modal').style.display = 'flex';
    });

    document.getElementById('close-new-folder-modal').addEventListener('click', function() {
        document.getElementById('new-folder-modal').classList.add('hidden');
        document.getElementById('new-folder-modal').style.display = 'none';
    });

    document.getElementById('create-folder').addEventListener('click', function() {
        var folderName = document.getElementById('folder-name').value;
        console.log('Folder name entered:', folderName); // Log the folder name to debug
        if (folderName) {
            createNewFolder(folderName, projectName, pseudo);
        } else {
            alert("Folder name cannot be empty.");
        }
    });

    document.getElementById('save-btn').addEventListener('click', function() {
        saveFileContent();
    });

    document.getElementById('close-new-file-modal').addEventListener('click', function() {
        document.getElementById('new-file-modal').classList.add('hidden');
        document.getElementById('new-file-modal').style.display = 'none';
    });

    document.getElementById('create-file').addEventListener('click', function() {
        var fileName = document.getElementById('file-name').value;
        var folderId = document.getElementById('create-file').dataset.folderId;
        console.log('File name entered:', fileName); // Log the file name to debug
        if (fileName) {
            createNewFile(fileName, folderId, projectName, pseudo);
        } else {
            alert("File name cannot be empty.");
        }
    });

    document.getElementById('close-all-folders').addEventListener('click', function() {
        closeAllFolders();
    });
};

var currentFileId = null;
var editor;

function getFileIconClass(extension) {
    switch (extension) {
        case 'java':
            return 'mdi mdi-language-java';
        case 'py':
            return 'mdi mdi-language-python';
        case 'c':
        case 'h':
            return 'mdi mdi-language-c';
        case 'js':
            return 'mdi mdi-language-javascript';
        case 'html':
            return 'mdi mdi-language-html5';
        case 'css':
            return 'mdi mdi-language-css3';
        case 'json':
            return 'mdi mdi-code-json';
        case 'md':
            return 'mdi mdi-language-markdown';
        default:
            return 'mdi mdi-file';
    }
}

function getEditorLanguage(extension) {
    switch (extension) {
        case 'java':
            return 'java';
        case 'py':
            return 'python';
        case 'c':
        case 'h':
            return 'c';
        case 'js':
            return 'javascript';
        case 'html':
            return 'html';
        case 'css':
            return 'css';
        case 'json':
            return 'json';
        case 'md':
            return 'markdown';
        default:
            return 'plaintext';
    }
}

// Fonction pour basculer la visibilité des fichiers dans un dossier
function toggleFolder(folderElement, iconElement) {
    var filesList = folderElement.querySelector(".files");
    filesList.classList.toggle("hidden");
    iconElement.classList.toggle("rotate-icon-down");
}

// Fonction pour gérer la sélection d'un fichier
function selectFile(event) {
    var allFiles = document.querySelectorAll('.file');
    allFiles.forEach(function(file) {
        file.classList.remove('selected');
    });

    event.currentTarget.classList.add('selected');
    var fileId = event.currentTarget.dataset.fileId;
    loadFileContent(fileId, event.currentTarget.textContent);

    event.stopPropagation();
}

function loadFoldersAndFiles(projectName, pseudo) {
    fetch('rest/dossiers?pseudo=' + encodeURIComponent(pseudo) + '&projectName=' + encodeURIComponent(projectName), {
        method: 'GET',
        headers: {
            'Authorization': getAuthToken() // Add the auth token
        }
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
        console.log('Data received:', data); // Log the data to inspect its structure

        if (!data || !data.dossiers) {
            throw new Error('Invalid response structure: "dossiers" property is missing');
        }

        var foldersContainer = document.getElementById('folders-container');
        foldersContainer.innerHTML = ''; // Efface les anciens dossiers

        data.dossiers.forEach(folder => {
            var folderElement = document.createElement('div');
            folderElement.className = 'folder';

            var folderHeader = document.createElement('div');
            folderHeader.className = 'folder-header';

            var folderIcon = document.createElement('i');
            folderIcon.className = 'fas fa-chevron-right rotate-icon';

            var folderNameElement = document.createElement('span');
            folderNameElement.className = 'folder-name';
            folderNameElement.innerHTML = '<i class="mdi mdi-folder"></i> ' + folder.nom;

            var addFileButton = document.createElement('button');
            addFileButton.className = 'add-file-button';
            addFileButton.innerHTML = '<i class="fas fa-plus"></i>';
            addFileButton.addEventListener('click', function(event) {
                event.stopPropagation(); // Prevent toggling the folder
                document.getElementById('new-file-modal').classList.remove('hidden');
                document.getElementById('new-file-modal').style.display = 'flex';
                document.getElementById('create-file').dataset.folderId = folder.id;
            });

            var deleteFolderButton = document.createElement('button');
            deleteFolderButton.className = 'delete-button';
            deleteFolderButton.innerHTML = '<i class="fas fa-trash"></i>';
            deleteFolderButton.addEventListener('click', function(event) {
                event.stopPropagation(); // Prevent toggling the folder
                if (confirm('Voulez-vous vraiment supprimer ce dossier?')) {
                    deleteFolder(folder.id, projectName, pseudo);
                }
            });

            folderHeader.appendChild(folderIcon);
            folderHeader.appendChild(folderNameElement);
            folderHeader.appendChild(addFileButton);
            folderHeader.appendChild(deleteFolderButton);

            var filesListElement = document.createElement('div');
            filesListElement.className = 'files hidden';

            folder.fichiers.forEach(file => {
                var fileElement = document.createElement('div');
                fileElement.className = 'file';
                var extension = file.nom.split('.').pop();
                var iconClass = getFileIconClass(extension);
                fileElement.innerHTML = '<span><i class="' + iconClass + ' file-icon"></i>' + file.nom + '</span>';

                var deleteFileButton = document.createElement('button');
                deleteFileButton.className = 'delete-button';
                deleteFileButton.innerHTML = '<i class="fas fa-trash"></i>';
                deleteFileButton.addEventListener('click', function(event) {
                    event.stopPropagation(); // Prevent selecting the file
                    if (confirm('Voulez-vous vraiment supprimer ce fichier?')) {
                        deleteFile(file.id, projectName, pseudo);
                    }
                });

                fileElement.appendChild(deleteFileButton);
                fileElement.dataset.fileId = file.id;
                fileElement.addEventListener('click', selectFile);
                filesListElement.appendChild(fileElement);
            });

            folderElement.appendChild(folderHeader);
            folderElement.appendChild(filesListElement);
            foldersContainer.appendChild(folderElement);

            folderHeader.addEventListener('click', function() {
                toggleFolder(folderElement, folderIcon);
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
            'Content-Type': 'application/json',
            'Authorization': getAuthToken() // Add the auth token
        },
        body: JSON.stringify({ folderName: folderName, projectName: projectName, pseudo: pseudo })
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
        document.getElementById('new-folder-modal').classList.add('hidden'); // Hide the modal
        document.getElementById('new-folder-modal').style.display = 'none'; // Hide the modal
        loadFoldersAndFiles(projectName, pseudo); // Reload folders to reflect the new folder
    })
    .catch(error => console.error('Error creating new folder:', error));
}

// Fonction pour créer un nouveau fichier
function createNewFile(fileName, folderId, projectName, pseudo) {
    fetch('rest/fichiers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken() // Add the auth token
        },
        body: JSON.stringify({ fileName: fileName, folderId: folderId, projectName: projectName, pseudo: pseudo })
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
        document.getElementById('file-name').value = ''; // Clear the input field
        document.getElementById('new-file-modal').classList.add('hidden'); // Hide the modal
        document.getElementById('new-file-modal').style.display = 'none'; // Hide the modal
        loadFoldersAndFiles(projectName, pseudo); // Reload folders to reflect the new file
    })
    .catch(error => console.error('Error creating new file:', error));
}

// Fonction pour charger le contenu d'un fichier
function loadFileContent(fileId, fileName) {
    console.log('Loading content for fileId:', fileId); // Ajout de log
    fetch('rest/fichiers/' + fileId, {
        method: 'GET',
        headers: {
            'Authorization': getAuthToken() // Add the auth token
        }
    })
    .then(response => {
        console.log('Response status:', response.status); // Ajout de log
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('File content received:', data); // Ajout de log
        if (data && data.hasOwnProperty('contenu')) {
            editor.setValue(data.contenu);
            currentFileId = fileId;
            console.log('File content set in editor:', data.contenu); // Ajout de log
            var extension = fileName.split('.').pop();
            var language = getEditorLanguage(extension);
            monaco.editor.setModelLanguage(editor.getModel(), language);
        } else {
            console.error('File content property is missing.');
        }
    })
    .catch(error => console.error('Error loading file content:', error));
}

// Fonction pour sauvegarder le contenu du fichier
function saveFileContent() {
    if (!currentFileId) {
        alert("No file selected to save.");
        return;
    }

    var fileContent = editor.getValue();
    fetch('rest/fichiers/' + currentFileId, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken() // Add the auth token
        },
        body: JSON.stringify({ contenu: fileContent })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        alert("File saved successfully.");
    })
    .catch(error => console.error('Error saving file content:', error));
}

// Fonction pour fermer tous les dossiers ouverts
function closeAllFolders() {
    var allFilesLists = document.querySelectorAll('.files');
    allFilesLists.forEach(function(filesList) {
        filesList.classList.add('hidden');
    });

    var allFolderIcons = document.querySelectorAll('.rotate-icon');
    allFolderIcons.forEach(function(icon) {
        icon.classList.remove('rotate-icon-down');
    });
}

// Fonction pour supprimer un dossier
function deleteFolder(folderId, projectName, pseudo) {
    fetch('rest/supprimer-dossier', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken() // Add the auth token
        },
        body: JSON.stringify({ folderId: folderId, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        loadFoldersAndFiles(projectName, pseudo); // Reload folders to reflect the deletion
    })
    .catch(error => console.error('Error deleting folder:', error));
}

// Fonction pour supprimer un fichier
function deleteFile(fileId, projectName, pseudo) {
    fetch('rest/supprimer-fichier', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken() // Add the auth token
        },
        body: JSON.stringify({ fileId: fileId, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'Network response was not ok');
            });
        }
        loadFoldersAndFiles(projectName, pseudo); // Reload folders to reflect the deletion
    })
    .catch(error => console.error('Error deleting file:', error));
}

// Configuration pour charger les fichiers nécessaires de Monaco Editor
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.27.0/min/vs' }});
require(['vs/editor/editor.main'], function () {
    editor = monaco.editor.create(document.getElementById('editor'), {
        value: '',
        language: 'java',
        theme: 'vs-dark'
    });
});

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
