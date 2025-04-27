package io.openems.edge.evcc_api.gridtariff;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Grid.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseGridTariffEvccImpl extends AbstractOpenemsComponent
		implements
			TimeOfUseTariff,
			OpenemsComponent,
			TimeOfUseGridTariffEvcc {

	private final Logger log = LoggerFactory
			.getLogger(TimeOfUseGridTariffEvccImpl.class);
	private final ScheduledExecutorService executor = Executors
			.newSingleThreadScheduledExecutor();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(
			TimeOfUsePrices.EMPTY_PRICES);

	private String apiURL = "http://localhost:7070/api/tariff/grid";

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	public TimeOfUseGridTariffEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseGridTariffEvcc.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		System.out.println("Aufruf von activate...");
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.apiURL = config.apiUrl();
		System.out.println("URL: " + this.apiURL);
		this.executor.schedule(this.task, 0, TimeUnit.SECONDS);
	}

	@Deactivate
	protected void deactivate() {
		System.out.println("Aufruf von deactivate...");
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	protected final Runnable task = () -> {
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(apiURL) //
				.build();

		int httpStatusCode = 0;
		boolean timeoutOccurred = false;
		boolean serverError = false;

		try (var response = client.newCall(request).execute()) {
			httpStatusCode = response.code();
			this.channel(TimeOfUseGridTariffEvcc.ChannelId.HTTP_STATUS_CODE)
					.setNextValue(httpStatusCode);

			if (response.isSuccessful() && response.body() != null) {
				var json = response.body().string();
				this.prices.set(parsePrices(json));
			} else {
				log.warn("Failed to fetch prices. HTTP status code: {}",
						httpStatusCode);
				if (httpStatusCode >= 500) {
					serverError = true;
				}
			}
		} catch (IOException e) {
			log.error("Error while fetching prices", e);
			timeoutOccurred = true;
		}

		this.channel(TimeOfUseGridTariffEvcc.ChannelId.STATUS_TIMEOUT)
				.setNextValue(timeoutOccurred);
		this.channel(TimeOfUseGridTariffEvcc.ChannelId.STATUS_SERVER_ERROR)
				.setNextValue(serverError);
	};

	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(), this.prices.get());
	}

	private TimeOfUsePrices parsePrices(String jsonData) {
		System.out.println("Aufruf von parsePrices...");
		try {
			// Parse JSON root object
			var jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
			var resultObject = jsonObject.getAsJsonObject("result");
			var ratesArray = resultObject.getAsJsonArray("rates");

			// Prepare ImmutableSortedMap for 15-minute intervals
			var result = ImmutableSortedMap
					.<ZonedDateTime, Double>naturalOrder();

			for (JsonElement rateElement : ratesArray) {
				// Ensure rateElement is a JsonObject
				if (rateElement.isJsonObject()) {
					JsonObject rateObject = rateElement.getAsJsonObject();

					// Extract necessary fields
					String startString = rateObject.get("start").getAsString();
					String endString = rateObject.get("end").getAsString();
					double value = rateObject.get("value").getAsDouble() * 1000; // Convert
																					// to
																					// Currency/MWh

					ZonedDateTime startsAt = ZonedDateTime
							.parse(startString, DateTimeFormatter.ISO_DATE_TIME)
							.withZoneSameInstant(ZonedDateTime.now().getZone());
					ZonedDateTime endsAt = ZonedDateTime
							.parse(endString, DateTimeFormatter.ISO_DATE_TIME)
							.withZoneSameInstant(ZonedDateTime.now().getZone());

					// Divide the hourly price evenly into four 15-minute
					// intervals
					long duration = Duration.between(startsAt, endsAt)
							.toMinutes();
					if (duration == 60) {
						for (int i = 0; i < 4; i++) {
							ZonedDateTime quarterStart = startsAt
									.plusMinutes(i * 15);
							result.put(quarterStart, value);
						}
					} else {
						throw new IllegalArgumentException(
								"Unexpected duration for rate: " + duration
										+ " minutes");
					}
				} else {
					log.error("Rate element is not a JsonObject: {}",
							rateElement);
				}
			}

			System.out.println("parsePrices ist erledigt:");
			System.out.println(TimeOfUsePrices.from(result.build()).toString());
			
			return TimeOfUsePrices.from(result.build());
		} catch (Exception e) {
			log.error("Failed to parse EVCC API data", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
