import { PUBLIC_BACKEND_URL } from '$env/static/public';

class MessageService {
	private ws?: WebSocket;
	public readonly messages = $state<string[]>([]);
	private _status = $state<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED'>('DISCONNECTED');
	private reconnectAttempts = 0;
	private reconnectId?: ReturnType<typeof setTimeout>;
	private reconnectDelayMs = 100;

	public get status() {
		return this._status;
	}

	constructor() {}

	connect() {
		if (this.ws) {
			this.ws.close();
		}

		this._status = 'CONNECTING';
		this.ws = new WebSocket(`${PUBLIC_BACKEND_URL}/ws`);

		this.ws.addEventListener('open', () => {
			this._status = 'CONNECTED';
			this.resetReconnectSettings();
		});

		this.ws.addEventListener('message', (messageEvent) => {
			this.messages.push(messageEvent.data);
		});

		this.ws.addEventListener('close', () => {
			this._status = 'DISCONNECTED';
			this.handleReconnect();
		});
	}

	send(message: string) {
		this.ws?.send(message);
	}

	private resetReconnectSettings() {
		if (this.reconnectId) clearTimeout(this.reconnectId);
		this.reconnectAttempts = 0;
		this.reconnectDelayMs = 100;
	}

	private handleReconnect() {
		this.reconnectAttempts++;
		this.reconnectDelayMs *= 2;
		this.reconnectId = setTimeout(() => {
			this.connect();
		}, this.reconnectDelayMs);
	}
}

export const messageService = new MessageService();
