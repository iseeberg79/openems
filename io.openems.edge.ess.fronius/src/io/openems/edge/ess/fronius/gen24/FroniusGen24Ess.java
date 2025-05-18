package io.openems.edge.ess.fronius.gen24;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface FroniusGen24Ess extends ManagedSymmetricEss, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG)), //
		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		DCW_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),

		/**
		 * Requested Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REQUESTED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)) //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Retrieves the scale factor for power measurements.
	 *
	 * @return the read channel for SCALE_FACTOR_POWER.
	 */
	public default IntegerReadChannel getScaleFactorPowerChannel() {
		return this.channel(ChannelId.DCW_SF);
	}

	/**
	 * Gets the power scale factor value.
	 *
	 * @return the integer value of the scale factor.
	 */
	public default Integer getScaleFactorPowerValue() {
		return this.getScaleFactorPowerChannel().value().get();
	}

	/**
	 * Sets the scale factor for power measurements.
	 *
	 * @param value the new scale factor value to set.
	 */
	public default void _setScaleFactorPower(Integer value) {
		this.getScaleFactorPowerChannel().setNextValue(value);
	}
	
}