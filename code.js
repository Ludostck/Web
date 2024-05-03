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

// La délégation des événements permet d'attacher des écouteurs d'événements aux fichiers, même s'ils sont ajoutés dynamiquement
document.getElementById('file-explorer').addEventListener('click', function(event) {
    if (event.target.classList.contains('file')) {
        selectFile(event);
    }
});

document.addEventListener('DOMContentLoaded', function() {
    // Attache les événements de clic pour les dossiers
    document.querySelectorAll('.folder-name').forEach(function(folderName) {
        folderName.addEventListener('click', function() {
            console.log('Dossier cliqué:', this.textContent);
            var filesList = this.nextElementSibling;
            console.log('État avant le clic:', filesList.classList.contains('hidden'));
            filesList.classList.toggle('hidden');
            console.log('État après le clic:', filesList.classList.contains('hidden'));
        });
        
    });
});


// script.js
document.addEventListener('DOMContentLoaded', function() {
    // Vérifier si l'élément existe avant de tenter d'ajouter un écouteur d'événements
    const cLangButton = document.getElementById('c-lang');
    if(cLangButton) {
        cLangButton.addEventListener('click', function() {
            document.getElementById('code-editor').style.color = 'blue';
        });
    } else {
        console.log('Element with id "c-lang" not found');
    }

    const javaLangButton = document.getElementById('java-lang');
    if(javaLangButton) {
        javaLangButton.addEventListener('click', function() {
            document.getElementById('code-editor').style.color = 'red';
        });
    } else {
        console.log('Element with id "java-lang" not found');
    }

    const pytohnLangButton = document.getElementById('python-lang');
    if(pytohnLangButton) {
        pytohnLangButton.addEventListener('click', function() {
            document.getElementById('code-editor').style.color = 'green';
        });
    } else {
        console.log('Element with id "python-lang" not found');
    }

    // Répéter pour les autres boutons...
});

