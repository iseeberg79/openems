package io.openems.edge.evcc_api.solartariff;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Predictor.PV.SolarForecastEvccModel", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PredictorSolarTariffEvccImpl extends AbstractPredictor
		implements
			Predictor,
			OpenemsComponent {

	private final Logger log = LoggerFactory
			.getLogger(PredictorSolarTariffEvccImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private PredictorSolarTariffEvccAPI solarForecastAPI;

	public PredictorSolarTariffEvccImpl() throws OpenemsNamedException {
		super(OpenemsComponent.ChannelId.values(),
				Controller.ChannelId.values(),
				PredictorSolarTariffEvcc.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws Exception {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(),
				this.config.enabled(), this.config.channelAddresses(),
				this.config.logVerbosity());

		// API-Initialisierung
		this.solarForecastAPI = new PredictorSolarTariffEvccAPI(config.url());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		try {
			log.info("SolarForecast.createNewPrediction wird aufgerufen.");
			Prediction prediction = solarForecastAPI.processSolarForecast();

			// Sicherstellen, dass die Channels aktualisiert werden
			if (prediction != null && prediction != Prediction.EMPTY_PREDICTION
					&& prediction.asArray() != null
					&& prediction.asArray().length > 0) {
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
						.setNextValue(true);
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.setNextValue(prediction.asArray()[0]);
				log.debug("Erstellte Vorhersage: " + prediction.asArray()[0]);
			} else {
				log.warn(
						"Keine gültige Vorhersage erhalten oder Werte nicht verfügbar.");
			}

			return prediction;
		} catch (Exception e) {
			log.error("Fehler bei der Erstellung der Vorhersage: ", e);
			return Prediction.EMPTY_PREDICTION;
		}
	}

	@Override
	public String debugLog() {
		return "Prediction: "
				+ this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.value().toString();
	}
}
