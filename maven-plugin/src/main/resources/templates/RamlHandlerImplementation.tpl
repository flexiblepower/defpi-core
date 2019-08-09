	/**
	 * Default implementation of the function to handles messages of type {{handle.type}} on the interface. 
	 * By default it is relayed to the RamlRequestHandler. If required this function can be overridden in 
	 * the implementing class.
	 * 
	 * @param message The RAML message to be handled
	 */
	default public void handle{{handle.type}}Message({{handle.type}} message) {
        RamlRequestHandler.handle(this, message);
	}