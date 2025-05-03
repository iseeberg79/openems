package io.openems.edge.evcc_api.solartariff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.predictor.api.prediction.Prediction;

public class PredictorSolarTariffEvccAPI {

	private final String apiUrl;
	private ZonedDateTime lastFetchTime = null;
	private JsonArray cachedForecast = null;

	public PredictorSolarTariffEvccAPI(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public Prediction processSolarForecast() throws OpenemsNamedException {
		JsonArray forecastData = getSolarForecast();
		return transformForecastData(forecastData);
	}

	private JsonArray getSolarForecast() throws OpenemsNamedException {
		ZonedDateTime currentHour = ZonedDateTime.now(ZoneId.of("UTC"))
				.withMinute(0).withSecond(0).withNano(0);

		// **Prüfen, ob bereits ein Abruf in dieser Stunde erfolgt ist**
		if (cachedForecast != null && lastFetchTime != null
				&& currentHour.isEqual(lastFetchTime)) {
			return cachedForecast;
		}

		// **Falls nein, neue Daten abrufen**
		JsonObject jsonResponse = sendGetRequest(apiUrl);
		if (jsonResponse != null && jsonResponse.has("result")) {
			cachedForecast = jsonResponse.getAsJsonObject("result")
					.getAsJsonArray("rates");
			lastFetchTime = currentHour;
			return cachedForecast;
		} else {
			throw new OpenemsException(
					"Ungültige oder leere Antwort von der API.");
		}
	}

	private JsonObject sendGetRequest(String urlString)
			throws OpenemsNamedException {
		try {
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			int status = con.getResponseCode();
			String body;
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()))) {
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
				}
				body = content.toString();
			}
			if (status < 300) {
				return JsonUtils.parseToJsonObject(body);
			} else {
				throw new OpenemsException(
						"Fehlerhafte API-Antwort. Response-Code: " + status
								+ ". " + body);
			}
		} catch (IOException e) {
			throw new OpenemsException(
					"Fehler beim Verbindungsaufbau zur Solar Forecast API.");
		}
	}

	private Prediction transformForecastData(JsonArray forecastData) {
		TreeMap<ZonedDateTime, Integer> solarData = new TreeMap<>();
		ZonedDateTime previousTime = null;

		for (JsonElement element : forecastData) {
			JsonObject jsonObject = element.getAsJsonObject();
			ZonedDateTime zonedDateTime = ZonedDateTime
					.parse(jsonObject.get("start").getAsString())
					.withZoneSameInstant(ZoneId.of("UTC"));

			// **Prüfung, ob 'value' oder 'price' vorhanden ist**
			Integer power = jsonObject.has("value")
					? jsonObject.get("value").getAsInt()
					: (jsonObject.has("price")
							? jsonObject.get("price").getAsInt()
							: null);

			if (power != null) {
				if (previousTime != null) {
					long minutesBetween = java.time.Duration
							.between(previousTime, zonedDateTime).toMinutes();

					if (minutesBetween == 60) {
						// Berechnung für 4x15 Minuten (geliefert in Wh, daher
						// ohne Umrechnung!)
						// int interpolatedValue = power / 4;
						solarData.put(previousTime.plusMinutes(0), power);
						solarData.put(previousTime.plusMinutes(15), power);
						solarData.put(previousTime.plusMinutes(30), power);
						solarData.put(previousTime.plusMinutes(45), power);
					} else if (minutesBetween == 15) {
						// Werte direkt übernehmen
						solarData.put(zonedDateTime, power);
					}
				}
			}
			previousTime = zonedDateTime;
		}

		// **Umwandlung ins Zielformat (4x15 min pro Stunde)**
		Integer[] values = new Integer[192];
		int i = 0;
		for (Entry<ZonedDateTime, Integer> entry : solarData.entrySet()) {
			if (i < values.length) {
				values[i++] = entry.getValue();
			}
		}

		return Prediction.from(ZonedDateTime.now(ZoneId.of("UTC")).withMinute(0)
				.withSecond(0).withNano(0), values);
	}
}
