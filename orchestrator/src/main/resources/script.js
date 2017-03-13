var dockerPrefix = "hub.servicelab.org/dsefpi/";
var apiLink = "";
var rancherLink = "";

var instance;

function openInstantiateModal(image){
	$("#image").val(image);
	service = model.getService(image);
	$("#tag").html('');
	$.each(service.tags, function(key, value){
		if(!value.tag.endsWith("-arm"))
			$("#tag").append("<option>"+value.tag+"</option>");
	});
	$("#startService").modal();
}
var c;
$(document).ready(function(){
	setTimeout(function(){$('#refresh').trigger('click')}, 1000);
	api.setup();
	c = new Clipboard('.copy');
	c.on('success', function(){
		showAlert(true, 'Copied to clipboard')
	});
	$(document).on("click", ".userSwitch", function(){
		api.username = $(this).data('username');
		api.password = $(this).data('password');
		$('#currentUser').html($(this).html());
		$('#refresh').trigger('click');
		api.setup();
		api.newHost().done(function(data){
			$('#newPublicHost').attr('data-clipboard-text', data.publicCommand);
			$('#newPrivateHost').attr('data-clipboard-text', data.privateCommand);
		});
	});
	
	$(document).on('keydown', function(e){
		if(e.keyCode === 191){
			console.log('refresh');
			$('#refresh').trigger('click');
		} 
	});
	
	api.newHost().done(function(data){
		$('#newPublicHost').attr('data-clipboard-text', data.publicCommand);
		$('#newPrivateHost').attr('data-clipboard-text', data.privateCommand);
	});
	$(document).on("click", ".hostActivate", function(){
		api.activateHost($(this).parent().data('id'));
	});
	$(document).on("click", ".hostDeactivate", function(){
		api.deactivateHost($(this).parent().data('id'));
	});
	$(document).on("click", ".hostRemove", function(){
		api.removeHost($(this).parent().data('id'));
	});
	$(document).on("click", "#createContainer", function(){
		var container = {
			name: $("#name").val(),
			description: $("#description").val(),
			image: dockerPrefix+$("#image").val()+":"+$("#tag").val(),
			host: $("#host").val(),
			environment: {}
		};
		$(".envpair").each(function(){
			key = $(this).children('.key').val();
			value = $(this).children('.value').val();
			container.environment[key] = value;
		});
		//console.log(container);
		api.createContainer(container).always(function(){$('#startService').modal('hide');}).done(function(){showAlert(true, 'Container successfully created')}).fail(function(){showAlert(false, 'Could not create the container')});
	});
	$(document).on("click", "#createProto", function(){
		var proto = {name: $("#s-name").val(), proto: $("#s-proto").val()};
		api.createProto(proto).done(function(){showAlert(true, 'Message description successfully uploaded')}).fail(function(){showAlert(false, 'Could not upload the message description')});
	});
	$(document).on("click", ".delete-proto", function(){
		hash = $(this).data('id');
		api.deleteProto(hash).done(function(){showAlert(true, 'Message description successfully deleted')}).fail(function(){showAlert(false, 'Could not delete the message description')});
	});
	
	$(document).on("click", ".node > i.delete", function(){
		id = $(this).parent().attr('id');
		container = model.getContainer(id);
		if(confirm("Delete container "+container.name +"?")){
			container.deleted = true;
			api.deleteContainer(id).done(function(){
				instance.remove(id);
				$("#refresh").trigger('click');
				showAlert(true, 'Container successfully stopped.');
			}).fail(function(){showAlert(false, 'Could not delete the container.')});
		}
	});
	
	$(document).on("click", ".node > i.upgrade", function(){
		id = $(this).parent().attr('id');
		if(confirm("Upgrade container "+model.getContainer(id).name +"?"))
			api.upgradeContainer(id).done(function(){
				instance.remove(id);
				$("#refresh").trigger('click');
				showAlert(true, 'Succesfully upgraded the container')
			}).fail(function(){showAlert(false, 'Could not upgrade the container')});
	});
	
	$(document).on("click", ".node > i.logs", function(){
		id = $(this).parent().attr('id');
		window.open(rancherLink+"/infra/container-log?instanceId="+model.getContainer(id).container.split("/").pop()+"&isPopup=true","_blank","toolbars=0,width=700,height=715,left=200,top=200");
	});

	$.templates({
		  servicesTempl: "#servicesTemplate",
		  protosTempl: "#protosTemplate",
	      hostsTempl: "#hostsTemplate"
	});

	jsPlumb.ready(function() {
		var prefix = window.location.pathname.search(/console/) > 0 ? "connection-manager/" : "";
		
		instance = jsPlumb.getInstance({Container: "graph"});
		
		instance.registerEndpointType("disabled", { paintStyle: { strokeStyle: "red" } });
		
		var _createTextNode = function(text, isSmall) {
			var node = document.createElement("p");
			if(isSmall) {
				node.className = "small";
			}
			node.innerHTML = text;
			return node;
		}
		
		var _addEndpoint = function(endpoint) {
			// First check if a node with that ID already exists, then just return
			if(document.getElementById(endpoint.id) != null) {
				return;
			}
			
			container = model.getContainer(endpoint.id);
			// Now create the node
			var node = $('<div class="node" id="' + endpoint.id + '" style="background: '+container.color+'"><p><i class="fa fa-circle ' + ((typeof container.ip === "undefined") ? 'red' : 'green') + '"></i>' + container.name + '</p><p class="small">' + container.serviceName + ' ('+container.tag+')</p><p class="small" style="display: none">'+container.uuid+'</p><p class="small" style="display:none;">' + container.ip + '</p><i class="delete">x</i><i class="upgrade fa fa-refresh"></i><i class="logs fa fa-file-text-o"></i></i></div>');
			
			//node.bind("mouseenter", function(event){});
			
			for(key in endpoint.style) {
				node.css(key, endpoint.style[key]);
			}
			
			// Add the node to the document
			node.appendTo($("#graph"));
			
			// Make it draggable
			instance.draggable(node);
			
			// Now create the ports
			for(ix in endpoint.ports) {
				port = endpoint.ports[ix];
				var ep = instance.addEndpoint(endpoint.id, {
			        endpoint: "Dot",
			        isSource: true,
			        isTarget: true,
			        connector: [ "Bezier", { curviness: 50, gap: 10 }],
			        paintStyle: {
			            strokeStyle: "rgb(122,193,3)",
			            fillStyle: "rgb(122,193,3)",
			            radius: 15,
			            lineWidth: 3,
			        },
			        connectorStyle: {
			        	strokeStyle: "rgb(122,193,3)",
			        	lineWidth: 3,
			        },
			        anchor: ["Continuous", { faces:[ "bottom", "top" ] } ],
			        uuid: endpoint.id + ":" + port.id,
			        label: port.label,
			        parameters: { potentialTargets: port.potentialConnections },
			        maxConnections: port.isMultiple ? -1 : 1,
				});
			}
		}
		$(document).on('click', '#refresh', function(){
			$.when(model.loadModel()).then(function(){
				$("#graph").html('');
				instance = jsPlumb.getInstance({Container: "graph"});
				instance.registerEndpointType("disabled", { paintStyle: { strokeStyle: "red" } });
				instance.batch(function(){
					var g = new dagre.graphlib.Graph();
					g.setGraph({});
					g.setDefaultEdgeLabel(function() { return {}; });
					$.each(model.containers, function (i, n){
						g.setNode(n.uuid, {width: 225, height: 105});
					});
					$.each(model.links, function(i, e){
						g.setEdge(e.container1.uuid,e.container2.uuid);
					});
					dagre.layout(g);
					g.nodes().forEach(function(v) {
						$("#" + v).css("left", g.node(v).x + "px");
						$("#" + v).css("top", g.node(v).y + "px");
					});
					$.each(model.containers, function (id, container){
						endpoint = {id: container.uuid, ports: []};
						$.each(container.interfaces, function(id, interface){
							port = {id: id, label: interface.name, isMultiple: interface.cardinality == 0, potentialConnections: []};
							containers = model.getContainers(interface.subscribeHash, interface.publishHash);
							endpoint.ports.push(port);
						});
						endpoint.style = {"height":"90px", "width":"225px", "left": g.node(container.uuid).x, "top":g.node(container.uuid).y};
						   
						_addEndpoint(endpoint);
					});
					$.each(model.links, function (id, link){
						interface2 = model.getInterface(link.container2, link.interface2, link.interface1);
						interface1Id = -1;
						$.each(link.container1.interfaces, function(id, elem){
							if(elem.name == link.interfaces.name) {
								interface1Id = id;
							}
						});
						interface2Id = -1;
						$.each(link.container2.interfaces, function(id, elem){
							if(elem.name == interface2.name) {
								interface2Id = id;
							}
						});
						instance.connect({uuids: [link.container1.uuid+":"+interface1Id, link.container2.uuid+":"+interface2Id]});
					});
					instance.bind("beforeDrag", beforeDragHandler);
					
					instance.bind("connectionDragStop", connectionDragStopHandler);
					
					instance.bind("click", clickHandler);
					
					instance.bind("connection", connectionHandler);
				
					instance.bind("connectionDetached", connectionDetachedHandler);
					instance.repaintEverything();
				});
			});
		});
	});

	$('#refresh').trigger('click');
});

function deleteService(image, tag){
	api.deleteService(image, tag).done(function(){
		showAlert(true, 'Service succesfully deleted');
		$('#refresh').trigger('click');
	}).fail(function(){
		showAlert(false, 'Service cannot be deleted');
		$('#refresh').trigger('click');
	});
}

function showAlert(success, message){
	var alert = $('<div class="alert alert-'+(success ? 'success' : 'danger')+'" role="alert""><strong>'+(success ? 'Success' : 'Error')+'</strong> '+message+'</div>');
	alert.appendTo(".alerts");
	setTimeout(function(){alert.fadeOut(400, function(){$(this).remove()})}, 3000);
}