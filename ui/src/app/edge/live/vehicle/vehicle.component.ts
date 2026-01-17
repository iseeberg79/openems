import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "../../../shared/shared";

@Component({
    selector: VehicleComponent.SELECTOR,
    templateUrl: "./vehicle.component.html",
    styleUrls: ["./vehicle.component.scss"],
    standalone: false,
})
export class VehicleComponent implements OnInit, OnDestroy {

    private static readonly SELECTOR = "vehicle";
    protected edge: Edge | null = null;
    protected component: EdgeConfig.Component | null = null;
    protected componentId: string = "vehicle0";

    constructor(
        private service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;

            // Get component ID from route or input
            this.route.params.subscribe(params => {
                this.componentId = params["componentId"] || "vehicle0";
            });
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, VehicleComponent.SELECTOR);
        }
    }

    /**
     * Gets vehicle name from current value
     */
    protected getVehicleName(): string | null {
        return this.edge?.currentData.value.channel[this.componentId + "/VehicleName"] ?? null;
    }

    /**
     * Gets SoC from current value
     */
    protected getSoc(): number | null {
        return this.edge?.currentData.value.channel[this.componentId + "/Soc"] ?? null;
    }

    /**
     * Gets range from current value
     */
    protected getRange(): number | null {
        return this.edge?.currentData.value.channel[this.componentId + "/Range"] ?? null;
    }

    /**
     * Gets odometer from current value
     */
    protected getOdometer(): number | null {
        return this.edge?.currentData.value.channel[this.componentId + "/Odometer"] ?? null;
    }

    /**
     * Gets connected status
     */
    protected getConnected(): boolean | null {
        return this.edge?.currentData.value.channel[this.componentId + "/Connected"] ?? null;
    }

    /**
     * Gets charging status
     */
    protected getCharging(): boolean | null {
        return this.edge?.currentData.value.channel[this.componentId + "/Charging"] ?? null;
    }

    /**
     * Gets the battery icon based on SoC level
     */
    protected getBatteryIcon(): string {
        const soc = this.getSoc();
        if (soc === null) {
            return "battery-dead";
        } else if (soc >= 80) {
            return "battery-full";
        } else if (soc >= 50) {
            return "battery-half";
        } else if (soc >= 20) {
            return "battery-charging";
        } else {
            return "battery-dead";
        }
    }

    /**
     * Gets the battery color based on SoC level
     */
    protected getBatteryColor(): string {
        const soc = this.getSoc();
        if (soc === null) {
            return "medium";
        } else if (soc >= 80) {
            return "success";
        } else if (soc >= 50) {
            return "primary";
        } else if (soc >= 20) {
            return "warning";
        } else {
            return "danger";
        }
    }
}
