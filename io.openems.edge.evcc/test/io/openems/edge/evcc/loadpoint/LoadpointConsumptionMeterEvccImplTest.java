package io.openems.edge.evcc.loadpoint;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.edge.common.currency.Currency.EUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class LoadpointConsumptionMeterEvccImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new LoadpointConsumptionMeterEvccImpl(); 
		final var httpTestBundle = new DummyBridgeHttpBundle();
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("meter1") //
						.setApiUrl("172.0.0.1") //
						.setLoadpointIndex(0) //
						.setMeterType(MeterType.CONSUMPTION_METERED)
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
								{
								    "loadpoints": [
								        {
								            "batteryBoost": false,
								            "chargeCurrents": [
								                0,
								                0,
								                0
								            ],
								            "chargeDuration": 7950,
								            "chargePower": 3847,
								            "chargeRemainingDuration": 0,
								            "chargeRemainingEnergy": 0,
								            "chargeTotalImport": 7867.728,
								            "chargeVoltages": [
								                223.9627686,
								                0,
								                0
								            ],
								            "chargedEnergy": 8828.125,
								            "chargerFeatureHeating": false,
								            "chargerFeatureIntegratedDevice": false,
								            "chargerIcon": null,
								            "chargerPhases1p3p": true,
								            "chargerSinglePhase": false,
								            "chargerStatusReason": "unknown",
								            "charging": false,
								            "connected": false,
								            "connectedDuration": 29854,
								            "disableDelay": 180,
								            "disableThreshold": 0,
								            "effectiveLimitSoc": 80,
								            "effectiveMaxCurrent": 16,
								            "effectiveMinCurrent": 8,
								            "effectivePlanId": 0,
								            "effectivePlanSoc": 0,
								            "effectivePlanTime": null,
								            "effectivePriority": 0,
								            "enableDelay": 60,
								            "enableThreshold": 0,
								            "enabled": false,
								            "limitEnergy": 0,
								            "limitSoc": 0,
								            "maxCurrent": 16,
								            "minCurrent": 6,
								            "mode": "pv",
								            "offeredCurrent": 0,
								            "phaseAction": "inactive",
								            "phaseRemaining": 0,
								            "phasesActive": 1,
								            "phasesConfigured": 0,
								            "planActive": false,
								            "planEnergy": 0,
								            "planOverrun": 0,
								            "planPrecondition": 0,
								            "planProjectedEnd": null,
								            "planProjectedStart": null,
								            "planTime": null,
								            "priority": 0,
								            "pvAction": "inactive",
								            "pvRemaining": 0,
								            "sessionCo2PerKWh": 2.01063289028666,
								            "sessionEnergy": 8828.125,
								            "sessionPrice": 0.836209774557078,
								            "sessionPricePerKWh": 0.094721107206465469,
								            "sessionSolarPercentage": 98.684,
								            "smartCostActive": false,
								            "smartCostLimit": null,
								            "smartCostNextStart": null,
								            "smartFeedInPriorityActive": false,
								            "smartFeedInPriorityLimit": null,
								            "smartFeedInPriorityNextStart": null,
								            "title": "Stellplatz",
								            "vehicleClimaterActive": null,
								            "vehicleDetectionActive": false,
								            "vehicleLimitSoc": 0,
								            "vehicleName": "db:11",
								            "vehicleOdometer": 119212,
								            "vehicleRange": 0,
								            "vehicleSoc": 0,
								            "vehicleTitle": "Zoe",
								            "vehicleWelcomeActive": false
								        }
								    ]
								}


																		"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("|3847 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 3847) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1344) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1368) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1326) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 224588) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 228238) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 221088) //
						// .output(ElectricityMeter.ChannelId.CURRENT, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 6049) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 6093) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 6006) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null)) //

				.next(new TestCase("Invalid read response").onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					httpTestBundle.triggerNextCycle();
				}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null)) //

				.deactivate();
	}

}