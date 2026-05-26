import { PUBLIC_BACKEND_URL } from '$env/static/public';

const MAX_DELAY_MS = 10000;
const INITIAL_DELAY_MS = 100;

type MessageHandler = (message: any) => void;
type OpenHandler = () => void;

interface SubscriberMap {
	onMessage: MessageHandler[];
	onOpen: OpenHandler[];
}

export class WebsocketService {
	private ws?: WebSocket;
	private _status = $state<'DISCONNECTED' | 'RECONNECTING' | 'CONNECTING' | 'CONNECTED'>(
		'DISCONNECTED'
	);
	private reconnectId?: ReturnType<typeof setTimeout>;
	private reconnectDelayMs = INITIAL_DELAY_MS;

	public get status() {
		return this._status;
	}

	private subscribers: SubscriberMap = {
		onMessage: [],
		onOpen: []
	};

	connect() {
		if (this.ws) {
			this.ws.close();
		}

		this._status = 'CONNECTING';
		this.ws = new WebSocket(`${PUBLIC_BACKEND_URL}/ws`);

		this.ws.addEventListener('open', () => {
			this._status = 'CONNECTED';
			this.resetReconnectSettings();
			this.subscribers.onOpen.forEach((handle) => handle());
		});

		this.ws.addEventListener('message', (messageEvent) => {
			this.subscribers.onMessage.forEach((handle) => handle(JSON.parse(messageEvent.data)));
		});

		this.ws.addEventListener('close', () => {
			this.handleReconnect();
		});
	}

	send(payload: object) {
		this.ws?.send(JSON.stringify(payload));
	}

	onMessage(handle: MessageHandler) {
		this.subscribers.onMessage.push(handle);
	}

	onOpen(handle: OpenHandler) {
		this.subscribers.onOpen.push(handle);
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
