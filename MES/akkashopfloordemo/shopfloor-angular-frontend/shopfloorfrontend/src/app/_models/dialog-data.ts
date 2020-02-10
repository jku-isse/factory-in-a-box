export interface DialogData {
    action: string;
    id: string;
}
export class ActionRequest implements DialogData {
    action: string;
    id: string;

    constructor(action: string, id: string) {
        this.action = action;
        this.id = id;
    }
}
