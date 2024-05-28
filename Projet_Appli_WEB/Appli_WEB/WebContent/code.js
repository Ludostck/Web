window.onload = function() {
    var projectName = new URLSearchParams(window.location.search).get('projectName');
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    
    document.getElementById('project-name-display').textContent = projectName;

    loadFoldersAndFiles(projectName, pseudo); 

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
        if (folderName) {
            createNewFolder(folderName, projectName, pseudo);
        } else {
            alert("Le nom du dossier ne peut pas être vide.");
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
        if (fileName) {
            createNewFile(fileName, folderId, projectName, pseudo);
        } else {
            alert("Le nom du fichier ne peut pas être vide.");
        }
    });

    document.getElementById('close-all-folders').addEventListener('click', function() {
        closeAllFolders();
    });

    document.getElementById('add-keyword').addEventListener('click', function() {
        document.getElementById('new-keyword-modal').classList.remove('hidden');
        document.getElementById('new-keyword-modal').style.display = 'flex';
    });

    document.getElementById('close-new-keyword-modal').addEventListener('click', function() {
        document.getElementById('new-keyword-modal').classList.add('hidden');
        document.getElementById('new-keyword-modal').style.display = 'none';
    });

    document.getElementById('create-keyword').addEventListener('click', function() {
        var keyword = document.getElementById('keyword').value;
        if (keyword) {
            addKeywordToProject(keyword, projectName, pseudo);
        } else {
            alert("Le mot-clé ne peut pas être vide.");
        }
    });
};

function addKeywordToProject(keyword, projectName, pseudo) {
    fetch('rest/keywords', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ keyword: keyword, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        return response.json();
    })
    .then(data => {
        document.getElementById('keyword').value = ''; 
        document.getElementById('new-keyword-modal').classList.add('hidden'); 
        document.getElementById('new-keyword-modal').style.display = 'none'; 
        alert("Mot-clé ajouté.");
    })
    .catch(error => console.error('erreur ajout mot clé :', error));
}

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

function toggleFolder(folderElement, iconElement) {
    var filesList = folderElement.querySelector(".files");
    filesList.classList.toggle("hidden");
    iconElement.classList.toggle("rotate-icon-down");
}

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
            'Authorization': getAuthToken()
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        return response.json();
    })
    .then(data => {
        if (!data || !data.dossiers) {
            throw new Error('propriété "dossiers" manquante.');
        }

        var foldersContainer = document.getElementById('folders-container');
        foldersContainer.innerHTML = '';

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
                event.stopPropagation(); 
                document.getElementById('new-file-modal').classList.remove('hidden');
                document.getElementById('new-file-modal').style.display = 'flex';
                document.getElementById('create-file').dataset.folderId = folder.id;
            });

            var deleteFolderButton = document.createElement('button');
            deleteFolderButton.className = 'delete-button';
            deleteFolderButton.innerHTML = '<i class="fas fa-trash"></i>';
            deleteFolderButton.addEventListener('click', function(event) {
                event.stopPropagation(); 
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
                    event.stopPropagation(); 
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
    .catch(error => console.error('Erreur chargement dossiers/fichiers :', error));
}

function createNewFolder(folderName, projectName, pseudo) {
    fetch('rest/dossiers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ folderName: folderName, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        return response.json();
    })
    .then(data => {
        document.getElementById('folder-name').value = ''; 
        document.getElementById('new-folder-modal').classList.add('hidden'); 
        document.getElementById('new-folder-modal').style.display = 'none';
        loadFoldersAndFiles(projectName, pseudo); 
    })
    .catch(error => console.error('Erreur création dossier :', error));
}

function createNewFile(fileName, folderId, projectName, pseudo) {
    fetch('rest/fichiers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ fileName: fileName, folderId: folderId, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        return response.json();
    })
    .then(data => {
        document.getElementById('file-name').value = ''; 
        document.getElementById('new-file-modal').classList.add('hidden'); 
        document.getElementById('new-file-modal').style.display = 'none'; 
        loadFoldersAndFiles(projectName, pseudo); 
    })
    .catch(error => console.error('Erreur création fichier :', error));
}

function loadFileContent(fileId, fileName) {
    fetch('rest/fichiers/' + fileId, {
        method: 'GET',
        headers: {
            'Authorization': getAuthToken()
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data && data.hasOwnProperty('contenu')) {
            editor.setValue(data.contenu);
            currentFileId = fileId;
            var extension = fileName.split('.').pop();
            var language = getEditorLanguage(extension);
            monaco.editor.setModelLanguage(editor.getModel(), language);
        } else {
            console.error('contenu du fichier manquant.');
        }
    })
    .catch(error => console.error('Erreur lors du chargement du contenu du fichier :', error));
}

function saveFileContent() {
    if (!currentFileId) {
        alert("Aucun fichier sélectionné à enregistrer.");
        return;
    }

    var fileContent = editor.getValue();
    fetch('rest/fichiers/' + currentFileId, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ contenu: fileContent })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        alert("Fichier enregistré avec succès.");
    })
    .catch(error => console.error('Erreur enregistrement fichier :', error));
}

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

function deleteFolder(folderId, projectName, pseudo) {
    fetch('rest/supprimer-dossier', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ folderId: folderId, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        loadFoldersAndFiles(projectName, pseudo); 
    })
    .catch(error => console.error('erreur suppression  dossier :', error));
}

function deleteFile(fileId, projectName, pseudo) {
    fetch('rest/supprimer-fichier', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': getAuthToken()
        },
        body: JSON.stringify({ fileId: fileId, projectName: projectName, pseudo: pseudo })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(error => {
                throw new Error(error.error || 'erreur réseau.');
            });
        }
        loadFoldersAndFiles(projectName, pseudo); 
    })
    .catch(error => console.error('Erreur suppression du fichier :', error));
}

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
            return c.substring(name.length, c.length);
        }
    }
    return "";
}
