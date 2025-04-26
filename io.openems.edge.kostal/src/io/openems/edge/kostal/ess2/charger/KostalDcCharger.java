package io.openems.edge.kostal.ess2.charger;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;

public interface KostalDcCharger
  extends
    EssDcCharger,
    OpenemsComponent, 
    ModbusSlave {
  public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
}
