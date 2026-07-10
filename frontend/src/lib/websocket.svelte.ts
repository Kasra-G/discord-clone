import { env } from '$env/dynamic/public';

export interface Command {
	command: string;
	payload: unknown;
}

type MessageHandler = (message: Command) => void;
type OpenHandler = () => void;

interface SubscriberMap {
	onMessage: MessageHandler[];
	onOpen: OpenHandler[];
}

export class WebsocketService {
	private ws?: WebSocket;
	private _status = $state<'DISCONNECTED' | 'RECONNECTING' | 'CONNECTING' | 'CONNECTED'>(
		'DISCONNECTED',
	);

	public get status() {
		return this._status;
	}

	private subscribers: SubscriberMap = {
		onMessage: [],
		onOpen: [],
	};

	destroy() {
		if (this.ws) {
			this.ws.onclose = () => {};
			this.ws.close();
		}
	}

	connect() {
		if (this.ws) {
			this.ws.close();
		}

		this._status = 'CONNECTING';

		this.ws = new WebSocket(`${env.PUBLIC_BACKEND_URL}/ws`);

		this.ws.addEventListener('open', () => {
			this._status = 'CONNECTED';
			this.subscribers.onOpen.forEach((handle) => handle());
		});

		this.ws.addEventListener('message', (messageEvent) => {
			this.subscribers.onMessage.forEach((handle) => handle(JSON.parse(messageEvent.data)));
		});

		window.addEventListener('beforeunload', () => {
			this.destroy();
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
}

export const websocketService = new WebsocketService();
