import { Component, OnInit } from '@angular/core';
import { MachineEvent } from '../events';
import { MachineService } from '../machine.service';
import { Router, ActivatedRoute } from '@angular/router';

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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private machineService: MachineService) { }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.machineId = params.get('id');
    });
    this.subscribe();
    this.reloadData();
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
        }
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
          // console.log('History data', element);
        });
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
}
