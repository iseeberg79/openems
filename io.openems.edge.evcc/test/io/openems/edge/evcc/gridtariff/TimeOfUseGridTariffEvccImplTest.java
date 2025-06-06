package io.openems.edge.evcc.gridtariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.currency.Currency.EUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class TimeOfUseGridTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new TimeOfUseGridTariffEvccImpl();
		final var api = new TimeOfUseGridTariffEvccApi();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = createDummyClock();
		final var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeofusetariff0") //
						.setApiUrl("http://evcc:7070/api/tariff/grid") //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()) //

				// Case: Invalid API response
				.next(new TestCase("Invalid API response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> {
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, sut.getPrices());
						})) //

				// Case: API Not Found (404)
				.next(new TestCase("API Not Found") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> {
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, sut.getPrices());
						})) //

				// Case: API Unknown Error
				.next(new TestCase("API Unknown Error") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(
									new HttpError.UnknownError(new Exception("Simulated failure")));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> {
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, sut.getPrices());
						}))

				// 60 minutes
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					// simulate response
					ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
					String jsonResponse = String.format("""
							    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
							""", now, now.plusMinutes(60));

					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(jsonResponse));
					httpTestBundle.triggerNextCycle();
					// simulate parsing
					TimeOfUsePrices prices = api.parsePrices(jsonResponse);
					assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
					assertEquals(prices.asArray().length, 4);
					assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
				}))//
				
				// 30 minnutes
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					// simulate response
					ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
					String jsonResponse = String.format("""
							    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
							""", now, now.plusMinutes(30));

					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(jsonResponse));
					httpTestBundle.triggerNextCycle();
					// simulate parsing
					TimeOfUsePrices prices = api.parsePrices(jsonResponse);
					assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
					assertEquals(prices.asArray().length, 2);
					assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
				}))//
				
				// 15 minutes
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					// simulate response
					ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
					String jsonResponse = String.format("""
							    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
							""", now, now.plusMinutes(15));

					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(jsonResponse));
					httpTestBundle.triggerNextCycle();
					// simulate parsing
					TimeOfUsePrices prices = api.parsePrices(jsonResponse);
					assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
					assertEquals(prices.asArray().length, 1);
					assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
				}))//
				
				.deactivate();
	}
}
