var model = {
	containers: [], 
	services: [], 
	links: [], 
	protos: [],
	hosts: [],
	colors: ["#5757FF", "#800080", "#59955C", "#d3d6c8", "#b4c6e4", "#9ad0bf", "#b59f82", "#9bbcf3", "#da88ce", "#fae593", "#a1b283", "#da80e7", "#8ff6b2", "#a89281", "#9cf3d4", "#c5ccec", "#a2d88f", "#a687fa", "#f68db3", "#f0d8cc"],
	getContainers: function(subscribeHash, publishHash){
		var c = [];
		$.each(model.containers, function(key, container){
			tmp = $.grep(container.interfaces, function(interface){
				return interface.subscribeHash == publishHash && interface.publishHash == subscribeHash;
			});
			if(tmp.length > 0){
				c.push(container)
			}
		});
		return c;
	},
	getService: function(image){
		return $.grep(model.services, function(elem){
			return elem.image == image.replace(dockerPrefix,"").split(":")[0];
		})[0];
	},
	getContainer: function(uuid){
		return $.grep(model.containers, function(elem){
			return elem.uuid == uuid;
		})[0];
	},
	getLink: function(id){
		return $.grep(model.links, function(elem){
			return elem.id == id;
		})[0];
	},
	queryLink: function(container1, container2, subscribeHash, publishHash){
		return $.grep(model.links, function(elem){
			return elem.container1.uuid == container1 && elem.container2.uuid == container2
				&& elem.interface1 == subscribeHash && elem.interface2 == publishHash;
		});
	},
	getInterface: function(container, subscribeHash, publishHash){
		return $.grep(container.interfaces, function(interface){
				return interface.subscribeHash == subscribeHash && interface.publishHash == publishHash;
		})[0];
	},
	getProto: function(hash){
		return $.grep(model.protos, function(elem){
			return elem.sha256 == hash;
		})[0];
	},
	
	loadModel: function(){
		return $.when(
				api.getContainers(),
				api.getLinks(),
				api.getProtos(),
				api.getServices(),
				api.getHosts())
		.then(function(containers, links, protos, services, hosts){
			containers = containers[0].containers;
			links = links[0].links;
			protos = protos[0].protos;
			model.services = services[0].services;
			model.hosts = hosts[0].hosts;
			$("#host").html('<option value="">Pick random public host</option>');
			$.each(model.hosts, function(id, host){
				host.color = model.colors[id%20];
				$("#host").append('<option value="'+host.id+'">'+host.hostname+' ('+host.labels.type+')</option>');
			});
			model.protos = [];
			$.each(protos, function(id, proto){
				model.protos.push(proto);
			});
			
			$.each(model.services, function(id, service){
				service.tags = service.tags.sort(function(a, b){
					  var aDate = new Date(a.created);
					  var bDate = new Date(b.created); 
					  return ((aDate < bDate) ? 1 : ((aDate > bDate) ? -1 : 0));
					});
				$.each(service.tags, function(id2, tag){
					$.each(tag.interfaces, function(id3, interface){
						subscribe = model.getProto(interface.subscribeHash);
						publish = model.getProto(interface.publishHash);
						interface.subscribeName = (typeof subscribe !== "undefined") ? subscribe.name : interface.subscribeHash;
						interface.publishName = (typeof publish !== "undefined") ? publish.name : interface.publishHash;
					})
				})
			})
			
			model.containers = [];
			$.each(containers, function(id, container){
				container.color = $.grep(model.hosts, function(elem){return elem.id == container.host})[0].color;
				model.containers.push(container);
				imageSplit = container.image.replace(dockerPrefix,"").split(":");
				if(typeof imageSplit[1] === "undefined"){
					container.tag = "latest";
				}else{
					container.tag = imageSplit[1];
				}
				container.serviceName = model.getService(container.image).name;
			});
					
			model.links = [];
			$.each(links, function(id, link){
				linkobj = {};
				model.links.push(link);
				linkobj = link;
				linkobj.container1 = model.getContainer(link.container1);
				linkobj.container2 = model.getContainer(link.container2);
				linkobj.interfaces = $.grep(linkobj.container1.interfaces, function (interface){
					return interface.subscribeHash == linkobj.interface1 && interface.publishHash == linkobj.interface2;
				})[0];
			});
			$.templates.protosTempl.link("#protos", model);
			$.observable(model.protos).refresh(model.protos);
			$.templates.servicesTempl.link("#services", model);
			$.observable(model.services).refresh(model.services);
			$.templates.hostsTempl.link("#hosts", model);
			$.observable(model.hosts).refresh(model.hosts);
		});
	}
};