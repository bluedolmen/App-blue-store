function main() {
	
	var 
		uri = '/bluedolmen/mp/status?guest=true',
		connector = remote.connect("alfresco"),
		result = connector.get(encodeURI(uri))
	;
	
	if (result.status.code != status.STATUS_OK) {
		status.code = result.status.code;
		status.message = msg.get("message.failure");
		status.redirect = true;
		return;
	}
		
	var appstore = eval("(" + result.response + ")");
	
	return jsonUtils.toJSONString(appstore);
   
}

model.appstore = main();

