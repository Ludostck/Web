var app = angular.module('signupApp', []);

app.controller('SignupController', function($scope, $http) {
    $scope.formData = {};

    $scope.submitForm = function() {
        $http.post("rest/signup", $scope.formData)
            .then(function(response) {
                var userPseudo = response.data.pseudo;
                window.location.href = 'projects.html?pseudo=' + encodeURIComponent(userPseudo);
            }, function(error) {
                $scope.response = "Erreur : " + error.statusText;
            });
    };
});
