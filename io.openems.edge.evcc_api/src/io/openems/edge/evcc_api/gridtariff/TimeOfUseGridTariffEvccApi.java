package io.openems.edge.evcc_api.gridtariff;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
					double value = rateObject.has("price") 
						    ? rateObject.get("price").getAsDouble() * 1000 
						    : rateObject.get("value").getAsDouble() * 1000; // Convert to Currency/MWh

					ZonedDateTime startsAt = ZonedDateTime
							.parse(startString, DateTimeFormatter.ISO_DATE_TIME)
							.withZoneSameInstant(ZonedDateTime.now().getZone());
					ZonedDateTime endsAt = ZonedDateTime
							.parse(endString, DateTimeFormatter.ISO_DATE_TIME)
							.withZoneSameInstant(ZonedDateTime.now().getZone());

					long duration = Duration.between(startsAt, endsAt)
							.toMinutes();

					switch ((int) duration) {
						case 60 :
							for (int i = 0; i < 4; i++) {
								ZonedDateTime quarterStart = startsAt
										.plusMinutes(i * 15);
								result.put(quarterStart, value);
							}
							break;
						case 15 :
							result.put(startsAt, value);
							break;
						default :
							throw new IllegalArgumentException(
									"Unexpected duration for rate: " + duration
											+ " minutes");
					}
				} else {
					log.error("Rate element is not a JsonObject: {}",
							rateElement);
				}
			}

			return TimeOfUsePrices.from(result.build());
		} catch (Exception e) {
			log.error("Failed to parse EVCC API data", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

}
