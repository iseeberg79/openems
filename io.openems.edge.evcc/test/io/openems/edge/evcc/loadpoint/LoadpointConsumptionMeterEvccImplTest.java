package io.openems.edge.evcc.loadpoint;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Evcs;

public class LoadpointConsumptionMeterEvccImplTest {

	private static final String COMPONENT_ID = "loadpoint0";

	@Test
	public void testActivation() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		final String jsonResponse = """
				{
				  "title": "Garage",
				  "mode": "pv",
				  "charging": true,
				  "enabled": true,
				  "connected": true,
				  "chargePower": 10350,
				  "chargeCurrents": [15.2, 15.1, 15.3],
				  "chargeVoltages": [227.0, 228.5, 226.8],
				  "phasesActive": 3,
				  "sessionEnergy": 4820,
				  "chargeTotalImport": 5707.201,
				  "vehicleSoc": 65,
				  "vehicleName": "e-Golf"
				}
				""";

		final var url = "http://evcc:7070/api/state";
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().contains("api/state")) {
				return HttpResponse.ok(jsonResponse);
			}
			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();
		final var factory = ofBridgeImpl(() -> endpointFetcher, () -> executor);
		final var cycleSubscriber = new DummyCycleSubscriber();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", factory)
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(cycleSubscriber))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl(url)
						.setLoadpointTitle("Garage")
						.build())
				.next(new TestCase("Check EVCS initialization")
						.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false))
				.deactivate();
	}

	@Test
	public void testCommunicationError() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();

		final var url = "http://evcc:7070/api/state";
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();
		final var factory = ofBridgeImpl(() -> endpointFetcher, () -> executor);
		final var cycleSubscriber = new DummyCycleSubscriber();

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", factory)
				.addReference("httpBridgeCycleServiceDefinition", new HttpBridgeCycleServiceDefinition(cycleSubscriber))
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl(url)
						.build())
				.next(new TestCase("Trigger cycle")
						.onAfterProcessImage(() -> cycleSubscriber.triggerNextCycle()))
				.next(new TestCase("Process HTTP error")
						.onAfterProcessImage(() -> executor.update()))
				.next(new TestCase("Check communication failed flag")
						.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, true))
				.deactivate();
	}
}
