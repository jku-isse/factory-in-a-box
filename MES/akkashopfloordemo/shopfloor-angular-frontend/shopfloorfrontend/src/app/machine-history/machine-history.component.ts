import { Component, OnInit, ViewChild } from '@angular/core';
import { MachineEvent } from '../_models/events';
import { MachineService } from '../_services/machine.service';
import { Router, ActivatedRoute } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { DataService } from '../_services/data.service';

@Component({
  selector: 'app-machine-history',
  templateUrl: './machine-history.component.html',
  styleUrls: ['./machine-history.component.css']
})
export class MachineHistoryComponent implements OnInit {
  displayedColumns: string[] = ['eventType', 'state', 'message', 'time'];
  machines: Map<string, MachineEvent> = new Map<string, MachineEvent>();
  machineId: string;
  latest = '';
  dataSource: MatTableDataSource<MachineEvent>;
  count: Map<string, number>;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private machineService: MachineService,
    private data: DataService) { }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.machineId = params.get('id');
    });
    this.subscribe();
    this.reloadData();
    this.data.currentMachineCount.subscribe(count => this.count = count);
  }

  private subscribe() {
    this.machineService.getMachineUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        // console.log('sse', json);
        if (json.machineId === this.machineId) {
          json.prettyTimestamp = this.parseTimestamp(json.timestamp);
          this.machines.set(json.machineId + json.eventType + json.timestamp + json.message + json.newValue, json);
          this.setLatest(json.timestamp);
          this.dataSource = new MatTableDataSource(this.getMachinesAsArray());
          if (this.dataSource) {
            this.dataSource.paginator = this.paginator;
          }
        }
        this.newCount();
      },
      err => { console.log('Error receiving SSE in History', err); },
      () => console.log('SSE stream completed')
    );
  }

  private reloadData() {
    this.machineService.getMachineHistory(this.machineId)
      .subscribe(data => {
        data.forEach(element => {
          element.prettyTimestamp = this.parseTimestamp(element.timestamp);
          this.machines.set(element.machineId + element.eventType + element.timestamp + element.message + element.newValue, element);
          this.setLatest(element.timestamp);
          this.dataSource = new MatTableDataSource(this.getMachinesAsArray());
          // console.log('History data', element);
        });
        if (this.dataSource) {
          this.dataSource.paginator = this.paginator;
        }
        this.newCount();
      }, error => console.log(error));
  }
  list() {
    this.router.navigate(['machines']);
  }

  getMachinesAsArray() {
    // console.log('Debug flag', this.machines.values());
    return Array.from(this.machines.values());
  }

  parseTimestamp(timestamp: string): string {
    const d: Date = new Date(Date.parse(timestamp.substring(0, timestamp.indexOf('+'))));
    return d.toLocaleTimeString() + `.${d.getMilliseconds()}`;
  }

  setLatest(timestamp: string) {
    if (this.latest === '' ||
      Date.parse(this.latest.substring(0, this.latest.indexOf('+'))) <= Date.parse(timestamp.substring(0, timestamp.indexOf('+')))) {
      this.latest = timestamp;
    }
  }

  applyFilter(filterValue: string) {
    console.log(this.dataSource);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  newCount() {
    this.data.changeMachineCount(new Map<string, number>(this.count), this.machineId, this.machines.size);
  }

}
