import { PUBLIC_BACKEND_URL } from '$env/static/public';

const MAX_DELAY_MS = 10000;
const INITIAL_DELAY_MS = 100;
class MessageService {
	private ws?: WebSocket;
	private _messages = $state<string[]>([]);
	private _status = $state<'DISCONNECTED' | 'RECONNECTING' | 'CONNECTING' | 'CONNECTED'>(
		'DISCONNECTED'
	);
	private reconnectId?: ReturnType<typeof setTimeout>;
	private reconnectDelayMs = INITIAL_DELAY_MS;

	public get status() {
		return this._status;
	}

	public get messages() {
		return this._messages;
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
			this._messages = [];
			this.resetReconnectSettings();
		});

		this.ws.addEventListener('message', (messageEvent) => {
			this._messages.push(messageEvent.data);
		});

		this.ws.addEventListener('close', () => {
			this.handleReconnect();
		});
	}

	send(message: string) {
		this.ws?.send(message);
	}

	private resetReconnectSettings() {
		if (this.reconnectId) clearTimeout(this.reconnectId);
		this.reconnectDelayMs = INITIAL_DELAY_MS;
	}

	private handleReconnect() {
		this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, MAX_DELAY_MS) * Math.random();
		this.reconnectId = setTimeout(() => {
			this.connect();
			this._status = 'RECONNECTING';
		}, this.reconnectDelayMs);
	}
}

export const messageService = new MessageService();
