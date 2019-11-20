export interface OrderEvent {
    orderId: string;
    eventType: string;
    machineId: string;
    timestamp: string;
  }

export interface MachineEvent {
  message: string;
  eventType: string;
  machineId: string;
  timestamp: string;
}
