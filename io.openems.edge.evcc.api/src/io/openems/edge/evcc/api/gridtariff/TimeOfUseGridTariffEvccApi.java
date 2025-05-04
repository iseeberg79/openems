package io.openems.edge.evcc.api.gridtariff;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

public class TimeOfUseGridTariffEvccApi {

	private static final Logger log = LoggerFactory
			.getLogger(TimeOfUseGridTariffEvccApi.class);
	private final HttpClient client;
	private final String apiUrl;

	public TimeOfUseGridTariffEvccApi(String apiUrl) {
		this.client = HttpClient.newBuilder()
				.connectTimeout(java.time.Duration.ofSeconds(5)).build();
		this.apiUrl = apiUrl;
	}

	/**
	 * Fetches time-of-use electricity prices from the API.
	 *
	 * This method sends an HTTP GET request to the configured API URL and retrieves 
	 * the response as a JSON string. If the request is successful (HTTP status code 
	 * 2xx), the response is parsed into a {@link TimeOfUsePrices} object. Otherwise, 
	 * it logs a warning and returns an empty prices object.
	 *
	 * The method applies a timeout of 5 seconds to both the connection and read operations.
	 *
	 * @return A {@link TimeOfUsePrices} object containing the parsed price data or an 
	 *         empty prices object if the request fails.
	 * @throws IOException If an I/O error occurs while sending the request.
	 * @throws InterruptedException If the request is interrupted before completion.
	 */
	public TimeOfUsePrices fetchPrices() {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.apiUrl))
				.GET().timeout(java.time.Duration.ofSeconds(5)).build();

		try {
			HttpResponse<String> response = this.client.send(request,
					HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300
					&& response.body() != null) {
				String json = response.body();
				return this.parsePrices(json);
			} else {
				log.warn("Failed to fetch prices. HTTP status code: {}",
						response.statusCode());
			}
		} catch (IOException | InterruptedException e) {
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
