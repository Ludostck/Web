window.onload = function() {
    var pseudo = new URLSearchParams(window.location.search).get('pseudo');
    if (!pseudo) {
        console.error("Pseudo non trouvé dans l'URL");
        return;
    }

    fetch('rest/userTheme?pseudo=' + encodeURIComponent(pseudo))
        .then(response => response.json())
        .then(data => {
            document.getElementById('current-theme').textContent = 'Thème actuel: ' + (data.theme ? 'Clair' : 'Sombre');
        });

    document.getElementById('update-pseudo-btn').addEventListener('click', function() {
        var newPseudo = document.getElementById('new-pseudo').value;

        if (!newPseudo) {
            alert("Veuillez entrer un nouveau pseudo.");
            return;
        }

        fetch('rest/updatePseudo', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ pseudo: pseudo, newPseudo: newPseudo })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Pseudo mis à jour.');
                window.location.href = 'projects.html?pseudo=' + newPseudo;
            } else {
                alert('Erreur: ' + data.error);
            }
        })
        .catch(error => console.error('Erreur:', error));
    });

    document.getElementById('update-password-btn').addEventListener('click', function() {
        var oldPassword = document.getElementById('old-password').value;
        var newPassword = document.getElementById('new-password').value;

        if (!oldPassword || !newPassword) {
            alert("Veuillez entrer l'ancien et le nouveau mot de passe.");
            return;
        }

        fetch('rest/updatePassword', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ pseudo: pseudo, oldPassword: oldPassword, newPassword: newPassword })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Mot de passe mis à jour.');
            } else {
                alert('Erreur: ' + data.error);
            }
        })
        .catch(error => console.error('Erreur:', error));
    });

    document.getElementById('update-theme-btn').addEventListener('click', function() {
        var theme = document.getElementById('theme').value;

        fetch('rest/updateTheme', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ pseudo: pseudo, theme: theme })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Thème mis à jour.');
                document.getElementById('current-theme').textContent = 'Thème actuel: ' + (theme === 'true' ? 'Clair' : 'Sombre');
            } else {
                alert('Erreur: ' + data.error);
            }
        })
        .catch(error => console.error('Erreur:', error));
    });
};