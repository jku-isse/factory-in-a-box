export class Order {
  orderId: string;
  jobStatus: Map<string, string>;
  capabilities: Map<string, Array<string>>;
}
