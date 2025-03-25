package io.openems.edge.kostal.ess2;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.kostal.ess2.charger.KostalDcCharger;

public interface KostalManagedEss extends OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Available Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		AVAIL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), // defined in external file

		/**
		 * Actual Current to or from the battery.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		BATT_ACTUAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * actual battery voltage.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		BATT_ACTUAL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * average battery temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Battery Lifetime Export Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		BATT_LIFETIME_EXPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Battery Lifetime Import Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		BATT_LIFETIME_IMPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * maximum battery temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Charge Power Wanted is the activePower wanted from controllers and internal.
		 * Channel for applyPower()-Method
		 *
		 * <p>
		 * negative values for charging
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		CHARGE_POWER_WANTED(Doc.of(OpenemsType.INTEGER) // Charge/Discharge-Power wanted from controllers
				.unit(Unit.WATT)), // defined in external file

		/**
		 * Power from Grid. Used to calculate pv production.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		GRID_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Scaling factor for grid power.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>Range: 0..100
		 * </ul>
		 */
		GRID_POWER_SCALE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH)), // defined in external file

		/**
		 * Charge continues power Varies with storage state of charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_CHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)), // defined in external file

		/*
		 * Storage AC Charge Limit is used to set the AC charge limit according to the
		 * policy set in the previous register. Either fixed in kWh or percentage is set
		 * (e.g. 100KWh or 70%). Relevant only for Storage AC Charge Policy = 2 or 3
		 */
		MAX_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent or kWh
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Charge continues power Varies with storage state of charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_CHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)), // defined in external file

		/**
		 * Battery charge power (DC) setpoint
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),
		
		/**
		 * Charge Power READ Channel always positive.
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Discharge continues power Varies with storage state of charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_DISCHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Discharge continues power Varies with storage state of charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_DISCHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Dicharge Power READ Channel always positive.
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * AC-Power produced by the ESS. Either for grid or consumption.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_AC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale factor for AC-Power.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Scale factor
		 * <li>
		 * </ul>
		 */
		POWER_AC_SCALE(Doc.of(OpenemsType.INTEGER) //

				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * DC-Power of the inverter.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale for the DC-Power value.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Scale
		 * <li>
		 * </ul>
		 */
		POWER_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Rated Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		RATED_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //


		SET_MAX_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent or kWh
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Charge Power WRITE Channel always positive
		 * 
		 * <p>
		 * Tells the ESS the charge power. 
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Discharge Power WRITE Channel always positive
		 * 
		 * <p>
		 * Tells the ESS the charge power. 
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Storage Backup Limit. Storage Backup Reserved Setting sets the percentage of
		 * reserved battery SOE to be used for backup purposes. Relevant only for
		 * inverters with backup functionality.
		 */
		SET_STORAGE_BACKUP_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent. Only relevant for backup systems
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * State Of Health.
		 *
		 * <p>
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * <li>
		 * </ul>
		 */
		SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.LOW)),

		/*
		 * Storage Backup Reserved Setting sets the percentage of reserved battery SOE
		 * to be used for backup purposes. Relevant only for inverters with backup
		 * functionality.
		 */
		STORAGE_BACKUP_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent. Only relevant for backup systems
				.unit(Unit.PERCENT) //
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargePowerWanted(Integer value) {
		this.getChargePowerWantedChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargePower(Integer value) {
		this.getSetChargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException throws named exception
	 */
	public default void _setMaxChargePower(Integer value) throws OpenemsNamedException {
		this.getSetMaxChargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value the next value for max. Discharge Power
	 * @throws OpenemsNamedException throws named exception
	 */
	public default void _setMaxDischargePower(Integer value) throws OpenemsNamedException {
		this.getSetMaxDischargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Not sure if necessary.
	 * @param charger Adds charger component
	 */
	public void addCharger(KostalDcCharger charger);

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPower() {
		return this.getAcPowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPowerChannel() {
		return this.channel(ChannelId.POWER_AC);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPowerScale() {
		return this.getAcPowerScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_AC_SCALE);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerWanted() {
		return this.getChargePowerWantedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargePowerWantedChannel() {
		return this.channel(ChannelId.CHARGE_POWER_WANTED);
	}

	// 	######################			

	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPower() {
		return this.getDcPowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerChannel() {
		return this.channel(ChannelId.POWER_DC);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPowerScale() {
		return this.getDcPowerScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_DC_SCALE);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPower() {
		return this.getGridPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerChannel() {
		return this.channel(ChannelId.GRID_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#GRID_POWER_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerScale() {
		return this.getGridPowerScaleChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_SCALE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerScaleChannel() {
		return this.channel(ChannelId.GRID_POWER_SCALE);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargeContinuesPower() {
		return this.getMaxChargeContinuesPowerChannel().value();
	}

	// ###########################
	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargeContinuesPowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_CONTINUES_POWER);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargePeakPower() {
		return this.getMaxChargePeakPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargePeakPowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_PEAK_POWER);
	}

	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargePower() {
		return this.getMaxChargePowerChannel().value();
	}

	// #############
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargePowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_POWER);
	}

	// ###########################

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxDischargeContinuesPower() {
		return this.getMaxDishargeContinuesPowerChannel().value();
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxDischargePeakPower() {
		return this.getMaxDischargePeakPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxDischargePeakPowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_PEAK_POWER);
	}

	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxDischargePower() {
		return this.getMaxDischargePowerChannel().value();
	}

	// 	#############		
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxDischargePowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_POWER);
	}

	// ###########################
	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxDishargeContinuesPowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_CONTINUES_POWER);
	}

	/**
	 * Sets the Modbus-ID from Config.
	 * @return Modbus-ID as String
	 */
	public String getModbusBridgeId();


	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxChargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxDischargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_DISCHARGE_POWER);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerChannel() {
		return this.channel(ChannelId.CHARGE_POWER);
	}
	
	/**
	 * Gets the Unit-ID for ESS.
	 * 
	 * @return the Unit-ID for the ESS
	 */
	public Integer getUnitId();

	/**
	 * Removes Charger.
	 * @param charger Removes given charger instance
	 */
	public void removeCharger(KostalDcCharger charger);

}