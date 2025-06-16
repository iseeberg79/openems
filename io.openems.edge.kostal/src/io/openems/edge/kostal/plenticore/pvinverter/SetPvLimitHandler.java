package io.openems.edge.kostal.plenticore.pvinverter;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.kostal.plenticore.pvinverter.KostalPvInverter.ChannelId;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public class SetPvLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final KostalPvInverterImpl parent;
	private final ManagedSymmetricPvInverter.ChannelId channelId;

	private Integer lastPLimitPerc = null;
	private LocalDateTime lastPLimitPercTime = LocalDateTime.MIN;

	public SetPvLimitHandler(KostalPvInverterImpl parent, ManagedSymmetricPvInverter.ChannelId activePowerLimit) {
		this.parent = parent;
		this.channelId = activePowerLimit;
	}

	@Override
	public void run() throws OpenemsNamedException {
		IntegerReadChannel maxPowerChannel = this.parent
				.channel(ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER);
		int maxPower = maxPowerChannel.value().get();

		IntegerWriteChannel channel = this.parent.channel(this.channelId);
		var powerOpt = channel.getNextWriteValueAndReset();

		int pLimitPerc;
		int power;
		if (powerOpt.isPresent()) {
			power = powerOpt.get();
			pLimitPerc = (int) ((double) power / (double) maxPower * 100.0);

			// keep percentage in range [0, 100]
			if (pLimitPerc > 100) {
				pLimitPerc = 100;
			}
			if (pLimitPerc < 0) {
				pLimitPerc = 0;
			}
		} else {
			// Reset limit
			power = maxPower;
			pLimitPerc = 100;
		}

		if (!Objects.equals(this.lastPLimitPerc, pLimitPerc) ||
		// there is no watchdog for Kostal PV inverter? The idea is to keep the limit
		// for 10 minutes at least
				this.lastPLimitPercTime.isBefore(LocalDateTime.now().minusSeconds(600 /* timeout */))) {

			// value needs to be set
			this.parent.logInfo(this.log, "Apply new limit: " + power + " W (" + pLimitPerc + " %)");
			IntegerWriteChannel pLimitPercCh = this.parent.channel(ChannelId.PV_LIMIT_MAX_PERCENT);
			pLimitPercCh.setNextWriteValue(pLimitPerc);

			this.lastPLimitPerc = pLimitPerc;
			this.lastPLimitPercTime = LocalDateTime.now();
		}
	}

}
