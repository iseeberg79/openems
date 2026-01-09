@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Consumption.Loadpoint.Evcc",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = { "type=CONSUMPTION_METERED" }
)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class LoadpointConsumptionMeterEvccImpl extends AbstractLoadpointMeterEvcc
        implements LoadpointConsumptionMeterEvcc, SocEvcs, Evcs, ElectricityMeter, OpenemsComponent, TimedataProvider {

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(LoadpointConsumptionMeterEvccImpl.class);
    }

    @Override
    public void init() {
        super.init();
        // Hier ist der neue Code, um den Fehler zu beheben
        this.getChannel().setNextValue(0.0); // Setze die nächste Werte auf 0.0
    }
}
