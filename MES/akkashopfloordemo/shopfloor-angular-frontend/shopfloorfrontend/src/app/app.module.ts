import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { OrderDetailsComponent } from './order-details/order-details.component';
import { OrderListComponent } from './order-list/order-list.component';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MaterialModule} from './material-module';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { MachineListComponent } from './machine-list/machine-list.component';
import { MachineHistoryComponent } from './machine-history/machine-history.component';

@NgModule({
  declarations: [
    AppComponent,
    OrderDetailsComponent,
    OrderListComponent,
    OrderHistoryComponent,
    MachineListComponent,
    MachineHistoryComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MaterialModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
