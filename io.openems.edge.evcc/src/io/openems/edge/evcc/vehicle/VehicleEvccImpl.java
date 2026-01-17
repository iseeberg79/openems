package io.openems.edge.evcc.vehicle;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Vehicle.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class VehicleEvccImpl extends AbstractOpenemsComponent implements Vehicle, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(VehicleEvccImpl.class);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;

	private String configuredVehicleName;

	public VehicleEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Vehicle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.configuredVehicleName = config.vehicleName();

		if (config.enabled() && this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

			// Query EVCC for loadpoint with matching vehicle name
			var escapedName = this.configuredVehicleName.replace("\\", "\\\\").replace("\"", "\\\"");
			var jqFilter = ".loadpoints[] | select(.vehicleName == \"" + escapedName + "\")";
			var url = UrlBuilder.parse(config.apiUrl()) //
					.withQueryParam("jq", jqFilter) //
					.toEncodedString();
			this.logInfo(this.log, "Subscribing to vehicle data for: " + this.configuredVehicleName);
			this.cycleService.subscribeJsonEveryCycle(url, this::processHttpResult);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		if (error != null) {
			this.logDebug(this.log, error.getMessage());
			return;
		}

		try {
			var lp = getAsJsonObject(result.data());

			if (lp.has("vehicleName") && !lp.get("vehicleName").isJsonNull()) {
				this._setVehicleName(lp.get("vehicleName").getAsString());
			} else {
				this._setVehicleName(null);
			}

			if (lp.has("vehicleSoc") && !lp.get("vehicleSoc").isJsonNull()) {
				this._setSoc((int) Math.round(getAsDouble(lp, "vehicleSoc")));
			} else {
				this._setSoc(null);
			}

			if (lp.has("vehicleRange") && !lp.get("vehicleRange").isJsonNull()) {
				this._setRange(getAsInt(lp, "vehicleRange"));
			} else {
				this._setRange(null);
			}

			if (lp.has("vehicleOdometer") && !lp.get("vehicleOdometer").isJsonNull()) {
				this._setOdometer(getAsInt(lp, "vehicleOdometer"));
			} else {
				this._setOdometer(null);
			}

			boolean connected = lp.has("connected") && lp.get("connected").getAsBoolean();
			this.channel(Vehicle.ChannelId.CONNECTED).setNextValue(connected);

			boolean charging = lp.has("charging") && lp.get("charging").getAsBoolean();
			this.channel(Vehicle.ChannelId.CHARGING).setNextValue(charging);

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse EVCC vehicle data: {}", e.getMessage());
		}
	}
}
