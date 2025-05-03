package io.openems.edge.kostal.ess2.charger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KostalDcChargerImplTest {

  @Test
  public void test() throws Exception {
    new ComponentTest(new KostalDcChargerImpl()) //
      .addReference("cm", new DummyConfigurationAdmin()) //
      .addReference("setModbus", new DummyModbusBridge("modbus0")) //
      .activate(
        MyConfig.create() //
          .setId("charger0") //
          .setReadOnly(true) //
          .setModbusId("modbus0") //
          .setModbusUnitId(71) //
          .setEssId("ess1")
          .build()
      ); //
  }
}
