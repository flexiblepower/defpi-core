	@Override
	public {{vitf.handler.interface}} build{{vitf.version}}(Connection connection) {
		return new {{vitf.handler.class}}(connection, this.service);
	}