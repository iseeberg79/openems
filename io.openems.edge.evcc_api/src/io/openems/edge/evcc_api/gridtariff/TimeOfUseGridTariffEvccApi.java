package io.openems.edge.evcc_api.gridtariff;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TimeOfUseGridTariffEvccApi {

	private static final Logger log = LoggerFactory
			.getLogger(TimeOfUseGridTariffEvccApi.class);
	private final OkHttpClient client = new OkHttpClient();
	private final String apiUrl;
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(
			TimeOfUsePrices.EMPTY_PRICES);

	public TimeOfUseGridTariffEvccApi(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public TimeOfUsePrices fetchPrices() {
		try (Response response = client
				.newCall(new Request.Builder().url(apiUrl).build()).execute()) {
			if (response.isSuccessful() && response.body() != null) {
				String json = response.body().string();
				return parsePrices(json);
			} else {
				log.warn("Failed to fetch prices. HTTP status code: {}",
						response.code());
			}
		} catch (IOException e) {
			log.error("Error while fetching prices", e);
		}
		return TimeOfUsePrices.EMPTY_PRICES;
	}

	private TimeOfUsePrices parsePrices(String jsonData) {
		try {
			JsonObject jsonObject = JsonParser.parseString(jsonData)
					.getAsJsonObject();
			JsonObject resultObject = jsonObject.getAsJsonObject("result");
			var ratesArray = resultObject.getAsJsonArray("rates");

			var result = ImmutableSortedMap
					.<ZonedDateTime, Double>naturalOrder();

			if (ratesArray.size() < 2) {
				log.warn(
						"Zu wenige Werte in der API-Antwort. Erwartet mindestens zwei.");
				return TimeOfUsePrices.EMPTY_PRICES;
			}

			// Ersten beiden Werte auslesen zur Intervallbestimmung
			JsonObject firstRate = ratesArray.get(0).getAsJsonObject();
			JsonObject secondRate = ratesArray.get(1).getAsJsonObject();

			ZonedDateTime firstStart = ZonedDateTime.parse(
					firstRate.get("start").getAsString(),
					DateTimeFormatter.ISO_DATE_TIME);
			ZonedDateTime secondStart = ZonedDateTime.parse(
					secondRate.get("start").getAsString(),
					DateTimeFormatter.ISO_DATE_TIME);

			long intervalMinutes = java.time.Duration
					.between(firstStart, secondStart).toMinutes();
			boolean isHourly = (intervalMinutes == 60);
			boolean isQuarterHourly = (intervalMinutes == 15);

			if (!isHourly && !isQuarterHourly) {
				log.warn(
						"Unerwartetes Zeitintervall zwischen den Werten: {} Minuten. Daten werden nicht übernommen.",
						intervalMinutes);
				return TimeOfUsePrices.EMPTY_PRICES;
			}

			// Werte korrekt übernehmen
			for (JsonElement rateElement : ratesArray) {
				if (rateElement.isJsonObject()) {
					JsonObject rateObject = rateElement.getAsJsonObject();
					ZonedDateTime startsAt = ZonedDateTime.parse(
							rateObject.get("start").getAsString(),
							DateTimeFormatter.ISO_DATE_TIME);

					// Überprüfung: Existiert `price` oder `value`?
					double priceOrValue = rateObject.has("price")
							? rateObject.get("price").getAsDouble() * 1000
							: rateObject.has("value")
									? rateObject.get("value").getAsDouble()
											* 1000
									: 0; // Fallback, falls keiner vorhanden ist

					if (isHourly) {
						result.put(startsAt, priceOrValue);
					} else if (isQuarterHourly) {
						result.put(startsAt, priceOrValue);
					}
				}
			}

			return TimeOfUsePrices.from(result.build());
		} catch (Exception e) {
			log.error("Fehler beim Parsen der EVCC API-Daten", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

}
