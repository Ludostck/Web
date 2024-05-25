
// Cette fonction permet de basculer la visibilité des fichiers dans un dossier
function toggleFolder(folderElement) {
    // Trouve le conteneur de fichiers le plus proche
    var filesList = folderElement.querySelector(".files");
    filesList.classList.toggle("hidden");
}

// Cette fonction gère la sélection d'un fichier
function selectFile(event) {
    // Désélectionne tous les fichiers précédemment sélectionnés
    var allFiles = document.querySelectorAll('.file');
    allFiles.forEach(function(file) {
        file.classList.remove('selected');
    });

    // Sélectionne le fichier cliqué et arrête la propagation de l'événement
    event.currentTarget.classList.add('selected');
    event.stopPropagation();
}

// Attache les événements de clic pour les dossiers
document.querySelectorAll('.folder-name').forEach(function(folderName) {
    folderName.addEventListener('click', function() {
        toggleFolder(folderName.parentElement);
    });
});



// Configuration pour charger les fichiers nécessaires de Monaco Editor
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.27.0/min/vs' }});
require(['vs/editor/editor.main'], function () {
    // Créer l'éditeur de code dans la balise <div> avec l'ID "editor"
    var editor = monaco.editor.create(document.getElementById('editor'), {
        value: [
            'function x() {',
            '\tconsole.log("Hello, world!");',
            '}'
        ].join('\n'),
        language: 'java', // Définir le langage du code (javascript, python, etc.)
        theme: 'vs-dark' // Définir le thème de l'éditeur (vs, vs-dark, etc.)
    });
});