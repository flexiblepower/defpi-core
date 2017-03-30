var api = {
	username: 'admin',
	password: 'password',
	authHeader: "",
	defaultHeader: {},
	setup: function(){
		api.authHeader = "Basic " + btoa(api.username + ":" + api.password);
		$.ajaxSetup({
			beforeSend: function(xhr, settings){
				if(settings.url.startsWith(apiLink)){
					xhr.setRequestHeader("Authorization", api.authHeader);
				}
			}
		});
	},
	getContainers: function(){
		return $.ajax({
			url: apiLink+"/containers",
			method: "GET"
		});
	},
	getLinks: function(){
		return $.ajax({
			url: apiLink+"/links",
			method: "GET"
		});
	},
	getProtos: function(){
		return $.ajax({
			url: apiLink+"/protos",
			method: "GET"
		});
	},
	getServices: function(){
		return $.ajax({
			url: apiLink+"/services",
			method: "GET"
		});
	},
	deleteService: function(image, tag){
		return $.ajax({
			url: apiLink+"/services/"+encodeURIComponent(image)+"/"+encodeURIComponent(tag),
			method: "DELETE"
		});
	},
	getHosts: function(){
		return $.ajax({
			url: apiLink+"/hosts",
			datatype: "json",
			headers: {
				'Accept': 'application/json'
			}
		});
	},
	newHost: function(){
		return $.ajax({
			url: apiLink+"/hosts/new"
		});
	},
	activateHost: function(host){
		return $.ajax({
			url: apiLink+"/hosts/activate/"+host,
			method: "POST"
		});
	},
	deactivateHost: function(host){
		return $.ajax({
			url: apiLink+"/hosts/deactivate/"+host,
			method: "POST"
		});
	},
	removeHost: function(host){
		return $.ajax({
			url: apiLink+"/hosts/remove/"+host,
			method: "POST"
		});
	},
	createContainer: function(container){
		return $.ajax({
			url:apiLink+"/containers",
			data: JSON.stringify(container),
			contentType: "application/json",
			method: "POST"
		});
	},
	createLink: function(link){
		return $.ajax({
			url:apiLink+"/links",
			data: JSON.stringify(link),
			contentType: "application/json",
			method: "POST"
		});
	},
	createProto: function(proto){
		return $.ajax({
			url:apiLink+"/protos",
			data: JSON.stringify(proto),
			contentType: "application/json",
			method: "POST"
		});
	},
	upgradeContainer: function(uuid){
		return $.ajax({
			method: "POST",
			url:apiLink+"/containers/"+uuid+"/upgrade"
		});
	},
	deleteContainer: function(uuid){
		return $.ajax({
			method: "DELETE",
			url:apiLink+"/containers/"+uuid
		});
	},
	deleteLink: function(id){
		return $.ajax({
			method: "DELETE",
			url:apiLink+"/links/"+id
		});
	},
	deleteProto: function(hash){
		return $.ajax({
			method: "DELETE",
			url:apiLink+"/protos/"+hash
		});
	}
};