var app = angular.module('loginApp', []);

app.controller('LoginController', function($scope, $http) {
    $scope.formData = {};

    $scope.submitForm = function() {
        console.log('Submitting form with data:', $scope.formData); // Debugging
        $http.post("rest/login", $scope.formData)
            .then(function(response) {
                console.log('Response from server:', response.data); // Debugging
                if (response.data.success) {
                    console.log('Redirecting to projects page'); // Debugging
                    // Redirect to projects page with the pseudo as a query parameter
                    window.location.href = 'projects.html?pseudo=' + encodeURIComponent(response.data.pseudo);
                } else {
                    // Display the error message
                    $scope.response = response.data.message;
                }
            }, function(error) {
                // Handle HTTP errors
                $scope.response = "Erreur : " + error.statusText;
            });
    };
});
