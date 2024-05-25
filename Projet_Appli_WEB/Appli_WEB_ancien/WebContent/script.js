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



document.addEventListener("DOMContentLoaded", function() {
    // Sélection de tous les éléments de classe "folder"
    var folders = document.querySelectorAll(".folder");
    
    // Ajout d'un gestionnaire d'événements click à chaque élément de classe "folder"
    folders.forEach(function(folder) {
        folder.addEventListener("click", function() {
            // Toggle l'affichage des sous-dossiers
            var subfolders = this.querySelector("ul");
            if (subfolders) {
                if (subfolders.style.display === "none") {
                    subfolders.style.display = "block";
                } else {
                    subfolders.style.display = "none";
                }
            }
        });
    });
});
