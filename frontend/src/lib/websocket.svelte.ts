import { PUBLIC_BACKEND_URL } from '$env/static/public';

const MAX_DELAY_MS = 10000;
const INITIAL_DELAY_MS = 100;

export class WebsocketService {
	private ws?: WebSocket;
	private _messages = $state<any[]>([]);
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

	private subscribers: ((message: string) => void)[] = [];

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
			this.subscribers.forEach((handle) => handle(JSON.parse(messageEvent.data)));
		});

		this.ws.addEventListener('close', () => {
			this.handleReconnect();
		});
	}

	send(payload: object) {
		this.ws?.send(JSON.stringify(payload));
	}

	onMessage(handle: (message: any) => void) {
		this.subscribers.push(handle);
	}

	sendPrivateMessage(message: string, recipient: string) {
		this.ws?.send(
			JSON.stringify({
				message: message,
				recipient: recipient,
				command: 'PRIVATE_MESSAGE'
			})
		);
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

export const websocketService = new WebsocketService();
