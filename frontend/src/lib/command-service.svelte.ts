import { websocketService } from './websocket.svelte';

export type MessageHandler = (message: any) => void;

class CommandService {
	private readonly subscribers: MessageHandler[] = [];
	constructor() {
		websocketService.onMessage((msg) => {
			if (msg.command === 'NEW_MESSAGE') this.subscribers.forEach((handle) => handle(msg.payload));
		});
	}

	onMessage(handler: MessageHandler) {
		this.subscribers.push(handler);
	}
}

export const commandService = new CommandService();
