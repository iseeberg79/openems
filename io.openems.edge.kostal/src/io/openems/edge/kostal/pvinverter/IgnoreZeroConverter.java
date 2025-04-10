package io.openems.edge.kostal.pvinverter;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;

public class IgnoreZeroConverter extends ElementToChannelConverter {

	/**
	 * Generates an ElementToChannelConverter for the use case covered by
	 * {@link IgnoreZeroConverter}.
	 *
	 * @param parent    the parent component
	 * @param converter an additional {@link ElementToChannelConverter}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter from(KostalPvInverter parent, ElementToChannelConverter converter) {
		if (converter == ElementToChannelConverter.DIRECT_1_TO_1) {
			return new IgnoreZeroConverter(parent);
		}
		return ElementToChannelConverter.chain(new IgnoreZeroConverter(parent), converter);
	}

	private IgnoreZeroConverter(KostalPvInverter parent) {
		super(value -> {
			// Is value null?
			if (value == null) {
				return null;
			}
			if (value instanceof Integer i && i != 0) {
				return value;
			}
			if (value instanceof Long l && l != 0L) {
				return value;
			}
			return value;
		});
	}

}