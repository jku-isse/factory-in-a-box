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
  displayedColumns: string[] = ['eventType', 'message', 'time'];
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
    this.machineService.getMachineUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        if (json.machineId === this.machineId) {
          this.machines.set(json.machineId + json.eventType + json.timestamp + json.message, json);
          this.setLatest(json.timestamp);
        }
      },
      err => { console.log('Error receiving SSE in History', err); },
      () => console.log('SSE stream completed')
    );
    this.machineService.getMachineHistory(this.machineId)
      .subscribe(data => {
        data.forEach(element => {
          this.machines.set(element.machineId + element.eventType + element.timestamp + element.message, element);
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

  setLatest(timestamp: string) {
    if (this.latest === '' ||
        Date.parse(this.latest.substring(0, this.latest.indexOf('+'))) <= Date.parse(timestamp.substring(0, timestamp.indexOf('+')))) {
      this.latest = timestamp;
    }
  }
}
