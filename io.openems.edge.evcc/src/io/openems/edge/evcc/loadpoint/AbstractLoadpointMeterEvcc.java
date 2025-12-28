package io.openems.edge.evcc.loadpoint;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * Abstract base class for EVCC loadpoint meters.
 *
 * <p>
 * Provides common functionality for:
 * <ul>
 * <li>Loadpoint identification using title/index-based fallback strategy</li>
 * <li>HTTP lifecycle management</li>
 * <li>Offset-based energy calculation to avoid jumps on restart</li>
 * <li>Per-phase energy calculation</li>
 * </ul>
 */
public abstract class AbstractLoadpointMeterEvcc extends AbstractOpenemsComponent
		implements TimedataProvider, EventHandler {

	@Reference
	protected BridgeHttpFactory httpBridgeFactory;

	@Reference
	protected HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedata;

	protected BridgeHttp httpBridge;
	protected HttpBridgeCycleService cycleService;

	// Loadpoint reference configuration
	private String configuredTitle;
	private int configuredIndex;
	private boolean fallbackWarningLogged = false;

	/**
	 * State machine for offset-based energy calculation.
	 * Follows the same pattern as {@link CalculateEnergyFromPower}.
	 */
	protected static enum EnergyState {
		TIMEDATA_QUERY_NOT_STARTED, TIMEDATA_QUERY_IS_RUNNING, CALCULATE_ENERGY
	}

	protected EnergyState energyState = EnergyState.TIMEDATA_QUERY_NOT_STARTED;

	/**
	 * The first chargeTotalImport value received from EVCC (in Wh).
	 * Used as baseline for calculating energy delta.
	 */
	protected Long initialTotalImportWh = null;

	/**
	 * The last known ACTIVE_PRODUCTION_ENERGY value loaded from Timedata.
	 * Used to continue from the previous value after restart.
	 */
	protected Long lastKnownProductionEnergy = null;

	/**
	 * Energy calculator for V2G/export (negative power).
	 */
	protected final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	/**
	 * Energy calculators for each phase (L1, L2, L3).
	 */
	protected final CalculateEnergyFromPower calculateProductionEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	protected final CalculateEnergyFromPower calculateConsumptionEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	protected final CalculateEnergyFromPower calculateProductionEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	protected final CalculateEnergyFromPower calculateConsumptionEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	protected final CalculateEnergyFromPower calculateProductionEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);
	protected final CalculateEnergyFromPower calculateConsumptionEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	protected AbstractLoadpointMeterEvcc(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Activates the HTTP subscription for loadpoint data.
	 *
	 * @param context         the component context
	 * @param id              the component ID
	 * @param alias           the component alias
	 * @param enabled         whether the component is enabled
	 * @param apiUrl          the EVCC API URL
	 * @param loadpointTitle  the loadpoint title for matching
	 * @param loadpointIndex  the loadpoint index as fallback
	 * @param log             the logger instance
	 */
	protected void activateHttpSubscription(ComponentContext context, String id, String alias, boolean enabled,
			String apiUrl, String loadpointTitle, int loadpointIndex, Logger log) {
		super.activate(context, id, alias, enabled);
		this.initializeLoadpointReference(loadpointTitle, loadpointIndex);

		if (enabled && this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

			var jqFilter = this.buildLoadpointFilter(loadpointTitle, loadpointIndex);
			var url = UrlBuilder.parse(apiUrl) //
					.withQueryParam("jq", jqFilter) //
					.toEncodedString();
			this.logInfo(log, "Subscribing to loadpoint with filter: " + jqFilter);
			this.cycleService.subscribeJsonEveryCycle(url, this::processHttpResult);
		}
	}

	/**
	 * Deactivates the HTTP subscription.
	 */
	protected void deactivateHttpSubscription() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			this.calculateEnergyPerPhase();
			break;
		}
	}

	/**
	 * Processes the HTTP result from EVCC API.
	 * Subclasses must implement phase-specific handling.
	 *
	 * @param result the HTTP response
	 * @param error  the HTTP error, if any
	 */
	protected abstract void processHttpResult(HttpResponse<JsonElement> result, HttpError error);

	/**
	 * Returns the logger for this component.
	 *
	 * @return the logger
	 */
	protected abstract Logger getLogger();

	/**
	 * Returns the active power channel value.
	 *
	 * @return the active power in W, or null
	 */
	protected abstract Integer getActivePowerValue();

	/**
	 * Returns the active power value for phase L1.
	 *
	 * @return the active power L1 in W, or null
	 */
	protected abstract Integer getActivePowerL1Value();

	/**
	 * Returns the active power value for phase L2.
	 *
	 * @return the active power L2 in W, or null
	 */
	protected abstract Integer getActivePowerL2Value();

	/**
	 * Returns the active power value for phase L3.
	 *
	 * @return the active power L3 in W, or null
	 */
	protected abstract Integer getActivePowerL3Value();

	/**
	 * Sets the active production energy.
	 *
	 * @param value the energy in Wh
	 */
	protected abstract void setActiveProductionEnergy(Long value);

	/**
	 * Initializes the loadpoint reference configuration.
	 *
	 * @param title the configured loadpoint title (can be empty)
	 * @param index the configured loadpoint index
	 */
	protected void initializeLoadpointReference(String title, int index) {
		this.configuredTitle = title;
		this.configuredIndex = index;
	}

	/**
	 * Builds a JQ filter to select the loadpoint.
	 *
	 * @param title the configured loadpoint title
	 * @param index the configured loadpoint index
	 * @return JQ filter expression
	 */
	protected String buildLoadpointFilter(String title, int index) {
		if (title != null && !title.trim().isEmpty()) {
			var escapedTitle = title.trim().replace("\\", "\\\\").replace("\"", "\\\"");
			return "(.loadpoints[] | select(.title == \"" + escapedTitle + "\")) // .loadpoints[" + index + "]";
		}
		return ".loadpoints[" + index + "]";
	}

	/**
	 * Checks if the received loadpoint matches the configured title.
	 *
	 * @param lp     the loadpoint JSON object
	 * @param logger the logger to use for warnings
	 */
	protected void checkLoadpointMatch(JsonObject lp, Logger logger) {
		if (this.configuredTitle == null || this.configuredTitle.trim().isEmpty()) {
			return;
		}
		if (!lp.has("title")) {
			return;
		}
		var actualTitle = lp.get("title").getAsString();
		if (!this.configuredTitle.trim().equals(actualTitle) && !this.fallbackWarningLogged) {
			logger.warn(
					"Loadpoint title mismatch! Configured title='{}' + index=[{}], but received title='{}'. "
							+ "Using fallback to index.",
					this.configuredTitle, this.configuredIndex, actualTitle);
			this.fallbackWarningLogged = true;
		}
	}

	/**
	 * Updates ACTIVE_PRODUCTION_ENERGY using offset-based approach to avoid jumps.
	 *
	 * @param currentTotalImportWh the current chargeTotalImport value from EVCC in Wh
	 */
	protected void updateProductionEnergy(long currentTotalImportWh) {
		var log = this.getLogger();
		switch (this.energyState) {
		case TIMEDATA_QUERY_NOT_STARTED -> {
			this.initialTotalImportWh = currentTotalImportWh;

			var td = this.timedata;
			var componentId = this.id();
			if (td == null || componentId == null) {
				this.lastKnownProductionEnergy = 0L;
				this.energyState = EnergyState.CALCULATE_ENERGY;
			} else {
				this.energyState = EnergyState.TIMEDATA_QUERY_IS_RUNNING;
				td.getLatestValue(
						new ChannelAddress(componentId, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY.id()))
						.whenComplete((latestValueOpt, error) -> {
							if (error != null) {
								this.lastKnownProductionEnergy = 0L;
								this.logWarn(log, "Timedata query failed, starting from 0: " + error.getMessage());
							} else if (latestValueOpt.isPresent()) {
								try {
									this.lastKnownProductionEnergy = TypeUtils.getAsType(OpenemsType.LONG,
											latestValueOpt.get());
									this.logInfo(log, "Loaded last known production energy from Timedata: "
											+ this.lastKnownProductionEnergy + " Wh");
								} catch (IllegalArgumentException e) {
									this.lastKnownProductionEnergy = 0L;
									this.logWarn(log, "Failed to parse last known production energy: " + e.getMessage());
								}
							} else {
								this.lastKnownProductionEnergy = 0L;
								this.logInfo(log, "No previous production energy found in Timedata, starting from 0");
							}
							this.energyState = EnergyState.CALCULATE_ENERGY;
						});
			}
		}
		case TIMEDATA_QUERY_IS_RUNNING -> {
			// Wait for async Timedata response
		}
		case CALCULATE_ENERGY -> {
			long deltaWh = currentTotalImportWh - this.initialTotalImportWh;
			long baseEnergy = this.lastKnownProductionEnergy != null ? this.lastKnownProductionEnergy : 0L;
			long newEnergy = Math.max(0, baseEnergy + deltaWh);
			this.setActiveProductionEnergy(newEnergy);
		}
		}
	}

	/**
	 * Calculate consumption energy from negative power values.
	 */
	protected void calculateEnergy() {
		final var activePower = this.getActivePowerValue();
		if (activePower == null || activePower >= 0) {
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateConsumptionEnergy.update(Math.abs(activePower));
		}
	}

	/**
	 * Calculate energy per phase from phase-specific power values.
	 */
	protected void calculateEnergyPerPhase() {
		// L1
		final var activePowerL1 = this.getActivePowerL1Value();
		if (activePowerL1 == null) {
			this.calculateProductionEnergyL1.update(null);
			this.calculateConsumptionEnergyL1.update(null);
		} else if (activePowerL1 > 0) {
			this.calculateProductionEnergyL1.update(Math.abs(activePowerL1));
			this.calculateConsumptionEnergyL1.update(0);
		} else {
			this.calculateProductionEnergyL1.update(0);
			this.calculateConsumptionEnergyL1.update(Math.abs(activePowerL1));
		}

		// L2
		final var activePowerL2 = this.getActivePowerL2Value();
		if (activePowerL2 == null) {
			this.calculateProductionEnergyL2.update(null);
			this.calculateConsumptionEnergyL2.update(null);
		} else if (activePowerL2 > 0) {
			this.calculateProductionEnergyL2.update(Math.abs(activePowerL2));
			this.calculateConsumptionEnergyL2.update(0);
		} else {
			this.calculateProductionEnergyL2.update(0);
			this.calculateConsumptionEnergyL2.update(Math.abs(activePowerL2));
		}

		// L3
		final var activePowerL3 = this.getActivePowerL3Value();
		if (activePowerL3 == null) {
			this.calculateProductionEnergyL3.update(null);
			this.calculateConsumptionEnergyL3.update(null);
		} else if (activePowerL3 > 0) {
			this.calculateProductionEnergyL3.update(Math.abs(activePowerL3));
			this.calculateConsumptionEnergyL3.update(0);
		} else {
			this.calculateProductionEnergyL3.update(0);
			this.calculateConsumptionEnergyL3.update(Math.abs(activePowerL3));
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePowerValue();
	}
}
