package io.openems.edge.evcc.vehicle;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Vehicle EVCC", //
		description = "Electric Vehicle integrated via EVCC")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "vehicle0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "EVCC API URL", description = "URL of the EVCC API (e.g. http://controller:7070/api/state)")
	String apiUrl() default "http://controller:7070/api/state";

	@AttributeDefinition(name = "Vehicle Name", description = "Name of the vehicle as configured in EVCC (e.g. 'e-Golf')")
	String vehicleName();

	@AttributeDefinition(name = "Loadpoint Index", description = "Index of the loadpoint this vehicle is connected to (0-based, default: 0)")
	int loadpointIndex() default 0;

	String webconsole_configurationFactory_nameHint() default "Vehicle EVCC [{id}]";
}
