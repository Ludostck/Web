var app = angular.module('loginApp', []);

app.controller('LoginController', function($scope, $http) {
    $scope.formData = {};

    $scope.submitForm = function() {
        $http.post("rest/login", $scope.formData)
            .then(function(response) {
                if (response.data.success) {
                    window.location.href = 'projects.html?pseudo=' + encodeURIComponent(response.data.pseudo);
                } else {
                    $scope.response = response.data.message;
                }
            }, function(error) {
                $scope.response = "Erreur : " + error.statusText;
            });
    };
});
