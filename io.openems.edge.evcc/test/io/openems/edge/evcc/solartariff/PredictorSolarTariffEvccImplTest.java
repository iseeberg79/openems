package io.openems.edge.evcc.solartariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.Prediction;

public class PredictorSolarTariffEvccImplTest {

	@Test
	public void testSolarTariffPrediction() throws Exception {
		final var sut = new PredictorSolarTariffEvccImpl();
		final var api = new PredictorSolarTariffEvccApi();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = createDummyClock();
		
		Random random = new Random();
		int randomHours = 6 + random.nextInt(13);
		clock.leap(randomHours, ChronoUnit.HOURS);
		api.setClock(clock);

		new ComponentTest(sut).addReference("httpBridgeFactory", httpTestBundle.factory())
				.addReference("componentManager", new DummyComponentManager(clock))
				.activate(MyConfig.create().setId("predictor0").setUrl("http://evcc:7070/api/tariff/solar")
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS).build())

				// Case: API not found
				.next(new TestCase("API Not Found").onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					httpTestBundle.triggerNextCycle();
				}).onAfterProcessImage(() -> {
					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));
					assertNotEquals(true, sut.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED).value());
				}))

				// Case: API unknown error
				.next(new TestCase("API Unknown Error").onBeforeProcessImage(() -> {
					httpTestBundle
							.forceNextFailedResult(new HttpError.UnknownError(new Exception("Simulated failure")));
					httpTestBundle.triggerNextCycle();
				}).onAfterProcessImage(() -> {
					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));
					assertNotEquals(true, sut.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED).value());
				}))

				// Case: simulated API processing
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					String jsonResponse = generateDynamicJson(clock);
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(jsonResponse));
					httpTestBundle.triggerNextCycle();

					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));

					Prediction prediction = api.parsePrediction(jsonResponse);
					LocalDateTime localCurrentHour = LocalDateTime.now(clock).withSecond(0).withNano(0).withMinute(0);
					ZoneId zoneId = ZoneId.of("UTC");
					ZonedDateTime currentHour = localCurrentHour.atZone(zoneId);

					int expectedFirstValue = getSolarPredictionValue(currentHour);
					int expectedSecondHourValue = getSolarPredictionValue(currentHour.plusHours(1));

					Integer[] predictions = prediction.asArray();
					assertEquals(8, predictions.length);
					assertEquals(currentHour, prediction.getFirstTime());
					assertEquals(expectedFirstValue, predictions[0].intValue());
					assertEquals(expectedFirstValue, predictions[1].intValue());
					assertEquals(expectedFirstValue, predictions[2].intValue());
					assertEquals(expectedFirstValue, predictions[3].intValue());
					assertEquals(expectedSecondHourValue, predictions[4].intValue());
				})).deactivate();
	}

	private String generateDynamicJson(Clock clock) {
		ZonedDateTime now = ZonedDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
		ZonedDateTime endTime = now.plusHours(2);

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{ \"result\": { \"rates\": [");

		for (ZonedDateTime time = now; time.isBefore(endTime); time = time.plusHours(1)) {
			jsonBuilder.append(String.format("""
					    { "start": "%s", "end": "%s", "value": %d },
					""", time, time.plusHours(1), getSolarPredictionValue(time)));
		}

		jsonBuilder.setLength(jsonBuilder.length() - 2);
		jsonBuilder.append("]}}");

		return jsonBuilder.toString();
	}

	// simulate solar production
	private int getSolarPredictionValue(ZonedDateTime time) {
		int baseValue = time.getHour();
		int factor = time.getHour() > 6 && time.getHour() < 18 ? (time.getHour() - 6) * 500 : 0;
		return baseValue + factor;
	}
}
