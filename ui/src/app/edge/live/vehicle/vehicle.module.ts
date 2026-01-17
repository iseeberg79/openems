import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";
import { VehicleComponent } from "./vehicle.component";

@NgModule({
    declarations: [
        VehicleComponent,
    ],
    imports: [
        SharedModule,
    ],
    exports: [
        VehicleComponent,
    ],
})
export class VehicleModule { }
