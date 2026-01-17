package io.openems.edge.evcc.vehicle;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents an Electric Vehicle.
 *
 * <p>
 * Provides basic vehicle information such as State of Charge (SoC),
 * name, range, and odometer reading.
 */
public interface Vehicle extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Vehicle Name/Title.
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: String
		 * </ul>
		 */
		VEHICLE_NAME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * State of Charge (SoC).
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Remaining Range.
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: Integer
		 * <li>Unit: Kilometers
		 * </ul>
		 */
		RANGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOMETRE) //
				.persistencePriority(PersistencePriority.MEDIUM)),

		/**
		 * Odometer Reading.
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: Integer
		 * <li>Unit: Kilometers
		 * </ul>
		 */
		ODOMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOMETRE) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Vehicle is connected to charger.
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: Boolean
		 * </ul>
		 */
		CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Vehicle is currently charging.
		 *
		 * <ul>
		 * <li>Interface: Vehicle
		 * <li>Type: Boolean
		 * </ul>
		 */
		CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH));

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
	 * Gets the Channel for {@link ChannelId#VEHICLE_NAME}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getVehicleNameChannel() {
		return this.channel(ChannelId.VEHICLE_NAME);
	}

	/**
	 * Gets the Vehicle Name. See {@link ChannelId#VEHICLE_NAME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getVehicleName() {
		return this.getVehicleNameChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSocChannel() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Gets the State of Charge. See {@link ChannelId#SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSoc() {
		return this.getSocChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RANGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRangeChannel() {
		return this.channel(ChannelId.RANGE);
	}

	/**
	 * Gets the Remaining Range. See {@link ChannelId#RANGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRange() {
		return this.getRangeChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ODOMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getOdometerChannel() {
		return this.channel(ChannelId.ODOMETER);
	}

	/**
	 * Gets the Odometer Reading. See {@link ChannelId#ODOMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOdometer() {
		return this.getOdometerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VEHICLE_NAME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVehicleName(String value) {
		this.getVehicleNameChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSoc(Integer value) {
		this.getSocChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RANGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRange(Integer value) {
		this.getRangeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ODOMETER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOdometer(Integer value) {
		this.getOdometerChannel().setNextValue(value);
	}
}
