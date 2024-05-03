document.getElementById('login-form').addEventListener('submit', function(event) {
    event.preventDefault();
    var username = document.getElementById('username').value;
    var password = document.getElementById('password').value;
    if (username && password) {
        // En production, ici on ferait une requête vers le serveur pour valider l'authentification.
        window.location.href = 'projects.html'; // Rediriger vers la page des projets.
    } else {
        alert('Veuillez remplir tous les champs.');
    }
});
