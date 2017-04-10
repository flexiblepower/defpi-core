// declare a new module called 'myApp', and make it require the `ng-admin` module as a dependency
var myApp = angular.module('myApp', [ 'ng-admin' ]);
// declare a function to run when the module bootstraps (during the 'config'
// phase)

myApp.config(function(RestangularProvider) {
	var login = 'admin', password = 'admin', token = window.btoa(login + ':'
			+ password);
	RestangularProvider.setDefaultHeaders({
		'Accept': 'application/json',
		'Authorization' : 'Basic ' + token,
	});
});

myApp.config([
		'NgAdminConfigurationProvider',
		function(nga) {
			var admin = nga.application('EF-Pi Orchestrator').baseApiUrl(
					'http://localhost:8080/');

			// Entities
			var user = nga.entity('user');

			user.listView().fields(
					[ nga.field('id'), nga.field('username'),
							nga.field('password'), nga.field('admin'),
							nga.field('email') ]);

			admin.addEntity(user);

			nga.configure(admin);
		} ]);