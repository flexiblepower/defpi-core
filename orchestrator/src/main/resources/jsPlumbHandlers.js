function beforeDragHandler(params) {
	instance.batch(function() {
		instance.selectEndpoints().setEnabled(false);
		instance.selectEndpoints().setType("disabled");

		params.endpoint.clearTypes();
		container = model.getContainer(params.sourceId);
		interface = container.interfaces[params.endpoint.getUuid().split(":")[1]];
		containers = model.getContainers(interface.subscribeHash, interface.publishHash);
		$.each(containers, function (key, potentialContainer){
			if(potentialContainer.uuid != container.uuid){
				$.each(potentialContainer.interfaces, function(id, interface2){
					if(interface2.publishHash === interface.subscribeHash && interface2.subscribeHash === interface.publishHash){
						var target = instance.getEndpoint(potentialContainer.uuid+":"+id);
						target.setEnabled(true);
						target.clearTypes()
					}
				});
			}
		});
	});
	return true;
}

function connectionDragStopHandler(params) {
	instance.selectEndpoints().setEnabled(true);
	instance.selectEndpoints().each(function(ep) {
		ep.clearTypes();
	});
}

function clickHandler(conn, originalEvent) {
	if ($.grep(instance.getConnections(), function(elem) {
		return elem.id == conn.id
	}).length > 0
			&& confirm("Delete connection from " + conn.sourceId + " to "
					+ conn.targetId + "?"))
		instance.detach(conn);
}

function connectionHandler(params) {
	uuid1 = params.sourceEndpoint.getUuid().split(":");
	uuid2 = params.targetEndpoint.getUuid().split(":");
	if(uuid1[0] === uuid2[0]){
		instance.detach(params.connection);
	}else{
		if(typeof model.getContainer(uuid1[0]).ip === 'undefined' || 
				typeof model.getContainer(uuid2[0]).ip === 'undefined'){
			showAlert(false, 'One of the containers is not ready yet');
			instance.detach(params.connection);
		}else{
			interface = model.getContainer(uuid1[0]).interfaces[uuid1[1]];
			exists = model.queryLink(uuid1[0], uuid2[0], interface.subscribeHash, interface.publishHash)
			if(exists.length == 0){
				link = {
						container1: uuid1[0],
						container2: uuid2[0],
						interface1: interface.subscribeHash,
						interface2: interface.publishHash
				};
				api.createLink(link).done(function(){
					showAlert(true, 'Link successfully created')
					link.container1 = {uuid: link.container1};
					link.container2 = {uuid: link.container2};
					model.links.push(link);
				}).fail(function(){
					showAlert(false, 'Link could not be created'); instance.detach(params.connection);
				});
			}
		}
	}
}

function connectionDetachedHandler(params) {
	uuid1 = params.sourceEndpoint.getUuid().split(":");
	uuid2 = params.targetEndpoint.getUuid().split(":");
	container1 = model.getContainer(uuid1[0]);
	container2 = model.getContainer(uuid2[0]);
	if(container1.deleted == true || container2.deleted == true){
		console.log("Container removed");
		return;
	}
	interface = model.getContainer(uuid1[0]).interfaces[uuid1[1]];
	exists = model.queryLink(uuid1[0], uuid2[0], interface.subscribeHash, interface.publishHash);
	if(exists.length > 0){
		console.log("Actually deleting");
		model.links = $.grep(model.links, function(elem){
			return elem.id !== exists[0].id;
		});
		api.deleteLink(exists[0].id).done(function(){showAlert(true, 'Link successfully deleted')}).fail(function(){showAlert(false, 'Could not delete link')});
	}
}