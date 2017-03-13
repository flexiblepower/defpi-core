			handlers.put("{{name}}",new InterfaceHandler(
					"{{subscribe}}", 
					"{{publish}}", 
					getFactory("{{package}}.handlers", "{{name}}").getDeclaredConstructor(Service.class).newInstance(this)));
