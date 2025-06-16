package io.openems.edge.kostal.plenticore.pvinverter;

import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface KostalPvInverter extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Represents the status for the PV limit set
		 */
		PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Limit failed")),
		
		/**
		 * Represents the current PV inverter (feed-in) limit as a percentage.
		 */
		PV_LIMIT_MAX_PERCENT(Doc.of(INTEGER) //
				.unit(PERCENT)),
		
		/**
		 * Is the PV inverter (feed-in) limit active
		 */
		PV_LIMIT_ENABLED(Doc.of(BOOLEAN));
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
