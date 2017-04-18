// declare a new module called 'myApp', and make it require the `ng-admin` module as a dependency
var myApp = angular.module('myApp', [ 'ng-admin' ]);
// declare a function to run when the module bootstraps (during the 'config'
// phase)

myApp.config(function(RestangularProvider) {
	var username = window.localStorage.getItem('efpi_orchestrator_username');
	var password = window.localStorage.getItem('efpi_orchestrator_password');

	var token = window.btoa(username + ':' + password);
	RestangularProvider.setDefaultHeaders({
		'Accept' : 'application/json',
		'Authorization' : 'Basic ' + token
	});
});

myApp
		.config([
				'NgAdminConfigurationProvider',
				function(nga) {
					var admin = nga.application('EF-Pi Orchestrator')
							.baseApiUrl('http://localhost:8080/');

					// Entities
					var user = nga.entity('user');
					var process = nga.entity('process');
					var service = nga.entity('service');

					// Views

					user.listView().fields(
							[ nga.field('username').isDetailLink(true),
									nga.field('email', 'email'),
									nga.field('admin', 'boolean') ]);

					user.creationView().fields(
							[ nga.field('username'),
									nga.field('email', 'email'),
									nga.field('password', 'password'),
									nga.field('admin', 'boolean').choices([ {
										value : true,
										label : 'true'
									}, {
										value : false,
										label : 'false'
									} ]) ]);
					user.editionView().fields(
							[ nga.field('username').editable(false),
									nga.field('email', 'email'),
									nga.field('password', 'password'),
									nga.field('admin', 'boolean').choices([ {
										value : true,
										label : 'true'
									}, {
										value : false,
										label : 'false'
									} ]) ]);

					process.listView().fields(
							[ nga.field('id'), nga.field('userName'),
									nga.field('processService'),
									nga.field('runningNode.id') ]);

					service.listView().fields([ nga.field('name') ]);

					// Add entities
					admin.addEntity(user);
					admin.addEntity(process);
					admin.addEntity(service);

					// Menu
					admin
							.menu(nga
									.menu()
									.addChild(
											nga
													.menu(user)
													.icon(
															'<span class="glyphicon glyphicon-user"></span>'))
									.addChild(
											nga
													.menu(process)
													.icon(
															'<span class="glyphicon glyphicon-repeat"></span>'))
									.addChild(
											nga
													.menu(service)
													.icon(
															'<span class="glyphicon glyphicon-play-circle"></span>')));

					var customHeaderTemplate = '<div class="navbar-header"><button type="button" class="navbar-toggle"ng-click="isCollapsed = !isCollapsed"><span class="icon-bar"></span> <span class="icon-bar"></span> <spanclass="icon-bar"></span></button><a class="navbar-brand" href="#" ng-click="appController.displayHome()">EF-Pi Orchestrator</a></div><ul class="nav navbar-top-links navbar-right hidden-xs"><li><a href="#" onclick="logout()"><iclass="fa fa-sign-out fa-fw"></i>Logout</a></li></ul>';

					admin.header(customHeaderTemplate);

					nga.configure(admin);
				} ]);