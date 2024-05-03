function shrink(element) {
    if (!element.classList.contains('clicked')) {
        element.classList.add('clicked');
        setTimeout(function() {
            element.classList.remove('clicked');
        }, 200); // L'effet dure 200ms et ensuite la classe est retirée
    }
}

// Attacher l'événement de clic à tous les éléments project
document.querySelectorAll('.project').forEach(project => {
    project.addEventListener('click', function() {
        shrink(project);
    });
});

// Si le bouton de retour est présent, lui attacher un gestionnaire de clic
const backButton = document.getElementById('back-button');
if (backButton) {
    backButton.addEventListener('click', function() {
        history.back();
    });
}

document.querySelectorAll('.project.empty').forEach(emptyProject => {
    emptyProject.addEventListener('click', function() {
        shake(emptyProject);
    });
});

function shake(element) {
    element.classList.add('shake');
    setTimeout(function() {
        element.classList.remove('shake');
    }, 820); // La durée doit correspondre à celle de l'animation CSS
}

const newProjectButton = document.getElementById('new-project-button');
newProjectButton.addEventListener('click', function() {
    // Ajoutez la classe qui déclenche l'animation
    
    newProjectButton.classList.add('ripple-effect');
    
    // Enlevez la classe après que l'animation soit terminée
    setTimeout(() => {
        newProjectButton.classList.remove('ripple-effect');
    }, 400); // 600 ms = durée de l'animation
});


document.getElementById('new-project-button').addEventListener('click', function(event) {
    event.preventDefault();

    window.location.href = 'code.html'; // Rediriger vers la page des projets.
   
});




