package io.openems.edge.evcc.loadpoint;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.test.TestUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

public class LoadpointConsumptionMeterEvccImplTest {

	private static final String COMPONENT_ID = "loadpoint0";

	@Test
	public void testCharging3Phase() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();
		final var clock = TestUtils.createDummyClock();

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

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", factory)
				.addReference("httpBridgeCycleServiceDefinition", HttpBridgeCycleServiceDefinition.INSTANCE)
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl(url)
						.setLoadpointTitle("Garage")
						.build())
				.next(new TestCase("Charging 3-phase")
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterProcessImage(() -> executor.update()))
				.next(new TestCase("Verify values")
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 10350)
						.output(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES, 3)
						.output(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_SOC, 65)
						.output(LoadpointConsumptionMeterEvcc.ChannelId.PLUG, PlugState.CONNECTED))
				.deactivate();
	}

	@Test
	public void testIdle() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();
		final var clock = TestUtils.createDummyClock();

		final String jsonResponse = """
				{
				  "title": "Garage",
				  "mode": "pv",
				  "charging": false,
				  "enabled": false,
				  "connected": false,
				  "chargePower": 0,
				  "phasesActive": 0,
				  "sessionEnergy": 0,
				  "chargeTotalImport": 5702.381
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

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", factory)
				.addReference("httpBridgeCycleServiceDefinition", HttpBridgeCycleServiceDefinition.INSTANCE)
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl(url)
						.build())
				.next(new TestCase("Idle state")
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterProcessImage(() -> executor.update()))
				.next(new TestCase("Verify idle values")
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
						.output(LoadpointConsumptionMeterEvcc.ChannelId.PLUG, PlugState.UNPLUGGED))
				.deactivate();
	}

	@Test
	public void testCommunicationError() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl();
		final var clock = TestUtils.createDummyClock();

		final var url = "http://evcc:7070/api/state";
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();
		final var factory = ofBridgeImpl(() -> endpointFetcher, () -> executor);

		new ComponentTest(sut)
				.addReference("httpBridgeFactory", factory)
				.addReference("httpBridgeCycleServiceDefinition", HttpBridgeCycleServiceDefinition.INSTANCE)
				.activate(MyConfig.create()
						.setId(COMPONENT_ID)
						.setApiUrl(url)
						.build())
				.next(new TestCase("Communication error")
						.timeleap(clock, 1, ChronoUnit.SECONDS)
						.onAfterProcessImage(() -> executor.update()))
				.deactivate();
	}
}
